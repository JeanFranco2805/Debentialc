package org.debentialc.swords.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.debentialc.service.CC;
import org.debentialc.service.General;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SwordAbilityListener implements Listener {

    private static final int SWORD_ITEM_ID = 6113;
    private static final int ABILITY_RANGE = 5;
    private static final long COOLDOWN_MILLIS = 0;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final ThreadLocal<Boolean> applyingArea = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (applyingArea.get()) {
            return;
        }

        Entity damager = event.getDamager();
        if (!(damager instanceof Player)) {
            return;
        }

        Player player = (Player) damager;
        ItemStack item = player.getItemInHand();
        if (item == null || item.getTypeId() != SWORD_ITEM_ID) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse != null && now - lastUse < COOLDOWN_MILLIS) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now);

        applyingArea.set(true);
        try {
            General.damageNpcByArea(player, ABILITY_RANGE);
            player.sendMessage(CC.translate("&a\u2694 \u00a1Tu espada ha golpeado a los enemigos cercanos!"));
        } catch (Exception e) {
            player.sendMessage(CC.translate("&c\u2717 Error al usar la habilidad de \u00e1rea."));
            e.printStackTrace();
        } finally {
            applyingArea.set(false);
        }
    }
}
