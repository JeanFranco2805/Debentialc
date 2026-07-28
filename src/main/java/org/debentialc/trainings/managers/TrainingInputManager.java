package org.debentialc.trainings.managers;

import org.bukkit.entity.Player;
import org.debentialc.service.CC;
import org.debentialc.trainings.model.Training;
import org.debentialc.trainings.model.TrainingLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrainingInputManager {

    public enum InputType {
        TRAINING_NAME,
        TRAINING_MATERIAL,
        TRAINING_PERMISSION,
        LEVEL_DESCRIPTION,
        LEVEL_TP_BONUS_PERCENT,
        LEVEL_TP_COST,
        LEVEL_STAT_COST,
        LEVEL_ADD_COMMAND,
        LEVEL_REMOVE_COMMAND
    }

    public static class InputState {
        public final String trainingId;
        public final int levelNumber;
        public final InputType type;
        public final Runnable onComplete;

        public InputState(String trainingId, int levelNumber, InputType type, Runnable onComplete) {
            this.trainingId = trainingId;
            this.levelNumber = levelNumber;
            this.type = type;
            this.onComplete = onComplete;
        }
    }

    private static final Map<UUID, InputState> playersInputting = new HashMap<>();

    public static void startInput(Player player, String trainingId, InputType type, Runnable onComplete) {
        startInput(player, trainingId, 0, type, onComplete);
    }

    public static void startInput(Player player, String trainingId, int levelNumber, InputType type, Runnable onComplete) {
        playersInputting.put(player.getUniqueId(), new InputState(trainingId, levelNumber, type, onComplete));
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        switch (type) {
            case TRAINING_NAME:
                player.sendMessage(CC.translate("&3&l Ingresa el nombre del training"));
                break;
            case TRAINING_MATERIAL:
                player.sendMessage(CC.translate("&3&l Ingresa el ID del material"));
                player.sendMessage(CC.translate("&7 Ejemplo: &f339 &7(papel)"));
                break;
            case TRAINING_PERMISSION:
                player.sendMessage(CC.translate("&3&l Ingresa el permiso requerido"));
                player.sendMessage(CC.translate("&7 Escribe &c'ninguno' &7para quitar el permiso"));
                break;
            case LEVEL_DESCRIPTION:
                player.sendMessage(CC.translate("&3&l Ingresa la descripción del nivel"));
                player.sendMessage(CC.translate("&7 Escribe &c'ninguno' &7para dejar vacío"));
                break;
            case LEVEL_TP_BONUS_PERCENT:
                player.sendMessage(CC.translate("&3&l Ingresa el bonus de TPs (%)"));
                player.sendMessage(CC.translate("&7 Ejemplo: &f5 &7(5%)"));
                break;
            case LEVEL_TP_COST:
                player.sendMessage(CC.translate("&3&l Ingresa el costo de TPs"));
                break;
            case LEVEL_STAT_COST:
                player.sendMessage(CC.translate("&3&l Ingresa el costo de stat"));
                player.sendMessage(CC.translate("&7 Formato: &fSTR 10 &7o &fSTR=10"));
                player.sendMessage(CC.translate("&7 Stats: STR, DEX, CON, WIL, SPI"));
                break;
            case LEVEL_ADD_COMMAND:
                player.sendMessage(CC.translate("&3&l Ingresa el comando a ejecutar"));
                player.sendMessage(CC.translate("&7 Usa &f%player% &7para el nombre"));
                break;
            case LEVEL_REMOVE_COMMAND:
                player.sendMessage(CC.translate("&3&l Ingresa el número o texto del comando a remover"));
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

        TrainingLevel level = state.levelNumber > 0 ? training.getLevel(state.levelNumber) : null;
        if (state.levelNumber > 0 && level == null) {
            player.sendMessage(CC.translate("&c✗ Nivel no encontrado."));
            finishInput(player);
            return;
        }

        boolean success = false;
        switch (state.type) {
            case TRAINING_NAME:
                success = handleTrainingName(player, training, input);
                break;
            case TRAINING_MATERIAL:
                success = handleTrainingMaterial(player, training, input);
                break;
            case TRAINING_PERMISSION:
                success = handleTrainingPermission(player, training, input);
                break;
            case LEVEL_DESCRIPTION:
                success = handleLevelDescription(player, training, level, input);
                break;
            case LEVEL_TP_BONUS_PERCENT:
                success = handleLevelTpBonus(player, training, level, input);
                break;
            case LEVEL_TP_COST:
                success = handleLevelTpCost(player, training, level, input);
                break;
            case LEVEL_STAT_COST:
                success = handleLevelStatCost(player, training, level, input);
                break;
            case LEVEL_ADD_COMMAND:
                success = handleLevelAddCommand(player, training, level, input);
                break;
            case LEVEL_REMOVE_COMMAND:
                success = handleLevelRemoveCommand(player, training, level, input);
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

    public static void finishInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }

    private static boolean handleTrainingName(Player player, Training training, String input) {
        if (input.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ El nombre no puede estar vacío."));
            return false;
        }
        training.setName(input);
        TrainingManager.getInstance().saveTraining(training);
        player.sendMessage(CC.translate("&a✓ Nombre de &e" + training.getId() + " &aestablecido a &r" + input));
        return true;
    }

    private static boolean handleTrainingMaterial(Player player, Training training, String input) {
        try {
            int value = Integer.parseInt(input);
            if (value < 0) {
                player.sendMessage(CC.translate("&c✗ El ID no puede ser negativo."));
                return false;
            }
            training.setMaterialId(value);
            TrainingManager.getInstance().saveTraining(training);
            player.sendMessage(CC.translate("&a✓ Material de &e" + training.getId() + " &aestablecido a &f" + value));
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c✗ Número inválido."));
            return false;
        }
    }

    private static boolean handleTrainingPermission(Player player, Training training, String input) {
        if (input.equalsIgnoreCase("ninguno")) {
            training.setRequiredPermission(null);
        } else {
            training.setRequiredPermission(input);
        }
        TrainingManager.getInstance().saveTraining(training);
        player.sendMessage(CC.translate("&a✓ Permiso de &e" + training.getId() + " &aactualizado."));
        return true;
    }

    private static boolean handleLevelDescription(Player player, Training training, TrainingLevel level, String input) {
        if (input.equalsIgnoreCase("ninguno")) {
            level.setDescription(null);
        } else {
            level.setDescription(input);
        }
        TrainingManager.getInstance().saveTraining(training);
        player.sendMessage(CC.translate("&a✓ Descripción del nivel &e" + level.getLevel() + " &aactualizada."));
        return true;
    }

    private static boolean handleLevelTpBonus(Player player, Training training, TrainingLevel level, String input) {
        try {
            double value = Double.parseDouble(input);
            if (value < 0) {
                player.sendMessage(CC.translate("&c✗ El porcentaje no puede ser negativo."));
                return false;
            }
            level.setTpBonusPercent(value);
            TrainingManager.getInstance().saveTraining(training);
            player.sendMessage(CC.translate("&a✓ Bonus de TP del nivel &e" + level.getLevel() + " &aestablecido a &f" + value + "%"));
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c✗ Número inválido."));
            return false;
        }
    }

    private static boolean handleLevelTpCost(Player player, Training training, TrainingLevel level, String input) {
        try {
            int value = Integer.parseInt(input);
            if (value < 0) {
                player.sendMessage(CC.translate("&c✗ El costo no puede ser negativo."));
                return false;
            }
            level.setTpCost(value);
            TrainingManager.getInstance().saveTraining(training);
            player.sendMessage(CC.translate("&a✓ Costo de TP del nivel &e" + level.getLevel() + " &aestablecido a &f" + value));
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c✗ Número inválido."));
            return false;
        }
    }

    private static boolean handleLevelStatCost(Player player, Training training, TrainingLevel level, String input) {
        String[] parts = input.split("[= ]+", 2);
        if (parts.length < 2) {
            player.sendMessage(CC.translate("&c✗ Formato inválido. Usa: STR 10 o STR=10"));
            return false;
        }
        String stat = parts[0].toUpperCase();
        if (!stat.matches("^(STR|DEX|CON|WIL|SPI)$")) {
            player.sendMessage(CC.translate("&c✗ Stat inválido. Usa: STR, DEX, CON, WIL, SPI"));
            return false;
        }
        try {
            int value = Integer.parseInt(parts[1]);
            if (value < 0) {
                player.sendMessage(CC.translate("&c✗ El costo no puede ser negativo."));
                return false;
            }
            if (value == 0) {
                level.getStatCosts().remove(stat);
            } else {
                level.getStatCosts().put(stat, value);
            }
            TrainingManager.getInstance().saveTraining(training);
            player.sendMessage(CC.translate("&a✓ Costo de &e" + stat + " &adel nivel &e" + level.getLevel() + " &aestablecido a &f" + value));
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c✗ Número inválido."));
            return false;
        }
    }

    private static boolean handleLevelAddCommand(Player player, Training training, TrainingLevel level, String input) {
        if (input.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ El comando no puede estar vacío."));
            return false;
        }
        level.getRewardCommands().add(input);
        TrainingManager.getInstance().saveTraining(training);
        player.sendMessage(CC.translate("&a✓ Comando agregado al nivel &e" + level.getLevel() + "&a."));
        return true;
    }

    private static boolean handleLevelRemoveCommand(Player player, Training training, TrainingLevel level, String input) {
        List<String> commands = level.getRewardCommands();
        if (commands.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ Este nivel no tiene comandos."));
            return false;
        }

        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < commands.size()) {
                String removed = commands.remove(index);
                TrainingManager.getInstance().saveTraining(training);
                player.sendMessage(CC.translate("&a✓ Comando removido: &f" + removed));
                return true;
            }
        } catch (NumberFormatException ignored) {
        }

        boolean removed = commands.remove(input);
        if (removed) {
            TrainingManager.getInstance().saveTraining(training);
            player.sendMessage(CC.translate("&a✓ Comando removido: &f" + input));
            return true;
        } else {
            player.sendMessage(CC.translate("&c✗ Comando no encontrado."));
            return false;
        }
    }
}
