package org.debentialc.customitems.events;

import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.debentialc.Main;
import org.debentialc.boosters.core.BoosterParser;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.service.CC;
import org.debentialc.service.General;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsumableItemListener implements Listener {

    private static final Map<String, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (item == null || item.getTypeId() == 0) {
            return;
        }

        CustomItemCommand itemCmd = new CustomItemCommand();
        CustomItem customItem = itemCmd.toItemCustom(item);

        if (customItem == null) {
            return;
        }


        boolean shouldConsume = customItem.isConsumable() ||
                customItem.getTpValue() > 0 ||
                (customItem.getCommands() != null && !customItem.getCommands().isEmpty());

        if (!shouldConsume) {
            return;
        }

        event.setCancelled(true);

        if (customItem.getCooldownSeconds() > 0) {
            String key = player.getUniqueId() + ":" + customItem.getId();
            long now = System.currentTimeMillis();
            Long expires = cooldowns.get(key);
            if (expires != null && expires > now) {
                long remainingSeconds = (expires - now) / 1000L;
                player.sendMessage(CC.translate("&c✗ Espera &e" + BoosterParser.formatSecondsToTime(remainingSeconds) + " &cpara volver a usar este item."));
                return;
            }
            cooldowns.put(key, now + customItem.getCooldownSeconds() * 1000L);
        }

        if (customItem.getCommands() != null && !customItem.getCommands().isEmpty()) {
            executeCommands(player, customItem.getCommands());
        }

        if (customItem.getTpValue() > 0) {
            int amount = item.getAmount();
            int consumed = 1;

            if (customItem.isTpConsumeStack()) {
                consumed = amount;
            }

            giveTP(player, customItem.getTpValue(), consumed);

            amount = amount - consumed;

            if (amount <= 0) {
                player.setItemInHand(null);
            } else {
                item.setAmount(amount);
            }
        }
        else if (customItem.isConsumable()) {
            int amount = item.getAmount();
            int remaining = amount - 1;

            if (remaining <= 0) {
                player.setItemInHand(null);
            } else {
                item.setAmount(remaining);
            }
        }
    }

    /**
     * Ejecuta los comandos configurados en orden
     */
    private void executeCommands(Player player, List<String> commands) {
        Player targetPlayer = getTargetPlayer(player);

        for (String cmd : commands) {
            String finalCmd = cmd;

            finalCmd = finalCmd.replace("@dp", player.getName());

            if (targetPlayer != null) {
                finalCmd = finalCmd.replace("@p", targetPlayer.getName());
            } else {
                finalCmd = finalCmd.replace("@p", player.getName());
            }

            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            } catch (Exception e) {
                player.sendMessage(CC.translate("&c✗ Error ejecutando comando: " + finalCmd));
                e.printStackTrace();
            }
        }
    }

    /**
     * Otorga TP al jugador
     */
    private void giveTP(Player player, int tpValue, int multiplier) {
        try {
            IDBCPlayer idbcPlayer = General.getDBCPlayer(player.getName());

            int totalTP = tpValue * multiplier;
            Bukkit.dispatchCommand(Main.instance.getServer().getConsoleSender(), "dartps " + player.getName() + " " +
                    totalTP);
        } catch (Exception e) {
            player.sendMessage(CC.translate("&c✗ Error al otorgar TP"));
            e.printStackTrace();
        }
    }

    /**
     * Obtiene el jugador al que está apuntando
     */
    private Player getTargetPlayer(Player player) {
        try {
            BlockIterator iterator = new BlockIterator(player, 10);

            while (iterator.hasNext()) {
                iterator.next();
                for (Player target : player.getWorld().getPlayers()) {
                    if (target.equals(player)) continue;

                    if (target.getLocation().distance(player.getLocation()) <= 10) {
                        if (isLookingAt(player, target)) {
                            return target;
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        return null;
    }

    /**
     * Verifica si el jugador está mirando a otro jugador
     */
    private boolean isLookingAt(Player player, Player target) {
        org.bukkit.util.Vector playerDirection = player.getLocation().getDirection().normalize();
        org.bukkit.util.Vector toTarget = target.getLocation().toVector()
                .subtract(player.getLocation().toVector()).normalize();

        double dot = playerDirection.dot(toTarget);
        return dot > 0.95;
    }
}