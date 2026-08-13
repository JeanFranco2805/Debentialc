package org.debentialc.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collection;

public class ServerUtil {

    private static Method getOnlinePlayersMethod;
    private static Method getHealthMethod;
    private static Method getMaxHealthMethod;

    private static Method findMethodNoArgs(Class<?> clazz, String name) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == 0) {
                return method;
            }
        }
        return null;
    }

    public static Player[] getOnlinePlayers() {
        try {
            if (getOnlinePlayersMethod == null) {
                getOnlinePlayersMethod = findMethodNoArgs(Bukkit.getServer().getClass(), "getOnlinePlayers");
            }
            if (getOnlinePlayersMethod == null) {
                return new Player[0];
            }
            Object result = getOnlinePlayersMethod.invoke(Bukkit.getServer());
            if (result instanceof Player[]) {
                return (Player[]) result;
            }
            if (result instanceof Collection) {
                Collection<?> collection = (Collection<?>) result;
                return collection.toArray(new Player[0]);
            }
            return new Player[0];
        } catch (Exception e) {
            e.printStackTrace();
            return new Player[0];
        }
    }

    public static double getHealth(Player player) {
        try {
            if (getHealthMethod == null) {
                getHealthMethod = findMethodNoArgs(Player.class, "getHealth");
            }
            if (getHealthMethod == null) {
                return 0;
            }
            Object result = getHealthMethod.invoke(player);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static double getMaxHealth(Player player) {
        try {
            if (getMaxHealthMethod == null) {
                getMaxHealthMethod = findMethodNoArgs(Player.class, "getMaxHealth");
            }
            if (getMaxHealthMethod == null) {
                return 20;
            }
            Object result = getMaxHealthMethod.invoke(player);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
            return 20;
        } catch (Exception e) {
            e.printStackTrace();
            return 20;
        }
    }
}
