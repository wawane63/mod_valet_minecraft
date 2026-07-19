package com.wawane.valet;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

/** Jeton non craftable qui lie explicitement un lit a un valet precis. */
public final class ValetBedBadgeItem extends Item {
    private static final String VALET_UUID_KEY = "ValetBedBadgeUuid";
    private static final String VALET_NAME_KEY = "ValetBedBadgeName";

    public ValetBedBadgeItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Villager villager) {
        ItemStack stack = new ItemStack(ValetMod.VALET_BED_BADGE_ITEM);
        CompoundTag tag = new CompoundTag();
        tag.putString(VALET_UUID_KEY, villager.getUUID().toString());
        tag.putString(VALET_NAME_KEY, villager.getDisplayName().getString());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        return stack;
    }

    public static UUID getValetUuid(ItemStack stack) {
        CompoundTag tag = getData(stack);
        if (tag == null) {
            return null;
        }
        String value = tag.getString(VALET_UUID_KEY).orElse("");
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean isFor(ItemStack stack, UUID valetUuid) {
        UUID stored = getValetUuid(stack);
        return stored != null && stored.equals(valetUuid);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = getData(stack);
        String name = tag == null ? "" : tag.getString(VALET_NAME_KEY).orElse("");
        return name.isBlank()
                ? super.getName(stack)
                : Component.translatable("item.valet.bed_badge.named", name);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.valet.bed_badge.tooltip"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    private static CompoundTag getData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || data.isEmpty() ? null : data.copyTag();
    }
}
