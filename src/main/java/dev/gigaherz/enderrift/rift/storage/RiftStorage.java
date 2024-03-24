package dev.gigaherz.enderrift.rift.storage;

import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;
import com.mojang.logging.LogUtils;
import dev.gigaherz.enderrift.EnderRiftMod;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = EnderRiftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RiftStorage
{
    private RiftStorage()
    {
        throw new RuntimeException("Class cannot be instantiated.");
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LevelResource DATA_DIR = new LevelResource("enderrift");

    private static final String EXTENSION = ".dat";
    private static final String TMP_EXTENSION = ".dat.tmp";

    private static final HashMap<UUID, RiftHolder> rifts = new HashMap<>();

    private static final ReentrantReadWriteUpdateLock lock = new ReentrantReadWriteUpdateLock();
    private static final Lock readLock = lock.readLock();
    private static final Lock updateLock = lock.updateLock();
    private static final Lock writeLock = lock.writeLock();

    private static Path dataDirectory;

    @SubscribeEvent
    public static void serverStart(ServerAboutToStartEvent event)
    {
        dataDirectory = event.getServer().getWorldPath(DATA_DIR);
        try
        {
            if (!Files.exists(dataDirectory))
            {
                Files.createDirectories(dataDirectory);
            }
        }
        catch(IOException ex)
        {
            LOGGER.error("Could not create rifts directory '" + dataDirectory  + "'", ex);
        }
    }

    @SubscribeEvent
    public static void serverSave(LevelEvent.Save event)
    {
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor instanceof ServerLevel sl && sl.dimension().equals(Level.OVERWORLD))
        {
            saveDirty();
        }
    }

    @SubscribeEvent
    public static void serverStop(ServerStoppingEvent event)
    {
        writeLock.lock();
        try
        {
            saveDirty();
            rifts.clear();
            dataDirectory = null;
        }
        finally
        {
            writeLock.unlock();
        }
    }

    private static RiftHolder createRift(UUID id)
    {
        writeLock.lock();
        try
        {
            return rifts.computeIfAbsent(id, RiftHolder::new);
        }
        finally
        {
            writeLock.unlock();
        }
    }

    public static RiftHolder newRift()
    {
        updateLock.lock();
        try
        {
            UUID id;
            do
            {
                id = UUID.randomUUID();
            } while (rifts.containsKey(id));

            return createRift(id);
        }
        finally
        {
            updateLock.unlock();
        }
    }

    public static RiftHolder getOrCreateRift(@Nullable UUID id)
    {
        if (id == null)
            return newRift();

        updateLock.lock();
        try
        {
            return Objects.requireNonNullElseGet(rifts.get(id), () -> createRift(id));
        }
        finally
        {
            updateLock.unlock();
        }
    }

    public static Optional<RiftHolder> findRift(UUID id)
    {
        readLock.lock();
        try
        {
            return Optional.ofNullable(rifts.get(id));
        }
        finally
        {
            readLock.unlock();
        }
    }

    static RiftInventory load(RiftHolder holder)
    {
        var inv = new RiftInventory(holder);
        UUID id = holder.getId();
        Path file = dataDirectory.resolve(id + EXTENSION);
        boolean loadedFromTemp = false;
        if (!Files.exists(file))
        {
            file = dataDirectory.resolve(id + TMP_EXTENSION);
            if (!Files.exists(file))
                return inv;
            loadedFromTemp = true;
        }

        LOGGER.info("Loading rift {}...", id);
        try
        {
            var tag = NbtIo.readCompressed(file, NbtAccounter.create(0x6400000L));
            inv.load(tag);
            if (loadedFromTemp)
                inv.markDirty();
        }
        catch (IOException e)
        {
            LOGGER.error("Could not load rift {}", id, e);
        }

        return inv;
    }

    public static void saveDirty()
    {
        readLock.lock();
        try
        {
            for (RiftHolder holder : rifts.values())
            {
                if (holder.isDirty())
                {
                    save(holder);
                    holder.clearDirty();
                }
            }
        }
        finally
        {
            readLock.unlock();
        }
    }

    private static void save(RiftHolder holder)
    {
        String id = holder.getId().toString();
        RiftInventory inventory = holder.getOrLoad();
        LOGGER.info("Saving rift {}...", id);
        try
        {
            var tmpFile = dataDirectory.resolve(id + TMP_EXTENSION);
            var file = dataDirectory.resolve(id + EXTENSION);

            NbtIo.writeCompressed(inventory.save(), tmpFile);

            try
            {
                // ATOMIC_MOVE may return an IOException in two cases:
                // 1. If a file exists and the operating system doesn't support replacing an existing file in the atomic move operation, or
                // 2. If atomic operations are not supported at all.
                Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException ex)
            {
                Files.move(tmpFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException ex)
        {
            LOGGER.error("Could not save rift {}", id, ex);
        }
    }

    public static void walkRifts(BiConsumer<UUID, RiftInventory> riftConsumer)
    {
        readLock.lock();
        try
        {
            rifts.forEach((id, holder) -> riftConsumer.accept(id, holder.getOrLoad()));
        }
        finally
        {
            readLock.unlock();
        }
    }
}