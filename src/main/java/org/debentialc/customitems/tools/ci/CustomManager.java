package org.debentialc.customitems.tools.ci;

import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.debentialc.Main;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.service.CC;
import org.debentialc.service.General;
import org.debentialc.service.ServerUtil;
import org.debentialc.customitems.tools.stats.StatsCalculator;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.debentialc.customitems.events.CustomArmor.playerArmorBonus;

public class CustomManager {

    public static void applyArmorBonus(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack armor : inventory.getArmorContents()) {
            RegisterItem item = new RegisterItem();
            if (armor == null) continue;
            if (armor.getTypeId() == Material.AIR.getId()) continue;
            CustomArmor ci = item.toItemCustom(armor);
            if (ci != null) {
                applyBonusToPlayer(player, ci.getId(), ci.getValueByStat(), ci.getOperation());
            }
        }
    }

    public static void applyHandItemBonus(Player player) {
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand == null || itemInHand.getTypeId() == Material.AIR.getId()) {
            return;
        }

        CustomItemCommand itemCmd = new CustomItemCommand();
        CustomItem ci = itemCmd.toItemCustom(itemInHand);

        if (ci != null && ci.isActive()) {
            applyBonusToPlayer(player, ci.getId(), ci.getValueByStat(), ci.getOperation());
        }
    }

    private static void applyBonusToPlayer(Player player, String itemId,
                                           HashMap<String, Double> stats,
                                           HashMap<String, String> operations) {
        try {
            IDBCPlayer idbcPlayer = General.getDBCPlayer(player.getName());

            stats.forEach((k, v) -> {
                String operation = operations.get(k);
                Set<String> bonuses = (!playerArmorBonus.containsKey(player.getUniqueId())) ?
                        new HashSet<>() : playerArmorBonus.get(player.getUniqueId());

                bonuses.add(itemId);
                playerArmorBonus.put(player.getUniqueId(), bonuses);

                try {
                    idbcPlayer.addBonusAttribute(General.BONUS_STATS.get(k.toUpperCase()), itemId,
                            operation, v);
                } catch (NullPointerException ignored) {
                }
            });
        } catch (Exception e) {
        }
    }

    public static void removeBonusFromPlayer(Player player, String itemId) {
        try {
            IDBCPlayer idbcPlayer = General.getDBCPlayer(player.getName());

            for (String stat : General.BONUS_STATS.values()) {
                try {
                    idbcPlayer.removeBonusAttribute(stat, itemId);
                } catch (Exception ignored) {
                }
            }

            if (playerArmorBonus.containsKey(player.getUniqueId())) {
                Set<String> bonuses = playerArmorBonus.get(player.getUniqueId());
                bonuses.remove(itemId);
                playerArmorBonus.put(player.getUniqueId(), bonuses);
            }
        } catch (Exception e) {
        }
    }

    public static void effectsTask() {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                Player[] players = ServerUtil.getOnlinePlayers();
                for (Player onlinePlayer : players) {
                    for (ItemStack armorContent : onlinePlayer.getInventory().getArmorContents()) {
                        if (armorContent == null || armorContent.getTypeId() == Material.AIR.getId()) continue;

                        RegisterItem registerItem = new RegisterItem();
                        CustomArmor ca = registerItem.toItemCustom(armorContent);
                        if (ca == null) continue;

                        applyEffects(onlinePlayer, ca.getEffects());
                    }

                    ItemStack itemInHand = onlinePlayer.getItemInHand();
                    if (itemInHand != null && itemInHand.getTypeId() != Material.AIR.getId()) {
                        CustomItemCommand itemCmd = new CustomItemCommand();
                        CustomItem ci = itemCmd.toItemCustom(itemInHand);
                        if (ci != null && ci.isActive()) {
                            applyEffects(onlinePlayer, ci.getEffects());
                        }
                    }
                }
            }
        };
        runnable.runTaskTimer(Main.instance, 1, 1);
    }


    public static void applyEffectToItemFromChat(org.bukkit.entity.Player player, String itemId,
                                                 String effectType, double value) {
        if (!CustomItemCommand.items.containsKey(itemId)) {
            player.sendMessage(CC.translate("&c✗ Item no encontrado"));
            return;
        }

        org.debentialc.customitems.tools.ci.CustomItem item = CustomItemCommand.items.get(itemId);
        item.getEffects().put(effectType, value);

        org.debentialc.customitems.tools.storage.CustomItemStorage.getInstance().saveItem(item);
    }

    public static void applyEffectToArmorFromChat(org.bukkit.entity.Player player, String armorId,
                                                  String effectType, double value) {
        if (!RegisterItem.items.containsKey(armorId)) {
            player.sendMessage(CC.translate("&c✗ Armadura no encontrada"));
            return;
        }

        org.debentialc.customitems.tools.ci.CustomArmor armor = RegisterItem.items.get(armorId);
        armor.getEffects().put(effectType, value);

        org.debentialc.customitems.tools.storage.CustomArmorStorage storage = org.debentialc.customitems.tools.storage.CustomArmorStorage.getInstance();
        storage.saveArmor(armor);

        RegisterItem.items.put(armorId, armor);
    }

    public static void applyBonusToItemFromChat(org.bukkit.entity.Player player, String itemId,
                                                String stat, String operation, double value) {
        if (!CustomItemCommand.items.containsKey(itemId)) {
            player.sendMessage(CC.translate("&c✗ Item no encontrado"));
            return;
        }

        org.debentialc.customitems.tools.ci.CustomItem item = CustomItemCommand.items.get(itemId);
        item.setOperation(operation, stat).setBonusStat(stat, value);

        org.debentialc.customitems.tools.storage.CustomItemStorage.getInstance().saveItem(item);

        applyHandItemBonus(player);
    }

    public static void applyBonusToArmorFromChat(org.bukkit.entity.Player player, String armorId,
                                                 String stat, String operation, double value) {
        if (!RegisterItem.items.containsKey(armorId)) {
            player.sendMessage(CC.translate("&c✗ Armadura no encontrada"));
            return;
        }

        org.debentialc.customitems.tools.ci.CustomArmor armor = RegisterItem.items.get(armorId);
        armor.setOperation(operation, stat).setBonusStat(stat, value);

        org.debentialc.customitems.tools.storage.CustomArmorStorage storage = org.debentialc.customitems.tools.storage.CustomArmorStorage.getInstance();
        storage.saveArmor(armor);

        RegisterItem.items.put(armorId, armor);

        applyArmorBonus(player);
    }
    static AtomicInteger counter = new AtomicInteger(0);

    private static void applyEffects(Player player, HashMap<String, Double> effects) {
        try {
            IDBCPlayer idbcPlayer = General.getDBCPlayer(player.getName());

            effects.forEach((k, v) -> {
                if (counter.incrementAndGet() % 5 != 0) return;

                try {
                    if (k.equalsIgnoreCase("HEALTHREGEN")) {
                        int max = StatsCalculator.getMaxHealth(idbcPlayer);
                        int bonus = (int) (v * max);
                        idbcPlayer.setHP(idbcPlayer.getHP() + bonus);
                    }
                    if (k.equalsIgnoreCase("KIREGEN")) {
                        int max = StatsCalculator.getKiMax(idbcPlayer);
                        int bonus = (int) (v * max);
                        idbcPlayer.setKi(idbcPlayer.getKi() + bonus);
                    }
                    if (k.equalsIgnoreCase("STAMINAREGEN")) {
                        int bonus = (int) (v * idbcPlayer.getStamina());
                        idbcPlayer.setStamina(idbcPlayer.getStamina() + bonus);
                    }
                    if (k.equalsIgnoreCase("KIREGEN"))
                        EffectsManager.spawnHologram(player, CC.translate("&9⚡"), 1.5, -1.0);
                    else if (k.equalsIgnoreCase("STAMINAREGEN"))
                        EffectsManager.spawnHologram(player, CC.translate("&e❃"), 0.4, 1.5);
                    else if (k.equalsIgnoreCase("HEALTHREGEN"))
                        EffectsManager.spawnHologram(player, CC.translate("&c❤"), 1.0, 1.0);
                } catch (Exception ignored) {}
            });

        } catch (Exception e) {
        }
    }

    public static void armorTask() {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                Player[] players = ServerUtil.getOnlinePlayers();
                for (Player player : players) {

                    // BUG 1 FIX: Calcular qué items están actualmente equipados/en mano
                    Set<String> currentItemIds = new HashSet<>();
                    RegisterItem registerItem = new RegisterItem();
                    CustomItemCommand itemCmd = new CustomItemCommand();

                    for (ItemStack armorContent : player.getInventory().getArmorContents()) {
                        if (armorContent == null) continue;
                        if (armorContent.getTypeId() == Material.AIR.getId()) continue;
                        if (registerItem.isCustom(armorContent)) {
                            CustomArmor ci = registerItem.toItemCustom(armorContent);
                            if (ci != null) {
                                currentItemIds.add(ci.getId());
                            }
                        }
                    }

                    ItemStack itemInHand = player.getItemInHand();
                    if (itemInHand != null && itemInHand.getTypeId() != Material.AIR.getId()) {
                        if (itemCmd.isCustom(itemInHand)) {
                            CustomItem ci = itemCmd.toItemCustom(itemInHand);
                            if (ci != null && ci.isActive()) {
                                currentItemIds.add(ci.getId());
                            }
                        }
                    }


                    if (playerArmorBonus.containsKey(player.getUniqueId())) {
                        Set<String> activeBonus = new HashSet<>(playerArmorBonus.get(player.getUniqueId()));
                        for (String bonus : activeBonus) {
                            if (!currentItemIds.contains(bonus)) {
                                removeBonusFromPlayer(player, bonus);
                            }
                        }
                    }

                    applyArmorBonus(player);
                    applyHandItemBonus(player);
                }
            }
        };
        runnable.runTaskTimer(Main.instance, 20L, 20L);
    }
}