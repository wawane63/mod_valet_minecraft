package com.wawane.valet.construction;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ConstructionBlueprintItem extends BlockItem {
    public ConstructionBlueprintItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(ConstructionBlueprintBlockEntity.CONSTRUCTION_NAME_KEY)) {
            return Text.translatable("block.valet.construction_blueprint.named", nbt.getString(ConstructionBlueprintBlockEntity.CONSTRUCTION_NAME_KEY));
        }
        return super.getName(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY)) {
            tooltip.add(Text.translatable("item.valet.construction_blueprint.tooltip"));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}
