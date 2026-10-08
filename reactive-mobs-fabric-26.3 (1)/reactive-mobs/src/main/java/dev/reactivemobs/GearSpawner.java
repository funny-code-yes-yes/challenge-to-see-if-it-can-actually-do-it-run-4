package dev.reactivemobs;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Gives hostile mobs gear on first load. Gear almost never drops. */
final class GearSpawner {
    private GearSpawner() {}

    /** Chance that a mob wears armor at all. */
    static final float ARMOR_CHANCE = 0.55f;
    /** Chance per piece once it's "an armored mob". */
    static final float PIECE_CHANCE = 0.65f;
    /** Chance a melee zombie carries a pickaxe (can dig toward you). */
    static final float PICKAXE_CHANCE = 0.12f;
    static final float SWORD_CHANCE = 0.25f;
    /** Drop chance for any gear we hand out (vanilla default is 0.085). */
    static final float DROP_CHANCE = 0.04f;

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static final Item[][] ARMOR = {
            {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
            {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
            {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
            {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
            {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
    };
    private static final int[] TIER_WEIGHTS = {30, 25, 25, 12, 8};

    private static final Item[] SWORDS = {
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.IRON_AXE};
    private static final Item[] PICKAXES = {
            Items.STONE_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE};

    static void equip(Mob mob) {
        if (!MobUtil.ARMORABLE.contains(mob.getType())) return;
        RandomSource r = mob.getRandom();

        if (r.nextFloat() < ARMOR_CHANCE) {
            int tier = pickTier(r);
            for (int i = 0; i < SLOTS.length; i++) {
                if (r.nextFloat() < PIECE_CHANCE) {
                    // mostly matching set, sometimes a mismatched piece
                    int t = r.nextFloat() < 0.8f ? tier : pickTier(r);
                    give(mob, SLOTS[i], ARMOR[t][i]);
                }
            }
        }

        if (MobUtil.MELEE_ZOMBIES.contains(mob.getType()) && mob.getMainHandItem().isEmpty()) {
            float roll = r.nextFloat();
            if (roll < PICKAXE_CHANCE) {
                give(mob, EquipmentSlot.MAINHAND, PICKAXES[r.nextInt(PICKAXES.length)]);
            } else if (roll < PICKAXE_CHANCE + SWORD_CHANCE) {
                give(mob, EquipmentSlot.MAINHAND, SWORDS[r.nextInt(SWORDS.length)]);
            }
        }
    }

    private static void give(Mob mob, EquipmentSlot slot, Item item) {
        if (!mob.getItemBySlot(slot).isEmpty()) return; // never overwrite vanilla gear
        mob.setItemSlot(slot, new ItemStack(item));
        mob.setDropChance(slot, DROP_CHANCE);
    }

    private static int pickTier(RandomSource r) {
        int total = 0;
        for (int w : TIER_WEIGHTS) total += w;
        int roll = r.nextInt(total);
        for (int i = 0; i < TIER_WEIGHTS.length; i++) {
            roll -= TIER_WEIGHTS[i];
            if (roll < 0) return i;
        }
        return 0;
    }
}
