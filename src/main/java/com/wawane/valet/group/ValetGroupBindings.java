package com.wawane.valet.group;

import com.wawane.valet.ValetMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class ValetGroupBindings {
    public static final String GROUP_ID_KEY = "ValetGroupId";
    public static final String GROUP_NAME_KEY = "ValetGroupName";

    private ValetGroupBindings() {
    }

    public static boolean canCarryGroup(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ValetMod.VALET_GROUP_CARD_ITEM) || stack.is(Items.GOAT_HORN));
    }

    public static int getGroupId(ItemStack stack) {
        if (!canCarryGroup(stack)) {
            return -1;
        }
        CompoundTag tag = getTag(stack);
        return tag == null ? -1 : tag.getIntOr(GROUP_ID_KEY, -1);
    }

    public static String getGroupName(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag == null) {
            return "";
        }
        String name = tag.getStringOr(GROUP_NAME_KEY, "");
        return name.length() <= ValetGroup.MAX_NAME_LENGTH ? name : name.substring(0, ValetGroup.MAX_NAME_LENGTH);
    }

    public static void setGroup(ItemStack stack, ValetGroup group) {
        if (!canCarryGroup(stack) || group == null) {
            return;
        }
        CompoundTag tag = getTag(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putInt(GROUP_ID_KEY, group.id());
        tag.putString(GROUP_NAME_KEY, group.name());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    private static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || data.isEmpty() ? null : data.copyTag();
    }
}
