package gigaherz.enderRift.automation.proxy;

import gigaherz.enderRift.automation.BlockAggregator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;

import javax.annotation.Nullable;

public class BlockProxy extends BlockAggregator<TileProxy>
{
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final AxisAlignedBB BOUNDS = new AxisAlignedBB(4 / 16f, 4 / 16f, 4 / 16f, 12 / 16f, 12 / 16f, 12 / 16f);
    private static final AxisAlignedBB BOUNDS_NORTH = new AxisAlignedBB(6 / 16f, 6 / 16f, 0 / 16f, 10 / 16f, 10 / 16f, 10 / 16f);
    private static final AxisAlignedBB BOUNDS_SOUTH = new AxisAlignedBB(6 / 16f, 6 / 16f, 6 / 16f, 10 / 16f, 10 / 16f, 16 / 16f);
    private static final AxisAlignedBB BOUNDS_EAST = new AxisAlignedBB(6 / 16f, 6 / 16f, 6 / 16f, 16 / 16f, 10 / 16f, 10 / 16f);
    private static final AxisAlignedBB BOUNDS_WEST = new AxisAlignedBB(0 / 16f, 6 / 16f, 6 / 16f, 10 / 16f, 10 / 16f, 10 / 16f);
    private static final AxisAlignedBB BOUNDS_UP = new AxisAlignedBB(6 / 16f, 6 / 16f, 6 / 16f, 10 / 16f, 16 / 16f, 10 / 16f);
    private static final AxisAlignedBB BOUNDS_DOWN = new AxisAlignedBB(6 / 16f, 0 / 16f, 6 / 16f, 10 / 16f, 10 / 16f, 10 / 16f);

    public BlockProxy(Properties properties)
    {
        super(properties);
        setDefaultState(getStateContainer().getBaseState()
                .with(NORTH, false)
                .with(SOUTH, false)
                .with(WEST, false)
                .with(EAST, false)
                .with(UP, false)
                .with(DOWN, false));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    {
        builder.add(NORTH, SOUTH, WEST, EAST, UP, DOWN);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    {
        return new TileProxy();
    }

    /*@Deprecated
    @Override
    public AxisAlignedBB getBoundingBox(BlockState state, IBlockReader source, BlockPos pos)
    {
        state = state.getActualState(source, pos);

        AxisAlignedBB bb = BOUNDS;
        if (state.getValue(NORTH)) bb = bb.union(BOUNDS_NORTH);
        if (state.getValue(SOUTH)) bb = bb.union(BOUNDS_SOUTH);
        if (state.getValue(EAST)) bb = bb.union(BOUNDS_EAST);
        if (state.getValue(WEST)) bb = bb.union(BOUNDS_WEST);
        if (state.getValue(UP)) bb = bb.union(BOUNDS_UP);
        if (state.getValue(DOWN)) bb = bb.union(BOUNDS_DOWN);
        return bb;
    }*/

    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos)
    {
        return stateIn
                .with(NORTH, isConnectableAutomation(worldIn, currentPos, Direction.NORTH))
                .with(SOUTH, isConnectableAutomation(worldIn, currentPos, Direction.SOUTH))
                .with(WEST, isConnectableAutomation(worldIn, currentPos, Direction.WEST))
                .with(EAST, isConnectableAutomation(worldIn, currentPos, Direction.EAST))
                .with(UP, isConnectableAutomation(worldIn, currentPos, Direction.UP))
                .with(DOWN, isConnectableAutomation(worldIn, currentPos, Direction.DOWN));
    }
}
