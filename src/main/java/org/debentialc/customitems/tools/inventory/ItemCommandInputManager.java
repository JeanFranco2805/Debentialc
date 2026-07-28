package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.debentialc.service.CC;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.tools.storage.CustomItemStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Gestiona el input de comandos para items consumibles
 */
public class ItemCommandInputManager {

    public static class CommandInputState {
        public String itemId;

        public CommandInputState(String itemId) {
            this.itemId = itemId;
        }
    }

    private static final HashMap<UUID, CommandInputState> playersInputting = new HashMap<>();

    public static void startCommandInput(Player player, String itemId) {
        playersInputting.put(player.getUniqueId(), new CommandInputState(itemId));

        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&e&l  Agregar Comando"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Escribe el comando sin &f/"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Placeholders:"));
        player.sendMessage(CC.translate("&f    @dp &7- Jugador que usa el item"));
        player.sendMessage(CC.translate("&f    @p  &7- Jugador al que apunta"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Ejemplo: &fgamemode 1 @dp"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isInputtingCommand(Player player) {
        return playersInputting.containsKey(player.getUniqueId());
    }

    public static void processCommandInput(Player player, String input) {
        CommandInputState state = playersInputting.get(player.getUniqueId());
        if (state == null) return;

        if (!CustomItemCommand.items.containsKey(state.itemId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Item no encontrado"));
            player.sendMessage("");
            finishCommandInput(player);
            return;
        }

        // Limpiar el comando (remover / si lo pusieron)
        String command = input.trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        if (command.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Comando vacío"));
            player.sendMessage("");
            startCommandInput(player, state.itemId);
            return;
        }

        // Agregar comando al item
        CustomItem item = CustomItemCommand.items.get(state.itemId);

        if (item.getCommands() == null) {
            item.setCommands(new ArrayList<>());
        }

        item.getCommands().add(command);

        // Guardar
        CustomItemStorage storage = CustomItemStorage.getInstance();
        storage.saveItem(item);

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Comando agregado"));
        player.sendMessage(CC.translate("&7/" + command));
        player.sendMessage("");

        String itemId = state.itemId;
        finishCommandInput(player);

        // Reabrir menú
        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(
                org.debentialc.Main.instance,
                () -> {
                    CustomItemAdvancedOptionsMenu.createCommandsMenu(itemId).open(player);
                },
                1L
        );
    }

    public static void cancelCommandInput(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishCommandInput(player);
    }

    private static void finishCommandInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }
}