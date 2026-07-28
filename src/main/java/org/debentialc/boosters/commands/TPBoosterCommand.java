package org.debentialc.boosters.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.debentialc.boosters.integration.BoosterTPAPI;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

public class TPBoosterCommand extends BaseCommand {

    @Command(name = "tpbooster", aliases = {"tpboost"}, permission = Permissions.COMMAND + "tpbooster", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (command.length() < 1) {
            sendHelp(command.getSender());
            return;
        }

        String action = command.getArgs(0).toLowerCase();

        switch (action) {
            case "give":
                handleGive(command);
                break;

            case "test":
                if (!command.isPlayer()) {
                    command.getSender().sendMessage(CC.translate("&cDebes ser un jugador para usar test"));
                    return;
                }
                handleTest(command);
                break;

            case "info":
                if (!command.isPlayer()) {
                    command.getSender().sendMessage(CC.translate("&cDebes ser un jugador para usar info"));
                    return;
                }
                handleInfo(command);
                break;

            case "calculate":
                if (!command.isPlayer()) {
                    command.getSender().sendMessage(CC.translate("&cDebes ser un jugador para usar calculate"));
                    return;
                }
                handleCalculate(command);
                break;

            default:
                command.getSender().sendMessage(CC.translate("&cAcción desconocida: " + action));
                sendHelp(command.getSender());
                break;
        }
    }

    private void handleGive(CommandArgs command) {
        Player player;
        int amountIndex;

        if (command.isPlayer()) {
            player = command.getPlayer();
            amountIndex = 1;
            if (command.length() >= 3) {
                Player target = Bukkit.getPlayer(command.getArgs(1));
                if (target != null) {
                    player = target;
                    amountIndex = 2;
                }
            }
        } else {
            if (command.length() < 3) {
                command.getSender().sendMessage(CC.translate("&cUso desde consola: /tpbooster give <jugador> <cantidad>"));
                return;
            }
            player = Bukkit.getPlayer(command.getArgs(1));
            amountIndex = 2;
            if (player == null) {
                command.getSender().sendMessage(CC.translate("&cJugador no encontrado: &f" + command.getArgs(1)));
                return;
            }
        }

        if (command.length() < amountIndex + 1) {
            command.getSender().sendMessage(CC.translate("&cUso: /tpbooster give [jugador] <cantidad>"));
            return;
        }

        try {
            int amount = Integer.parseInt(command.getArgs(amountIndex));

            if (amount <= 0) {
                command.getSender().sendMessage(CC.translate("&cLa cantidad debe ser mayor a 0"));
                return;
            }

            BoosterTPAPI.giveTPsWithBooster(player, amount);
            command.getSender().sendMessage(CC.translate("&a✓ Se han dado &6" + amount + " &aTPs con booster a &6" + player.getName()));

        } catch (NumberFormatException e) {
            command.getSender().sendMessage(CC.translate("&cCantidad inválida"));
        }
    }

    private void handleTest(CommandArgs command) {
        Player player = command.getPlayer();

        player.sendMessage(CC.translate("&6=== Test de Boosters de TPs ==="));
        player.sendMessage("");

        int[] testAmounts = {10, 50, 100, 500, 1000};

        for (int amount : testAmounts) {
            int boosted = BoosterTPAPI.calculateBoostedTPs(player, amount);
            int bonus = boosted - amount;
            double mult = BoosterTPAPI.getCombinedMultiplier(player);

            player.sendMessage(CC.translate(String.format(
                    "  &e%d TPs &7→ &a%d TPs &7(+%d) &6[x%.2f]",
                    amount, boosted, bonus, mult
            )));
        }

        player.sendMessage("");
    }

    private void handleInfo(CommandArgs command) {
        Player player = command.getPlayer();

        player.sendMessage(CC.translate("&6=== Info de Boosters de TPs ==="));
        player.sendMessage("");

        double multiplier = BoosterTPAPI.getCombinedMultiplier(player);
        boolean hasBooster = BoosterTPAPI.hasActiveBooster(player);

        player.sendMessage(CC.translate("  &eEstado: " + (hasBooster ? "&aActivo" : "&cInactivo")));
        player.sendMessage(CC.translate("  &eMultiplicador: &6x" + String.format("%.2f", multiplier)));

        String percent = String.format("%.0f%%", (multiplier - 1.0) * 100);
        player.sendMessage(CC.translate("  &eBonus: &a+" + percent));

        player.sendMessage("");
    }

    private void handleCalculate(CommandArgs command) {
        Player player = command.getPlayer();

        if (command.length() < 2) {
            player.sendMessage(CC.translate("&cUso: /tpbooster calculate <cantidad>"));
            return;
        }

        try {
            int amount = Integer.parseInt(command.getArgs(1));

            if (amount <= 0) {
                player.sendMessage(CC.translate("&cLa cantidad debe ser mayor a 0"));
                return;
            }

            int boosted = BoosterTPAPI.calculateBoostedTPs(player, amount);
            int bonus = boosted - amount;
            double mult = BoosterTPAPI.getCombinedMultiplier(player);

            player.sendMessage(CC.translate("&6=== Cálculo de Boosters ==="));
            player.sendMessage("");
            player.sendMessage(CC.translate("  &eTPs Base: &a" + amount));
            player.sendMessage(CC.translate("  &eBonus: &6+" + bonus));
            player.sendMessage(CC.translate("  &eTotal: &b" + boosted + " TPs"));
            player.sendMessage(CC.translate("  &eMultiplicador: &6x" + String.format("%.2f", mult)));
            player.sendMessage("");

        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&cCantidad inválida"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(CC.translate("&6=== Boosters de TPs ==="));
        sender.sendMessage("");
        sender.sendMessage(CC.translate("&a/tpbooster give [jugador] <cantidad>"));
        sender.sendMessage(CC.translate("  &7Da TPs con booster aplicado"));
        sender.sendMessage(CC.translate("&a/tpbooster test"));
        sender.sendMessage(CC.translate("  &7Muestra ejemplos de boosters"));
        sender.sendMessage(CC.translate("&a/tpbooster info"));
        sender.sendMessage(CC.translate("  &7Información de boosters activos"));
        sender.sendMessage(CC.translate("&a/tpbooster calculate <cantidad>"));
        sender.sendMessage(CC.translate("  &7Calcula TPs con booster"));
        sender.sendMessage("");
    }
}