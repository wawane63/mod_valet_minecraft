package com.wawane.valet.construction;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class ConstructionBlueprintItem extends BlockItem {
    public ConstructionBlueprintItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag nbt = ConstructionBlueprintNbt.get(stack);
        if (nbt != null && nbt.contains(ConstructionBlueprintBlockEntity.CONSTRUCTION_NAME_KEY)) {
            return Component.translatable("block.valet.construction_blueprint.named", nbt.getString(ConstructionBlueprintBlockEntity.CONSTRUCTION_NAME_KEY).orElse(""));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundTag nbt = ConstructionBlueprintNbt.get(stack);
        if (nbt != null && nbt.contains(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY)) {
            tooltip.accept(Component.translatable("item.valet.construction_blueprint.tooltip"));
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
