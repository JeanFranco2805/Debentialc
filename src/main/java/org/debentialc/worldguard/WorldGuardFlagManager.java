package org.debentialc.worldguard;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.debentialc.Main;

import java.lang.reflect.Method;

public class WorldGuardFlagManager {

    public static IntegerFlag ENTRY_LVL_FLAG;
    public static IntegerFlag ENTRY_QUEST_FLAG;

    public static void registerFlags() {
        try {
            WorldGuardPlugin worldGuard = WorldGuardPlugin.inst();
            if (worldGuard == null) {
                Main.instance.getLogger().warning("[WorldGuard] WorldGuardPlugin.inst() es null. No se registraron flags.");
                return;
            }

            FlagRegistry registry = worldGuard.getFlagRegistry();
            if (registry == null) {
                Main.instance.getLogger().warning("[WorldGuard] FlagRegistry es null. No se registraron flags.");
                return;
            }

            ENTRY_LVL_FLAG = new IntegerFlag("entry-lvl");
            ENTRY_QUEST_FLAG = new IntegerFlag("entry-quest");

            registerFlag(registry, ENTRY_LVL_FLAG);
            registerFlag(registry, ENTRY_QUEST_FLAG);

            Main.instance.getLogger().info("[WorldGuard] Flags entry-lvl y entry-quest registradas.");
        } catch (Exception e) {
            Main.instance.getLogger().warning("[WorldGuard] No se pudieron registrar las flags: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void registerFlag(FlagRegistry registry, IntegerFlag flag) {
        try {
            registry.register(flag);
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get(flag.getName());
            if (existing instanceof IntegerFlag) {
                if ("entry-lvl".equals(flag.getName())) {
                    ENTRY_LVL_FLAG = (IntegerFlag) existing;
                } else if ("entry-quest".equals(flag.getName())) {
                    ENTRY_QUEST_FLAG = (IntegerFlag) existing;
                }
                Main.instance.getLogger().info("[WorldGuard] Flag " + flag.getName() + " ya estaba registrada, usando existente.");
            } else {
                Main.instance.getLogger().warning("[WorldGuard] Conflicto al registrar flag " + flag.getName() + ": " + e.getMessage());
            }
        } catch (IllegalStateException e) {
            Main.instance.getLogger().info("[WorldGuard] Registro inicializado, forzando registro de " + flag.getName() + "...");
            tryForceRegister(registry, flag);
        }
    }

    private static void tryForceRegister(FlagRegistry registry, IntegerFlag flag) {
        try {
            Method forceRegister = registry.getClass().getDeclaredMethod("forceRegister", Flag.class);
            forceRegister.setAccessible(true);
            forceRegister.invoke(registry, flag);
        } catch (Exception ex) {
            Main.instance.getLogger().warning("[WorldGuard] No se pudo forzar el registro de " + flag.getName() + ": " + ex.getMessage());
        }
    }
}
