package org.debentialc.factions.menus;

import org.bukkit.entity.Player;
import org.debentialc.factions.zones.FactionZone;
import org.debentialc.factions.zones.FactionZoneManager;
import org.debentialc.service.CC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FactionZoneInputManager {

    private static final Map<UUID, InputState> states = new HashMap<>();

    public static void startCreate(Player player) {
        states.put(player.getUniqueId(), new InputState(InputType.CREATE, null, 0));
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&c&l  Crear Zona"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Escribe en el chat:"));
        player.sendMessage(CC.translate("&f<id> <regionWG> <mundo> <costoPower> <rareza> [nombreDisplay]"));
        player.sendMessage(CC.translate("&7  Ejemplo: &fspawnarena spawn_region world 100 LEGENDARIO Arena Spawn"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startEdit(Player player, String zoneId) {
        states.put(player.getUniqueId(), new InputState(InputType.EDIT, zoneId, 0));
        player.closeInventory();
        FactionZone zone = FactionZoneManager.getInstance().getZone(zoneId);
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&c&l  Editar Zona: &f" + (zone != null ? zone.getDisplayName() : zoneId)));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Escribe en el chat:"));
        player.sendMessage(CC.translate("&f<costoPower> <rareza> [nombreDisplay]"));
        player.sendMessage(CC.translate("&7  Ejemplo: &f150 EPICO Nueva Arena"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startEditName(Player player, String zoneId) {
        states.put(player.getUniqueId(), new InputState(InputType.EDIT_NAME, zoneId, 0));
        player.closeInventory();
        FactionZone zone = FactionZoneManager.getInstance().getZone(zoneId);
        player.sendMessage("");
        player.sendMessage(CC.translate("&c&l  Editar nombre de: &f" + (zone != null ? zone.getDisplayName() : zoneId)));
        player.sendMessage(CC.translate("&7  Escribe el nuevo nombre display."));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage("");
    }

    public static void startEditCost(Player player, String zoneId) {
        states.put(player.getUniqueId(), new InputState(InputType.EDIT_COST, zoneId, 0));
        player.closeInventory();
        FactionZone zone = FactionZoneManager.getInstance().getZone(zoneId);
        player.sendMessage("");
        player.sendMessage(CC.translate("&c&l  Editar costo de: &f" + (zone != null ? zone.getDisplayName() : zoneId)));
        player.sendMessage(CC.translate("&7  Escribe el nuevo costo de power (número)."));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage("");
    }

    public static void startEditRarity(Player player, String zoneId) {
        states.put(player.getUniqueId(), new InputState(InputType.EDIT_RARITY, zoneId, 0));
        player.closeInventory();
        FactionZone zone = FactionZoneManager.getInstance().getZone(zoneId);
        player.sendMessage("");
        player.sendMessage(CC.translate("&c&l  Editar rareza de: &f" + (zone != null ? zone.getDisplayName() : zoneId)));
        player.sendMessage(CC.translate("&7  Escribe la nueva rareza (por ejemplo: COMUN, RARO, EPICO, LEGENDARIO)."));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage("");
    }

    public static boolean isEditing(Player player) {
        return states.containsKey(player.getUniqueId());
    }

    public static void processInput(Player player, String input) {
        InputState state = states.remove(player.getUniqueId());
        if (state == null) return;

        if (input.equalsIgnoreCase("cancelar")) {
            player.sendMessage(CC.translate("&c✗ Cancelado"));
            return;
        }

        String[] args = input.split(" ");
        if (state.type == InputType.CREATE) {
            if (args.length < 5) {
                player.sendMessage(CC.translate("&c✗ Formato inválido. Usa: <id> <regionWG> <mundo> <costoPower> <rareza> [nombreDisplay]"));
                return;
            }
            String id = args[0].toLowerCase();
            String region = args[1];
            String world = args[2];
            int costo;
            try {
                costo = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(CC.translate("&c✗ Costo inválido."));
                return;
            }
            String rarity = args[4].toUpperCase();
            StringBuilder displayName = new StringBuilder();
            for (int i = 5; i < args.length; i++) {
                displayName.append(args[i]).append(" ");
            }
            String name = displayName.length() > 0 ? displayName.toString().trim() : id;

            if (FactionZoneManager.getInstance().createZone(id, CC.translate(name), region, world, costo, rarity)) {
                player.sendMessage(CC.translate("&a✓ Zona &f" + id + " &acreada."));
            } else {
                player.sendMessage(CC.translate("&c✗ No se pudo crear la zona. Verifica ID, región y mundo."));
            }
        } else if (state.type == InputType.EDIT) {
            if (args.length < 2) {
                player.sendMessage(CC.translate("&c✗ Formato inválido. Usa: <costoPower> <rareza> [nombreDisplay]"));
                return;
            }
            int costo;
            try {
                costo = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(CC.translate("&c✗ Costo inválido."));
                return;
            }
            String rarity = args[1].toUpperCase();
            StringBuilder displayName = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                displayName.append(args[i]).append(" ");
            }
            String name = displayName.length() > 0 ? CC.translate(displayName.toString().trim()) : null;

            if (FactionZoneManager.getInstance().updateZone(state.zoneId, name, costo, rarity)) {
                player.sendMessage(CC.translate("&a✓ Zona actualizada."));
            } else {
                player.sendMessage(CC.translate("&c✗ No se pudo actualizar la zona."));
            }
        } else if (state.type == InputType.EDIT_NAME) {
            String name = CC.translate(input);
            if (FactionZoneManager.getInstance().updateZone(state.zoneId, name, null, null)) {
                player.sendMessage(CC.translate("&a✓ Nombre actualizado a &f" + name));
            } else {
                player.sendMessage(CC.translate("&c✗ No se pudo actualizar la zona."));
            }
        } else if (state.type == InputType.EDIT_COST) {
            int costo;
            try {
                costo = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                player.sendMessage(CC.translate("&c✗ Costo inválido."));
                return;
            }
            if (FactionZoneManager.getInstance().updateZone(state.zoneId, null, costo, null)) {
                player.sendMessage(CC.translate("&a✓ Costo de power actualizado a &f" + costo));
            } else {
                player.sendMessage(CC.translate("&c✗ No se pudo actualizar la zona."));
            }
        } else if (state.type == InputType.EDIT_RARITY) {
            String rarity = input.toUpperCase();
            if (FactionZoneManager.getInstance().updateZone(state.zoneId, null, null, rarity)) {
                player.sendMessage(CC.translate("&a✓ Rareza actualizada a &f" + rarity));
            } else {
                player.sendMessage(CC.translate("&c✗ No se pudo actualizar la zona."));
            }
        }

        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, () -> FactionAdminZoneMenu.reopenLastPage(player), 1L);
    }

    public static void cancel(Player player) {
        states.remove(player.getUniqueId());
        player.sendMessage(CC.translate("&c✗ Cancelado"));
    }

    private enum InputType {
        CREATE, EDIT, EDIT_NAME, EDIT_COST, EDIT_RARITY
    }

    private static class InputState {
        InputType type;
        String zoneId;
        int step;

        InputState(InputType type, String zoneId, int step) {
            this.type = type;
            this.zoneId = zoneId;
            this.step = step;
        }
    }
}
