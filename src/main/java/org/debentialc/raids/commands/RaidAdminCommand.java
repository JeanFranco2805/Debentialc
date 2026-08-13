package org.debentialc.raids.commands;

import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;
import org.debentialc.raids.menus.RaidAdminCategoryMenu;
import org.debentialc.raids.menus.RaidMainMenu;
import org.debentialc.raids.menus.RaidListMenu;
import org.debentialc.raids.menus.RaidChatInputManager;
import org.debentialc.raids.managers.RaidConfig;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.raids.managers.RaidCategoryManager;
import org.debentialc.raids.managers.RaidStorageManager;
import org.debentialc.raids.managers.CooldownManager;
import org.debentialc.raids.managers.RaidManager;
import org.debentialc.raids.managers.RaidSessionManager;
import org.debentialc.raids.managers.PartyManager;
import org.debentialc.raids.models.Party;
import org.debentialc.raids.models.Raid;
import org.debentialc.raids.models.RaidSession;
import org.debentialc.service.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RaidAdminCommand - Comando /raidadmin para administración de raids
 */
public class RaidAdminCommand extends BaseCommand {

    @Command(name = "raidadmin",
            permission = "dbcplugin.raids.admin",
            description = "Comandos de administración de raids",
            inGameOnly = false)
    public void onCommand(CommandArgs args) {
        CommandSender sender = args.getSender();
        if (sender == null) {
            return;
        }

        try {
            if (args.length() == 0) {
                if (args.isPlayer()) {
                    RaidAdminCategoryMenu.open(args.getPlayer());
                } else {
                    sendInfoHelp(sender);
                }
                return;
            }

            String subCommand = args.getArgs(0).toLowerCase();

            switch (subCommand) {
                case "create":
                case "list":
                case "menu":
                case "info":
                    if (!args.isPlayer()) {
                        sender.sendMessage(CC.translate("&c✗ El subcomando '&f" + subCommand + "&c' solo puede usarse en el juego."));
                        return;
                    }
                    Player player = args.getPlayer();
                    switch (subCommand) {
                        case "create":
                            RaidChatInputManager.startCreateRaidInput(player);
                            break;
                        case "list":
                            RaidListMenu.createRaidListMenu(1).open(player);
                            break;
                        case "menu":
                            RaidMainMenu.createMainMenu().open(player);
                            break;
                        case "info":
                            sendInfoHelp(player);
                            break;
                    }
                    break;

                case "reload":
                    RaidStorageManager.loadAllRaids();
                    CooldownManager.loadCooldowns();
                    sender.sendMessage(CC.translate("&a✓ Sistema de raids recargado"));
                    sender.sendMessage(CC.translate("&7Raids cargadas y cooldowns restaurados"));
                    break;

                case "save":
                    RaidStorageManager.saveAllRaids();
                    RaidStorageManager.saveRaidSystemData();
                    CooldownManager.saveCooldowns();
                    sender.sendMessage(CC.translate("&a✓ Raids y cooldowns guardados"));
                    break;

                case "start":
                    handleStart(sender, args);
                    break;

                case "restart":
                    handleRestart(sender, args);
                    break;

                case "restartraid":
                    handleRestartRaidId(sender, args);
                    break;

                case "resetcd":
                    handleResetCooldown(sender, args);
                    break;

                case "resetcdall":
                    handleResetAllCooldowns(sender, args);
                    break;

                case "resetcdglobal":
                    handleResetGlobalCooldowns(sender);
                    break;

                case "seticon":
                    handleSetIcon(sender, args);
                    break;

                case "setcategory":
                    handleSetCategory(sender, args);
                    break;

                case "category":
                    handleCategory(sender, args);
                    break;

                case "setname":
                    handleSetName(sender, args);
                    break;

                case "setid":
                    handleSetId(sender, args);
                    break;

                default:
                    sender.sendMessage(CC.translate("&c✗ Subcomando no reconocido: &f" + subCommand));
                    sendInfoHelp(sender);
            }
        } catch (Exception e) {
            sender.sendMessage(CC.translate("&c✗ Error interno en /raidadmin. Revisa la consola."));
            e.printStackTrace();
        }
    }

    private void handleStart(CommandSender sender, CommandArgs args) {
        if (args.length() < 3) {
            sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin start <jugador> <id_raid>"));
            return;
        }

        String targetName = args.getArgs(1);
        String raidId = args.getArgs(2);

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(CC.translate("&c✗ Jugador no encontrado: &f" + targetName));
            return;
        }

        Raid raid = RaidManager.getRaidById(raidId);
        if (raid == null) {
            sender.sendMessage(CC.translate("&c✗ Raid no encontrada: &f" + raidId));
            return;
        }

        if (!RaidManager.isRaidReadyToPlay(raidId)) {
            sender.sendMessage(CC.translate("&c✗ La raid no está configurada o deshabilitada"));
            return;
        }

        Party party = PartyManager.getPlayerParty(target.getUniqueId());
        List<Player> players = new ArrayList<>();

        // Si el jugador tiene party, iniciar para todos los miembros online
        if (party != null) {
            for (UUID memberId : party.getActivePlayers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    players.add(member);
                }
            }
        } else {
            players.add(target);
        }

        int minPlayers = raid.getMinPlayers();
        int maxPlayers = raid.getMaxPlayers();

        // Validación de party si la raid requiere más de 1 jugador
        if (minPlayers > 1 && party == null) {
            sender.sendMessage(CC.translate("&c✗ Esta raid requiere party (&f" + minPlayers + "-" + maxPlayers + "&c jugadores)"));
            return;
        }

        // Validación de cantidad mínima y máxima de jugadores
        if (players.size() < minPlayers) {
            sender.sendMessage(CC.translate("&c✗ Se necesitan al menos &f" + minPlayers + "&c jugadores online"));
            sender.sendMessage(CC.translate("&7Jugadores online en el grupo: &f" + players.size()));
            return;
        }
        if (players.size() > maxPlayers) {
            sender.sendMessage(CC.translate("&c✗ Máximo &f" + maxPlayers + "&c jugadores permitidos"));
            sender.sendMessage(CC.translate("&7Jugadores en el grupo: &f" + players.size()));
            return;
        }

        // Validación de cooldown para todos los jugadores involucrados (incluidos offline si están en party)
        List<UUID> membersToCheck = new ArrayList<>();
        if (party != null) {
            membersToCheck.addAll(party.getActivePlayers());
        } else {
            membersToCheck.add(target.getUniqueId());
        }

        for (UUID memberId : membersToCheck) {
            if (CooldownManager.hasCooldown(memberId, raidId)) {
                Player online = Bukkit.getPlayer(memberId);
                String memberName = online != null ? online.getName() : memberId.toString();
                String timeFormatted = CooldownManager.getCooldownFormattedTime(memberId, raidId);
                sender.sendMessage("");
                sender.sendMessage(CC.translate("&c✗ &f" + memberName + " tiene cooldown activo"));
                sender.sendMessage(CC.translate("&7Tiempo restante: &f" + timeFormatted));
                sender.sendMessage("");
                return;
            }
        }

        if (RaidSessionManager.hasActiveSession(raidId)) {
            sender.sendMessage(CC.translate("&c✗ Ya hay una sesión activa para esta raid"));
            return;
        }

        for (Player p : players) {
            if (RaidSessionManager.getPlayerSession(p.getUniqueId()) != null) {
                sender.sendMessage(CC.translate("&c✗ &f" + p.getName() + " ya está en una raid"));
                return;
            }
        }

        boolean started = RaidSessionManager.startRaid(raid, players, sender);
        if (started) {
            if (players.size() > 1) {
                sender.sendMessage(CC.translate("&a✓ Raid &f" + raid.getDisplayName() + " &ainiciada para la party de &f" + target.getName()));
            } else {
                sender.sendMessage(CC.translate("&a✓ Raid &f" + raid.getDisplayName() + " &ainiciada para &f" + target.getName()));
            }
            for (Player p : players) {
                p.sendMessage(CC.translate("&bℹ §fUn administrador te ha iniciado la raid &e" + raid.getDisplayName()));
            }
        } else {
            sender.sendMessage(CC.translate("&c✗ No se pudo iniciar la raid"));
        }
    }

    private void handleRestart(CommandSender sender, CommandArgs args) {
        if (args.length() < 2) {
            sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin restart <jugador>"));
            return;
        }

        String targetName = args.getArgs(1);
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(CC.translate("&c✗ Jugador no encontrado: &f" + targetName));
            return;
        }

        RaidSession session = RaidSessionManager.getPlayerSession(target.getUniqueId());
        if (session == null) {
            sender.sendMessage(CC.translate("&c✗ &f" + target.getName() + " &cno está en una raid"));
            return;
        }

        boolean restarted = RaidSessionManager.restartRaid(session, sender);
        if (restarted) {
            sender.sendMessage(CC.translate("&a✓ Raid &f" + session.getRaid().getDisplayName() + " &areiniciada para &f" + target.getName()));
            for (UUID playerId : session.getActivePlayers()) {
                Player p = Bukkit.getPlayer(playerId);
                if (p != null) {
                    p.sendMessage(CC.translate("&bℹ §fUn administrador reinició la raid &e" + session.getRaid().getDisplayName()));
                }
            }
        } else {
            sender.sendMessage(CC.translate("&c✗ No se pudo reiniciar la raid"));
        }
    }

    private void handleRestartRaidId(CommandSender sender, CommandArgs args) {
        if (args.length() < 2) {
            sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin restartraid <id_raid>"));
            return;
        }

        String raidId = args.getArgs(1);
        Raid raid = RaidManager.getRaidById(raidId);
        if (raid == null) {
            sender.sendMessage(CC.translate("&c✗ Raid no encontrada: &f" + raidId));
            return;
        }

        RaidSession session = RaidSessionManager.getSessionByRaid(raid);
        if (session == null) {
            sender.sendMessage(CC.translate("&c✗ No hay una sesión activa para la raid &f" + raidId));
            return;
        }

        boolean restarted = RaidSessionManager.restartRaid(session, sender);
        if (restarted) {
            sender.sendMessage(CC.translate("&a✓ Raid &f" + session.getRaid().getDisplayName() + " &areiniciada por ID"));
            for (UUID playerId : session.getActivePlayers()) {
                Player p = Bukkit.getPlayer(playerId);
                if (p != null) {
                    p.sendMessage(CC.translate("&bℹ §fUn administrador reinició la raid &e" + session.getRaid().getDisplayName()));
                }
            }
        } else {
            sender.sendMessage(CC.translate("&c✗ No se pudo reiniciar la raid"));
        }
    }

    private void handleResetCooldown(CommandSender sender, CommandArgs args) {
        if (args.length() < 3) {
            sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin resetcd <jugador> <id_raid>"));
            return;
        }

        String targetName = args.getArgs(1);
        String raidId = args.getArgs(2);

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(CC.translate("&c✗ Jugador no encontrado: &f" + targetName));
            return;
        }

        Raid raid = RaidManager.getRaidById(raidId);
        if (raid == null) {
            sender.sendMessage(CC.translate("&c✗ Raid no encontrada: &f" + raidId));
            return;
        }

        CooldownManager.clearCooldown(target.getUniqueId(), raidId);
        sender.sendMessage(CC.translate("&a✓ Cooldown de la raid &f" + raid.getDisplayName() + " &areiniciado para &f" + target.getName()));
        target.sendMessage(CC.translate("&bℹ §fUn administrador reinició tu cooldown para &e" + raid.getDisplayName()));
    }

    private void handleResetAllCooldowns(CommandSender sender, CommandArgs args) {
        if (args.length() < 2) {
            sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin resetcdall <jugador>"));
            return;
        }

        String targetName = args.getArgs(1);
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(CC.translate("&c✗ Jugador no encontrado: &f" + targetName));
            return;
        }

        CooldownManager.clearAllPlayerCooldowns(target.getUniqueId());
        sender.sendMessage(CC.translate("&a✓ Todos los cooldowns de raid reiniciados para &f" + target.getName()));
        target.sendMessage(CC.translate("&bℹ §fUn administrador reinició todos tus cooldowns de raid"));
    }

    private void handleResetGlobalCooldowns(CommandSender sender) {
        CooldownManager.clearAllCooldowns();
        sender.sendMessage(CC.translate("&a✓ Todos los cooldowns de raid de todos los jugadores han sido reiniciados"));
        Bukkit.broadcastMessage(CC.translate("&bℹ §fUn administrador reinició todos los cooldowns de raid"));
    }

    private void handleSetIcon(CommandSender sender, CommandArgs args) {
        if (args.length() < 3) {
            sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin seticon <id_raid> <material>"));
            return;
        }

        String raidId = args.getArgs(1);
        String itemStr = args.getArgs(2);

        Raid raid = RaidManager.getRaidById(raidId);
        if (raid == null) {
            sender.sendMessage(CC.translate("&c✗ Raid no encontrada: &f" + raidId));
            return;
        }

        Material material = Material.getMaterial(itemStr.toUpperCase());
        if (material == null) {
            sender.sendMessage(CC.translate("&c✗ Material inválido: &f" + itemStr));
            return;
        }

        raid.setMenuItem(itemStr.toUpperCase());
        RaidManager.updateRaid(raid);
        RaidStorageManager.saveAllRaids();
        sender.sendMessage(CC.translate("&a✓ Ícono de &f" + raid.getDisplayName() + " &acambiado a &f" + material.name()));
    }

    private void handleSetName(CommandSender sender, CommandArgs args) {
        if (args.length() < 3) {
            sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin setname <id_raid> <nombre>"));
            return;
        }
        Raid raid = RaidManager.getRaidById(args.getArgs(1));
        if (raid == null) {
            sender.sendMessage(CC.translate("&c✗ Raid no encontrada"));
            return;
        }
        String name = joinArgs(args.getArgs(), 2);
        if (name.equals("-") || name.isEmpty()) {
            raid.setRaidName(null);
        } else {
            raid.setRaidName(name);
        }
        RaidManager.updateRaid(raid);
        RaidStorageManager.saveAllRaids();
        sender.sendMessage(CC.translate("&a✓ Nombre visible actualizado: &f" + raid.getDisplayName()));
    }

    private void handleSetId(CommandSender sender, CommandArgs args) {
        if (args.length() < 3) {
            sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin setid <id_raid> <nuevo_id>"));
            return;
        }
        String oldId = args.getArgs(1);
        String newId = args.getArgs(2);
        if (RaidManager.getRaidById(oldId) == null) {
            sender.sendMessage(CC.translate("&c✗ Raid no encontrada"));
            return;
        }
        if (RaidManager.changeRaidId(oldId, newId)) {
            sender.sendMessage(CC.translate("&a✓ ID cambiado a &f" + newId));
        } else {
            sender.sendMessage(CC.translate("&c✗ No se pudo cambiar el ID. ¿Ya existe o es inválido?"));
        }
    }

    private void handleSetCategory(CommandSender sender, CommandArgs args) {
        if (args.length() < 3) {
            sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin setcategory <id_raid> <categoria>"));
            sender.sendMessage(CC.translate("&7Usa '&fninguna&7' para quitar la categoría."));
            return;
        }

        String raidId = args.getArgs(1);
        String category = args.getArgs(2);

        Raid raid = RaidManager.getRaidById(raidId);
        if (raid == null) {
            sender.sendMessage(CC.translate("&c✗ Raid no encontrada: &f" + raidId));
            return;
        }

        boolean remove = category.equalsIgnoreCase("ninguna") || category.equalsIgnoreCase("none") || category.equals("-");
        String targetCat = remove ? null : category;

        if (!remove && !RaidCategoryManager.categoryExists(category)) {
            sender.sendMessage(CC.translate("&c✗ Categoría no existe: &f" + category));
            sender.sendMessage(CC.translate("&7Créala con &f/raidadmin category create " + category + " <nombre> <item_id>"));
            return;
        }

        if (RaidCategoryManager.assignRaidCategory(raid, targetCat)) {
            if (remove) {
                sender.sendMessage(CC.translate("&a✓ Categoría removida de &f" + raid.getDisplayName()));
            } else {
                sender.sendMessage(CC.translate("&a✓ Categoría de &f" + raid.getDisplayName() + " &acambiada a &f" + RaidCategoryManager.getDisplayName(category)));
                sender.sendMessage(CC.translate("&7Orden en categoría: &f" + raid.getCategoryOrder()));
            }
        } else {
            sender.sendMessage(CC.translate("&c✗ No se pudo cambiar la categoría."));
        }
    }

    private void handleCategory(CommandSender sender, CommandArgs args) {
        if (args.length() < 2) {
            sendCategoryHelp(sender);
            return;
        }

        String sub = args.getArgs(1).toLowerCase();
        switch (sub) {
            case "create":
                if (args.length() < 5) {
                    sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin category create <id> <nombre> <item_id>"));
                    return;
                }
                handleCategoryCreate(sender, args);
                break;
            case "delete":
                if (args.length() < 3) {
                    sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin category delete <id>"));
                    return;
                }
                if (RaidCategoryManager.deleteCategory(args.getArgs(2))) {
                    sender.sendMessage(CC.translate("&a✓ Categoría eliminada."));
                } else {
                    sender.sendMessage(CC.translate("&c✗ Categoría no encontrada."));
                }
                break;
            case "setname":
                if (args.length() < 4) {
                    sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin category setname <id> <nombre>"));
                    return;
                }
                {
                    String id = args.getArgs(2);
                    String name = joinArgs(args.getArgs(), 3);
                    if (RaidCategoryManager.setDisplayName(id, name)) {
                        sender.sendMessage(CC.translate("&a✓ Nombre de categoría actualizado."));
                    } else {
                        sender.sendMessage(CC.translate("&c✗ Categoría no encontrada."));
                    }
                }
                break;
            case "setitem":
                if (args.length() < 4) {
                    sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin category setitem <id> <item_id>"));
                    return;
                }
                {
                    String id = args.getArgs(2);
                    String itemId = args.getArgs(3);
                    if (!CustomItemCommand.items.containsKey(itemId)) {
                        sender.sendMessage(CC.translate("&c✗ No existe un custom item con id &f" + itemId));
                        return;
                    }
                    if (RaidCategoryManager.setMenuItemId(id, itemId)) {
                        sender.sendMessage(CC.translate("&a✓ Ícono de categoría actualizado."));
                    } else {
                        sender.sendMessage(CC.translate("&c✗ Categoría no encontrada."));
                    }
                }
                break;
            case "list":
                java.util.List<org.debentialc.raids.models.RaidCategory> cats = RaidCategoryManager.getAllCategories();
                if (cats.isEmpty()) {
                    sender.sendMessage(CC.translate("&cNo hay categorías registradas."));
                    return;
                }
                sender.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                sender.sendMessage(CC.translate("&6&lCategorías de Raid"));
                for (org.debentialc.raids.models.RaidCategory cat : cats) {
                    sender.sendMessage(CC.translate("&f" + cat.getId() + " &7- &e" + cat.getDisplayName() + " &7Item: &f" + cat.getMenuItemId()));
                }
                sender.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                break;
            case "help":
            default:
                sendCategoryHelp(sender);
                break;
        }
    }

    private void handleCategoryCreate(CommandSender sender, CommandArgs args) {
        String id = args.getArgs(2).toLowerCase();
        if (!id.matches("[a-z0-9_]+")) {
            sender.sendMessage(CC.translate("&c✗ El id debe ser alfanumérico y minúsculas."));
            return;
        }
        String displayName = joinArgs(args.getArgs(), 3);
        // El último argumento es el item_id; extraerlo del nombre
        int lastSpace = displayName.lastIndexOf(' ');
        if (lastSpace == -1) {
            sender.sendMessage(CC.translate("&c✗ Uso: /raidadmin category create <id> <nombre> <item_id>"));
            return;
        }
        String name = displayName.substring(0, lastSpace).trim();
        String itemId = displayName.substring(lastSpace + 1).trim();

        if (name.isEmpty() || itemId.isEmpty()) {
            sender.sendMessage(CC.translate("&c✗ Debes indicar nombre e item_id."));
            return;
        }
        if (!CustomItemCommand.items.containsKey(itemId)) {
            sender.sendMessage(CC.translate("&c✗ No existe un custom item con id &f" + itemId));
            return;
        }
        if (RaidCategoryManager.createCategory(id, name, itemId)) {
            sender.sendMessage(CC.translate("&a✓ Categoría &f" + id + " &acreada."));
        } else {
            sender.sendMessage(CC.translate("&c✗ Ya existe una categoría con id &f" + id));
        }
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private void sendCategoryHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        sender.sendMessage(CC.translate("&6&lAdmin - Categorías de Raid"));
        sender.sendMessage("");
        sender.sendMessage(CC.translate("&f/raidadmin category create <id> <nombre> <item_id>"));
        sender.sendMessage(CC.translate("&7  Crea una categoría con un item custom de ícono."));
        sender.sendMessage(CC.translate("&f/raidadmin category delete <id>"));
        sender.sendMessage(CC.translate("&f/raidadmin category setname <id> <nombre>"));
        sender.sendMessage(CC.translate("&f/raidadmin category setitem <id> <item_id>"));
        sender.sendMessage(CC.translate("&f/raidadmin category list"));
        sender.sendMessage(CC.translate("&f/raidadmin setcategory <id_raid> <categoria>"));
        sender.sendMessage(CC.translate("&7  Asigna una raid a una categoría. Usa 'ninguna' para quitar."));
        sender.sendMessage("");
        sender.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    private void sendInfoHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        sender.sendMessage(CC.translate("&6&l  ADMINISTRACIÓN DE RAIDS"));
        sender.sendMessage("");
        sender.sendMessage(CC.translate("&f  /raidadmin &7- Abrir menú visual"));
        sender.sendMessage(CC.translate("&f  /raidadmin create &7- Crear nueva raid"));
        sender.sendMessage(CC.translate("&f  /raidadmin list &7- Ver todas las raids"));
        sender.sendMessage(CC.translate("&f  /raidadmin menu &7- Menú principal"));
        sender.sendMessage(CC.translate("&f  /raidadmin info &7- Esta información"));
        sender.sendMessage(CC.translate("&f  /raidadmin reload &7- Recargar datos"));
        sender.sendMessage(CC.translate("&f  /raidadmin save &7- Guardar todo"));
        sender.sendMessage(CC.translate("&f  /raidadmin start <jugador> <id_raid> &7- Iniciar raid para un jugador o su party"));
        sender.sendMessage(CC.translate("&f  /raidadmin restart <jugador> &7- Reiniciar la raid actual de un jugador"));
        sender.sendMessage(CC.translate("&f  /raidadmin restartraid <id_raid> &7- Reiniciar la sesión activa de una raid por ID"));
        sender.sendMessage(CC.translate("&f  /raidadmin seticon <id_raid> <material> &7- Cambiar ícono de la raid en menús"));
        sender.sendMessage(CC.translate("&f  /raidadmin setname <id_raid> <nombre> &7- Cambiar nombre visible"));
        sender.sendMessage(CC.translate("&f  /raidadmin setid <id_raid> <nuevo_id> &7- Cambiar ID técnico de la raid"));
        sender.sendMessage(CC.translate("&f  /raidadmin setcategory <id_raid> <categoria> &7- Asignar raid a una categoría"));
        sender.sendMessage(CC.translate("&f  /raidadmin category ... &7- Administrar categorías"));
        sender.sendMessage(CC.translate("&f  /raidadmin resetcd <jugador> <id_raid> &7- Reiniciar cooldown de una raid para un jugador"));
        sender.sendMessage(CC.translate("&f  /raidadmin resetcdall <jugador> &7- Reiniciar todos los cooldowns de raid de un jugador"));
        sender.sendMessage(CC.translate("&f  /raidadmin resetcdglobal &7- Reiniciar todos los cooldowns de raid de todos los jugadores"));
        sender.sendMessage("");
        sender.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        sender.sendMessage("");
    }
}
