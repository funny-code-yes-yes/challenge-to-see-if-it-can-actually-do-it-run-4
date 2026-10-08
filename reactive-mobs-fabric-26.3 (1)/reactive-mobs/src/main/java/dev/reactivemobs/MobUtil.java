package dev.reactivemobs;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Shared predicates/helpers. Uses EntityType sets so we don't depend on mob class packages. */
public final class MobUtil {
    private MobUtil() {}

    /** Passive animals that panic when a neighbour gets attacked. */
    public static final Set<EntityType<?>> PASSIVE = Set.<EntityType<?>>of(
            EntityType.COW, EntityType.MOOSHROOM, EntityType.SHEEP, EntityType.PIG,
            EntityType.CHICKEN, EntityType.RABBIT, EntityType.GOAT, EntityType.HORSE,
            EntityType.DONKEY, EntityType.MULE, EntityType.CAMEL);

    /** "Common hostile mobs". */
    public static final Set<EntityType<?>> HOSTILE = Set.<EntityType<?>>of(
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED,
            EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.CREEPER);

    /** Hostiles that can wear armor / use gear. */
    public static final Set<EntityType<?>> ARMORABLE = Set.<EntityType<?>>of(
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED);

    /** Zombies that may spawn with melee tools (and pickaxes). */
    public static final Set<EntityType<?>> MELEE_ZOMBIES = Set.<EntityType<?>>of(
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.ZOMBIE_VILLAGER);

    public static boolean isPassive(Mob m) { return PASSIVE.contains(m.getType()); }
    public static boolean isCommonHostile(Mob m) { return HOSTILE.contains(m.getType()); }
    public static boolean isCow(Mob m) {
        return m.getType() == EntityType.COW || m.getType() == EntityType.MOOSHROOM;
    }

    public static String itemPath(ItemStack s) {
        return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
    }

    public static boolean isGold(ItemStack s) {
        return !s.isEmpty() && itemPath(s).contains("gold");
    }

    /** True if the player holds any gold item/tool or wears any gold armor piece. */
    public static boolean holdsOrWearsGold(Player p) {
        if (isGold(p.getMainHandItem()) || isGold(p.getOffhandItem())) return true;
        return isGold(p.getItemBySlot(EquipmentSlot.HEAD))
                || isGold(p.getItemBySlot(EquipmentSlot.CHEST))
                || isGold(p.getItemBySlot(EquipmentSlot.LEGS))
                || isGold(p.getItemBySlot(EquipmentSlot.FEET));
    }

    /** Armor/weapons that hostile mobs will walk over to grab. */
    public static boolean isWantedLoot(ItemStack s) {
        if (s.isEmpty()) return false;
        String p = itemPath(s);
        return p.endsWith("_helmet") || p.endsWith("_chestplate") || p.endsWith("_leggings")
                || p.endsWith("_boots") || p.endsWith("_sword") || p.endsWith("_axe")
                || p.endsWith("_pickaxe") || p.equals("bow");
    }

    /** A point roughly {@code dist} blocks away from {@code threat}, with a little angle jitter. */
    public static Vec3 awayFrom(Mob mob, Vec3 threat, double dist) {
        Vec3 d = mob.position().subtract(threat);
        double x = d.x, z = d.z;
        if (x * x + z * z < 1.0E-4) {
            x = mob.getRandom().nextDouble() - 0.5;
            z = mob.getRandom().nextDouble() - 0.5;
        }
        double len = Math.sqrt(x * x + z * z);
        x /= len; z /= len;
        double a = (mob.getRandom().nextDouble() - 0.5) * 1.0; // +-0.5 rad
        double c = Math.cos(a), s = Math.sin(a);
        double nx = x * c - z * s, nz = x * s + z * c;
        return mob.position().add(nx * dist, 0, nz * dist);
    }
}
