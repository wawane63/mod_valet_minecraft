package com.wawane.valet.progress;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ValetCombatProgress {
    public static final int DATA_VERSION = 1;
    private static final String DATA_VERSION_KEY = "ValetCombatProgressDataVersion";
    private static final Map<UUID, Data> DATA = new ConcurrentHashMap<>();

    private ValetCombatProgress() {
    }

    public static int getLevel(VillagerEntity villager, ValetCombatSkillTree tree) {
        SkillData skillData = skillData(villager, tree);
        normalize(skillData);
        return skillData.level;
    }

    public static int getXp(VillagerEntity villager, ValetCombatSkillTree tree) {
        SkillData skillData = skillData(villager, tree);
        normalize(skillData);
        return skillData.xp;
    }

    public static int getNextLevelXp(VillagerEntity villager, ValetCombatSkillTree tree) {
        SkillData skillData = skillData(villager, tree);
        normalize(skillData);
        return xpForNextLevel(skillData.level);
    }

    public static int getPendingPerks(VillagerEntity villager, ValetCombatSkillTree tree) {
        SkillData skillData = skillData(villager, tree);
        normalize(skillData);
        return skillData.pendingPerks;
    }

    public static void addXp(VillagerEntity villager, ValetCombatSkillTree tree, int amount) {
        if (amount <= 0 || tree == null) {
            return;
        }

        SkillData skillData = skillData(villager, tree);
        normalize(skillData);
        skillData.xp += amount;
        while (skillData.xp >= xpForNextLevel(skillData.level)) {
            skillData.xp -= xpForNextLevel(skillData.level);
            skillData.level++;
            skillData.pendingPerks++;
        }
    }

    public static boolean hasPerk(VillagerEntity villager, ValetCombatPerk perk) {
        if (perk == null) {
            return false;
        }
        return data(villager).perks[perk.ordinal()];
    }

    public static boolean[] getPerks(VillagerEntity villager) {
        boolean[] perks = Arrays.copyOf(data(villager).perks, ValetCombatPerk.values().length);
        for (ValetCombatPerk perk : ValetCombatPerk.values()) {
            perks[perk.ordinal()] = hasPerk(villager, perk);
        }
        return perks;
    }

    public static boolean choosePerk(VillagerEntity villager, ValetCombatPerk perk) {
        if (perk == null || hasPerk(villager, perk)) {
            return false;
        }

        Data data = data(villager);
        SkillData skillData = data.skills[perk.getTree().ordinal()];
        normalize(skillData);
        if (skillData.pendingPerks <= 0 || !canChoosePerk(data, perk)) {
            return false;
        }

        data.perks[perk.ordinal()] = true;
        skillData.pendingPerks--;
        return true;
    }

    public static boolean hasData(VillagerEntity villager) {
        return DATA.containsKey(villager.getUuid());
    }

    public static boolean hasNbt(NbtCompound nbt) {
        if (nbt.contains(DATA_VERSION_KEY)) {
            return true;
        }
        for (ValetCombatSkillTree tree : ValetCombatSkillTree.values()) {
            if (nbt.contains(levelKey(tree)) || nbt.contains(xpKey(tree)) || nbt.contains(pendingPerksKey(tree))) {
                return true;
            }
        }
        for (ValetCombatPerk perk : ValetCombatPerk.values()) {
            if (nbt.contains(perk.getNbtKey())) {
                return true;
            }
        }
        return false;
    }

    public static void clear(UUID uuid) {
        DATA.remove(uuid);
    }

    public static void clearAll() {
        DATA.clear();
    }

    public static void writeToNbt(VillagerEntity villager, NbtCompound nbt) {
        Data data = data(villager);
        nbt.putInt(DATA_VERSION_KEY, DATA_VERSION);
        for (ValetCombatSkillTree tree : ValetCombatSkillTree.values()) {
            SkillData skillData = data.skills[tree.ordinal()];
            normalize(skillData);
            nbt.putInt(levelKey(tree), skillData.level);
            nbt.putInt(xpKey(tree), skillData.xp);
            nbt.putInt(pendingPerksKey(tree), skillData.pendingPerks);
        }
        for (ValetCombatPerk perk : ValetCombatPerk.values()) {
            nbt.putBoolean(perk.getNbtKey(), data.perks[perk.ordinal()]);
        }
    }

    public static void readFromNbt(VillagerEntity villager, NbtCompound nbt) {
        Data data = data(villager);
        if (!hasNbt(nbt)) {
            normalize(data);
            return;
        }

        for (ValetCombatSkillTree tree : ValetCombatSkillTree.values()) {
            SkillData skillData = data.skills[tree.ordinal()];
            if (nbt.contains(levelKey(tree))) {
                skillData.level = Math.max(1, nbt.getInt(levelKey(tree)));
            }
            if (nbt.contains(xpKey(tree))) {
                skillData.xp = Math.max(0, nbt.getInt(xpKey(tree)));
            }
            if (nbt.contains(pendingPerksKey(tree))) {
                skillData.pendingPerks = Math.max(0, nbt.getInt(pendingPerksKey(tree)));
            }
        }
        for (ValetCombatPerk perk : ValetCombatPerk.values()) {
            if (nbt.contains(perk.getNbtKey())) {
                data.perks[perk.ordinal()] = nbt.getBoolean(perk.getNbtKey());
            }
        }
        normalize(data);
    }

    private static int xpForNextLevel(int level) {
        return 30 + Math.max(0, level - 1) * 20;
    }

    private static SkillData skillData(VillagerEntity villager, ValetCombatSkillTree tree) {
        return data(villager).skills[tree.ordinal()];
    }

    private static Data data(VillagerEntity villager) {
        return DATA.computeIfAbsent(villager.getUuid(), uuid -> new Data());
    }

    private static void normalize(Data data) {
        for (SkillData skillData : data.skills) {
            normalize(skillData);
        }
    }

    private static void normalize(SkillData skillData) {
        skillData.level = Math.max(1, skillData.level);
        skillData.xp = Math.max(0, skillData.xp);
        skillData.pendingPerks = Math.max(0, skillData.pendingPerks);
    }

    private static String levelKey(ValetCombatSkillTree tree) {
        return "Valet" + tree.getNbtPrefix() + "Level";
    }

    private static String xpKey(ValetCombatSkillTree tree) {
        return "Valet" + tree.getNbtPrefix() + "Xp";
    }

    private static String pendingPerksKey(ValetCombatSkillTree tree) {
        return "Valet" + tree.getNbtPrefix() + "PendingPerks";
    }

    private static boolean canChoosePerk(Data data, ValetCombatPerk perk) {
        return switch (perk) {
            case SWORD_STRENGTH, ALLY_AWARENESS -> true;
            case SWORD_RECOVERY, SWORD_DEFENSE -> data.perks[ValetCombatPerk.SWORD_STRENGTH.ordinal()];
            case BOW_QUICK_SHOT, BOW_STRENGTH -> data.perks[ValetCombatPerk.ALLY_AWARENESS.ordinal()];
            case BOW_RECYCLE_ARROW -> data.perks[ValetCombatPerk.BOW_STRENGTH.ordinal()];
        };
    }

    private static final class Data {
        private final SkillData[] skills = new SkillData[ValetCombatSkillTree.values().length];
        private final boolean[] perks = new boolean[ValetCombatPerk.values().length];

        private Data() {
            for (ValetCombatSkillTree tree : ValetCombatSkillTree.values()) {
                skills[tree.ordinal()] = new SkillData();
            }
        }
    }

    private static final class SkillData {
        private int level = 1;
        private int xp;
        private int pendingPerks;
    }
}
