package com.wawane.valet.group;

import java.util.function.Consumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ValetGroupCardItem extends Item {
    public ValetGroupCardItem(Properties settings) {
        super(settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        String groupName = ValetGroupBindings.getGroupName(stack);
        if (!groupName.isBlank()) {
            return Component.translatable("item.valet.valet_group_card.bound", groupName);
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        return ValetGroupInteractions.useBoundControlItem(player, world, hand);
    }

    @Override
    @SuppressWarnings("deprecation") // Legacy CUSTOM_DATA items still need the item tooltip hook.
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        int groupId = ValetGroupBindings.getGroupId(stack);
        if (groupId > 0) {
            tooltip.accept(Component.translatable("item.valet.valet_group_card.tooltip"));
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
