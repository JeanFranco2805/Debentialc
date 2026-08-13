package org.debentialc.raids.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.raids.managers.*;
import org.debentialc.raids.models.Party;
import org.debentialc.raids.models.Raid;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Menú público de raids disponibles.
 * Se abre con /raid o /raids.
 * Al hacer clic en una raid se inicia automáticamente para la party del jugador.
 */
public class RaidUserMenu {

    public static void open(Player player) {
        RaidCategoryMenu.open(player);
    }

    public static SmartInventory createMenu() {
        List<Raid> availableRaids = RaidManager.getEnabledRaids();

        return SmartInventory.builder()
                .id("raid_user_menu")
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        ItemStack borderItem = RaidConfig.getBorderItem();
                        contents.fillBorders(ClickableItem.empty(borderItem));

                        // Título decorativo
                        ItemStack titleItem = RaidConfig.getMenuItem();
                        ItemMeta titleMeta = titleItem.getItemMeta();
                        titleMeta.setDisplayName(CC.translate(RaidConfig.getMenuTitle()));
                        titleItem.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(titleItem));

                        if (availableRaids.isEmpty()) {
                            ItemStack emptyItem = RaidConfig.getMenuItem().clone();
                            ItemMeta emptyMeta = emptyItem.getItemMeta();
                            emptyMeta.setDisplayName(CC.translate("&cNo hay raids disponibles"));
                            emptyItem.setItemMeta(emptyMeta);
                            contents.set(1, 4, ClickableItem.empty(emptyItem));
                            return;
                        }

                        int row = 1;
                        int col = 1;

                        for (Raid raid : availableRaids) {
                            final Raid finalRaid = raid;

                            ItemStack item;
                            if (raid.getMenuItem() != null && !raid.getMenuItem().isEmpty()) {
                                item = RaidConfig.parseItem(raid.getMenuItem()).clone();
                            } else {
                                item = RaidConfig.getMenuItem().clone();
                            }
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate("&6&l" + raid.getDisplayName()));

                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7" + (raid.getDescription() != null ? raid.getDescription() : "Sin descripción")));
                            lore.add("");
                            lore.add(CC.translate("&eOleadas: &f" + raid.getTotalWaves()));
                            lore.add(CC.translate("&eJugadores: &f" + raid.getMinPlayers() + "-" + raid.getMaxPlayers()));
                            lore.add(CC.translate("&eCooldown: &f" + formatCooldown(raid.getCooldownSeconds())));
                            lore.add("");
                            lore.add(CC.translate("&a[CLICK] &7Iniciar con tu party"));
                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            contents.set(row, col, ClickableItem.of(item, e -> {
                                player.closeInventory();
                                attemptStartRaid(player, finalRaid);
                            }));

                            col++;
                            if (col >= 8) {
                                col = 1;
                                row++;
                                if (row >= 4) break;
                            }
                        }
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(RaidConfig.getMenuRows(), 9)
                .title(CC.translate(RaidConfig.getMenuTitle()))
                .build();
    }

    private static void attemptStartRaid(Player player, Raid raid) {
        Party party = PartyManager.getPlayerParty(player.getUniqueId());

        if (party == null) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ No estás en una party"));
            player.sendMessage(CC.translate("&7Crea una con &f/party create"));
            player.sendMessage("");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Solo el líder de la party puede iniciar una raid"));
            player.sendMessage("");
            return;
        }

        if (!PartyManager.canStartRaid(party)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Se necesitan al menos 2 jugadores en la party (actual: " + party.getMemberCount() + ")"));
            player.sendMessage("");
            return;
        }

        // Validación de cooldown para todos los miembros
        for (UUID memberId : party.getActivePlayers()) {
            if (CooldownManager.hasCooldown(memberId, raid.getRaidId())) {
                Player member = org.bukkit.Bukkit.getPlayer(memberId);
                String memberName = member != null ? member.getName() : memberId.toString();
                String timeFormatted = CooldownManager.getCooldownFormattedTime(memberId, raid.getRaidId());

                player.sendMessage("");
                player.sendMessage(CC.translate("&c✗ &f" + memberName + " tiene cooldown activo"));
                player.sendMessage(CC.translate("&7Tiempo restante: &f" + timeFormatted));
                player.sendMessage("");
                return;
            }
        }

        if (RaidSessionManager.hasActiveSession(raid.getRaidId())) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Otra party ya está haciendo esta raid. Espera a que terminen."));
            player.sendMessage("");
            return;
        }

        List<Player> partyPlayers = new ArrayList<>();
        for (UUID memberId : party.getActivePlayers()) {
            Player member = org.bukkit.Bukkit.getPlayer(memberId);
            if (member != null) {
                partyPlayers.add(member);
            }
        }

        boolean started = RaidSessionManager.startRaid(raid, partyPlayers, player);
        if (started) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&a✓ Raid iniciada: &f" + raid.getDisplayName()));
            player.sendMessage("");
        } else {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ No se pudo iniciar la raid. Contacta un administrador."));
            player.sendMessage("");
        }
    }

    private static String formatCooldown(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}
