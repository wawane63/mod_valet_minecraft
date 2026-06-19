package com.wawane.valet.construction;

import com.wawane.valet.ValetMod;
import com.wawane.valet.order.ValetOrders;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ConstructionBlueprintBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public ConstructionBlueprintBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.SOUTH));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ConstructionBlueprintBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.getBlockEntity(pos) instanceof ConstructionBlueprintBlockEntity blueprint) {
            blueprint.setFromStackNbt(itemStack.getNbt());
        }
        activateTargetValet(world, pos, itemStack.getNbt());
    }

    private void activateTargetValet(World world, BlockPos pos, @Nullable NbtCompound nbt) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld) || nbt == null || !nbt.contains(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY)) {
            return;
        }

        int constructionId = nbt.getInt(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY);
        VillagerEntity valet = findTargetValet(serverWorld, pos, nbt);
        if (valet != null) {
            ValetOrders.setConstructionTarget(valet, constructionId);
        }
    }

    @Nullable
    private VillagerEntity findTargetValet(ServerWorld world, BlockPos pos, NbtCompound nbt) {
        Box searchBox = new Box(pos).expand(96.0D);
        List<VillagerEntity> valets = world.getEntitiesByClass(VillagerEntity.class, searchBox, villager -> villager.getVillagerData().getProfession() == ValetMod.VALET_PROFESSION);
        if (nbt.containsUuid(ConstructionBlueprintBlockEntity.VALET_UUID_KEY)) {
            UUID valetUuid = nbt.getUuid(ConstructionBlueprintBlockEntity.VALET_UUID_KEY);
            for (VillagerEntity valet : valets) {
                if (valet.getUuid().equals(valetUuid)) {
                    return valet;
                }
            }
        }
        return valets.isEmpty() ? null : valets.get(0);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
