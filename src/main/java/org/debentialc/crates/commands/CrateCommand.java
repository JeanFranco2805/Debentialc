package org.debentialc.crates.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.debentialc.crates.managers.CrateManager;
import org.debentialc.crates.menus.CrateMenus;
import org.debentialc.crates.models.Crate;
import org.debentialc.crates.models.CrateItem;
import org.debentialc.crates.models.CrateRarity;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.util.Map;

public class CrateCommand extends BaseCommand {

    @Command(name = "crate", aliases = {"crates"}, permission = "dbcplugin.crates.admin", inGameOnly = false)
    public void onCommand(CommandArgs command) {
        if (command.length() < 1) {
            if (command.isPlayer()) {
                CrateMenus.openMainMenu(command.getPlayer(), 1);
            } else {
                sendHelp(command.getSender());
            }
            return;
        }

        String sub = command.getArgs(0).toLowerCase();

        switch (sub) {
            case "create":
                handleCreate(command);
                break;
            case "delete":
                handleDelete(command);
                break;
            case "add":
                handleAdd(command);
                break;
            case "remove":
                handleRemove(command);
                break;
            case "key":
                handleKey(command);
                break;
            case "list":
                handleList(command);
                break;
            case "info":
                handleInfo(command);
                break;
            case "rarity":
                handleRarity(command);
                break;
            case "reload":
                handleReload(command);
                break;
            case "menu":
                if (command.isPlayer()) {
                    CrateMenus.openMainMenu(command.getPlayer(), 1);
                } else {
                    command.getSender().sendMessage(CC.translate("&c✗ Solo jugadores pueden abrir el menú."));
                }
                break;
            default:
                sendHelp(command.getSender());
        }
    }

    private void handleCreate(CommandArgs command) {
        if (!command.isPlayer()) {
            command.getSender().sendMessage(CC.translate("&c✗ Solo jugadores pueden crear crates físicas."));
            return;
        }
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&cUso: /crate create <id> [nombre]"));
            return;
        }
        Player player = command.getPlayer();
        String id = command.getArgs(1).toLowerCase();
        String name = command.length() > 2 ? command.getArgs(2) : id;
        Location loc = player.getLocation().clone().subtract(0, 1, 0).getBlock().getLocation();
        if (CrateManager.getInstance().createCrate(id, CC.translate(name), loc)) {
            command.getSender().sendMessage(CC.translate("&a✓ Crate &f" + id + " &acreada debajo de ti."));
        } else {
            command.getSender().sendMessage(CC.translate("&c✗ La crate &f" + id + " &cya existe."));
        }
    }

    private void handleDelete(CommandArgs command) {
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&cUso: /crate delete <id>"));
            return;
        }
        String id = command.getArgs(1).toLowerCase();
        if (CrateManager.getInstance().deleteCrate(id)) {
            command.getSender().sendMessage(CC.translate("&a✓ Crate &f" + id + " &aeliminada."));
        } else {
            command.getSender().sendMessage(CC.translate("&c✗ La crate &f" + id + " &cno existe."));
        }
    }

    private void handleAdd(CommandArgs command) {
        if (!(command.getSender() instanceof Player)) {
            command.getSender().sendMessage(CC.translate("&c✗ Solo jugadores pueden usar este comando."));
            return;
        }
        Player player = (Player) command.getSender();
        if (command.length() < 3) {
            player.sendMessage(CC.translate("&cUso: /crate add <crate> <rareza> [id_item]"));
            return;
        }
        String crateId = command.getArgs(1).toLowerCase();
        String rarityId = command.getArgs(2).toUpperCase();
        Crate crate = CrateManager.getInstance().getCrate(crateId);
        if (crate == null) {
            player.sendMessage(CC.translate("&c✗ Crate no encontrada: &f" + crateId));
            return;
        }
        ItemStack item = player.getItemInHand();
        if (item == null || item.getTypeId() == 0) {
            player.sendMessage(CC.translate("&c✗ Debes tener un item en la mano."));
            return;
        }
        String itemId = command.length() > 3 ? command.getArgs(3).toLowerCase() : ("item_" + System.currentTimeMillis());
        if (CrateManager.getInstance().addItemToCrate(crateId, itemId, rarityId, item)) {
            player.sendMessage(CC.translate("&a✓ Item agregado a la crate &f" + crateId + " &acon rareza &f" + rarityId + "&a."));
        } else {
            player.sendMessage(CC.translate("&c✗ No se pudo agregar el item. Verifica que la crate y la rareza existan."));
        }
    }

    private void handleRemove(CommandArgs command) {
        if (command.length() < 3) {
            command.getSender().sendMessage(CC.translate("&cUso: /crate remove <crate> <id_item>"));
            return;
        }
        String crateId = command.getArgs(1).toLowerCase();
        String itemId = command.getArgs(2).toLowerCase();
        if (CrateManager.getInstance().removeItemFromCrate(crateId, itemId)) {
            command.getSender().sendMessage(CC.translate("&a✓ Item &f" + itemId + " &aeliminado de la crate &f" + crateId + "&a."));
        } else {
            command.getSender().sendMessage(CC.translate("&c✗ No se pudo eliminar el item."));
        }
    }

    private void handleKey(CommandArgs command) {
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&cUso: /crate key <crate> [jugador] [cantidad]"));
            return;
        }
        String crateId = command.getArgs(1).toLowerCase();
        Player target;
        int amount = 1;
        if (command.length() >= 3) {
            target = Bukkit.getPlayerExact(command.getArgs(2));
            if (target == null) {
                command.getSender().sendMessage(CC.translate("&c✗ Jugador no encontrado: &f" + command.getArgs(2)));
                return;
            }
            if (command.length() >= 4) {
                try {
                    amount = Integer.parseInt(command.getArgs(3));
                } catch (NumberFormatException e) {
                    command.getSender().sendMessage(CC.translate("&c✗ Cantidad inválida."));
                    return;
                }
            }
        } else if (command.getSender() instanceof Player) {
            target = (Player) command.getSender();
        } else {
            command.getSender().sendMessage(CC.translate("&c✗ Debes especificar un jugador desde consola."));
            return;
        }
        CrateManager.getInstance().giveKey(target, crateId, amount);
        command.getSender().sendMessage(CC.translate("&a✓ Llave de &f" + crateId + " &adada a &f" + target.getName() + " &a(x" + amount + ")."));
    }

    private void handleList(CommandArgs command) {
        Map<String, Crate> crates = CrateManager.getInstance().getAllCrates();
        command.getSender().sendMessage(CC.translate("&8&m---------------------------------------"));
        command.getSender().sendMessage(CC.translate("&3Crates disponibles"));
        command.getSender().sendMessage(CC.translate("&8&m---------------------------------------"));
        if (crates.isEmpty()) {
            command.getSender().sendMessage(CC.translate("&7No hay crates registradas."));
        } else {
            for (Crate crate : crates.values()) {
                command.getSender().sendMessage(CC.translate("&7  &f" + crate.getId() + " &7- " + crate.getDisplayName() + " &7(" + crate.getItems().size() + " items)"));
            }
        }
        command.getSender().sendMessage(CC.translate("&8&m---------------------------------------"));
    }

    private void handleInfo(CommandArgs command) {
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&cUso: /crate info <id>"));
            return;
        }
        Crate crate = CrateManager.getInstance().getCrate(command.getArgs(1));
        if (crate == null) {
            command.getSender().sendMessage(CC.translate("&c✗ Crate no encontrada."));
            return;
        }
        command.getSender().sendMessage(CC.translate("&8&m---------------------------------------"));
        command.getSender().sendMessage(CC.translate("&3Crate: &f" + crate.getDisplayName()));
        command.getSender().sendMessage(CC.translate("&7ID: &f" + crate.getId()));
        command.getSender().sendMessage(CC.translate("&7Items: &f" + crate.getItems().size()));
        for (CrateItem item : crate.getItems().values()) {
            CrateRarity rarity = CrateManager.getInstance().getRarity(item.getRarityId());
            String rarityName = rarity != null ? rarity.getDisplayName() : item.getRarityId();
            command.getSender().sendMessage(CC.translate("&7  &f" + item.getId() + " &7- " + rarityName));
        }
        command.getSender().sendMessage(CC.translate("&8&m---------------------------------------"));
    }

    private void handleRarity(CommandArgs command) {
        if (command.length() < 2) {
            sendRarityHelp(command.getSender());
            return;
        }
        String raritySub = command.getArgs(1).toLowerCase();
        switch (raritySub) {
            case "create":
                if (command.length() < 3) {
                    command.getSender().sendMessage(CC.translate("&cUso: /crate rarity create <id> [display] [peso] [color] [announce]"));
                    return;
                }
                String id = command.getArgs(2).toUpperCase();
                String display = command.length() > 3 ? command.getArgs(3) : id;
                double weight = 1.0;
                if (command.length() > 4) {
                    try {
                        weight = Double.parseDouble(command.getArgs(4));
                        if (weight <= 0) {
                            command.getSender().sendMessage(CC.translate("&c✗ El peso debe ser mayor que 0."));
                            return;
                        }
                    } catch (NumberFormatException e) {
                        command.getSender().sendMessage(CC.translate("&c✗ Peso inválido."));
                        return;
                    }
                }
                String color = command.length() > 5 ? command.getArgs(5) : "&f";
                boolean announce = command.length() > 6 && (command.getArgs(6).equalsIgnoreCase("true") || command.getArgs(6).equalsIgnoreCase("yes") || command.getArgs(6).equalsIgnoreCase("si"));
                if (CrateManager.getInstance().createRarity(id, CC.translate(display), color, weight, announce)) {
                    command.getSender().sendMessage(CC.translate("&a✓ Rareza &f" + id + " &acreada con peso &f" + weight + "&a."));
                } else {
                    command.getSender().sendMessage(CC.translate("&c✗ La rareza &f" + id + " &cya existe."));
                }
                break;
            case "edit":
                if (command.length() < 3) {
                    command.getSender().sendMessage(CC.translate("&cUso: /crate rarity edit <id> [display] [peso] [color] [announce]"));
                    return;
                }
                String editId = command.getArgs(2).toUpperCase();
                String editDisplay = command.length() > 3 ? command.getArgs(3) : null;
                Double editWeight = null;
                if (command.length() > 4) {
                    try {
                        editWeight = Double.parseDouble(command.getArgs(4));
                        if (editWeight <= 0) {
                            command.getSender().sendMessage(CC.translate("&c✗ El peso debe ser mayor que 0."));
                            return;
                        }
                    } catch (NumberFormatException e) {
                        command.getSender().sendMessage(CC.translate("&c✗ Peso inválido."));
                        return;
                    }
                }
                String editColor = command.length() > 5 ? command.getArgs(5) : null;
                Boolean editAnnounce = null;
                if (command.length() > 6) {
                    editAnnounce = command.getArgs(6).equalsIgnoreCase("true") || command.getArgs(6).equalsIgnoreCase("yes") || command.getArgs(6).equalsIgnoreCase("si");
                }
                if (CrateManager.getInstance().updateRarity(editId, editDisplay != null ? CC.translate(editDisplay) : null, editWeight, editColor, editAnnounce)) {
                    command.getSender().sendMessage(CC.translate("&a✓ Rareza &f" + editId + " &aactualizada."));
                } else {
                    command.getSender().sendMessage(CC.translate("&c✗ La rareza &f" + editId + " &cno existe."));
                }
                break;
            case "setweight":
            case "setprob":
            case "prob":
                if (command.length() < 4) {
                    command.getSender().sendMessage(CC.translate("&cUso: /crate rarity setweight <id> <peso>"));
                    return;
                }
                String weightId = command.getArgs(2).toUpperCase();
                double newWeight;
                try {
                    newWeight = Double.parseDouble(command.getArgs(3));
                    if (newWeight <= 0) {
                        command.getSender().sendMessage(CC.translate("&c✗ El peso debe ser mayor que 0."));
                        return;
                    }
                } catch (NumberFormatException e) {
                    command.getSender().sendMessage(CC.translate("&c✗ Peso inválido."));
                    return;
                }
                if (CrateManager.getInstance().setRarityWeight(weightId, newWeight)) {
                    command.getSender().sendMessage(CC.translate("&a✓ Peso de &f" + weightId + " &aactualizado a &f" + newWeight + "&a."));
                } else {
                    command.getSender().sendMessage(CC.translate("&c✗ La rareza &f" + weightId + " &cno existe."));
                }
                break;
            case "delete":
                if (command.length() < 3) {
                    command.getSender().sendMessage(CC.translate("&cUso: /crate rarity delete <id>"));
                    return;
                }
                if (CrateManager.getInstance().deleteRarity(command.getArgs(2))) {
                    command.getSender().sendMessage(CC.translate("&a✓ Rareza eliminada."));
                } else {
                    command.getSender().sendMessage(CC.translate("&c✗ No se pudo eliminar. Verifica que exista y no esté en uso."));
                }
                break;
            case "list":
                Map<String, CrateRarity> rarities = CrateManager.getInstance().getAllRarities();
                command.getSender().sendMessage(CC.translate("&8&m---------------------------------------"));
                command.getSender().sendMessage(CC.translate("&3Rarezas disponibles"));
                command.getSender().sendMessage(CC.translate("&8&m---------------------------------------"));
                if (rarities.isEmpty()) {
                    command.getSender().sendMessage(CC.translate("&7No hay rarezas registradas."));
                } else {
                    for (CrateRarity rarity : rarities.values()) {
                        command.getSender().sendMessage(CC.translate("&7  &f" + rarity.getId() + " &7- " + rarity.getColor() + rarity.getDisplayName() + " &7(peso: " + rarity.getWeight() + ", anunciar: " + (rarity.isAnnounce() ? "sí" : "no") + ")"));
                    }
                }
                command.getSender().sendMessage(CC.translate("&8&m---------------------------------------"));
                break;
            default:
                sendRarityHelp(command.getSender());
        }
    }

    private void handleReload(CommandArgs command) {
        CrateManager.getInstance().reload();
        command.getSender().sendMessage(CC.translate("&a✓ Crates recargadas."));
    }

    private void sendHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(CC.translate("&8&m---------------------------------------"));
        sender.sendMessage(CC.translate("&3Crate Manager"));
        sender.sendMessage(CC.translate("&8&m---------------------------------------"));
        sender.sendMessage(CC.translate("&e/crate create <id> [nombre]"));
        sender.sendMessage(CC.translate("&e/crate delete <id>"));
        sender.sendMessage(CC.translate("&e/crate add <crate> <rareza> [id_item]"));
        sender.sendMessage(CC.translate("&e/crate remove <crate> <id_item>"));
        sender.sendMessage(CC.translate("&e/crate key <crate> [jugador] [cantidad]"));
        sender.sendMessage(CC.translate("&e/crate list"));
        sender.sendMessage(CC.translate("&e/crate info <id>"));
        sender.sendMessage(CC.translate("&e/crate rarity <create|edit|setweight|delete|list>"));
        sender.sendMessage(CC.translate("&e/crate menu"));
        sender.sendMessage(CC.translate("&e/crate reload"));
        sender.sendMessage(CC.translate("&8&m---------------------------------------"));
    }

    private void sendRarityHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(CC.translate("&8&m---------------------------------------"));
        sender.sendMessage(CC.translate("&3Rarezas - Ayuda"));
        sender.sendMessage(CC.translate("&8&m---------------------------------------"));
        sender.sendMessage(CC.translate("&e/crate rarity create <id> [display] [peso] [color] [announce]"));
        sender.sendMessage(CC.translate("&e/crate rarity edit <id> [display] [peso] [color] [announce]"));
        sender.sendMessage(CC.translate("&e/crate rarity setweight <id> <peso>"));
        sender.sendMessage(CC.translate("&e/crate rarity delete <id>"));
        sender.sendMessage(CC.translate("&e/crate rarity list"));
        sender.sendMessage(CC.translate("&8&m---------------------------------------"));
    }
}
