package org.debentialc.customitems.tools.pastebin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.service.CC;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * BUG 4 FIX: Los problemas eran dos:
 *
 * 1. DOBLE LISTENER: Tanto PastebinInputListener como ItemEditListener
 *    manejaban el mismo evento de chat para Pastebin. Al recibir el mensaje,
 *    ambos lo procesaban: el primero limpiaba el estado (finishPastebinInput),
 *    y el segundo fallaba porque el estado ya no existía. Esto requería enviar
 *    la URL varias veces hasta que "ganara" el handler correcto.
 *    SOLUCIÓN: Eliminar PastebinInputListener por completo. ItemEditListener
 *    ya cubre este caso. (Ver nota al final de este archivo.)
 *
 * 2. HTTP SÍNCRONO EN HILO ASYNC DE CHAT: AsyncPlayerChatEvent corre en un
 *    hilo async, y dentro se hacía la petición HTTP de forma bloqueante. Si
 *    tardaba más que el timeout del hilo, fallaba silenciosamente.
 *    SOLUCIÓN: La descarga HTTP se delega a un hilo Bukkit async separado,
 *    y el resultado se aplica de vuelta en el hilo principal.
 */
public class PastebinLoreManager {

    public static class PastebinLoreState {
        public String itemId;
        public String type; // "item" o "armor"

        public PastebinLoreState(String itemId, String type) {
            this.itemId = itemId;
            this.type = type;
        }
    }

    private static final HashMap<UUID, PastebinLoreState> playersInputting = new HashMap<>();

    public static void startPastebinInput(Player player, String itemId, String type) {
        playersInputting.put(player.getUniqueId(), new PastebinLoreState(itemId, type));

        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&3&l  Pastebin Lore Editor"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Ingresa la URL de Pastebin"));
        player.sendMessage(CC.translate("&7  Formatos válidos:"));
        player.sendMessage(CC.translate("&f  • &bpastebin.com/xxxxx"));
        player.sendMessage(CC.translate("&f  • &bpastebin.com/raw/xxxxx"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isInputtingPastebin(Player player) {
        return playersInputting.containsKey(player.getUniqueId());
    }

    public static void processPastebinInput(Player player, String input) {
        PastebinLoreState state = playersInputting.get(player.getUniqueId());
        if (state == null) return;

        // Validar formato URL
        String url = input.trim();
        if (!isValidPastebinUrl(url)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ URL inválida"));
            player.sendMessage(CC.translate("&7Usa: &fpastebin.com/xxxxx"));
            player.sendMessage("");
            startPastebinInput(player, state.itemId, state.type);
            return;
        }

        // Convertir a formato raw
        String rawUrl = convertToRawUrl(url);

        // Capturar datos necesarios antes de limpiar el estado
        final String itemId = state.itemId;
        final String type = state.type;

        // Limpiar estado AHORA, antes de entrar al hilo async,
        // para que ningún otro handler procese más mensajes de este jugador durante la descarga.
        finishPastebinInput(player);

        player.sendMessage("");
        player.sendMessage(CC.translate("&7Descargando desde Pastebin..."));

        // BUG 4 FIX: Ejecutar la descarga HTTP en un hilo async de Bukkit dedicado.
        // Antes la descarga se hacía directamente en el hilo del AsyncPlayerChatEvent,
        // lo que podía causar conflictos con el scheduler de Bukkit y fallos silenciosos.
        Bukkit.getScheduler().runTaskAsynchronously(
                org.debentialc.Main.instance,
                () -> {
                    List<String> lines = PastebinReader.downloadPastebinContent(rawUrl);

                    // Volver al hilo principal para modificar datos del juego
                    Bukkit.getScheduler().runTask(org.debentialc.Main.instance, () -> {
                        if (lines == null || lines.isEmpty()) {
                            player.sendMessage("");
                            player.sendMessage(CC.translate("&c✗ Error al descargar"));
                            player.sendMessage(CC.translate("&7Verifica la URL e inténtalo de nuevo"));
                            player.sendMessage("");
                            // Reabrir el input para que pueda reintentar
                            startPastebinInput(player, itemId, type);
                            return;
                        }

                        boolean success = false;
                        if ("item".equals(type)) {
                            success = applyLoreToItem(player, itemId, lines);
                        } else if ("armor".equals(type)) {
                            success = applyLoreToArmor(player, itemId, lines);
                        }

                        if (success) {
                            player.sendMessage("");
                            player.sendMessage(CC.translate("&a✓ Lore actualizado"));
                            player.sendMessage(CC.translate("&7Líneas: &f" + lines.size()));
                            player.sendMessage("");
                        } else {
                            player.sendMessage("");
                            player.sendMessage(CC.translate("&c✗ Error al aplicar lore"));
                            player.sendMessage("");
                        }

                        // Reabrir menú en el hilo principal
                        Bukkit.getScheduler().scheduleSyncDelayedTask(
                                org.debentialc.Main.instance,
                                () -> {
                                    if ("item".equals(type)) {
                                        org.debentialc.customitems.tools.inventory.CustomItemMenus
                                                .openEditItemMenu(itemId).open(player);
                                    } else {
                                        org.debentialc.customitems.tools.inventory.CustomArmorMenus
                                                .openEditArmorMenu(itemId).open(player);
                                    }
                                },
                                20L
                        );
                    });
                }
        );
    }

    private static boolean applyLoreToItem(Player player, String itemId, List<String> lines) {
        if (!CustomItemCommand.items.containsKey(itemId)) {
            return false;
        }

        org.debentialc.customitems.tools.ci.CustomItem item = CustomItemCommand.items.get(itemId);

        List<String> translatedLines = new java.util.ArrayList<>();
        for (String line : lines) {
            translatedLines.add(CC.translate(line));
        }

        item.setLore(translatedLines);

        org.debentialc.customitems.tools.storage.CustomItemStorage.getInstance().saveItem(item);

        return true;
    }

    private static boolean applyLoreToArmor(Player player, String armorId, List<String> lines) {
        if (!RegisterItem.items.containsKey(armorId)) {
            return false;
        }

        org.debentialc.customitems.tools.ci.CustomArmor armor = RegisterItem.items.get(armorId);

        List<String> translatedLines = new java.util.ArrayList<>();
        for (String line : lines) {
            translatedLines.add(CC.translate(line));
        }

        armor.setLore(translatedLines);

        org.debentialc.customitems.tools.storage.CustomArmorStorage storage =
                org.debentialc.customitems.tools.storage.CustomArmorStorage.getInstance();
        storage.saveArmor(armor);

        return true;
    }

    private static boolean isValidPastebinUrl(String url) {
        return url.matches("^(https?://)?(www\\.)?pastebin\\.com/(raw/)?[a-zA-Z0-9]+$") ||
                url.matches("^[a-zA-Z0-9]+$");
    }

    private static String convertToRawUrl(String url) {
        if (url.matches("^[a-zA-Z0-9]+$")) {
            return "https://pastebin.com/raw/" + url;
        }
        if (url.contains("/raw/")) {
            if (!url.startsWith("http")) {
                return "https://" + url;
            }
            return url;
        }
        url = url.replace("https://", "").replace("http://", "").replace("www.", "");
        String code = url.replace("pastebin.com/", "");
        return "https://pastebin.com/raw/" + code;
    }

    public static void cancelPastebinInput(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishPastebinInput(player);
    }

    private static void finishPastebinInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }
}

