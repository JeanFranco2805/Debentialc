package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.customitems.tools.ci.CustomArmor;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.tools.storage.CustomArmorStorage;
import org.debentialc.customitems.tools.storage.CustomItemStorage;
import org.debentialc.rebirths.managers.RebirthBlockManager;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.service.CC;

import java.util.HashMap;
import java.util.UUID;

/**
 * Maneja la entrada por chat de requisitos opcionales para items y armaduras custom:
 * - Bloque de rebirth requerido + nivel local dentro de ese bloque.
 * - Permiso personalizado requerido.
 */
public class CustomRequirementInputManager {

    public enum Mode {
        REBIRTH_BLOCK,
        REBIRTH_LEVEL,
        PERMISSION
    }

    public static class RequirementState {
        public String type;      // "item" o "armor"
        public String targetId;
        public Mode mode;
        public String pendingBlockId = null;

        public RequirementState(String type, String targetId, Mode mode) {
            this.type = type;
            this.targetId = targetId;
            this.mode = mode;
        }
    }

    private static final HashMap<UUID, RequirementState> playersInputting = new HashMap<>();

    public static void startRebirthBlockInput(Player player, String type, String targetId) {
        playersInputting.put(player.getUniqueId(), new RequirementState(type, targetId, Mode.REBIRTH_BLOCK));
        sendInputHeader(player, "Bloque de Rebirth", "Ingresa el ID alfanumérico del bloque de rebirth requerido (ej: A1)");
    }

    public static void startRebirthLevelInput(Player player, String type, String targetId, String blockId) {
        RequirementState state = new RequirementState(type, targetId, Mode.REBIRTH_LEVEL);
        state.pendingBlockId = blockId;
        playersInputting.put(player.getUniqueId(), state);
        sendInputHeader(player, "Nivel de Rebirth", "Ingresa el nivel local requerido dentro del bloque (1, 2, 3...)");
    }

    public static void startPermissionInput(Player player, String type, String targetId) {
        playersInputting.put(player.getUniqueId(), new RequirementState(type, targetId, Mode.PERMISSION));
        sendInputHeader(player, "Permiso", "Ingresa el permiso personalizado requerido");
    }

    private static void sendInputHeader(Player player, String title, String subtitle) {
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  " + title));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  " + subtitle));
        player.sendMessage(CC.translate("&7  Escribe &c'eliminar' &7para quitar el requisito"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isInputtingRequirement(Player player) {
        return playersInputting.containsKey(player.getUniqueId());
    }

    public static void processRequirementInput(Player player, String input) {
        RequirementState state = playersInputting.get(player.getUniqueId());
        if (state == null) return;

        String message = input.trim();

        if ("eliminar".equalsIgnoreCase(message)) {
            removeRequirement(player, state);
            finishRequirementInput(player);
            return;
        }

        switch (state.mode) {
            case REBIRTH_BLOCK:
                processRebirthBlock(player, state, message);
                break;
            case REBIRTH_LEVEL:
                processRebirthLevel(player, state, message);
                break;
            case PERMISSION:
                processPermission(player, state, message);
                break;
        }
    }

    private static void processRebirthBlock(Player player, RequirementState state, String message) {
        RebirthBlock block = RebirthBlockManager.getInstance().getBlock(message);
        if (block == null) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ No existe un bloque de rebirth con ID &f" + message));
            player.sendMessage("");
            return;
        }

        finishRequirementInput(player);
        startRebirthLevelInput(player, state.type, state.targetId, message);
    }

    private static void processRebirthLevel(Player player, RequirementState state, String message) {
        int level;
        try {
            level = Integer.parseInt(message);
            if (level < 1) {
                player.sendMessage("");
                player.sendMessage(CC.translate("&c✗ El nivel debe ser mayor a 0"));
                player.sendMessage("");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Debes ingresar un número entero"));
            player.sendMessage("");
            return;
        }

        RebirthBlock block = RebirthBlockManager.getInstance().getBlock(state.pendingBlockId);
        int maxLevel = block != null ? block.getRebirthIds().size() : 0;
        if (maxLevel > 0 && level > maxLevel) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ El bloque solo tiene &f" + maxLevel + " &crebirths"));
            player.sendMessage("");
            return;
        }

        if ("item".equals(state.type)) {
            CustomItem item = CustomItemCommand.items.get(state.targetId);
            if (item != null) {
                item.setRequiredRebirthBlock(state.pendingBlockId);
                item.setRequiredRebirthLevel(level);
                CustomItemStorage.getInstance().saveItem(item);
            }
        } else if ("armor".equals(state.type)) {
            CustomArmor armor = RegisterItem.items.get(state.targetId);
            if (armor != null) {
                armor.setRequiredRebirthBlock(state.pendingBlockId);
                armor.setRequiredRebirthLevel(level);
                CustomArmorStorage.getInstance().saveArmor(armor);
            }
        }

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Requisito de rebirth actualizado"));
        player.sendMessage(CC.translate("&7Bloque: &f" + state.pendingBlockId + " &7Nivel: &f" + level));
        player.sendMessage("");

        finishRequirementInput(player);
        reopenMenu(player, state.type, state.targetId);
    }

    private static void processPermission(Player player, RequirementState state, String message) {
        if (message.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ El permiso no puede estar vacío"));
            player.sendMessage("");
            return;
        }

        if ("item".equals(state.type)) {
            CustomItem item = CustomItemCommand.items.get(state.targetId);
            if (item != null) {
                item.setRequiredPermission(message);
                CustomItemStorage.getInstance().saveItem(item);
            }
        } else if ("armor".equals(state.type)) {
            CustomArmor armor = RegisterItem.items.get(state.targetId);
            if (armor != null) {
                armor.setRequiredPermission(message);
                CustomArmorStorage.getInstance().saveArmor(armor);
            }
        }

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Permiso requerido actualizado"));
        player.sendMessage(CC.translate("&7Permiso: &f" + message));
        player.sendMessage("");

        finishRequirementInput(player);
        reopenMenu(player, state.type, state.targetId);
    }

    private static void removeRequirement(Player player, RequirementState state) {
        if ("item".equals(state.type)) {
            CustomItem item = CustomItemCommand.items.get(state.targetId);
            if (item != null) {
                if (state.mode == Mode.PERMISSION) {
                    item.setRequiredPermission(null);
                    player.sendMessage(CC.translate("&a✓ Permiso requerido eliminado"));
                } else {
                    item.setRequiredRebirthBlock(null);
                    item.setRequiredRebirthLevel(0);
                    player.sendMessage(CC.translate("&a✓ Requisito de rebirth eliminado"));
                }
                CustomItemStorage.getInstance().saveItem(item);
            }
        } else if ("armor".equals(state.type)) {
            CustomArmor armor = RegisterItem.items.get(state.targetId);
            if (armor != null) {
                if (state.mode == Mode.PERMISSION) {
                    armor.setRequiredPermission(null);
                    player.sendMessage(CC.translate("&a✓ Permiso requerido eliminado"));
                } else {
                    armor.setRequiredRebirthBlock(null);
                    armor.setRequiredRebirthLevel(0);
                    player.sendMessage(CC.translate("&a✓ Requisito de rebirth eliminado"));
                }
                CustomArmorStorage.getInstance().saveArmor(armor);
            }
        }
        reopenMenu(player, state.type, state.targetId);
    }

    private static void reopenMenu(Player player, String type, String targetId) {
        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, new Runnable() {
            public void run() {
                if ("item".equals(type)) {
                    CustomItemAdvancedOptionsMenu.createAdvancedOptionsMenu(targetId).open(player);
                } else if ("armor".equals(type)) {
                    CustomArmorAdvancedOptionsMenu.createAdvancedOptionsMenu(targetId).open(player);
                }
            }
        }, 1L);
    }

    public static void cancelRequirementInput(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishRequirementInput(player);
    }

    private static void finishRequirementInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }
}
