package dev.reactivemobs;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

/** Piglins and zombified piglins stand down if you hold/wear gold (unless you hit them first). */
final class PiglinBehavior {
    private PiglinBehavior() {}

    /** How long (ticks) a player stays "provoking" after hitting the piglin. */
    static final int PROVOKE_TICKS = 200;

    static void tick(Mob mob) {
        Optional<LivingEntity> brainTarget = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        LivingEntity t = brainTarget.orElse(mob.getTarget());
        if (!(t instanceof Player p)) return;
        if (!MobUtil.holdsOrWearsGold(p)) return;
        if (provoked(mob, p)) return;

        mob.setTarget(null);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        if (mob instanceof NeutralMob n) n.stopBeingAngry();
    }

    private static boolean provoked(Mob mob, Player p) {
        return p.equals(mob.getLastHurtByMob())
                && mob.tickCount - mob.getLastHurtByMobTimestamp() < PROVOKE_TICKS;
    }
}
