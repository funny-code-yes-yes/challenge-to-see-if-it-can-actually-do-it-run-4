# Reactive Mobs (Fabric, Minecraft 26.3)

Mobs that actually react to what you do.

| Feature | Behaviour |
|---|---|
| Cows (and mooshrooms) | Hit one and it charges you, 1 heart per hit with knockback. Gives up after ~10s or if you get away. |
| Passive animals | Sheep, pigs, chickens, rabbits, goats, horses, donkeys, mules, camels within 16 blocks all flee the attacker. |
| Hostile packs | Idle hostiles drift toward each other; hitting one alerts everything within 20 blocks. |
| Retreat | At half a heart (1 HP) a mob drops its target, speeds up and runs to the nearest healthy ally (or away). The pack covers the retreat. |
| Witches | Much more common (spawn weight 18, groups 1-3). Heal wounded hostiles, buff ones in combat, and trail the pack. |
| Piglins / zombified piglins | Won't attack while you hold or wear anything gold, unless you hit them first. |
| Loot | Common hostiles pick up items and equip better gear; they also walk to dropped armor/weapons (like your death pile). |
| Gear | ~55% spawn armored (leather to diamond), zombies may carry swords/axes or pickaxes. Drop chance is 4% (vanilla 8.5%). |
| Skittish animals | After an attack, witnesses stay wary for 2 minutes and bolt if you come within 7 blocks. Sneaking calms them. |
| Skeleton kiting | Bow skeletons step back about once a second when you close within 4 blocks. |
| Shade seeking | Undead that are burning in daylight run for a spot without sky access. |
| Miner zombies | Zombies/husks/zombie villagers with a pickaxe dig through blocks toward you. Respects mobGriefing. |

Tuning constants live at the top of each class (`GearSpawner`, `PassiveBehavior`, `HostileBehavior`, `WitchBehavior`).

## Build
**Easiest:** push this folder to a GitHub repo. The included workflow (`.github/workflows/build.yml`) builds the jar for you; download it from the Actions tab > Artifacts.

**Locally:**
Requires JDK 25.
1. Copy `gradlew`, `gradlew.bat`, and the `gradle/` folder from the official Fabric example mod
   (or run `gradle wrapper --gradle-version 9.2` if you have Gradle installed).
2. `./gradlew build`
3. Jar is at `build/libs/reactive-mobs-1.0.0.jar`. Put it in `mods/` with Fabric API 0.160.5+26.3.
