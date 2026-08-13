package org.debentialc.trainings.managers;

import org.bukkit.entity.Player;
import org.debentialc.service.CC;
import org.debentialc.trainings.model.Training;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrainingNpcInputManager {

    public enum InputType {
        ADD_NPC,
        EDIT_NPC_TP
    }

    public static class InputState {
        public final String trainingId;
        public final String npcId;
        public final InputType type;
        public final Runnable onComplete;

        public InputState(String trainingId, String npcId, InputType type, Runnable onComplete) {
            this.trainingId = trainingId;
            this.npcId = npcId;
            this.type = type;
            this.onComplete = onComplete;
        }
    }

    private static final Map<UUID, InputState> playersInputting = new HashMap<>();

    public static void startAddNpcInput(Player player, String trainingId, Runnable onComplete) {
        playersInputting.put(player.getUniqueId(), new InputState(trainingId, null, InputType.ADD_NPC, onComplete));
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  Agregar NPC al Training"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Formato: &f<id_npc> <tp_base>"));
        player.sendMessage(CC.translate("&7  Ejemplo: &fnpc_goku 150"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startEditNpcTpInput(Player player, String trainingId, String npcId, Runnable onComplete) {
        playersInputting.put(player.getUniqueId(), new InputState(trainingId, npcId, InputType.EDIT_NPC_TP, onComplete));
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  Editar TP Base del NPC"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  NPC: &f" + npcId));
        player.sendMessage(CC.translate("&7  Ingresa la nueva cantidad de TP base"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isInputting(Player player) {
        return playersInputting.containsKey(player.getUniqueId());
    }

    public static void processInput(Player player, String input) {
        InputState state = playersInputting.get(player.getUniqueId());
        if (state == null) return;

        if (input.equalsIgnoreCase("cancelar")) {
            cancelInput(player);
            return;
        }

        Training training = TrainingManager.getInstance().getTraining(state.trainingId);
        if (training == null) {
            player.sendMessage(CC.translate("&c✗ Training no encontrado."));
            finishInput(player);
            return;
        }

        boolean success = false;
        switch (state.type) {
            case ADD_NPC:
                success = handleAddNpc(player, training, input);
                break;
            case EDIT_NPC_TP:
                success = handleEditNpcTp(player, training, state.npcId, input);
                break;
        }

        if (success) {
            finishInput(player);
            if (state.onComplete != null) {
                state.onComplete.run();
            }
        }
    }

    public static void cancelInput(Player player) {
        player.sendMessage(CC.translate("&c✗ Cancelado."));
        finishInput(player);
    }

    private static void finishInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }

    private static boolean handleAddNpc(Player player, Training training, String input) {
        String[] parts = input.trim().split("\\s+", 2);
        if (parts.length < 2) {
            player.sendMessage(CC.translate("&c✗ Formato inválido. Usa: <id_npc> <tp_base>"));
            return false;
        }

        String npcId = parts[0].trim();
        if (npcId.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ El ID del NPC no puede estar vacío."));
            return false;
        }

        int baseTp;
        try {
            baseTp = Integer.parseInt(parts[1].trim());
            if (baseTp < 0) {
                player.sendMessage(CC.translate("&c✗ Los TP base no pueden ser negativos."));
                return false;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c✗ La cantidad de TP base debe ser un número."));
            return false;
        }

        training.addNpc(npcId, baseTp);
        TrainingManager.getInstance().saveTraining(training);
        player.sendMessage(CC.translate("&a✓ NPC &f" + npcId + " &aagregado con &f" + baseTp + " &aTP base."));
        return true;
    }

    private static boolean handleEditNpcTp(Player player, Training training, String npcId, String input) {
        int baseTp;
        try {
            baseTp = Integer.parseInt(input.trim());
            if (baseTp < 0) {
                player.sendMessage(CC.translate("&c✗ Los TP base no pueden ser negativos."));
                return false;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c✗ La cantidad de TP base debe ser un número."));
            return false;
        }

        training.addNpc(npcId, baseTp);
        TrainingManager.getInstance().saveTraining(training);
        player.sendMessage(CC.translate("&a✓ TP base de &f" + npcId + " &aactualizado a &f" + baseTp + "&a."));
        return true;
    }
}
