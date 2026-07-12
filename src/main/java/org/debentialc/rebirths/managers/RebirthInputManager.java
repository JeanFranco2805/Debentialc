package org.debentialc.rebirths.managers;

import org.bukkit.entity.Player;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.service.CC;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RebirthInputManager {

    public enum InputType {
        REQUIRED_LEVEL,
        TP_BONUS_PERCENT,
        DISPLAY_NAME,
        ADD_REGION,
        REMOVE_REGION,
        ADD_COMMAND,
        REMOVE_COMMAND
    }

    public static class InputState {
        public final int rebirthId;
        public final InputType type;
        public final Runnable onComplete;

        public InputState(int rebirthId, InputType type, Runnable onComplete) {
            this.rebirthId = rebirthId;
            this.type = type;
            this.onComplete = onComplete;
        }
    }

    private static final HashMap<UUID, InputState> playersInputting = new HashMap<>();

    public static void startInput(Player player, int rebirthId, InputType type) {
        startInput(player, rebirthId, type, null);
    }

    public static void startInput(Player player, int rebirthId, InputType type, Runnable onComplete) {
        playersInputting.put(player.getUniqueId(), new InputState(rebirthId, type, onComplete));
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        switch (type) {
            case REQUIRED_LEVEL:
                player.sendMessage(CC.translate("&3&l Ingresa el nivel requerido"));
                player.sendMessage(CC.translate("&7 Ejemplo: &f1000"));
                break;
            case TP_BONUS_PERCENT:
                player.sendMessage(CC.translate("&3&l Ingresa el porcentaje de bonus de TPs"));
                player.sendMessage(CC.translate("&7 Ejemplo: &f15 &7(15%)"));
                break;
            case DISPLAY_NAME:
                player.sendMessage(CC.translate("&3&l Ingresa el nombre mostrado"));
                player.sendMessage(CC.translate("&7 Usa '&' para colores. Ejemplo: &f&6Rebirth &cI"));
                break;
            case ADD_REGION:
                player.sendMessage(CC.translate("&3&l Ingresa el nombre de la región de WorldGuard"));
                player.sendMessage(CC.translate("&7 Ejemplo: &fspawn_combate"));
                break;
            case REMOVE_REGION:
                player.sendMessage(CC.translate("&3&l Ingresa el nombre de la región a remover"));
                break;
            case ADD_COMMAND:
                player.sendMessage(CC.translate("&3&l Ingresa el comando a ejecutar"));
                player.sendMessage(CC.translate("&7 Usa &f%player% &7para el nombre del jugador"));
                player.sendMessage(CC.translate("&7 Ejemplo: &fgive %player% diamond 1"));
                break;
            case REMOVE_COMMAND:
                player.sendMessage(CC.translate("&3&l Ingresa el número o texto del comando a remover"));
                player.sendMessage(CC.translate("&7 Escribe parte del comando o el número mostrado"));
                break;
        }
        player.sendMessage(CC.translate("&7 Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isInputting(Player player) {
        return playersInputting.containsKey(player.getUniqueId());
    }

    public static void processInput(Player player, String input) {
        InputState state = playersInputting.get(player.getUniqueId());
        if (state == null) return;

        Rebirth rebirth = RebirthManager.getInstance().getRebirth(state.rebirthId);
        if (rebirth == null) {
            player.sendMessage(CC.translate("&c✗ Rebirth no encontrado."));
            finishInput(player);
            return;
        }

        if (input.equalsIgnoreCase("cancelar")) {
            cancelInput(player);
            return;
        }

        boolean success = false;
        switch (state.type) {
            case REQUIRED_LEVEL:
                success = handleRequiredLevel(player, rebirth, input);
                break;
            case TP_BONUS_PERCENT:
                success = handleTpBonusPercent(player, rebirth, input);
                break;
            case DISPLAY_NAME:
                success = handleDisplayName(player, rebirth, input);
                break;
            case ADD_REGION:
                success = handleAddRegion(player, rebirth, input);
                break;
            case REMOVE_REGION:
                success = handleRemoveRegion(player, rebirth, input);
                break;
            case ADD_COMMAND:
                success = handleAddCommand(player, rebirth, input);
                break;
            case REMOVE_COMMAND:
                success = handleRemoveCommand(player, rebirth, input);
                break;
        }

        if (success) {
            finishInput(player);
            if (state.onComplete != null) {
                state.onComplete.run();
            }
        }
    }

    private static boolean handleRequiredLevel(Player player, Rebirth rebirth, String input) {
        try {
            int value = Integer.parseInt(input);
            if (value < 0) {
                player.sendMessage(CC.translate("&c✗ El nivel no puede ser negativo."));
                return false;
            }
            rebirth.setRequiredLevel(value);
            RebirthManager.getInstance().saveRebirth(rebirth);
            player.sendMessage(CC.translate("&a✓ Nivel requerido de &e" + rebirth.getId() + " &aestablecido a &f" + value));
            org.debentialc.Main.instance.getLogger().info("[Rebirths Debug] Admin " + player.getName() + " cambió requiredLevel de rebirth " + rebirth.getId() + " a " + value);
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c✗ Número inválido."));
            return false;
        }
    }

    private static boolean handleTpBonusPercent(Player player, Rebirth rebirth, String input) {
        try {
            double value = Double.parseDouble(input);
            if (value < 0) {
                player.sendMessage(CC.translate("&c✗ El porcentaje no puede ser negativo."));
                return false;
            }
            rebirth.setTpBonusPercent(value);
            RebirthManager.getInstance().saveRebirth(rebirth);
            player.sendMessage(CC.translate("&a✓ Bonus de TPs de &e" + rebirth.getId() + " &aestablecido a &f" + value + "%"));
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c✗ Número inválido."));
            return false;
        }
    }

    private static boolean handleDisplayName(Player player, Rebirth rebirth, String input) {
        if (input.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ El nombre no puede estar vacío."));
            return false;
        }
        rebirth.setDisplayName(input);
        RebirthManager.getInstance().saveRebirth(rebirth);
        player.sendMessage(CC.translate("&a✓ Nombre de &e" + rebirth.getId() + " &aestablecido a &r" + input));
        return true;
    }

    private static boolean handleAddRegion(Player player, Rebirth rebirth, String input) {
        if (input.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ La región no puede estar vacía."));
            return false;
        }
        if (rebirth.getAllowedRegions().contains(input)) {
            player.sendMessage(CC.translate("&c✗ La región ya está agregada."));
            return false;
        }
        rebirth.getAllowedRegions().add(input);
        RebirthManager.getInstance().saveRebirth(rebirth);
        player.sendMessage(CC.translate("&a✓ Región &f" + input + " &aagregada a &e" + rebirth.getId()));
        return true;
    }

    private static boolean handleRemoveRegion(Player player, Rebirth rebirth, String input) {
        if (!rebirth.getAllowedRegions().remove(input)) {
            player.sendMessage(CC.translate("&c✗ La región no existe en este rebirth."));
            return false;
        }
        RebirthManager.getInstance().saveRebirth(rebirth);
        player.sendMessage(CC.translate("&a✓ Región &f" + input + " &aremovida de &e" + rebirth.getId()));
        return true;
    }

    public static void cancelInput(Player player) {
        player.sendMessage(CC.translate("&c✗ Cancelado."));
        finishInput(player);
    }

    private static boolean handleAddCommand(Player player, Rebirth rebirth, String input) {
        if (input.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ El comando no puede estar vacío."));
            return false;
        }
        rebirth.getRewardCommands().add(input);
        RebirthManager.getInstance().saveRebirth(rebirth);
        player.sendMessage(CC.translate("&a✓ Comando agregado a &e" + rebirth.getId() + "&a: &f" + input));
        return true;
    }

    private static boolean handleRemoveCommand(Player player, Rebirth rebirth, String input) {
        List<String> commands = rebirth.getRewardCommands();
        if (commands.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ Este rebirth no tiene comandos."));
            return false;
        }

        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < commands.size()) {
                String removed = commands.remove(index);
                RebirthManager.getInstance().saveRebirth(rebirth);
                player.sendMessage(CC.translate("&a✓ Comando removido: &f" + removed));
                return true;
            }
        } catch (NumberFormatException ignored) {
        }

        boolean removed = commands.remove(input);
        if (removed) {
            RebirthManager.getInstance().saveRebirth(rebirth);
            player.sendMessage(CC.translate("&a✓ Comando removido: &f" + input));
            return true;
        } else {
            player.sendMessage(CC.translate("&c✗ Comando no encontrado."));
            return false;
        }
    }

    public static void finishInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }
}
