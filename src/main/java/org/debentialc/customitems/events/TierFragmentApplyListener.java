package org.debentialc.customitems.events;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.debentialc.customitems.tools.fragments.CustomizedArmor;
import org.debentialc.customitems.tools.fragments.FragmentManager;
import org.debentialc.customitems.tools.fragments.TierFragment;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Listener para manejar la aplicación de fragmentos de tier a armaduras
 * VERSIÓN CORREGIDA: Si un atributo excede el límite del nuevo tier o usa una operación
 * no permitida, se reinicia a 0 en lugar de bloquear el upgrade.
 */
public class TierFragmentApplyListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTierFragmentUse(PlayerInteractEvent event) {
        // Solo clic derecho
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getItemInHand();

        // Verificar si es un fragmento de tier
        if (!TierFragment.isTierFragment(itemInHand)) {
            return;
        }

        // Cancelar el evento
        event.setCancelled(true);

        // Buscar pieza de armadura equipada
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        ItemStack targetArmor = null;
        int targetSlot = -1;

        // Buscar primera pieza equipada (helmet -> chest -> legs -> boots)
        for (int i = 3; i >= 0; i--) {
            if (armorContents[i] != null && armorContents[i].getType() != Material.AIR) {
                targetArmor = armorContents[i];
                targetSlot = i;
                break;
            }
        }

        if (targetArmor == null) {
            player.sendMessage(CC.translate("&c✗ No tienes ninguna armadura equipada"));
            player.sendMessage(CC.translate("&7Equipa al menos una pieza de armadura"));
            return;
        }

        // Verificar que sea armadura personalizada
        if (!CustomizedArmor.isCustomized(targetArmor)) {
            player.sendMessage(CC.translate("&c✗ Esta armadura no está personalizada"));
            player.sendMessage(CC.translate("&7Solo se pueden upgradear armaduras con fragmentos aplicados"));
            return;
        }

        // Obtener datos
        String targetTier = TierFragment.getTargetTier(itemInHand);
        String currentTier = CustomizedArmor.getTier(targetArmor);

        // Validar upgrade secuencial
        if (!TierFragment.canUpgrade(currentTier, targetTier)) {
            int currentNum = TierFragment.getTierNumber(currentTier);
            int targetNum = TierFragment.getTierNumber(targetTier);

            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ No se puede aplicar este fragmento"));
            player.sendMessage(CC.translate("&7Tier actual: &f" + currentTier));
            player.sendMessage(CC.translate("&7Tier del fragmento: &f" + targetTier));
            player.sendMessage("");

            if (targetNum <= currentNum) {
                player.sendMessage(CC.translate("&7La armadura ya tiene un tier igual o superior"));
            } else {
                player.sendMessage(CC.translate("&7Los upgrades deben ser secuenciales"));
                player.sendMessage(CC.translate("&7Necesitas: &fTIER_" + (currentNum + 1)));
            }
            player.sendMessage("");
            return;
        }

        // Cargar armadura
        CustomizedArmor customArmor = CustomizedArmor.fromItemStack(targetArmor);

        if (customArmor == null) {
            player.sendMessage(CC.translate("&c✗ Error al cargar la armadura"));
            return;
        }

        List<String> defaultOperations = FragmentManager.getInstance().getTierConfig().getAllowedOperations(targetTier);
        String defaultOperation = defaultOperations.isEmpty() ? "+" : defaultOperations.get(0);

        List<String> resetAttributes = new ArrayList<>();

        // Detectar atributos que deben reiniciarse
        Iterator<Map.Entry<String, Integer>> attrIterator = customArmor.getAttributes().entrySet().iterator();
        while (attrIterator.hasNext()) {
            Map.Entry<String, Integer> entry = attrIterator.next();
            String attr = entry.getKey();
            int currentValue = entry.getValue();
            String operation = customArmor.getOperations().getOrDefault(attr, "+");

            boolean needsReset = false;

            // Operación no permitida en el nuevo tier
            if (!FragmentManager.getInstance().getTierConfig().isOperationAllowed(targetTier, operation)) {
                needsReset = true;
            }

            // Valor actual excede el límite del nuevo tier
            if (!needsReset && FragmentManager.getInstance().getTierConfig().exceedsLimit(targetTier, attr, currentValue, operation)) {
                needsReset = true;
            }

            if (needsReset) {
                resetAttributes.add(attr);
                attrIterator.remove();
                customArmor.getOperations().remove(attr);
            }
        }

        // APLICAR UPGRADE
        String oldTier = customArmor.getTier();
        customArmor.setTier(targetTier);

        // Actualizar ItemStack con el nuevo tier
        customArmor.applyToItemStack(targetArmor);

        // Guardar en almacenamiento
        FragmentManager.getInstance().getArmorStorage().saveArmor(customArmor);

        // Actualizar armadura en el slot
        armorContents[targetSlot] = targetArmor;
        player.getInventory().setArmorContents(armorContents);
        player.updateInventory();

        // Consumir el fragmento
        if (itemInHand.getAmount() > 1) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        } else {
            player.setItemInHand(new ItemStack(Material.AIR));
        }

        // Feedback visual y sonoro
        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ ¡Armadura upgradeada exitosamente!"));
        player.sendMessage(CC.translate("&7Tier anterior: &f" + oldTier));
        player.sendMessage(CC.translate("&7Tier nuevo: &a" + targetTier));
        player.sendMessage("");

        if (!resetAttributes.isEmpty()) {
            player.sendMessage(CC.translate("&e⚠ Algunos atributos eran incompatibles con &f" + targetTier + " &ey se reiniciaron a 0:"));
            StringBuilder attrs = new StringBuilder();
            for (int i = 0; i < resetAttributes.size(); i++) {
                if (i > 0) attrs.append(", ");
                attrs.append(resetAttributes.get(i));
            }
            player.sendMessage(CC.translate("&7" + attrs.toString()));
            player.sendMessage("");
        } else {
            player.sendMessage(CC.translate("&7Todos los stats y operaciones se mantuvieron"));
            player.sendMessage("");
        }

        // Efectos
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
        player.getWorld().strikeLightningEffect(player.getLocation());
    }
}
