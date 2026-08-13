package org.debentialc.raids.menus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.raids.managers.RaidConfig;
import org.debentialc.raids.managers.RaidManager;
import org.debentialc.raids.managers.RaidStorageManager;
import org.debentialc.raids.models.Raid;
import org.debentialc.raids.models.Wave;
import org.debentialc.raids.models.SpawnPoint;
import org.debentialc.raids.models.WaveReward;
import org.debentialc.service.CC;

import java.util.HashMap;
import java.util.UUID;

/**
 * Gestiona inputs de chat para el sistema de raids
 * Similar a ArmorEditManager, ItemEditManager, etc. del sistema de custom items
 *
 * IMPORTANTE: processInput se llama desde AsyncPlayerChatEvent (hilo async).
 * Todas las operaciones de Bukkit (abrir menú, tp, etc.) deben ejecutarse en el hilo principal.
 */
public class RaidChatInputManager {

    public static class RaidInputState {
        public String inputType;
        public String raidId;
        public int waveIndex;
        public int step;
        public String tempName;
        public String tempDescription;
        public String tempNpcName;
        public int tempNpcTab;
        public int tempQuantity;
        public String tempCommand;
        // Para guardar la ubicación del jugador al momento de iniciar el input
        public Location savedLocation;

        public RaidInputState(String inputType, String raidId) {
            this.inputType = inputType;
            this.raidId = raidId;
            this.waveIndex = -1;
            this.step = 0;
            this.tempNpcTab = 10;
        }

        public RaidInputState(String inputType, String raidId, int waveIndex) {
            this.inputType = inputType;
            this.raidId = raidId;
            this.waveIndex = waveIndex;
            this.step = 0;
            this.tempNpcTab = 10;
        }
    }

    private static final HashMap<UUID, RaidInputState> playersInputting = new HashMap<>();

    public static boolean isInputting(Player player) {
        return playersInputting.containsKey(player.getUniqueId());
    }

    public static void cancelInput(Player player) {
        if (playersInputting.remove(player.getUniqueId()) != null) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Cancelado"));
            player.sendMessage("");
        }
    }

    /**
     * Finaliza el input de forma atómica. Retorna true si el jugador estaba inputting
     * y fue removido exitosamente (previene doble procesamiento).
     */
    private static boolean finishInput(Player player) {
        return playersInputting.remove(player.getUniqueId()) != null;
    }

    // ====== INICIAR INPUTS ======

    public static void startCreateRaidInput(Player player) {
        playersInputting.put(player.getUniqueId(), new RaidInputState("create_raid", null));
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  Crear Nueva Raid"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Paso 1/3: Ingresa el &fID &7de la raid"));
        player.sendMessage(CC.translate("&7  Ejemplo: &fRAID_SSJF"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startRaidRenameInput(Player player, String raidId) {
        playersInputting.put(player.getUniqueId(), new RaidInputState("rename", raidId));
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&e&l  Cambiar Nombre Visible"));
        player.sendMessage(CC.translate("&7  Ingresa el nombre visible en los menús"));
        player.sendMessage(CC.translate("&7  Deja vacío o escribe '-' para mostrar el ID"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startRaidIdInput(Player player, String raidId) {
        playersInputting.put(player.getUniqueId(), new RaidInputState("change_id", raidId));
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&e&l  Cambiar ID de Raid"));
        player.sendMessage(CC.translate("&7  Ingresa el nuevo ID"));
        player.sendMessage(CC.translate("&7  Ejemplo: &fRAID_NUEVO"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startRaidMenuItemInput(Player player, String raidId) {
        playersInputting.put(player.getUniqueId(), new RaidInputState("set_menu_item", raidId));
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&e&l  Cambiar Ícono de Raid"));
        player.sendMessage(CC.translate("&7  Ingresa el material o ID de custom item"));
        player.sendMessage(CC.translate("&7  Ejemplos: &fDIAMOND_SWORD &7o &f6207"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startRaidDescriptionInput(Player player, String raidId) {
        playersInputting.put(player.getUniqueId(), new RaidInputState("description", raidId));
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&b&l  Editar Descripción"));
        player.sendMessage(CC.translate("&7  Ingresa la nueva descripción"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startCooldownInput(Player player, String raidId) {
        playersInputting.put(player.getUniqueId(), new RaidInputState("cooldown", raidId));
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&3&l  Cambiar Cooldown"));
        player.sendMessage(CC.translate("&7  Ingresa los minutos de cooldown"));
        player.sendMessage(CC.translate("&7  Ejemplo: &f60 &7(= 1 hora)"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startPlayersInput(Player player, String raidId) {
        playersInputting.put(player.getUniqueId(), new RaidInputState("players", raidId));
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&b&l  Configurar Jugadores"));
        player.sendMessage(CC.translate("&7  Formato: &fmin max"));
        player.sendMessage(CC.translate("&7  Ejemplo: &f2 5"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startSpawnPointInput(Player player, String raidId, int waveIndex) {
        RaidInputState state = new RaidInputState("spawn_point", raidId, waveIndex);
        state.step = 0;
        playersInputting.put(player.getUniqueId(), state);
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  Crear Punto de Spawn"));
        player.sendMessage(CC.translate("&7  Paso 1/3: Ingresa el &fnombre del NPC"));
        player.sendMessage(CC.translate("&7  Paso 2/3: Ingresa el &ftab del NPC"));
        player.sendMessage(CC.translate("&7  La posición se guardará al ingresar la cantidad"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startRewardInput(Player player, String raidId, int waveIndex) {
        RaidInputState state = new RaidInputState("reward", raidId, waveIndex);
        state.step = 0;
        playersInputting.put(player.getUniqueId(), state);
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&b&l  Agregar Recompensa"));
        player.sendMessage(CC.translate("&7  Paso 1/2: Ingresa el &fcomando &7(sin /)"));
        player.sendMessage(CC.translate("&7  Ejemplo: &fgive @p diamond 1"));
        player.sendMessage(CC.translate("&7  @p = jugadores de la raid"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startWaveDescriptionInput(Player player, String raidId, int waveIndex) {
        playersInputting.put(player.getUniqueId(), new RaidInputState("wave_desc", raidId, waveIndex));
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&e&l  Descripción de Oleada"));
        player.sendMessage(CC.translate("&7  Ingresa la descripción"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    // ====== PROCESAR INPUT ======

    /**
     * Procesa el input del jugador. Este método se llama desde el AsyncPlayerChatEvent
     * así que sincronizamos con el hilo principal para cualquier operación Bukkit.
     */
    public static void processInput(Player player, String input) {
        RaidInputState state = playersInputting.get(player.getUniqueId());
        if (state == null) return;

        // Ejecutar en el hilo principal de Bukkit para evitar problemas de concurrencia
        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(
                org.debentialc.Main.instance,
                () -> {
                    // Verificar de nuevo que sigue en input (podría haber cancelado)
                    if (!playersInputting.containsKey(player.getUniqueId())) return;

                    switch (state.inputType) {
                        case "create_raid":
                            processCreateRaid(player, input, state);
                            break;
                        case "rename":
                            processRename(player, input, state);
                            break;
                        case "change_id":
                            processChangeRaidId(player, input, state);
                            break;
                        case "set_menu_item":
                            processSetMenuItem(player, input, state);
                            break;
                        case "description":
                            processDescription(player, input, state);
                            break;
                        case "cooldown":
                            processCooldown(player, input, state);
                            break;
                        case "players":
                            processPlayers(player, input, state);
                            break;
                        case "spawn_point":
                            processSpawnPoint(player, input, state);
                            break;
                        case "reward":
                            processReward(player, input, state);
                            break;
                        case "wave_desc":
                            processWaveDescription(player, input, state);
                            break;
                    }
                },
                0L
        );
    }

    private static void processCreateRaid(Player player, String input, RaidInputState state) {
        switch (state.step) {
            case 0:
                String raidId = input.trim();
                if (raidId.isEmpty() || raidId.length() > 30 || !raidId.matches("[A-Za-z0-9_]+")) {
                    player.sendMessage(CC.translate("&c✗ ID: 1-30 caracteres, solo letras, números y guiones bajos"));
                    return;
                }
                if (RaidManager.raidExists(raidId)) {
                    player.sendMessage(CC.translate("&c✗ Ya existe una raid con ese ID"));
                    return;
                }
                state.tempName = raidId;
                state.step = 1;
                player.sendMessage(CC.translate("&a✓ ID: &f" + state.tempName));
                player.sendMessage(CC.translate("&7Paso 2/3: Ingresa la &fdescripción"));
                break;

            case 1:
                if (input.trim().isEmpty() || input.length() > 100) {
                    player.sendMessage(CC.translate("&c✗ Descripción: 1-100 caracteres"));
                    return;
                }
                state.tempDescription = input.trim();
                state.step = 2;
                player.sendMessage(CC.translate("&a✓ Descripción: &f" + state.tempDescription));
                player.sendMessage(CC.translate("&7Paso 3/3: Ingresa el &fcooldown en minutos"));
                break;

            case 2:
                try {
                    int minutes = Integer.parseInt(input.trim());
                    if (minutes < 1 || minutes > 1440) {
                        player.sendMessage(CC.translate("&c✗ Cooldown: 1-1440 minutos"));
                        return;
                    }

                    // Finalizar input ANTES de crear la raid (previene doble procesamiento)
                    if (!finishInput(player)) return;

                    Raid raid = RaidManager.createRaidWithId(state.tempName, null);
                    if (raid == null) {
                        player.sendMessage(CC.translate("&c✗ No se pudo crear la raid. El ID ya existe."));
                        return;
                    }
                    raid.setDescription(state.tempDescription);
                    raid.setCooldownSeconds(minutes * 60L);
                    RaidManager.updateRaid(raid);
                    RaidStorageManager.saveRaid(raid);
                    RaidStorageManager.saveAllRaids();

                    player.sendMessage("");
                    player.sendMessage(CC.translate("&a✓ Raid creada: &f" + raid.getDisplayName()));
                    player.sendMessage(CC.translate("&7ID: &f" + raid.getRaidId()));
                    player.sendMessage(CC.translate("&7Usa &f/raidadmin setname " + raid.getRaidId() + " <nombre> &7para cambiar el nombre visible"));
                    player.sendMessage("");

                    final String newRaidId = raid.getRaidId();
                    openMenuDelayed(player, () -> RaidConfigMenu.createRaidConfigMenu(newRaidId).open(player));
                } catch (NumberFormatException e) {
                    player.sendMessage(CC.translate("&c✗ Ingresa un número válido"));
                }
                break;
        }
    }

    private static void processRename(Player player, String input, RaidInputState state) {
        String trimmed = input.trim();
        if (trimmed.equals("-") || trimmed.isEmpty()) {
            trimmed = "";
        } else if (trimmed.length() > 30) {
            player.sendMessage(CC.translate("&c✗ Nombre: 1-30 caracteres"));
            return;
        }

        Raid raid = RaidManager.getRaidById(state.raidId);
        if (raid == null) {
            player.sendMessage(CC.translate("&c✗ Raid no encontrada"));
            finishInput(player);
            return;
        }

        if (!finishInput(player)) return;

        raid.setRaidName(trimmed.isEmpty() ? null : trimmed);
        RaidManager.updateRaid(raid);
        RaidStorageManager.saveRaid(raid);

        player.sendMessage(CC.translate("&a✓ Nombre visible actualizado: &f" + raid.getDisplayName()));
        openMenuDelayed(player, () -> RaidConfigMenu.createRaidConfigMenu(state.raidId).open(player));
    }

    private static void processChangeRaidId(Player player, String input, RaidInputState state) {
        String newId = input.trim();
        if (newId.isEmpty() || newId.length() > 30 || !newId.matches("[A-Za-z0-9_]+")) {
            player.sendMessage(CC.translate("&c✗ ID: 1-30 caracteres, solo letras, números y guiones bajos"));
            return;
        }

        Raid raid = RaidManager.getRaidById(state.raidId);
        if (raid == null) {
            player.sendMessage(CC.translate("&c✗ Raid no encontrada"));
            finishInput(player);
            return;
        }

        if (!finishInput(player)) return;

        if (RaidManager.changeRaidId(state.raidId, newId)) {
            player.sendMessage(CC.translate("&a✓ ID actualizado: &f" + newId));
            openMenuDelayed(player, () -> RaidConfigMenu.createRaidConfigMenu(newId).open(player));
        } else {
            player.sendMessage(CC.translate("&c✗ No se pudo cambiar el ID. ¿Ya existe?"));
            openMenuDelayed(player, () -> RaidConfigMenu.createRaidConfigMenu(state.raidId).open(player));
        }
    }

    private static void processSetMenuItem(Player player, String input, RaidInputState state) {
        String value = input.trim();
        if (value.isEmpty() || value.length() > 50) {
            player.sendMessage(CC.translate("&c✗ Valor inválido."));
            return;
        }

        Raid raid = RaidManager.getRaidById(state.raidId);
        if (raid == null) {
            player.sendMessage(CC.translate("&c✗ Raid no encontrada"));
            finishInput(player);
            return;
        }

        boolean validItemId = RaidConfig.isValidItemId(value);
        boolean validCustomItem = CustomItemCommand.items.containsKey(value);
        if (!validItemId && !validCustomItem) {
            player.sendMessage(CC.translate("&c✗ No es un material, ID de item o custom item válido."));
            return;
        }

        if (!finishInput(player)) return;

        // Normalizar materiales de Bukkit a mayúsculas, dejar IDs numéricos/custom tal cual
        String stored = value;
        if (Material.getMaterial(value.toUpperCase()) != null) {
            stored = value.toUpperCase();
        }
        raid.setMenuItem(stored);
        RaidManager.updateRaid(raid);
        RaidStorageManager.saveAllRaids();
        player.sendMessage(CC.translate("&a✓ Ícono de &f" + raid.getDisplayName() + " &aactualizado."));
        openMenuDelayed(player, () -> RaidConfigMenu.createRaidConfigMenu(state.raidId).open(player));
    }

    private static void processDescription(Player player, String input, RaidInputState state) {
        if (input.trim().isEmpty() || input.length() > 100) {
            player.sendMessage(CC.translate("&c✗ Descripción: 1-100 caracteres"));
            return;
        }

        Raid raid = RaidManager.getRaidById(state.raidId);
        if (raid == null) {
            player.sendMessage(CC.translate("&c✗ Raid no encontrada"));
            finishInput(player);
            return;
        }

        if (!finishInput(player)) return;

        raid.setDescription(input.trim());
        RaidManager.updateRaid(raid);
        RaidStorageManager.saveRaid(raid);

        player.sendMessage(CC.translate("&a✓ Descripción actualizada"));
        openMenuDelayed(player, () -> RaidConfigMenu.createRaidConfigMenu(state.raidId).open(player));
    }

    private static void processCooldown(Player player, String input, RaidInputState state) {
        try {
            int minutes = Integer.parseInt(input.trim());
            if (minutes < 1 || minutes > 1440) {
                player.sendMessage(CC.translate("&c✗ Cooldown: 1-1440 minutos"));
                return;
            }

            Raid raid = RaidManager.getRaidById(state.raidId);
            if (raid == null) {
                player.sendMessage(CC.translate("&c✗ Raid no encontrada"));
                finishInput(player);
                return;
            }

            if (!finishInput(player)) return;

            raid.setCooldownSeconds(minutes * 60L);
            RaidManager.updateRaid(raid);
            RaidStorageManager.saveRaid(raid);

            player.sendMessage(CC.translate("&a✓ Cooldown: &f" + minutes + " minutos"));
            openMenuDelayed(player, () -> RaidConfigMenu.createRaidConfigMenu(state.raidId).open(player));
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c✗ Ingresa un número válido"));
        }
    }

    private static void processPlayers(Player player, String input, RaidInputState state) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length != 2) {
            player.sendMessage(CC.translate("&c✗ Formato: &fmin max &7(ejemplo: &f2 5&7)"));
            return;
        }

        try {
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);

            if (min < 1 || min > 10 || max < min || max > 10) {
                player.sendMessage(CC.translate("&c✗ Valores: 1-10, min <= max"));
                return;
            }

            Raid raid = RaidManager.getRaidById(state.raidId);
            if (raid == null) {
                player.sendMessage(CC.translate("&c✗ Raid no encontrada"));
                finishInput(player);
                return;
            }

            if (!finishInput(player)) return;

            raid.setMinPlayers(min);
            raid.setMaxPlayers(max);
            RaidManager.updateRaid(raid);
            RaidStorageManager.saveRaid(raid);

            player.sendMessage(CC.translate("&a✓ Jugadores: &f" + min + "-" + max));
            openMenuDelayed(player, () -> RaidConfigMenu.createRaidConfigMenu(state.raidId).open(player));
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c✗ Ingresa dos números válidos separados por espacio"));
        }
    }

    private static void processSpawnPoint(Player player, String input, RaidInputState state) {
        Raid raid = RaidManager.getRaidById(state.raidId);
        if (raid == null || raid.getWaveByIndex(state.waveIndex) == null) {
            player.sendMessage(CC.translate("&c✗ Raid u oleada no encontrada"));
            finishInput(player);
            return;
        }

        switch (state.step) {
            case 0: // Nombre NPC
                if (input.trim().isEmpty() || input.length() > 30) {
                    player.sendMessage(CC.translate("&c✗ Nombre: 1-30 caracteres"));
                    return;
                }
                state.tempNpcName = input.trim();
                state.step = 1;
                player.sendMessage(CC.translate("&a✓ NPC: &f" + state.tempNpcName));
                player.sendMessage(CC.translate("&7Paso 2/3: Ingresa el &ftab del NPC &7(1-100)"));
                break;

            case 1: // Tab
                try {
                    int tab = Integer.parseInt(input.trim());
                    if (tab < 1 || tab > 100) {
                        player.sendMessage(CC.translate("&c✗ Tab: 1-100"));
                        return;
                    }
                    state.tempNpcTab = tab;
                    state.step = 2;
                    player.sendMessage(CC.translate("&a✓ Tab: &f" + tab));
                    player.sendMessage(CC.translate("&7Paso 3/3: Ingresa la &fcantidad de NPCs &7(1-100)"));
                } catch (NumberFormatException e) {
                    player.sendMessage(CC.translate("&c✗ Ingresa un número válido"));
                }
                break;

            case 2: // Cantidad
                try {
                    int quantity = Integer.parseInt(input.trim());
                    if (quantity < 1 || quantity > 100) {
                        player.sendMessage(CC.translate("&c✗ Cantidad: 1-100"));
                        return;
                    }

                    // Capturar posición exacta justo antes de registrar
                    Location loc = player.getLocation().clone();

                    // Finalizar input ANTES de guardar
                    if (!finishInput(player)) return;
                    Wave wave = raid.getWaveByIndex(state.waveIndex);
                    SpawnPoint spawn = new SpawnPoint(loc, state.tempNpcName, state.tempNpcTab, quantity);
                    wave.addSpawnPoint(spawn);
                    RaidManager.updateRaid(raid);
                    RaidStorageManager.saveRaid(raid);

                    // MEJORADO: Feedback claro
                    player.sendMessage("");
                    player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                    player.sendMessage(CC.translate("&a&l✓ SPAWN POINT CREADO"));
                    player.sendMessage("");
                    player.sendMessage(CC.translate("&7NPC: &f" + state.tempNpcName));
                    player.sendMessage(CC.translate("&7Tab: &f" + state.tempNpcTab));
                    player.sendMessage(CC.translate("&7Cantidad: &f" + quantity));
                    player.sendMessage(CC.translate("&7Posición: &f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
                    player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                    player.sendMessage("");

                    final String finalRaidId = state.raidId;
                    final int waveIdx = state.waveIndex;
                    org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(
                            org.debentialc.Main.instance,
                            () -> RaidWaveConfigMenu.createWaveConfigMenu(finalRaidId, waveIdx).open(player),
                            5L
                    );
                } catch (NumberFormatException e) {
                    player.sendMessage(CC.translate("&c✗ Ingresa un número válido"));
                }
                break;
        }
    }

    private static void processReward(Player player, String input, RaidInputState state) {
        Raid raid = RaidManager.getRaidById(state.raidId);
        if (raid == null || raid.getWaveByIndex(state.waveIndex) == null) {
            player.sendMessage(CC.translate("&c✗ Raid u oleada no encontrada"));
            finishInput(player);
            return;
        }

        switch (state.step) {
            case 0: // Comando
                String cmd = input.trim();
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                if (cmd.isEmpty() || cmd.length() > 200) {
                    player.sendMessage(CC.translate("&c✗ Comando: 1-200 caracteres"));
                    return;
                }
                state.tempCommand = cmd;
                state.step = 1;
                player.sendMessage(CC.translate("&a✓ Comando: &f/" + cmd));
                player.sendMessage(CC.translate("&7Paso 2/2: Ingresa la &fprobabilidad &7(0-100)"));
                break;

            case 1: // Probabilidad
                try {
                    int probability = Integer.parseInt(input.trim());
                    if (probability < 0 || probability > 100) {
                        player.sendMessage(CC.translate("&c✗ Probabilidad: 0-100"));
                        return;
                    }

                    if (!finishInput(player)) return;

                    Wave wave = raid.getWaveByIndex(state.waveIndex);
                    WaveReward reward = new WaveReward(state.tempCommand, probability);
                    wave.addReward(reward);
                    RaidManager.updateRaid(raid);
                    RaidStorageManager.saveRaid(raid);

                    // MEJORADO: Feedback claro
                    player.sendMessage("");
                    player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                    player.sendMessage(CC.translate("&a&l✓ RECOMPENSA AGREGADA"));
                    player.sendMessage("");
                    player.sendMessage(CC.translate("&7Comando: &f/" + state.tempCommand));
                    player.sendMessage(CC.translate("&7Probabilidad: &f" + probability + "%"));
                    player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                    player.sendMessage("");

                    final String finalRaidId = state.raidId;
                    final int waveIdx = state.waveIndex;
                    org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(
                            org.debentialc.Main.instance,
                            () -> RaidWaveConfigMenu.createWaveConfigMenu(finalRaidId, waveIdx).open(player),
                            5L
                    );
                } catch (NumberFormatException e) {
                    player.sendMessage(CC.translate("&c✗ Ingresa un número válido"));
                }
                break;
        }
    }

    private static void processWaveDescription(Player player, String input, RaidInputState state) {
        Raid raid = RaidManager.getRaidById(state.raidId);
        if (raid == null || raid.getWaveByIndex(state.waveIndex) == null) {
            player.sendMessage(CC.translate("&c✗ Raid u oleada no encontrada"));
            finishInput(player);
            return;
        }

        if (!finishInput(player)) return;

        Wave wave = raid.getWaveByIndex(state.waveIndex);
        wave.setDescription(input.trim());
        RaidManager.updateRaid(raid);
        RaidStorageManager.saveRaid(raid);

        player.sendMessage(CC.translate("&a✓ Descripción de oleada actualizada"));

        final String finalRaidId = state.raidId;
        final int waveIdx = state.waveIndex;
        openMenuDelayed(player, () -> RaidWaveConfigMenu.createWaveConfigMenu(finalRaidId, waveIdx).open(player));
    }

    /**
     * Abre un menú con delay para evitar problemas de sincronización
     */
    private static void openMenuDelayed(Player player, Runnable menuOpener) {
        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(
                org.debentialc.Main.instance,
                menuOpener,
                2L
        );
    }
}