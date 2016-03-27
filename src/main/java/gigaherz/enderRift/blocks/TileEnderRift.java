package gigaherz.enderRift.blocks;

import com.google.common.base.Predicate;
import gigaherz.capabilities.api.energy.EnergyBuffer;
import gigaherz.capabilities.api.energy.IEnergyHandler;
import gigaherz.enderRift.ConfigValues;
import gigaherz.enderRift.EnderRiftMod;
import gigaherz.enderRift.automation.IAutomationProvider;
import gigaherz.enderRift.automation.IInventoryAutomation;
import gigaherz.enderRift.storage.RiftInventory;
import gigaherz.enderRift.storage.RiftStorageWorldData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

public class TileEnderRift
        extends TileEntity
        implements IAutomationProvider
{
    public final Random rand = new Random();

    private EnergyBuffer energyBuffer = new EnergyBuffer(10000000);

    private int riftId;
    private RiftInventory inventory;

    private AutomationEnergyWrapper automation = new AutomationEnergyWrapper();

    public void assemble(int id)
    {
        inventory = null;
        riftId = id;
        markDirty();
    }

    public void unassemble()
    {
        inventory = null;
        riftId = -1;
        markDirty();
    }

    public RiftInventory getInventory()
    {
        if (riftId < 0)
            return null;

        if (inventory == null)
        {
            inventory = RiftStorageWorldData.get(worldObj).getRift(riftId);
            inventory.addWeakListener(this);
        }
        return inventory;
    }

    public IEnergyHandler getEnergyBuffer() { return energyBuffer; }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate)
    {
        return oldState.getBlock() != newSate.getBlock();
    }

    @Override
    public boolean shouldRenderInPass(int pass)
    {
        return pass == 1;
    }

    public double getEnergyInsert()
    {
        if (getInventory() == null)
            return 0;
        int sizeInventory = getInventory().getSizeInventory();
        int sizeInventory2 = sizeInventory * sizeInventory;
        return ConfigValues.PowerPerInsertionConstant
                + (sizeInventory * ConfigValues.PowerPerInsertionLinear)
                + (sizeInventory2 * ConfigValues.PowerPerInsertionGeometric);
    }

    public double getEnergyExtract()
    {
        if (getInventory() == null)
            return 0;
        int sizeInventory = getInventory().getSizeInventory();
        int sizeInventory2 = sizeInventory * sizeInventory;
        return ConfigValues.PowerPerExtractionConstant
                + (sizeInventory * ConfigValues.PowerPerExtractionLinear)
                + (sizeInventory2 * ConfigValues.PowerPerExtractionGeometric);
    }

    public int countInventoryStacks()
    {
        return getInventory().getSizeInventory();
    }

    private int getEffectivePowerUsageToInsert(int stackSize)
    {
        return worldObj.isRemote ? 0 : (int) Math.ceil(getEnergyInsert() * stackSize);
    }

    private int getEffectivePowerUsageToExtract(int limit)
    {
        return worldObj.isRemote ? 0 : (int) Math.ceil(getEnergyExtract() * limit);
    }

    public ItemStack getRiftItem()
    {
        ItemStack stack = new ItemStack(EnderRiftMod.riftOrb);

        NBTTagCompound tag = new NBTTagCompound();

        tag.setInteger("RiftId", riftId);

        stack.setTagCompound(tag);

        return stack;
    }

    public void readFromNBT(NBTTagCompound nbtTagCompound)
    {
        super.readFromNBT(nbtTagCompound);
        energyBuffer.setEnergy(nbtTagCompound.getInteger("Energy"));
        riftId = nbtTagCompound.getInteger("RiftId");
    }

    public void writeToNBT(NBTTagCompound nbtTagCompound)
    {
        super.writeToNBT(nbtTagCompound);
        nbtTagCompound.setInteger("Energy", energyBuffer.getEnergy());
        nbtTagCompound.setInteger("RiftId", riftId);
    }

    @Override
    public IInventoryAutomation getAutomation(EnumFacing face)
    {
        return automation;
    }

    class AutomationEnergyWrapper implements IInventoryAutomation
    {
        @Override
        public ItemStack pushItems(@Nonnull ItemStack stack)
        {
            if (getInventory() == null)
                return stack;

            int stackSize = stack.stackSize;
            int cost = getEffectivePowerUsageToInsert(stackSize);
            while (cost > energyBuffer.getEnergy() && stackSize > 0)
            {
                stackSize--;
            }

            if (stackSize <= 0)
                return stack;

            ItemStack temp = stack;
            if (stackSize != stack.stackSize)
            {
                temp = stack.copy();
                temp.stackSize = stackSize;
            }

            ItemStack remaining = getInventory().pushItems(temp);
            if (remaining != null)
                stackSize -= remaining.stackSize;

            int actualCost = getEffectivePowerUsageToInsert(stackSize);
            energyBuffer.extractEnergy(actualCost, false);

            return remaining;
        }

        @Override
        public ItemStack pullItems(int limit, Predicate<ItemStack> filter)
        {
            if (getInventory() == null)
                return null;

            int cost = getEffectivePowerUsageToExtract(limit);
            while (cost > energyBuffer.getEnergy() && limit > 0)
            {
                limit--;
            }

            if (limit <= 0)
                return null;

            ItemStack extracted = getInventory().pullItems(limit, filter);
            if (extracted == null)
                return null;

            int actualCost = getEffectivePowerUsageToExtract(extracted.stackSize);
            energyBuffer.extractEnergy(actualCost, false);

            return extracted;
        }

        @Override
        public ItemStack extractItems(@Nonnull ItemStack stack, int wanted, boolean simulate)
        {
            if (getInventory() == null)
                return null;

            int cost = getEffectivePowerUsageToExtract(wanted);
            while (cost > energyBuffer.getEnergy() && wanted > 0)
            {
                wanted--;
            }

            if (wanted <= 0)
                return null;

            ItemStack extracted = getInventory().extractItems(stack, wanted, simulate);
            if (extracted == null)
                return null;

            if (!simulate)
            {
                int actualCost = getEffectivePowerUsageToExtract(extracted.stackSize);
                energyBuffer.extractEnergy(actualCost, false);
            }

            return extracted;
        }

        @Override
        public int getSizeInventory()
        {
            if (getInventory() == null)
                return 0;

            return getInventory().getSizeInventory();
        }

        @Override
        public ItemStack getStackInSlot(int index)
        {
            if (getInventory() == null)
                return null;

            return getInventory().getStackInSlot(index);
        }
    }

    public ItemStack chooseRandomStack()
    {
        if (getInventory() == null)
            return null;

        int max = getInventory().getSizeInventory();

        if (max <= 0)
            return null;

        int slot = rand.nextInt(max);

        return automation.getStackInSlot(slot);
    }

    public int getRiftId()
    {
        return riftId;
    }
}