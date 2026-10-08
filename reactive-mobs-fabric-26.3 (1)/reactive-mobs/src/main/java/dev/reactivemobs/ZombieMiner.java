package dev.reactivemobs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;

/** Zombies holding a pickaxe dig through blocks that stand between them and their target. */
final class ZombieMiner {
    private ZombieMiner() {}

    static final float MAX_HARDNESS = 10.0f; // skips obsidian & co.
    static final double MAX_RANGE = 24.0;

    private static final class State {
        BlockPos pos; int progress; int stuck;
    }
    private static final Map<UUID, State> STATES = new HashMap<>();

    static void forget(UUID id) { STATES.remove(id); }

    static void tick(ServerLevel level, Mob mob) {
        if (!MobUtil.MELEE_ZOMBIES.contains(mob.getType())) return;
        if (!mob.getMainHandItem().is(ItemTags.PICKAXES)) return;

        State st = STATES.computeIfAbsent(mob.getUUID(), k -> new State());
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) { reset(level, mob, st); return; }

        double d2 = mob.distanceToSqr(target);
        if (d2 < 4.0 || d2 > MAX_RANGE * MAX_RANGE) { reset(level, mob, st); return; }

        boolean stuck = mob.getNavigation().isDone() || mob.horizontalCollision;
        if (!stuck) { st.stuck = 0; return; }
        if (++st.stuck < 3) return;                 // ~15 ticks of being blocked
        if (!GriefCheck.allowed(level)) return;

        double dx = target.getX() - mob.getX();
        double dy = target.getY() - mob.getY();
        double dz = target.getZ() - mob.getZ();
        Direction dir = Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);

        BlockPos base = mob.blockPosition();
        BlockPos[] candidates = {
                base.above().relative(dir),   // head height, in front
                base.relative(dir),           // feet height, in front
                dy > 1.5 ? base.above(2) : null,   // dig up
                dy < -1.5 ? base.below() : null    // dig down
        };

        BlockPos chosen = null;
        float hardness = 0;
        for (BlockPos c : candidates) {
            if (c == null) continue;
            BlockState s = level.getBlockState(c);
            if (s.isAir() || !s.getFluidState().isEmpty()) continue;
            if (s.getCollisionShape(level, c).isEmpty()) continue;
            float h = s.getDestroySpeed(level, c);
            if (h < 0 || h > MAX_HARDNESS) continue;
            chosen = c; hardness = h;
            break;
        }
        if (chosen == null) return;

        if (!chosen.equals(st.pos)) {
            if (st.pos != null) level.destroyBlockProgress(mob.getId(), st.pos, -1);
            st.pos = chosen;
            st.progress = 0;
        }

        st.progress++;
        int needed = Math.max(2, Math.round(hardness * 4.0f));
        mob.getLookControl().setLookAt(chosen.getX() + 0.5, chosen.getY() + 0.5, chosen.getZ() + 0.5);
        mob.swing(InteractionHand.MAIN_HAND);

        if (st.progress >= needed) {
            level.destroyBlockProgress(mob.getId(), chosen, -1);
            level.destroyBlock(chosen, true, mob);
            st.pos = null;
            st.progress = 0;
            st.stuck = 0;
        } else {
            level.destroyBlockProgress(mob.getId(), chosen, Math.min(9, st.progress * 10 / needed));
        }
    }

    private static void reset(ServerLevel level, Mob mob, State st) {
        if (st.pos != null) level.destroyBlockProgress(mob.getId(), st.pos, -1);
        st.pos = null;
        st.progress = 0;
        st.stuck = 0;
    }
}
