package dev.reactivemobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Isolated on purpose: the game rule API changed a lot in recent versions.
 * If this file fails to compile on your exact build, replace the body with `return true;`
 */
final class GriefCheck {
    private GriefCheck() {}

    static boolean allowed(ServerLevel level) {
        return level.getGameRules().get(GameRules.MOB_GRIEFING);
    }
}
