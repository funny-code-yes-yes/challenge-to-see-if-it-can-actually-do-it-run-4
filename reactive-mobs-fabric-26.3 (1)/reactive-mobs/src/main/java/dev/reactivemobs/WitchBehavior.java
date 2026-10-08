package dev.reactivemobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Witches support hostile mobs: heal the wounded, buff the ones in combat.
 * Healing is applied directly (with a "thrown potion" particle trail) because a real
 * Instant Health potion would HURT undead mobs.
 */
final class WitchBehavior {
    private WitchBehavior() {}

    static final float HEAL_AMOUNT = 8.0f;
    static final double RANGE = 16.0;
    private static final Map<UUID, Long> NEXT_ACTION = new HashMap<>();

    static void forget(UUID id) { NEXT_ACTION.remove(id); }

    static void tick(ServerLevel level, Mob witch, List<Mob> hostiles, long now) {
        Long next = NEXT_ACTION.get(witch.getUUID());
        if (next != null && now < next) return;

        // 1) heal the most wounded ally in sight
        Mob patient = null;
        float worst = 0.75f;
        for (Mob m : hostiles) {
            if (m.distanceToSqr(witch) > RANGE * RANGE || !witch.hasLineOfSight(m)) continue;
            float ratio = m.getHealth() / m.getMaxHealth();
            if (ratio < worst) { worst = ratio; patient = m; }
        }
        if (patient != null) {
            throwFx(level, witch, patient);
            patient.heal(HEAL_AMOUNT);
            NEXT_ACTION.put(witch.getUUID(), now + 50);
            return;
        }

        // 2) buff an ally that is fighting
        Mob fighter = null;
        double bd = RANGE * RANGE;
        for (Mob m : hostiles) {
            if (m.getTarget() == null || m.hasEffect(MobEffects.REGENERATION)) continue;
            double d = m.distanceToSqr(witch);
            if (d < bd && witch.hasLineOfSight(m)) { bd = d; fighter = m; }
        }
        if (fighter != null) {
            throwFx(level, witch, fighter);
            fighter.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            fighter.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0));
            NEXT_ACTION.put(witch.getUUID(), now + 80);
            return;
        }

        // 3) trail the pack so there's someone to support
        Mob lead = null;
        bd = 24.0 * 24.0;
        for (Mob m : hostiles) {
            if (m.getTarget() == null) continue;
            double d = m.distanceToSqr(witch);
            if (d < bd) { bd = d; lead = m; }
        }
        if (lead != null && bd > 10.0 * 10.0) {
            witch.getNavigation().moveTo(lead.getX(), lead.getY(), lead.getZ(), 1.0);
        }
    }

    private static void throwFx(ServerLevel level, Mob witch, Mob target) {
        Vec3 from = witch.getEyePosition();
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        for (int i = 0; i <= 8; i++) {
            Vec3 p = from.lerp(to, i / 8.0);
            level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, to.x, to.y, to.z, 12, 0.4, 0.5, 0.4, 0.02);
        witch.getLookControl().setLookAt(target, 30.0f, 30.0f);
        witch.swing(InteractionHand.MAIN_HAND);
        witch.playSound(SoundEvents.WITCH_THROW, 1.0f, 1.0f);
    }
}
