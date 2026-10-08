package dev.reactivemobs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Cows fight back (lightly); every other passive animal nearby flees the attacker. */
final class PassiveBehavior {
    private PassiveBehavior() {}

    static final float COW_DAMAGE = 2.0f;        // 1 heart
    static final double PANIC_RADIUS = 16.0;
    static final long FLEE_TICKS = 160;
    static final long REVENGE_TICKS = 200;

    private record Flee(UUID player, long until) {}
    private static final class Revenge {
        final UUID player; long until; long nextHit;
        Revenge(UUID p, long u, long n) { player = p; until = u; nextHit = n; }
    }

    private static final Map<UUID, Flee> FLEE = new HashMap<>();
    private static final Map<UUID, Revenge> REVENGE = new HashMap<>();
    /** Animals that saw an attack stay skittish around that player for a while (sneak to calm them). */
    private static final Map<UUID, Flee> WARY = new HashMap<>();

    static final long WARY_TICKS = 2400;      // 2 minutes
    static final double WARY_DISTANCE = 7.0;

    static void forget(UUID id) { FLEE.remove(id); REVENGE.remove(id); WARY.remove(id); }

    static void onHurt(ServerLevel level, Mob victim, Player attacker) {
        long now = level.getGameTime();
        boolean cow = MobUtil.isCow(victim);
        if (cow) {
            Revenge existing = REVENGE.get(victim.getUUID());
            long nextHit = existing != null ? existing.nextHit : now + 10;
            REVENGE.put(victim.getUUID(), new Revenge(attacker.getUUID(), now + REVENGE_TICKS, nextHit));
            FLEE.remove(victim.getUUID());
        }
        for (Mob m : level.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(PANIC_RADIUS),
                MobUtil::isPassive)) {
            if (REVENGE.containsKey(m.getUUID())) continue; // fighting cows stay
            FLEE.put(m.getUUID(), new Flee(attacker.getUUID(), now + FLEE_TICKS));
            if (!MobUtil.isCow(m)) WARY.put(m.getUUID(), new Flee(attacker.getUUID(), now + WARY_TICKS));
        }
    }

    static void tick(ServerLevel level, Mob mob, long now) {
        UUID id = mob.getUUID();
        Revenge r = REVENGE.get(id);
        if (r != null) { tickRevenge(level, mob, r, now); return; }
        Flee f = FLEE.get(id);
        if (f != null) { tickFlee(level, mob, f, now); return; }
        tickWary(level, mob, now);
    }

    private static void tickWary(ServerLevel level, Mob mob, long now) {
        Flee w = WARY.get(mob.getUUID());
        if (w == null) return;
        Player p = level.getPlayerByUUID(w.player());
        if (p == null || now > w.until()) { WARY.remove(mob.getUUID()); return; }
        if (p.isCrouching() || p.isCreative() || p.isSpectator()) return;
        if (mob.distanceToSqr(p) < WARY_DISTANCE * WARY_DISTANCE) {
            FLEE.put(mob.getUUID(), new Flee(p.getUUID(), now + 60));
        }
    }

    private static void tickRevenge(ServerLevel level, Mob cow, Revenge r, long now) {
        Player p = level.getPlayerByUUID(r.player);
        if (p == null || !p.isAlive() || p.isSpectator() || p.isCreative() || now > r.until
                || cow.distanceToSqr(p) > 24 * 24) {
            REVENGE.remove(cow.getUUID());
            return;
        }
        cow.getLookControl().setLookAt(p, 30.0f, 30.0f);
        if (cow.distanceToSqr(p) <= 2.5 * 2.5) {
            if (now >= r.nextHit) {
                r.nextHit = now + 20;
                cow.swing(InteractionHand.MAIN_HAND);
                p.hurtServer(level, level.damageSources().mobAttack(cow), COW_DAMAGE);
                p.knockback(0.4, cow.getX() - p.getX(), cow.getZ() - p.getZ());
            }
        } else {
            cow.getNavigation().moveTo(p, 1.4);
        }
    }

    private static void tickFlee(ServerLevel level, Mob mob, Flee f, long now) {
        Player p = level.getPlayerByUUID(f.player());
        if (p == null || now > f.until() || mob.distanceToSqr(p) > 28 * 28) {
            FLEE.remove(mob.getUUID());
            return;
        }
        if (mob.getNavigation().isDone() || mob.tickCount % 20 < 5) {
            Vec3 d = MobUtil.awayFrom(mob, p.position(), 14.0);
            mob.getNavigation().moveTo(d.x, d.y, d.z, 1.5);
        }
    }
}
