package com.wawane.valet.ai.tasks.combat;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetDebug;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.progress.ValetCombatPerk;
import com.wawane.valet.progress.ValetCombatProgress;
import com.wawane.valet.progress.ValetCombatSkillTree;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class CombatRuntimeTask {
    private static final double ARROW_CHEST_REACH_SQUARED = 5.0D;
    private static final int SWORD_XP_PER_ATTACK = 2;
    private static final int BOW_XP_PER_SHOT = 2;

    private final Control control;
    private LivingEntity target;
    private BlockPos arrowChestPos;
    private int attackCooldownTicks;
    private int pathRefreshTicks;
    private int arrowSearchCooldownTicks;

    public CombatRuntimeTask(Control control) {
        this.control = control;
    }

    public boolean tick(ServerWorld world) {
        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }
        if (pathRefreshTicks > 0) {
            pathRefreshTicks--;
        }
        if (arrowSearchCooldownTicks > 0) {
            arrowSearchCooldownTicks--;
        }

        if (!control.isDefenseEnabled()) {
            clearTarget();
            ensureWoodenPickaxe();
            return false;
        }

        LivingEntity nextTarget = ValetCombatTargeting.chooseTarget(
                world,
                control.villager(),
                target,
                control.combatSearchRadius(),
                control.combatChaseRadius()
        );
        if (nextTarget == null) {
            if (target != null) {
                clearTarget();
                control.onCombatFinished();
            }
            ensureWoodenPickaxe();
            return false;
        }

        if (target == null || target.getId() != nextTarget.getId()) {
            clearArrowChestTarget();
            target = nextTarget;
            pathRefreshTicks = 0;
            control.onCombatStarted(target);
        }

        fight(world, target);
        return true;
    }

    public String debugSummary() {
        return control.isDefenseEnabled()
                ? "combat=" + (target == null ? "ready" : ValetDebug.shortPos(target.getBlockPos()))
                + " arrows=" + countInventoryItem(control.villager().getInventory(), Items.ARROW, control.getUsableInventorySlots(control.villager().getInventory()))
                + " arrowChest=" + shortPos(arrowChestPos)
                + " sword=" + skillSummary(control.villager(), ValetCombatSkillTree.SWORD)
                + " bow=" + skillSummary(control.villager(), ValetCombatSkillTree.BOW)
                + " allyAware=" + hasAllyAwareness(control.villager())
                : "combat=inactive";
    }

    private void fight(ServerWorld world, LivingEntity target) {
        VillagerEntity villager = control.villager();
        villager.getLookControl().lookAt(target, 30.0F, 30.0F);
        double distanceSquared = villager.squaredDistanceTo(target);

        if (distanceSquared <= control.combatAttackRangeSquared()) {
            ensureWoodenSword();
            villager.getNavigation().stop();
            if (attackCooldownTicks <= 0 && villager.canSee(target)) {
                villager.swingHand(Hand.MAIN_HAND, true);
                world.getChunkManager().sendToNearbyPlayers(
                        villager,
                        new EntityAnimationS2CPacket(villager, EntityAnimationS2CPacket.SWING_MAIN_HAND)
                );
                world.playSound(null, villager.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                target.damage(world.getDamageSources().mobAttack(villager), control.combatAttackDamage());
                addCombatXp(villager, ValetCombatSkillTree.SWORD, SWORD_XP_PER_ATTACK);
                attackCooldownTicks = control.combatAttackCooldownTicks();
            }
            return;
        }

        if (distanceSquared <= control.combatRangedAttackRangeSquared()) {
            if (hasInventoryItem(Items.ARROW)) {
                ensureBow();
                villager.getNavigation().stop();
                if (attackCooldownTicks <= 0 && villager.canSee(target)) {
                    if (hasAllyAwareness(villager) && hasAllyInArrowPath(world, villager, target)) {
                        ValetDebug.record(villager, "combat arrow_blocked ally_in_line target=" + ValetDebug.shortPos(target.getBlockPos()));
                        ensureWoodenSword();
                        chaseTarget(target);
                        return;
                    }
                    shootArrow(world, target);
                }
                return;
            }

            if (tryRestockArrows(world)) {
                return;
            }
        }

        ensureWoodenSword();
        chaseTarget(target);
    }

    private void shootArrow(ServerWorld world, LivingEntity target) {
        VillagerEntity villager = control.villager();
        if (!ValetInventoryTransfer.takeOneItem(villager.getInventory(), Items.ARROW, control.getUsableInventorySlots(villager.getInventory()))) {
            return;
        }

        villager.swingHand(Hand.MAIN_HAND, true);
        world.getChunkManager().sendToNearbyPlayers(
                villager,
                new EntityAnimationS2CPacket(villager, EntityAnimationS2CPacket.SWING_MAIN_HAND)
        );

        ArrowEntity arrow = new ArrowEntity(world, villager);
        arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        arrow.setDamage(control.combatArrowDamage());

        double dx = target.getX() - villager.getX();
        double dz = target.getZ() - villager.getZ();
        double dy = target.getBodyY(0.5D) - arrow.getY();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        arrow.setVelocity(dx, dy + horizontalDistance * 0.2D, dz, 1.6F, 8.0F);

        world.spawnEntity(arrow);
        world.playSound(null, villager.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        addCombatXp(villager, ValetCombatSkillTree.BOW, BOW_XP_PER_SHOT);
        attackCooldownTicks = control.combatArrowCooldownTicks();
        ValetDebug.record(villager, "combat arrow target=" + ValetDebug.shortPos(target.getBlockPos())
                + " arrows=" + countInventoryItem(villager.getInventory(), Items.ARROW, control.getUsableInventorySlots(villager.getInventory())));
    }

    private void chaseTarget(LivingEntity target) {
        VillagerEntity villager = control.villager();
        if (pathRefreshTicks <= 0 || villager.getNavigation().isIdle()) {
            villager.getNavigation().startMovingTo(target, control.combatMoveSpeed());
            pathRefreshTicks = 8;
        }
    }

    private void addCombatXp(VillagerEntity villager, ValetCombatSkillTree tree, int amount) {
        ValetCombatProgress.addXp(villager, tree, amount);
        ValetDebug.record(villager, "combat xp tree=" + tree.name().toLowerCase()
                + " level=" + ValetCombatProgress.getLevel(villager, tree)
                + " xp=" + ValetCombatProgress.getXp(villager, tree)
                + "/" + ValetCombatProgress.getNextLevelXp(villager, tree));
    }

    private boolean hasAllyInArrowPath(ServerWorld world, VillagerEntity shooter, LivingEntity target) {
        Vec3d start = new Vec3d(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        Vec3d end = new Vec3d(target.getX(), target.getBodyY(0.5D), target.getZ());
        Box searchBox = new Box(start, end).expand(0.9D);
        for (VillagerEntity ally : world.getEntitiesByClass(VillagerEntity.class, searchBox, ally -> isValetAlly(shooter, ally))) {
            if (ally.getBoundingBox().expand(0.35D).raycast(start, end).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldProtectAllyFromProjectile(Entity owner, Entity hit) {
        return owner instanceof VillagerEntity shooter
                && hit instanceof VillagerEntity ally
                && hasAllyAwareness(shooter)
                && isValetAlly(shooter, ally);
    }

    private static boolean hasAllyAwareness(VillagerEntity villager) {
        return ValetCombatProgress.hasPerk(villager, ValetCombatPerk.ALLY_AWARENESS);
    }

    private static boolean isValetAlly(VillagerEntity shooter, VillagerEntity ally) {
        return ally != shooter
                && ally.isAlive()
                && !ally.isRemoved()
                && ally.getVillagerData().getProfession() == ValetMod.VALET_PROFESSION
                && shooter.getVillagerData().getProfession() == ValetMod.VALET_PROFESSION;
    }

    private boolean tryRestockArrows(ServerWorld world) {
        VillagerEntity villager = control.villager();
        Inventory inventory = villager.getInventory();
        int usableSlots = control.getUsableInventorySlots(inventory);
        if (countInventoryItem(inventory, Items.ARROW, usableSlots) >= control.combatArrowRestockCount()) {
            return false;
        }

        if (arrowChestPos == null || !isArrowContainer(world, arrowChestPos)) {
            if (arrowSearchCooldownTicks > 0) {
                return false;
            }

            arrowChestPos = findBestArrowContainer(world);
            arrowSearchCooldownTicks = 20;
            if (arrowChestPos == null) {
                ValetDebug.record(villager, "combat no_arrow_chest");
                return false;
            }
            ValetDebug.record(villager, "combat arrow_chest=" + ValetDebug.shortPos(arrowChestPos));
        }

        ensureBow();
        villager.getLookControl().lookAt(arrowChestPos.getX() + 0.5D, arrowChestPos.getY() + 0.5D, arrowChestPos.getZ() + 0.5D);
        if (squaredDistance(villager.getBlockPos(), arrowChestPos) <= ARROW_CHEST_REACH_SQUARED) {
            int moved = takeArrowsFromContainer(world, arrowChestPos);
            if (moved > 0) {
                control.animateChestUse(world, arrowChestPos);
                ValetDebug.record(villager, "combat took_arrows count=" + moved + " chest=" + ValetDebug.shortPos(arrowChestPos));
            }
            clearArrowChestTarget();
            return moved > 0;
        }

        if (pathRefreshTicks <= 0 || villager.getNavigation().isIdle()) {
            boolean moving = villager.getNavigation().startMovingTo(
                    arrowChestPos.getX() + 0.5D,
                    arrowChestPos.getY(),
                    arrowChestPos.getZ() + 0.5D,
                    control.combatMoveSpeed()
            );
            pathRefreshTicks = 8;
            if (!moving) {
                ValetDebug.record(villager, "combat no_arrow_chest_path chest=" + ValetDebug.shortPos(arrowChestPos));
                clearArrowChestTarget();
                return false;
            }
        }
        return true;
    }

    private int takeArrowsFromContainer(ServerWorld world, BlockPos containerPos) {
        if (world.getBlockState(containerPos).isOf(ValetMod.INFINITE_ARROW_CHEST)) {
            Inventory target = control.villager().getInventory();
            int usableSlots = control.getUsableInventorySlots(target);
            int needed = control.combatArrowRestockCount() - countInventoryItem(target, Items.ARROW, usableSlots);
            if (needed <= 0) {
                return 0;
            }

            ItemStack arrows = new ItemStack(Items.ARROW, needed);
            ValetInventoryTransfer.insertStack(target, arrows, usableSlots);
            return needed - arrows.getCount();
        }

        Inventory source = ValetInventoryTransfer.getContainerInventory(world, containerPos);
        if (source == null) {
            return 0;
        }

        Inventory target = control.villager().getInventory();
        int usableSlots = control.getUsableInventorySlots(target);
        int needed = control.combatArrowRestockCount() - countInventoryItem(target, Items.ARROW, usableSlots);
        if (needed <= 0) {
            return 0;
        }

        int movedTotal = 0;
        for (int slot = 0; slot < source.size() && needed > 0; slot++) {
            ItemStack sourceStack = source.getStack(slot);
            if (!sourceStack.isOf(Items.ARROW)) {
                continue;
            }

            int amount = Math.min(sourceStack.getCount(), needed);
            ItemStack arrows = new ItemStack(Items.ARROW, amount);
            ValetInventoryTransfer.insertStack(target, arrows, usableSlots);
            int moved = amount - arrows.getCount();
            if (moved <= 0) {
                break;
            }

            sourceStack.decrement(moved);
            if (sourceStack.isEmpty()) {
                source.setStack(slot, ItemStack.EMPTY);
            }
            movedTotal += moved;
            needed -= moved;
        }

        if (movedTotal > 0) {
            source.markDirty();
            target.markDirty();
        }
        return movedTotal;
    }

    private BlockPos findBestArrowContainer(ServerWorld world) {
        VillagerEntity villager = control.villager();
        BlockPos nearVillager = findNearestArrowContainer(world, villager.getBlockPos());
        BlockPos workOrigin = control.getWorkOrigin(world);
        BlockPos nearWork = workOrigin == null ? null : findNearestArrowContainer(world, workOrigin);
        if (nearVillager == null) {
            return nearWork;
        }
        if (nearWork == null) {
            return nearVillager;
        }
        return squaredDistance(villager.getBlockPos(), nearVillager) <= squaredDistance(villager.getBlockPos(), nearWork)
                ? nearVillager
                : nearWork;
    }

    private BlockPos findNearestArrowContainer(ServerWorld world, BlockPos origin) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int radius = control.chestRadius();
        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, 4, radius)) {
            BlockPos immutable = pos.toImmutable();
            if (!isArrowContainer(world, immutable)) {
                continue;
            }

            double distance = squaredDistance(origin, immutable);
            if (distance < nearestDistance) {
                nearest = immutable;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean isArrowContainer(ServerWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isOf(ValetMod.INFINITE_ARROW_CHEST)) {
            return true;
        }
        if (!blockState.isOf(Blocks.CHEST) && !blockState.isOf(Blocks.TRAPPED_CHEST) && !blockState.isOf(Blocks.BARREL)) {
            return false;
        }

        Inventory inventory = ValetInventoryTransfer.getContainerInventory(world, pos);
        return inventory != null && ValetInventoryTransfer.inventoryHasItem(inventory, Items.ARROW, inventory.size());
    }

    private void clearTarget() {
        target = null;
        pathRefreshTicks = 0;
        clearArrowChestTarget();
    }

    private void clearArrowChestTarget() {
        arrowChestPos = null;
    }

    private void ensureWoodenSword() {
        ensureHeldItem(Items.WOODEN_SWORD);
    }

    private void ensureBow() {
        ensureHeldItem(Items.BOW);
    }

    private void ensureWoodenPickaxe() {
        ensureHeldItem(Items.WOODEN_PICKAXE);
    }

    private void ensureHeldItem(Item item) {
        VillagerEntity villager = control.villager();
        if (villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(item)) {
            return;
        }

        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(item));
        villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private boolean hasInventoryItem(Item item) {
        Inventory inventory = control.villager().getInventory();
        return ValetInventoryTransfer.inventoryHasItem(inventory, item, control.getUsableInventorySlots(inventory));
    }

    private static int countInventoryItem(Inventory inventory, Item item, int maxSlots) {
        int count = 0;
        int slots = Math.min(inventory.size(), Math.max(0, maxSlots));
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static double squaredDistance(BlockPos first, BlockPos second) {
        int dx = first.getX() - second.getX();
        int dy = first.getY() - second.getY();
        int dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static String shortPos(BlockPos pos) {
        return pos == null ? "-" : ValetDebug.shortPos(pos);
    }

    private static String skillSummary(VillagerEntity villager, ValetCombatSkillTree tree) {
        return ValetCombatProgress.getLevel(villager, tree)
                + ":" + ValetCombatProgress.getXp(villager, tree)
                + "/" + ValetCombatProgress.getNextLevelXp(villager, tree)
                + "+" + ValetCombatProgress.getPendingPerks(villager, tree);
    }

    public interface Control {
        VillagerEntity villager();

        BlockPos getWorkOrigin(ServerWorld world);

        boolean isDefenseEnabled();

        double combatSearchRadius();

        double combatChaseRadius();

        double combatAttackRangeSquared();

        double combatRangedAttackRangeSquared();

        double combatMoveSpeed();

        float combatAttackDamage();

        int combatAttackCooldownTicks();

        float combatArrowDamage();

        int combatArrowCooldownTicks();

        int combatArrowRestockCount();

        int chestRadius();

        int getUsableInventorySlots(Inventory inventory);

        void animateChestUse(ServerWorld world, BlockPos pos);

        void onCombatStarted(LivingEntity target);

        void onCombatFinished();
    }
}
