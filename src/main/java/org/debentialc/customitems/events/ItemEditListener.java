package org.debentialc.customitems.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;
import org.debentialc.customitems.tools.inventory.*;
import org.debentialc.customitems.tools.pastebin.PastebinLoreManager;

public class ItemEditListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        if (AttackDamageInputManager.isInputtingAttackDamage(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                AttackDamageInputManager.cancelAttackDamageInput(player);
            } else {
                AttackDamageInputManager.processAttackDamageInput(player, message);
            }
            return;
        }

        if (ItemEditManager.isEditingItem(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                ItemEditManager.cancelItemEdit(player);
            } else {
                ItemEditManager.processItemEdit(player, message);
            }
            return;
        }

        if (PastebinLoreManager.isInputtingPastebin(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                PastebinLoreManager.cancelPastebinInput(player);
            } else {
                PastebinLoreManager.processPastebinInput(player, message);
            }
            return;
        }

        if (ArmorEditManager.isEditingArmor(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                ArmorEditManager.cancelArmorEdit(player);
            } else {
                ArmorEditManager.processArmorEdit(player, message);
            }
            return;
        }

        if (BonusInputManager.isInputtingBonus(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                BonusInputManager.cancelBonusInput(player);
            } else {
                BonusInputManager.processBonusInput(player, message);
            }
            return;
        }

        if (EffectInputManager.isInputtingEffect(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                EffectInputManager.cancelEffectInput(player);
            } else {
                EffectInputManager.processEffectInput(player, message);
            }
            return;
        }

        if (DurabilityInputManager.isInputtingDurability(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                DurabilityInputManager.cancelDurabilityInput(player);
            } else {
                DurabilityInputManager.processDurabilityInput(player, message);
            }
            return;
        }

        if (AttackDamageInputManager.isInputtingAttackDamage(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                AttackDamageInputManager.cancelAttackDamageInput(player);
            } else {
                AttackDamageInputManager.processAttackDamageInput(player, message);
            }
            return;
        }

        if (ItemCommandInputManager.isInputtingCommand(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                ItemCommandInputManager.cancelCommandInput(player);
            } else {
                ItemCommandInputManager.processCommandInput(player, message);
            }
            return;
        }

        if (ItemTPInputManager.isInputtingTPValue(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                ItemTPInputManager.cancelTPValueInput(player);
            } else {
                ItemTPInputManager.processTPValueInput(player, message);
            }
            return;
        }

        if (ItemCooldownInputManager.isInputtingCooldown(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                ItemCooldownInputManager.cancelCooldownInput(player);
            } else {
                ItemCooldownInputManager.processCooldownInput(player, message);
            }
            return;
        }

        if (org.debentialc.customitems.tools.scripts.ScriptInputManager.isInputtingScript(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                org.debentialc.customitems.tools.scripts.ScriptInputManager.cancelScriptInput(player);
            } else {
                org.debentialc.customitems.tools.scripts.ScriptInputManager.processScriptInput(player, message);
            }
            return;
        }

        if (ItemIdChangeManager.isChangingMaterialId(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                ItemIdChangeManager.cancelMaterialIdChange(player);
            } else {
                ItemIdChangeManager.processMaterialIdChange(player, message);
            }
            return;
        }

        if (CustomIdChangeManager.isChangingId(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                CustomIdChangeManager.cancelIdChange(player);
            } else {
                CustomIdChangeManager.processIdChange(player, message);
            }
            return;
        }

        if (CustomRequirementInputManager.isInputtingRequirement(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                CustomRequirementInputManager.cancelRequirementInput(player);
            } else {
                CustomRequirementInputManager.processRequirementInput(player, message);
            }
            return;
        }

        if (CategoryInputManager.isChangingCategory(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancelar")) {
                CategoryInputManager.cancelCategoryChange(player);
            } else {
                CategoryInputManager.processCategoryInput(player, message);
            }
            return;
        }
    }
}
