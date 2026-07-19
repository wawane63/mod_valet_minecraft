package com.wawane.valet.ai.tasks.combat;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.network.packets.ValetMagicCastPayload;
import com.wawane.valet.progress.ValetCombatPerk;
import com.wawane.valet.progress.ValetCombatProgress;
import com.wawane.valet.progress.ValetCombatSkillTree;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetProgress;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CombatRuntimeTask {
    private static final double ARROW_CHEST_REACH_SQUARED = 5.0D;
    private static final int SWORD_XP_PER_ATTACK = 2;
    private static final int BOW_XP_PER_SHOT = 2;
    private static final int MAGIC_ICE_XP_PER_SHOT = 2;
    private static final int MAGIC_ICE_SHOTS_BEFORE_FANGS = 3;
    private static final int MAGIC_FANG_COUNT = 6;
    private static final float MAGIC_ICE_DAMAGE = 3.0F;
    private static final float MAGIC_ICE_DAMAGE_BONUS = 1.0F;
    private static final float MAGIC_SHATTER_DAMAGE_BONUS = 1.0F;
    private static final int MAGIC_ICE_SLOW_TICKS = 80;
    private static final int MAGIC_SUPPORT_COOLDOWN_TICKS = 80;
    private static final int MAGIC_SUPPORT_SCAN_INTERVAL_TICKS = 20;
    private static final double MAGIC_SUPPORT_RADIUS = 8.0D;
    private static final float MAGIC_HEAL_AMOUNT = 2.0F;
    private static final int ARROW_SEARCH_INTERVAL_TICKS = 20;
    private static final int ARROW_PATH_FAILURE_BACKOFF_TICKS = 100;

    private final Control control;
    private LivingEntity target;
    private BlockPos arrowChestPos;
    private int attackCooldownTicks;
    private int pathRefreshTicks;
    private int arrowSearchCooldownTicks;
    private int magicIceShots;
    private int magicSupportCooldownTicks;
    private boolean targetCommanded;

    public CombatRuntimeTask(Control control) {
        this.control = control;
    }

    public boolean tick(ServerLevel world) {
        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }
        if (pathRefreshTicks > 0) {
            pathRefreshTicks--;
        }
        if (arrowSearchCooldownTicks > 0) {
            arrowSearchCooldownTicks--;
        }
        if (magicSupportCooldownTicks > 0) {
            magicSupportCooldownTicks--;
        }

        if (!control.isDefenseEnabled() && !control.isMagicEnabled()) {
            magicIceShots = 0;
            magicSupportCooldownTicks = 0;
            clearTarget();
            return false;
        }

        if (control.isMagicEnabled()) {
            tickMagicSupport(world);
        }

        LivingEntity nextTarget = control.chooseCommandedTarget(world, target);
        boolean commanded = nextTarget != null;
        if (nextTarget == null) {
            nextTarget = ValetCombatTargeting.chooseTarget(
                    world,
                    control.villager(),
                    target,
                    control.combatSearchRadius(),
                    control.combatChaseRadius()
            );
        }
        if (!commanded && nextTarget != null && !control.isWithinWorkZone(world, nextTarget.blockPosition())) {
            nextTarget = null;
        }
        if (nextTarget == null) {
            if (target != null) {
                clearTarget();
                control.onCombatFinished();
            }
            if (control.isMagicEnabled()) {
                clearHeldItem();
            } else {
                ensureWoodenSword();
            }
            return false;
        }

        if (target == null || target.getId() != nextTarget.getId()) {
            clearArrowChestTarget();
            target = nextTarget;
            pathRefreshTicks = 0;
            control.onCombatStarted(target);
        }
        targetCommanded = commanded;

        applyDefensePerk();
        fight(world, target);
        return true;
    }

    public String debugSummary() {
        return control.isDefenseEnabled() || control.isMagicEnabled()
                ? "combat=" + (target == null ? "ready" : ValetDebug.shortPos(target.blockPosition()))
                + " mode=" + (control.isMagicEnabled() ? "magic" : "combatant")
                + " arrows=" + countInventoryItem(control.villager().getInventory(), Items.ARROW, control.getUsableInventorySlots(control.villager().getInventory()))
                + " arrowChest=" + shortPos(arrowChestPos)
                + " sword=" + skillSummary(control.villager(), ValetCombatSkillTree.SWORD)
                + " bow=" + skillSummary(control.villager(), ValetCombatSkillTree.BOW)
                + " allyAware=" + hasAllyAwareness(control.villager())
                + (control.isMagicEnabled()
                ? " ice=" + magicIceShots + "/" + MAGIC_ICE_SHOTS_BEFORE_FANGS
                + " fangs=" + ValetProgress.hasPerk(control.villager(), ValetPerk.MAGIC_FANGS)
                : "")
                : "combat=inactive";
    }

    private void fight(ServerLevel world, LivingEntity target) {
        Villager villager = control.villager();
        villager.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distanceSquared = villager.distanceToSqr(target);

        if (control.isMagicEnabled()) {
            fightWithMagic(world, target, distanceSquared);
            return;
        }

        if (distanceSquared <= control.combatAttackRangeSquared()) {
            ensureWoodenSword();
            villager.getNavigation().stop();
            if (attackCooldownTicks <= 0 && villager.hasLineOfSight(target)) {
                villager.swing(InteractionHand.MAIN_HAND, true);
                world.getChunkSource().sendToTrackingPlayersAndSelf(
                        villager,
                        new ClientboundAnimatePacket(villager, ClientboundAnimatePacket.SWING_MAIN_HAND)
                );
                world.playSound(null, villager.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 1.0F, 1.0F);
                if (target.hurtServer(world, world.damageSources().mobAttack(villager), control.combatAttackDamage())) {
                    addCombatXp(villager, ValetCombatSkillTree.SWORD, SWORD_XP_PER_ATTACK);
                }
                attackCooldownTicks = control.combatAttackCooldownTicks();
            }
            return;
        }

        if (distanceSquared <= control.combatRangedAttackRangeSquared()) {
            if (hasInventoryItem(Items.ARROW)) {
                ensureBow();
                villager.getNavigation().stop();
                if (attackCooldownTicks <= 0 && villager.hasLineOfSight(target)) {
                    if (hasAllyAwareness(villager) && hasAllyInArrowPath(world, villager, target)) {
                        ValetDebug.record(villager, "combat arrow_blocked ally_in_line target=" + ValetDebug.shortPos(target.blockPosition()));
                        ensureWoodenSword();
                        chaseTarget(world, target);
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
        chaseTarget(world, target);
    }

    private void fightWithMagic(ServerLevel world, LivingEntity target, double distanceSquared) {
        Villager villager = control.villager();
        clearHeldItem();
        if (distanceSquared <= control.magicAttackRangeSquared()) {
            villager.getNavigation().stop();
            if (attackCooldownTicks <= 0 && villager.hasLineOfSight(target)) {
                if (ValetProgress.hasPerk(villager, ValetPerk.MAGIC_FANGS) && magicIceShots >= MAGIC_ICE_SHOTS_BEFORE_FANGS) {
                    castFangs(world, target);
                } else {
                    shootIceBolt(world, target);
                }
            }
            return;
        }

        chaseTarget(world, target);
    }

    private void castFangs(ServerLevel world, LivingEntity target) {
        Villager villager = control.villager();
        playMagicCast(world, villager, SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F);

        double dx = target.getX() - villager.getX();
        double dz = target.getZ() - villager.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.atan2(dz, dx);
        if (distance < 0.001D) {
            spawnFangs(world, target.getX(), target.getY(), target.getZ(), yaw, 0);
        } else {
            for (int i = 1; i <= MAGIC_FANG_COUNT; i++) {
                double step = Math.min(distance, i * 1.25D);
                double x = villager.getX() + Math.cos(yaw) * step;
                double z = villager.getZ() + Math.sin(yaw) * step;
                spawnFangs(world, x, target.getY(), z, yaw, i * 2);
            }
        }

        applyEnemyAlteration(villager, target);
        attackCooldownTicks = control.magicAttackCooldownTicks();
        magicIceShots = 0;
        ValetDebug.record(villager, "combat magic_fangs target=" + ValetDebug.shortPos(target.blockPosition()));
    }

    private void spawnFangs(ServerLevel world, double x, double y, double z, float yaw, int warmupTicks) {
        world.addFreshEntity(new EvokerFangs(world, x, y, z, yaw, warmupTicks, control.villager()));
    }

    private void shootIceBolt(ServerLevel world, LivingEntity target) {
        Villager villager = control.villager();
        playMagicCast(world, villager, SoundEvents.SNOWBALL_THROW, 0.85F);

        Vec3 start = new Vec3(villager.getX(), villager.getEyeY() - 0.1D, villager.getZ());
        Vec3 direction = new Vec3(target.getX() - start.x, target.getY(0.5D) - start.y, target.getZ() - start.z);
        if (direction.lengthSqr() < 1.0E-7D) {
            direction = villager.getLookAngle();
        }

        Snowball iceBolt = new Snowball(world, villager, new ItemStack(Items.SNOWBALL));
        iceBolt.setPos(start.x, start.y, start.z);
        iceBolt.shoot(direction.x, direction.y, direction.z, 1.45F, 2.0F);
        world.addFreshEntity(iceBolt);
        if (applyIceDamage(world, villager, iceBolt, target)) {
            addMagicXp(villager, MAGIC_ICE_XP_PER_SHOT);
        }
        if (ValetProgress.hasPerk(villager, ValetPerk.MAGIC_FANGS)) {
            magicIceShots = Math.min(MAGIC_ICE_SHOTS_BEFORE_FANGS, magicIceShots + 1);
        } else {
            magicIceShots = 0;
        }
        attackCooldownTicks = control.magicAttackCooldownTicks();
        ValetDebug.record(villager, "combat magic_ice target=" + ValetDebug.shortPos(target.blockPosition())
                + " ice=" + magicIceShots + "/" + MAGIC_ICE_SHOTS_BEFORE_FANGS
                + " fangs=" + ValetProgress.hasPerk(villager, ValetPerk.MAGIC_FANGS));
    }

    private boolean applyIceDamage(ServerLevel world, Villager villager, Snowball iceBolt, LivingEntity target) {
        float damage = MAGIC_ICE_DAMAGE;
        if (ValetProgress.hasPerk(villager, ValetPerk.MAGIC_ICE)) {
            damage += MAGIC_ICE_DAMAGE_BONUS;
        }
        if (ValetProgress.hasPerk(villager, ValetPerk.MAGIC_SHATTER)) {
            damage += MAGIC_SHATTER_DAMAGE_BONUS;
        }

        if (!target.hurtServer(world, world.damageSources().thrown(iceBolt, villager), damage)) {
            return false;
        }
        int slowAmplifier = ValetProgress.hasPerk(villager, ValetPerk.MAGIC_ICE) ? 1 : 0;
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, MAGIC_ICE_SLOW_TICKS, slowAmplifier, true, true));
        if (target.canFreeze()) {
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze(), target.getTicksFrozen() + 80));
        }
        applyEnemyAlteration(villager, target);
        return true;
    }

    private void applyEnemyAlteration(Villager villager, LivingEntity target) {
        if (!ValetProgress.hasPerk(villager, ValetPerk.MAGIC_WEAKEN)) {
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 1, true, true));
    }

    private void tickMagicSupport(ServerLevel world) {
        Villager villager = control.villager();
        boolean canHeal = ValetProgress.hasPerk(villager, ValetPerk.MAGIC_HEAL);
        boolean canRegen = ValetProgress.hasPerk(villager, ValetPerk.MAGIC_REGEN_AURA);
        boolean canWard = ValetProgress.hasPerk(villager, ValetPerk.MAGIC_WARD);
        if ((!canHeal && !canRegen && !canWard) || magicSupportCooldownTicks > 0) {
            return;
        }

        boolean cast = false;
        AABB supportArea = villager.getBoundingBox().inflate(MAGIC_SUPPORT_RADIUS);
        for (Villager ally : world.getEntitiesOfClass(Villager.class, supportArea, ally -> isValetAlly(villager, ally))) {
            boolean injured = ally.getHealth() < ally.getMaxHealth();
            if (canHeal && injured) {
                ally.heal(MAGIC_HEAL_AMOUNT);
                cast = true;
            }
            if (canRegen && injured && needsEffectRefresh(ally, MobEffects.REGENERATION)) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
                cast = true;
            }
            if (canWard && (injured || hasActiveCombatTarget()) && needsEffectRefresh(ally, MobEffects.RESISTANCE)) {
                ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, true, false));
                cast = true;
            }
        }

        magicSupportCooldownTicks = cast ? MAGIC_SUPPORT_COOLDOWN_TICKS : MAGIC_SUPPORT_SCAN_INTERVAL_TICKS;
        if (!cast) {
            return;
        }

        playMagicCast(world, villager, SoundEvents.EVOKER_CAST_SPELL, 1.25F);
        addMagicXp(villager, 1);
        ValetDebug.record(villager, "combat magic_support heal=" + canHeal + " regen=" + canRegen + " ward=" + canWard);
    }

    private boolean hasActiveCombatTarget() {
        return target != null && target.isAlive() && !target.isRemoved();
    }

    private static boolean needsEffectRefresh(
            LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect
    ) {
        MobEffectInstance current = entity.getEffect(effect);
        return current == null || current.getDuration() <= 20;
    }

    private void playMagicCast(ServerLevel world, Villager villager, SoundEvent sound, float pitch) {
        sendMagicCast(villager);
        villager.swing(InteractionHand.MAIN_HAND, true);
        world.getChunkSource().sendToTrackingPlayersAndSelf(
                villager,
                new ClientboundAnimatePacket(villager, ClientboundAnimatePacket.SWING_MAIN_HAND)
        );
        world.playSound(null, villager.blockPosition(), sound, SoundSource.NEUTRAL, 1.0F, pitch);
    }

    private void shootArrow(ServerLevel world, LivingEntity target) {
        Villager villager = control.villager();
        boolean savedArrow = control.combatCanRecycleArrow() && villager.getRandom().nextFloat() < 0.5F;
        if (!savedArrow && !ValetInventoryTransfer.takeOneItem(villager.getInventory(), Items.ARROW, control.getUsableInventorySlots(villager.getInventory()))) {
            return;
        }

        villager.swing(InteractionHand.MAIN_HAND, true);
        world.getChunkSource().sendToTrackingPlayersAndSelf(
                villager,
                new ClientboundAnimatePacket(villager, ClientboundAnimatePacket.SWING_MAIN_HAND)
        );

        Arrow arrow = new Arrow(world, villager, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(control.combatArrowDamage());

        double dx = target.getX() - villager.getX();
        double dz = target.getZ() - villager.getZ();
        double dy = target.getY(0.5D) - arrow.getY();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontalDistance * 0.2D, dz, 1.6F, 8.0F);

        world.addFreshEntity(arrow);
        world.playSound(null, villager.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        addCombatXp(villager, ValetCombatSkillTree.BOW, BOW_XP_PER_SHOT);
        attackCooldownTicks = control.combatArrowCooldownTicks();
        ValetDebug.record(villager, "combat arrow target=" + ValetDebug.shortPos(target.blockPosition())
                + " arrows=" + countInventoryItem(villager.getInventory(), Items.ARROW, control.getUsableInventorySlots(villager.getInventory()))
                + " saved=" + savedArrow);
    }

    private void applyDefensePerk() {
        if (control.combatHasDefense() && needsEffectRefresh(control.villager(), MobEffects.RESISTANCE)) {
            control.villager().addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, control.combatDefenseAmplifier(), true, false));
        }
    }

    private void chaseTarget(ServerLevel world, LivingEntity target) {
        Villager villager = control.villager();
        if (pathRefreshTicks <= 0 || villager.getNavigation().isDone()) {
            control.moveToward(world, target.blockPosition(), control.combatMoveSpeed(), targetCommanded);
            pathRefreshTicks = 8;
        }
    }

    private void addCombatXp(Villager villager, ValetCombatSkillTree tree, int amount) {
        ValetCombatProgress.addXp(villager, tree, amount);
        ValetDebug.record(villager, "combat xp tree=" + tree.name().toLowerCase()
                + " level=" + ValetCombatProgress.getLevel(villager, tree)
                + " xp=" + ValetCombatProgress.getXp(villager, tree)
                + "/" + ValetCombatProgress.getNextLevelXp(villager, tree));
    }

    private void addMagicXp(Villager villager, int amount) {
        ValetProgress.addXp(villager, amount);
        ValetDebug.record(villager, "combat magic_xp level=" + ValetProgress.getLevel(villager)
                + " xp=" + ValetProgress.getXp(villager)
                + "/" + ValetProgress.getNextLevelXp(villager));
    }

    private boolean hasAllyInArrowPath(ServerLevel world, Villager shooter, LivingEntity target) {
        Vec3 start = new Vec3(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        Vec3 end = new Vec3(target.getX(), target.getY(0.5D), target.getZ());
        AABB searchBox = new AABB(start, end).inflate(0.9D);
        for (Villager ally : world.getEntitiesOfClass(Villager.class, searchBox, ally -> isValetAlly(shooter, ally))) {
            if (ally.getBoundingBox().inflate(0.35D).clip(start, end).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldProtectAllyFromProjectile(Entity owner, Entity hit) {
        return owner instanceof Villager shooter
                && hit instanceof Villager ally
                && hasAllyAwareness(shooter)
                && isValetAlly(shooter, ally);
    }

    private static boolean hasAllyAwareness(Villager villager) {
        return ValetCombatProgress.hasPerk(villager, ValetCombatPerk.ALLY_AWARENESS);
    }

    private static boolean isValetAlly(Villager shooter, Villager ally) {
        return ally != shooter
                && ally.isAlive()
                && !ally.isRemoved()
                && ValetMod.isValet(ally)
                && ValetMod.isValet(shooter);
    }

    private boolean tryRestockArrows(ServerLevel world) {
        Villager villager = control.villager();
        Container inventory = villager.getInventory();
        int usableSlots = control.getUsableInventorySlots(inventory);
        if (countInventoryItem(inventory, Items.ARROW, usableSlots) >= control.combatArrowRestockCount()) {
            return false;
        }

        if (arrowChestPos == null
                || !control.isWithinWorkZone(world, arrowChestPos)
                || !isArrowContainer(world, arrowChestPos)) {
            if (arrowSearchCooldownTicks > 0) {
                return false;
            }

            arrowChestPos = findBestArrowContainer(world);
            arrowSearchCooldownTicks = ARROW_SEARCH_INTERVAL_TICKS;
            if (arrowChestPos == null) {
                arrowSearchCooldownTicks = ARROW_PATH_FAILURE_BACKOFF_TICKS;
                ValetDebug.record(villager, "combat no_arrow_chest");
                return false;
            }
            ValetDebug.record(villager, "combat arrow_chest=" + ValetDebug.shortPos(arrowChestPos));
        }

        ensureBow();
        villager.getLookControl().setLookAt(arrowChestPos.getX() + 0.5D, arrowChestPos.getY() + 0.5D, arrowChestPos.getZ() + 0.5D);
        if (squaredDistance(villager.blockPosition(), arrowChestPos) <= ARROW_CHEST_REACH_SQUARED) {
            int moved = takeArrowsFromContainer(world, arrowChestPos);
            if (moved > 0) {
                control.animateChestUse(world, arrowChestPos);
                ValetDebug.record(villager, "combat took_arrows count=" + moved + " chest=" + ValetDebug.shortPos(arrowChestPos));
            }
            clearArrowChestTarget();
            return moved > 0;
        }

        if (pathRefreshTicks <= 0 || villager.getNavigation().isDone()) {
            boolean moving = control.moveToward(world, arrowChestPos, control.combatMoveSpeed(), false);
            pathRefreshTicks = 8;
            if (!moving) {
                ValetDebug.record(villager, "combat no_arrow_chest_path chest=" + ValetDebug.shortPos(arrowChestPos));
                clearArrowChestTarget();
                arrowSearchCooldownTicks = ARROW_PATH_FAILURE_BACKOFF_TICKS;
                return false;
            }
        }
        return true;
    }

    private int takeArrowsFromContainer(ServerLevel world, BlockPos containerPos) {
        if (world.getBlockState(containerPos).is(ValetMod.INFINITE_ARROW_CHEST)) {
            Container target = control.villager().getInventory();
            int usableSlots = control.getUsableInventorySlots(target);
            int needed = control.combatArrowRestockCount() - countInventoryItem(target, Items.ARROW, usableSlots);
            if (needed <= 0) {
                return 0;
            }

            ItemStack arrows = new ItemStack(Items.ARROW, needed);
            ValetInventoryTransfer.insertStack(target, arrows, usableSlots);
            return needed - arrows.getCount();
        }

        Container source = ValetInventoryTransfer.getContainerInventory(world, containerPos);
        if (source == null) {
            return 0;
        }

        Container target = control.villager().getInventory();
        int usableSlots = control.getUsableInventorySlots(target);
        int needed = control.combatArrowRestockCount() - countInventoryItem(target, Items.ARROW, usableSlots);
        if (needed <= 0) {
            return 0;
        }

        int movedTotal = 0;
        for (int slot = 0; slot < source.getContainerSize() && needed > 0; slot++) {
            ItemStack sourceStack = source.getItem(slot);
            if (!sourceStack.is(Items.ARROW)) {
                continue;
            }

            int amount = Math.min(sourceStack.getCount(), needed);
            ItemStack arrows = new ItemStack(Items.ARROW, amount);
            ValetInventoryTransfer.insertStack(target, arrows, usableSlots);
            int moved = amount - arrows.getCount();
            if (moved <= 0) {
                break;
            }

            sourceStack.shrink(moved);
            if (sourceStack.isEmpty()) {
                source.setItem(slot, ItemStack.EMPTY);
            }
            movedTotal += moved;
            needed -= moved;
        }

        if (movedTotal > 0) {
            source.setChanged();
            target.setChanged();
        }
        return movedTotal;
    }

    private BlockPos findBestArrowContainer(ServerLevel world) {
        Villager villager = control.villager();
        BlockPos nearVillager = findNearestArrowContainer(world, villager.blockPosition());
        BlockPos workOrigin = control.getWorkOrigin(world);
        BlockPos nearWork = workOrigin == null ? null : findNearestArrowContainer(world, workOrigin);
        if (nearVillager == null) {
            return nearWork;
        }
        if (nearWork == null) {
            return nearVillager;
        }
        return squaredDistance(villager.blockPosition(), nearVillager) <= squaredDistance(villager.blockPosition(), nearWork)
                ? nearVillager
                : nearWork;
    }

    private BlockPos findNearestArrowContainer(ServerLevel world, BlockPos origin) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int radius = control.chestRadius();
        for (BlockPos pos : BlockPos.withinManhattan(origin, radius, 4, radius)) {
            BlockPos immutable = pos.immutable();
            if (!control.isWithinWorkZone(world, immutable) || !isArrowContainer(world, immutable)) {
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

    private boolean isArrowContainer(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.is(ValetMod.INFINITE_ARROW_CHEST)) {
            return true;
        }
        if (!blockState.is(Blocks.CHEST) && !blockState.is(Blocks.TRAPPED_CHEST) && !blockState.is(Blocks.BARREL)) {
            return false;
        }

        Container inventory = ValetInventoryTransfer.getContainerInventory(world, pos);
        return inventory != null && ValetInventoryTransfer.inventoryHasItem(inventory, Items.ARROW, inventory.getContainerSize());
    }

    private void clearTarget() {
        target = null;
        targetCommanded = false;
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

    private void clearHeldItem() {
        Villager villager = control.villager();
        if (!villager.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    private void sendMagicCast(Villager villager) {
        ValetMagicCastPayload payload = new ValetMagicCastPayload(villager.getId());
        for (ServerPlayer player : PlayerLookup.tracking(villager)) {
            if (ServerPlayNetworking.canSend(player, ValetMagicCastPayload.TYPE)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private void ensureHeldItem(Item item) {
        Villager villager = control.villager();
        if (villager.getItemBySlot(EquipmentSlot.MAINHAND).is(item)) {
            return;
        }

        villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private boolean hasInventoryItem(Item item) {
        Container inventory = control.villager().getInventory();
        return ValetInventoryTransfer.inventoryHasItem(inventory, item, control.getUsableInventorySlots(inventory));
    }

    private static int countInventoryItem(Container inventory, Item item, int maxSlots) {
        int count = 0;
        int slots = Math.min(inventory.getContainerSize(), Math.max(0, maxSlots));
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
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

    private static String skillSummary(Villager villager, ValetCombatSkillTree tree) {
        return ValetCombatProgress.getLevel(villager, tree)
                + ":" + ValetCombatProgress.getXp(villager, tree)
                + "/" + ValetCombatProgress.getNextLevelXp(villager, tree)
                + "+" + ValetCombatProgress.getPendingPerks(villager, tree);
    }

    public interface Control {
        Villager villager();

        BlockPos getWorkOrigin(ServerLevel world);

        boolean isDefenseEnabled();

        boolean isMagicEnabled();

        double combatSearchRadius();

        double combatChaseRadius();

        double combatAttackRangeSquared();

        double combatRangedAttackRangeSquared();

        double combatMoveSpeed();

        float combatAttackDamage();

        int combatAttackCooldownTicks();

        float combatArrowDamage();

        int combatArrowCooldownTicks();

        double magicAttackRangeSquared();

        int magicAttackCooldownTicks();

        int combatArrowRestockCount();

        boolean combatHasDefense();

        int combatDefenseAmplifier();

        boolean combatCanRecycleArrow();

        int chestRadius();

        boolean isWithinWorkZone(ServerLevel world, BlockPos pos);

        boolean moveToward(ServerLevel world, BlockPos target, double speed, boolean allowOutsideWorkZone);

        int getUsableInventorySlots(Container inventory);

        LivingEntity chooseCommandedTarget(ServerLevel world, LivingEntity currentTarget);

        void animateChestUse(ServerLevel world, BlockPos pos);

        void onCombatStarted(LivingEntity target);

        void onCombatFinished();
    }
}
