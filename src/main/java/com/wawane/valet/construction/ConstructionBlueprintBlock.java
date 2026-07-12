package com.wawane.valet.construction;

import com.mojang.serialization.MapCodec;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
import com.wawane.valet.order.ValetOrders;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class ConstructionBlueprintBlock extends BaseEntityBlock {
    public static final MapCodec<ConstructionBlueprintBlock> CODEC = simpleCodec(ConstructionBlueprintBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ConstructionBlueprintBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConstructionBlueprintBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        CompoundTag nbt = ConstructionBlueprintNbt.get(itemStack);
        if (world.getBlockEntity(pos) instanceof ConstructionBlueprintBlockEntity blueprint) {
            blueprint.setFromStackNbt(nbt, placer != null && placer.isShiftKeyDown());
        }
        activateTargetValet(world, pos, nbt);
    }

    private void activateTargetValet(Level world, BlockPos pos, @Nullable CompoundTag nbt) {
        if (world.isClientSide() || !(world instanceof ServerLevel serverWorld) || nbt == null || !nbt.contains(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY)) {
            return;
        }

        int constructionId = nbt.getInt(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY).orElse(-1);
        if (constructionId <= 0 || ValetConstructionStorage.get(serverWorld).getBlueprint(constructionId) == null) {
            return;
        }
        Villager valet = findTargetValet(serverWorld, pos, nbt);
        if (valet != null) {
            ValetOrders.setConstructionTarget(valet, constructionId);
        }
    }

    @Nullable
    private Villager findTargetValet(ServerLevel world, BlockPos pos, CompoundTag nbt) {
        AABB searchBox = new AABB(pos).inflate(96.0D);
        List<Villager> valets = world.getEntitiesOfClass(Villager.class, searchBox, valet ->
                valet.isAlive()
                        && !valet.isRemoved()
                        && ValetMod.isValet(valet)
                        && ValetRole.get(world, valet) == ValetRole.ARTISAN);
        if (nbt.contains(ConstructionBlueprintBlockEntity.VALET_UUID_KEY)) {
            UUID valetUuid = readUuid(nbt, ConstructionBlueprintBlockEntity.VALET_UUID_KEY);
            if (valetUuid != null) {
                for (Villager valet : valets) {
                    if (valet.getUUID().equals(valetUuid)) {
                        return valet;
                    }
                }
            }
        }
        Villager nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Villager valet : valets) {
            double distance = valet.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distance < nearestDistance) {
                nearest = valet;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    @Nullable
    private static UUID readUuid(CompoundTag nbt, String key) {
        return nbt.getString(key)
                .flatMap(value -> {
                    try {
                        return java.util.Optional.of(UUID.fromString(value));
                    } catch (IllegalArgumentException ignored) {
                        return java.util.Optional.empty();
                    }
                })
                .orElse(null);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
