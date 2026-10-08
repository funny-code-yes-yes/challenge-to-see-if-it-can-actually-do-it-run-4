package dev.reactivemobs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class ReactiveMobs implements ModInitializer {
    public static final String MOD_ID = "reactivemobs";
    /** Entity tag so gear is only handed out once per mob. */
    public static final String INIT_TAG = "reactivemobs_init";
    /** Mobs within this many blocks of a player get processed. */
    public static final double ACTIVE_RADIUS = 48.0;

    @Override
    public void onInitialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> onLoad(entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            var id = entity.getUUID();
            PassiveBehavior.forget(id);
            WitchBehavior.forget(id);
            ZombieMiner.forget(id);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(ReactiveMobs::onDamage);
        ServerTickEvents.END_SERVER_TICK.register(ReactiveMobs::onTick);

        // More witches, everywhere in the overworld.
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
                MobCategory.MONSTER, EntityType.WITCH, 18, 1, 3);
    }

    private static void onLoad(Entity entity) {
        if (!(entity instanceof Mob mob)) return;
        if (!MobUtil.isCommonHostile(mob)) return;
        mob.setCanPickUpLoot(true);
        if (!mob.getTags().contains(INIT_TAG)) {
            mob.addTag(INIT_TAG);
            GearSpawner.equip(mob);
        }
    }

    private static void onDamage(LivingEntity entity, DamageSource source, float base, float taken, boolean blocked) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!(entity instanceof Mob mob)) return;
        Entity attacker = source.getEntity();
        if (attacker == null) return;

        if (MobUtil.isPassive(mob) && attacker instanceof Player player) {
            PassiveBehavior.onHurt(level, mob, player);
        } else if (MobUtil.isCommonHostile(mob) && attacker instanceof LivingEntity living) {
            HostileBehavior.onHurt(level, mob, living);
        }
    }

    private static void onTick(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0) return; // controller runs 4x/second

        for (ServerLevel level : server.getAllLevels()) {
            List<ServerPlayer> players = level.players();
            if (players.isEmpty()) continue;

            Set<Mob> mobs = new LinkedHashSet<>();
            Set<ItemEntity> loot = new LinkedHashSet<>();
            for (ServerPlayer p : players) {
                if (p.isSpectator()) continue;
                AABB box = p.getBoundingBox().inflate(ACTIVE_RADIUS);
                mobs.addAll(level.getEntitiesOfClass(Mob.class, box));
                loot.addAll(level.getEntitiesOfClass(ItemEntity.class, box, i -> MobUtil.isWantedLoot(i.getItem())));
            }

            List<Mob> hostiles = new ArrayList<>();
            for (Mob m : mobs) {
                if (m.isAlive() && MobUtil.isCommonHostile(m)) hostiles.add(m);
            }
            List<ItemEntity> lootList = new ArrayList<>(loot);
            long now = level.getGameTime();

            for (Mob m : mobs) {
                if (!m.isAlive()) continue;
                EntityType<?> type = m.getType();
                if (MobUtil.isPassive(m)) {
                    PassiveBehavior.tick(level, m, now);
                } else if (MobUtil.isCommonHostile(m)) {
                    HostileBehavior.tick(level, m, hostiles, lootList);
                    ZombieMiner.tick(level, m);
                } else if (type == EntityType.WITCH) {
                    WitchBehavior.tick(level, m, hostiles, now);
                } else if (type == EntityType.PIGLIN || type == EntityType.ZOMBIFIED_PIGLIN) {
                    PiglinBehavior.tick(m);
                }
            }
        }
    }
}
