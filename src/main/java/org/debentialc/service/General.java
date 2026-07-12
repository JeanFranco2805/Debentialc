package org.debentialc.service;

import JinRyuu.JRMCore.JRMCoreH;
import kamkeel.npcdbc.constants.DBCRace;
import net.minecraft.entity.player.EntityPlayerMP;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.entity.Player;

import java.util.HashMap;


public class General {


    public static final String[] ranks = {"founder", "developer", "manager", "admin", "moderador", "quester", "helper", "constructor", "programador", "dev"
            , "staff", "moderador-manager", "dueño", "owner", "admin-plus", "ayudante", "helper", "builder", "builder-manager"};

    public static String DEX2 = "jrmcDexI";
    public static String SPI2 = "jrmcCncI";
    public static String CON2 = "jrmcCnsI";
    public static String STR2 = "jrmcStrI";
    public static String WIL2 = "jrmcWilI";
    public static String MND2 = "jrmcIntI";


    public static String DEX = "dexterity";
    public static String SPI = "spirit";
    public static String CON = "constitution";
    public static String STR = "strength";
    public static String WIL = "willpower";
    public static String MND = "mind";

    public static HashMap<String, String> BONUS_STATS = new HashMap<>();
    public static HashMap<String, String> STATS_MAP = new HashMap<>();

    static {
        STATS_MAP.put("STR", STR2);
        STATS_MAP.put("DEX", DEX2);
        STATS_MAP.put("CON", CON2);
        STATS_MAP.put("WIL", WIL2);
        STATS_MAP.put("MND", MND2);
        STATS_MAP.put("SPI", SPI2);
    }
    public static String getRankColorCode ( Player player ) {
        int level = getLVL ( player );
        if (level >= 300 && level <= 1000) {
            return "&8[&fF&8]";
        } else if (level >= 1001 && level <= 3000) {
            return "&8[&fE&8]";
        } else if (level >= 3001 && level <= 6000) {
            return "&8[&fD&8]";
        } else if (level >= 6001 && level <= 10000) {
            return "&8[&2C&8]";
        } else if (level >= 10001 && level <= 14000) {
            return "&8[&2B&8]";
        } else if (level >= 14001 && level <= 18000) {
            return "&8[&aA&8]";
        } else if (level >= 18001 && level <= 28000) {
            return "&8[&aA&c+&8]";
        } else if (level >= 28001 && level <= 38000) {
            return "&8[&5S&8]";
        } else if (level >= 38001 && level <= 50000) {
            return "&8[&5S&c+&8]";
        } else if (level >= 50001 && level <= 70000) {
            return "&8[&cZ&8]";
        } else if (level >= 70001) {
            return "&8[&cZ&4+&8]";
        } else {
            return "&8[?]";
        }
    }
    public static IDBCPlayer getDBCPlayer(String name) {
        return NpcAPI.Instance().getPlayer(name).getDBCPlayer();
    }
    public static String getRace ( Player player ) {
        IDBCPlayer dbcPlayer = NpcAPI.Instance ( ).getPlayer ( player.getName ( ) ).getDBCPlayer ( );
        switch (dbcPlayer.getRace ( )) {
            case DBCRace.HALFSAIYAN:
                return "Semi-Saiyan";
            case DBCRace.ARCOSIAN:
                return "Arcosiano";
            case DBCRace.MAJIN:
                return "Majin";
            case DBCRace.HUMAN:
                return "Humano";
            case DBCRace.SAIYAN:
                return "Saiyan";
            case DBCRace.NAMEKIAN:
                return "Namekiano";
        }
        return "N/A";
    }

    public static int getSTAT(String stat, Player entity) {
        return JRMCoreH.getInt(toPlayerMP(entity), STATS_MAP.get(stat.toUpperCase()));
    }
    public static void setSTAT(String stat, Player entity, int value) {
        JRMCoreH.setInt(value, toPlayerMP(entity), STATS_MAP.get(stat.toUpperCase()));
    }

    public static EntityPlayerMP toPlayerMP(Player player) {
        return (EntityPlayerMP) NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer().getMCEntity();
    }

    static {
        BONUS_STATS.put("STR", STR);
        BONUS_STATS.put("DEX", DEX);
        BONUS_STATS.put("CON", CON);
        BONUS_STATS.put("WIL", WIL);
        BONUS_STATS.put("MND", MND);
        BONUS_STATS.put("SPI", SPI);
    }

    public static int getLVL(Player player) {
        int str = JRMCoreH.getInt(toPlayerMP(player), STR2);
        int dex = JRMCoreH.getInt(toPlayerMP(player), DEX2);
        int con = JRMCoreH.getInt(toPlayerMP(player), CON2);
        int wil = JRMCoreH.getInt(toPlayerMP(player), WIL2);
        int mnd = JRMCoreH.getInt(toPlayerMP(player), MND2);
        int spi = JRMCoreH.getInt(toPlayerMP(player), SPI2);
        int lvl = (str + dex + con + wil + mnd + spi) / 5 - 11;
        return lvl;
    }

    public static void spawnNpc(int x, int y, int z, int tab, String npcname, Player player) {
        IDBCPlayer idbcPlayer = NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer();
        IWorld world = idbcPlayer.getWorld();
        ICustomNpc<?> npc = (ICustomNpc<?>) world.spawnClone(x, y, z, tab, npcname);
    }



}
