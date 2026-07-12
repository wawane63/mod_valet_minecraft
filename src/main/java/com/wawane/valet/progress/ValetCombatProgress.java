package com.wawane.valet.progress;

import com.mojang.serialization.Codec;
import com.wawane.valet.ValetRole;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ValetCombatProgress {
    public static final int DATA_VERSION = 1;
    private static final String DATA_VERSION_KEY = "ValetCombatProgressDataVersion";
    private static final Map<UUID, Data> DATA = new ConcurrentHashMap<>();

    private ValetCombatProgress() {
    }

    public static int getLevel(Villager villager, ValetCombatSkillTree tree) {
        SkillData skillData = skillData(villager, tree);
        normalize(skillData);
        return skillData.level;
    }

    public static int getXp(Villager villager, ValetCombatSkillTree tree) {
        SkillData skillData = skillData(villager, tree);
        normalize(skillData);
        return skillData.xp;
    }

    public static int getNextLevelXp(Villager villager, ValetCombatSkillTree tree) {
        SkillData skillData = skillData(villager, tree);
        normalize(skillData);
        return xpForNextLevel(skillData.level);
    }

    public static int getPendingPerks(Villager villager, ValetCombatSkillTree tree) {
        SkillData skillData = skillData(villager, tree);
        normalize(skillData);
        return skillData.pendingPerks;
    }

    public static void addXp(Villager villager, ValetCombatSkillTree tree, int amount) {
        if (amount <= 0 || tree == null) {
            return;
        }

        SkillData skillData = skillData(villager, tree);
        normalize(skillData);
        skillData.xp = saturatingAdd(skillData.xp, amount);
        while (skillData.xp >= xpForNextLevel(skillData.level)) {
            skillData.xp -= xpForNextLevel(skillData.level);
            if (skillData.level == Integer.MAX_VALUE) {
                skillData.xp = 0;
                break;
            }
            skillData.level++;
            skillData.pendingPerks = saturatingAdd(skillData.pendingPerks, 1);
        }
    }

    public static boolean hasPerk(Villager villager, ValetCombatPerk perk) {
        if (perk == null) {
            return false;
        }
        return data(villager).perks[perk.ordinal()];
    }

    public static boolean[] getPerks(Villager villager) {
        return Arrays.copyOf(data(villager).perks, ValetCombatPerk.values().length);
    }

    public static boolean choosePerk(Villager villager, ValetCombatPerk perk) {
        if (perk == null || hasPerk(villager, perk)) {
            return false;
        }
        if (!(villager.level() instanceof ServerLevel world) || ValetRole.get(world, villager) != ValetRole.COMBATANT) {
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

    public static boolean hasData(Villager villager) {
        return DATA.containsKey(villager.getUUID());
    }

    public static boolean hasNbt(ValueInput input) {
        if (input.getInt(DATA_VERSION_KEY).isPresent()) {
            return true;
        }
        for (ValetCombatSkillTree tree : ValetCombatSkillTree.values()) {
            if (input.getInt(levelKey(tree)).isPresent()
                    || input.getInt(xpKey(tree)).isPresent()
                    || input.getInt(pendingPerksKey(tree)).isPresent()) {
                return true;
            }
        }
        for (ValetCombatPerk perk : ValetCombatPerk.values()) {
            if (input.read(perk.getNbtKey(), Codec.BOOL).isPresent()) {
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

    public static void writeToNbt(Villager villager, ValueOutput output) {
        Data data = data(villager);
        output.putInt(DATA_VERSION_KEY, DATA_VERSION);
        for (ValetCombatSkillTree tree : ValetCombatSkillTree.values()) {
            SkillData skillData = data.skills[tree.ordinal()];
            normalize(skillData);
            output.putInt(levelKey(tree), skillData.level);
            output.putInt(xpKey(tree), skillData.xp);
            output.putInt(pendingPerksKey(tree), skillData.pendingPerks);
        }
        for (ValetCombatPerk perk : ValetCombatPerk.values()) {
            output.putBoolean(perk.getNbtKey(), data.perks[perk.ordinal()]);
        }
    }

    public static void readFromNbt(Villager villager, ValueInput input) {
        Data data = new Data();
        DATA.put(villager.getUUID(), data);
        if (!hasNbt(input)) {
            return;
        }

        for (ValetCombatSkillTree tree : ValetCombatSkillTree.values()) {
            SkillData skillData = data.skills[tree.ordinal()];
            if (input.getInt(levelKey(tree)).isPresent()) {
                skillData.level = Math.max(1, input.getIntOr(levelKey(tree), 1));
            }
            if (input.getInt(xpKey(tree)).isPresent()) {
                skillData.xp = Math.max(0, input.getIntOr(xpKey(tree), 0));
            }
            if (input.getInt(pendingPerksKey(tree)).isPresent()) {
                skillData.pendingPerks = Math.max(0, input.getIntOr(pendingPerksKey(tree), 0));
            }
        }
        for (ValetCombatPerk perk : ValetCombatPerk.values()) {
            if (input.read(perk.getNbtKey(), Codec.BOOL).isPresent()) {
                data.perks[perk.ordinal()] = input.getBooleanOr(perk.getNbtKey(), false);
            }
        }
        normalize(data);
    }

    private static int xpForNextLevel(int level) {
        long threshold = 30L + Math.max(0L, (long) level - 1L) * 20L;
        return (int) Math.min(Integer.MAX_VALUE, threshold);
    }

    private static int saturatingAdd(int value, int amount) {
        return (int) Math.min(Integer.MAX_VALUE, (long) value + amount);
    }

    private static SkillData skillData(Villager villager, ValetCombatSkillTree tree) {
        return data(villager).skills[tree.ordinal()];
    }

    private static Data data(Villager villager) {
        return DATA.computeIfAbsent(villager.getUUID(), uuid -> new Data());
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
            case SWORD_REACH -> data.perks[ValetCombatPerk.SWORD_RECOVERY.ordinal()];
            case SWORD_GUARDIAN -> data.perks[ValetCombatPerk.SWORD_DEFENSE.ordinal()];
            case BOW_QUICK_SHOT, BOW_STRENGTH -> data.perks[ValetCombatPerk.ALLY_AWARENESS.ordinal()];
            case BOW_RANGE -> data.perks[ValetCombatPerk.BOW_STRENGTH.ordinal()];
            case BOW_VOLLEY -> data.perks[ValetCombatPerk.BOW_QUICK_SHOT.ordinal()];
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
