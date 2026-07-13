package org.debentialc.rebirths.managers;

import org.bukkit.entity.Player;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.service.CC;

import java.util.HashMap;
import java.util.UUID;

public class RebirthBlockInputManager {

    public enum InputType {
        BLOCK_NAME
    }

    public static class InputState {
        public final int blockId;
        public final InputType type;
        public final Runnable onComplete;

        public InputState(int blockId, InputType type, Runnable onComplete) {
            this.blockId = blockId;
            this.type = type;
            this.onComplete = onComplete;
        }
    }

    private static final HashMap<UUID, InputState> playersInputting = new HashMap<>();

    public static void startInput(Player player, int blockId, InputType type) {
        startInput(player, blockId, type, null);
    }

    public static void startInput(Player player, int blockId, InputType type, Runnable onComplete) {
        playersInputting.put(player.getUniqueId(), new InputState(blockId, type, onComplete));
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        switch (type) {
            case BLOCK_NAME:
                player.sendMessage(CC.translate("&3&l Ingresa el nombre del bloque"));
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

        RebirthBlock block = RebirthBlockManager.getInstance().getBlock(state.blockId);
        if (block == null) {
            player.sendMessage(CC.translate("&c✗ Bloque no encontrado."));
            finishInput(player);
            return;
        }

        if (input.equalsIgnoreCase("cancelar")) {
            cancelInput(player);
            return;
        }

        boolean success = false;
        switch (state.type) {
            case BLOCK_NAME:
                success = handleBlockName(player, block, input);
                break;
        }

        if (success) {
            finishInput(player);
            if (state.onComplete != null) {
                state.onComplete.run();
            }
        }
    }

    private static boolean handleBlockName(Player player, RebirthBlock block, String input) {
        if (input.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ El nombre no puede estar vacío."));
            return false;
        }
        block.setName(input);
        RebirthBlockManager.getInstance().saveBlock(block);
        player.sendMessage(CC.translate("&a✓ Nombre del bloque establecido a &r" + input));
        return true;
    }

    public static void cancelInput(Player player) {
        player.sendMessage(CC.translate("&c✗ Cancelado."));
        finishInput(player);
    }

    public static void finishInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }
}
