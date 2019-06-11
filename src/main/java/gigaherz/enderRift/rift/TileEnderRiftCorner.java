package gigaherz.enderRift.rift;

import gigaherz.enderRift.EnderRiftMod;
import gigaherz.enderRift.automation.TileAggregator;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nullable;
import java.util.Optional;

public class TileEnderRiftCorner extends TileAggregator
{
    @ObjectHolder("enderrift:rift_corner")
    public static TileEntityType<?> TYPE;

    TileEnderRift energyParent;

    LazyOptional<IEnergyStorage> bufferProvider = LazyOptional.of(() -> getEnergyBuffer().orElse(null));

    public TileEnderRiftCorner()
    {
        super(TYPE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing)
    {
        if (capability == CapabilityEnergy.ENERGY)
            return bufferProvider.cast();
        return super.getCapability(capability, facing);
    }

    @Override
    public Optional<IEnergyStorage> getEnergyBuffer()
    {
        return getInternalBuffer();
    }

    @Override
    public Optional<IEnergyStorage> getInternalBuffer()
    {
        return getParent().flatMap(TileEnderRift::getEnergyBuffer);
    }

    @Override
    protected void lazyDirty()
    {
        // Nothing to do
    }

    @Override
    protected boolean canConnectSide(Direction side)
    {
        return false;
    }

    public Optional<TileEnderRift> getParent()
    {
        if (energyParent == null)
        {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() != EnderRiftMod.structure)
                return Optional.empty();

            TileEntity te = world.getTileEntity(getRiftFromCorner(state, pos));
            if (te instanceof TileEnderRift)
            {
                energyParent = (TileEnderRift) te;
            }
            else
            {
                return Optional.empty();
            }
        }
        return Optional.of(energyParent);
    }

    private static BlockPos getRiftFromCorner(BlockState state, BlockPos pos)
    {
        boolean base = state.get(BlockStructure.BASE);
        BlockStructure.Corner corner = state.get(BlockStructure.CORNER);
        int xParent = pos.getX();
        int yParent = pos.getY() + (base ? 1 : -1);
        int zParent = pos.getZ();
        switch (corner)
        {
            case NE:
                xParent -= 1;
                zParent += 1;
                break;
            case NW:
                xParent += 1;
                zParent += 1;
                break;
            case SE:
                xParent -= 1;
                zParent -= 1;
                break;
            case SW:
                xParent += 1;
                zParent -= 1;
                break;
        }
        return new BlockPos(xParent, yParent, zParent);
    }
}