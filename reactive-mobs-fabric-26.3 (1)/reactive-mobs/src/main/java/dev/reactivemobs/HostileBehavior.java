package dev.reactivemobs;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Pack behaviour, aggro sharing, retreat at half a heart, and loot grabbing. */
final class HostileBehavior {
    private HostileBehavior() {}

    /** Half a heart. */
    static final float FLEE_HP = 1.0f;
    static final double ALERT_RADIUS = 20.0;
    /** Bow users that back off when you get in their face. */
    static final Set<EntityType<?>> KITERS = Set.<EntityType<?>>of(
            EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED);
    static final double KITE_DISTANCE = 4.0;

    /** Hurt by a player: nearby hostiles join the fight. */
    static void onHurt(ServerLevel level, Mob victim, LivingEntity attacker) {
        if (!(attacker instanceof Player p) || p.isCreative() || p.isSpectator()) return;
        for (Mob m : level.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(ALERT_RADIUS),
                MobUtil::isCommonHostile)) {
            if (m != victim && m.getTarget() == null && m.getHealth() > FLEE_HP) {
                m.setTarget(p);
            }
        }
    }

    static void tick(ServerLevel level, Mob mob, List<Mob> hostiles, List<ItemEntity> loot) {
        mob.setCanPickUpLoot(true); // vanilla then equips better armor/weapons it picks up

        if (mob.getMaxHealth() > FLEE_HP && mob.getHealth() <= FLEE_HP) {
            flee(level, mob, hostiles);
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target != null) {
            kite(mob, target);
            return;
        }
        if (mob.tickCount % 40 >= 5) return; // once per ~2s

        // 0) burning in daylight? run for shade
        if (MobUtil.ARMORABLE.contains(mob.getType()) && mob.isOnFire()
                && level.canSeeSky(mob.blockPosition())) {
            for (int i = 0; i < 8; i++) {
                BlockPos p = mob.blockPosition().offset(
                        mob.getRandom().nextInt(21) - 10, 0, mob.getRandom().nextInt(21) - 10);
                if (!level.canSeeSky(p)) {
                    mob.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.2);
                    return;
                }
            }
        }

        // 1) walk to dropped armor/weapons (your death pile!)
        if (MobUtil.ARMORABLE.contains(mob.getType())) {
            ItemEntity best = null;
            double bd = 16.0 * 16.0;
            for (ItemEntity i : loot) {
                double d = i.distanceToSqr(mob);
                if (d < bd) { bd = d; best = i; }
            }
            if (best != null) {
                mob.getNavigation().moveTo(best.getX(), best.getY(), best.getZ(), 1.0);
                return;
            }
        }

        // 2) group up with the nearest healthy hostile
        Mob ally = nearestHealthyAlly(mob, hostiles, 24.0);
        if (ally != null && ally.distanceToSqr(mob) > 6.0 * 6.0) {
            mob.getNavigation().moveTo(ally.getX(), ally.getY(), ally.getZ(), 0.9);
        }
    }

    /** Skeletons try to keep some distance, once a second. Not fast enough to be annoying. */
    private static void kite(Mob mob, LivingEntity target) {
        if (!KITERS.contains(mob.getType()) || mob.tickCount % 20 >= 5) return;
        if (!mob.getMainHandItem().is(Items.BOW)) return;
        if (mob.distanceToSqr(target) >= KITE_DISTANCE * KITE_DISTANCE) return;
        Vec3 d = MobUtil.awayFrom(mob, target.position(), 6.0);
        mob.getNavigation().moveTo(d.x, d.y, d.z, 1.1);
    }

    private static void flee(ServerLevel level, Mob mob, List<Mob> hostiles) {
        Player threat = level.getNearestPlayer(mob, 24.0);
        mob.setTarget(null);
        mob.setAggressive(false);
        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 1, false, false));

        Vec3 dest = null;
        Mob refuge = nearestHealthyAlly(mob, hostiles, 24.0);
        if (refuge != null && refuge.distanceToSqr(mob) > 4.0 * 4.0) {
            dest = refuge.position();            // run to the pack
        } else if (threat != null) {
            dest = MobUtil.awayFrom(mob, threat.position(), 14.0); // or just run
        }
        if (dest != null) mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.3);

        // the pack covers the retreat
        if (threat != null && !threat.isCreative() && !threat.isSpectator()) {
            for (Mob a : hostiles) {
                if (a != mob && a.getHealth() > FLEE_HP && a.getTarget() == null
                        && a.distanceToSqr(mob) < 16.0 * 16.0) {
                    a.setTarget(threat);
                }
            }
        }
    }

    private static Mob nearestHealthyAlly(Mob self, List<Mob> hostiles, double range) {
        Mob best = null;
        double bd = range * range;
        for (Mob m : hostiles) {
            if (m == self || m.getHealth() <= FLEE_HP) continue;
            double d = m.distanceToSqr(self);
            if (d < bd) { bd = d; best = m; }
        }
        return best;
    }
}
