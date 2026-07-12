package org.debentialc.claims.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.claims.managers.ClaimsPermissions;
import org.debentialc.claims.managers.TerrainManager;
import org.debentialc.claims.models.Terrain;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TerritoryInfoMenu {

    public static SmartInventory createInfoMenu(final Terrain terrain, final Player viewer) {
        return SmartInventory.builder()
                .id("territory_info_" + terrain.getId())
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(TerritoryMenu.createGlassPane((short) 7)));

                        // ── Info principal ──────────────────────────────────────────
                        ItemStack infoItem = new ItemStack(Material.MAP);
                        ItemMeta infoMeta = infoItem.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&f&l" + terrain.getId()));
                        List<String> infoLore = new ArrayList<String>();
                        infoLore.add(CC.translate("&7Tamaño: &f" + terrain.getChunks() + " chunk(s)"));
                        infoLore.add(CC.translate("&7Bloques: &f" + terrain.getSizeInBlocks() + "x" + terrain.getSizeInBlocks()));
                        infoLore.add(CC.translate("&7Precio: &f$" + (int) terrain.getPrice()));
                        if (!terrain.isCommitted()) {
                            infoLore.add(CC.translate("&7Estado: &8Sin generar en el mundo"));
                        } else if (terrain.hasOwner()) {
                            infoLore.add(CC.translate("&7Propietario: &f" + terrain.getOwnerName()));
                        } else {
                            infoLore.add(CC.translate("&7Estado: &aEn venta"));
                        }
                        if (terrain.getOrigin() != null) {
                            infoLore.add(CC.translate("&7Ubicación: &f" + terrain.getOrigin().getBlockX()
                                    + ", " + terrain.getOrigin().getBlockZ()));
                            infoLore.add(CC.translate("&7Mundo: &f" + terrain.getOrigin().getWorld().getName()));
                        }
                        infoMeta.setLore(infoLore);
                        infoItem.setItemMeta(infoMeta);
                        contents.set(1, 4, ClickableItem.empty(infoItem));

                        // ── Miembros ────────────────────────────────────────────────
                        if (!terrain.getMembers().isEmpty()) {
                            ItemStack membersItem = new ItemStack(Material.SKULL_ITEM);
                            ItemMeta membersMeta = membersItem.getItemMeta();
                            membersMeta.setDisplayName(CC.translate("&b&lMiembros &7(" + terrain.getMembers().size() + ")"));
                            List<String> membersLore = new ArrayList<String>();
                            for (Map.Entry<UUID, List<Terrain.MemberRole>> entry : terrain.getMembers().entrySet()) {
                                StringBuilder roles = new StringBuilder();
                                for (Terrain.MemberRole role : entry.getValue()) {
                                    if (roles.length() > 0) roles.append(", ");
                                    roles.append(role.name().toLowerCase());
                                }
                                membersLore.add(CC.translate("&7- &f"
                                        + entry.getKey().toString().substring(0, 8)
                                        + "... &8[" + roles.toString() + "]"));
                            }
                            membersMeta.setLore(membersLore);
                            membersItem.setItemMeta(membersMeta);
                            contents.set(2, 4, ClickableItem.empty(membersItem));
                        }

                        boolean isOwner = terrain.isOwner(player.getUniqueId());
                        boolean isAdmin = player.hasPermission(ClaimsPermissions.ADMIN_MANAGE);

                        // ── Teletransporte ──────────────────────────────────────────
                        // Visible para: propietario, miembro, y visitante si el terreno
                        // ya fue generado (tiene origen).
                        if (terrain.isCommitted() && terrain.getOrigin() != null) {
                            boolean isMember = terrain.isMember(player.getUniqueId());

                            ItemStack tpBtn;
                            if (isOwner) {
                                tpBtn = new ItemStack(Material.ENDER_PEARL);
                            } else {
                                tpBtn = new ItemStack(Material.EYE_OF_ENDER);
                            }
                            ItemMeta tpMeta = tpBtn.getItemMeta();

                            if (isOwner) {
                                tpMeta.setDisplayName(CC.translate("&b&lIr a mi Terreno"));
                                tpMeta.setLore(Arrays.asList(
                                        CC.translate("&7Te teletransporta al centro de tu terreno."),
                                        CC.translate(""),
                                        CC.translate("&b[CLICK PARA TELETRANSPORTARTE]")
                                ));
                            } else {
                                tpMeta.setDisplayName(CC.translate("&a&lVisitar Terreno"));
                                String ownerLabel = terrain.hasOwner()
                                        ? "&7Propietario: &f" + terrain.getOwnerName()
                                        : "&7Terreno en venta";
                                tpMeta.setLore(Arrays.asList(
                                        CC.translate(ownerLabel),
                                        CC.translate(""),
                                        CC.translate("&a[CLICK PARA VISITAR]")
                                ));
                            }
                            tpBtn.setItemMeta(tpMeta);

                            contents.set(2, 2, ClickableItem.of(tpBtn, e -> {
                                player.closeInventory();
                                Location origin = terrain.getOrigin();
                                int size = terrain.getSizeInBlocks();
                                double cx = origin.getBlockX() + (size / 2.0);
                                double cz = origin.getBlockZ() + (size / 2.0);
                                Location dest = new Location(
                                        origin.getWorld(), cx, origin.getBlockY() + 1, cz,
                                        player.getLocation().getYaw(), player.getLocation().getPitch()
                                );
                                player.teleport(dest);
                                if (isOwner) {
                                    player.sendMessage(CC.translate("&7Teletransportado a tu terreno &f" + terrain.getId() + "&7."));
                                } else {
                                    player.sendMessage(CC.translate("&7Visitando el terreno &f" + terrain.getId()
                                            + (terrain.hasOwner() ? " &7de &f" + terrain.getOwnerName() : "") + "&7."));
                                }
                            }));
                        }

                        // ── Comprar ─────────────────────────────────────────────────
                        if (!terrain.hasOwner() && terrain.isCommitted() && terrain.getPrice() > 0
                                && TerrainManager.getInstance().getEconomy() != null) {

                            boolean canAfford = TerrainManager.getInstance().getEconomy()
                                    .has(player.getName(), terrain.getPrice());

                            ItemStack buyButton = new ItemStack(canAfford ? Material.EMERALD : Material.REDSTONE);
                            ItemMeta buyMeta = buyButton.getItemMeta();
                            buyMeta.setDisplayName(CC.translate(canAfford ? "&a&lComprar Territorio" : "&c&lSin Fondos"));
                            buyMeta.setLore(Arrays.asList(
                                    CC.translate("&7Precio: &f$" + (int) terrain.getPrice()),
                                    CC.translate(canAfford
                                            ? "&a[CLICK PARA COMPRAR]"
                                            : "&c&7No tienes suficiente dinero")));
                            buyButton.setItemMeta(buyMeta);

                            if (canAfford) {
                                contents.set(3, 4, ClickableItem.of(buyButton, e -> {
                                    boolean purchased = TerrainManager.getInstance().purchaseTerrain(terrain, player);
                                    if (purchased) {
                                        player.sendMessage(CC.translate("&7Compraste el terreno &f" + terrain.getId()
                                                + " &7por &f$" + (int) terrain.getPrice() + "&7."));
                                        player.closeInventory();
                                    } else {
                                        player.sendMessage(CC.translate("&7No se pudo completar la compra."));
                                    }
                                }));
                            } else {
                                contents.set(3, 4, ClickableItem.empty(buyButton));
                            }
                        }

                        // ── Vender ──────────────────────────────────────────────────
                        if ((isOwner || isAdmin) && terrain.hasOwner()) {
                            ItemStack sellButton = new ItemStack(Material.GOLD_INGOT);
                            ItemMeta sellMeta = sellButton.getItemMeta();
                            sellMeta.setDisplayName(CC.translate("&e&lPoner en Venta"));
                            double refund = terrain.getPrice() / 2;
                            sellMeta.setLore(Arrays.asList(
                                    CC.translate("&7Devuelve el territorio al mercado"),
                                    CC.translate("&7Reembolso: &f$" + (int) refund),
                                    CC.translate(""),
                                    CC.translate("&e[CLICK PARA VENDER]")));
                            sellButton.setItemMeta(sellMeta);
                            contents.set(3, 3, ClickableItem.of(sellButton, e -> {
                                if (!terrain.hasOwner()) {
                                    player.sendMessage(CC.translate("&7Este terreno ya está a la venta."));
                                    return;
                                }
                                terrain.setOwner(null);
                                terrain.setOwnerName(null);
                                terrain.getMembers().clear();
                                TerrainManager.getInstance().save(terrain);
                                TerrainManager.getInstance().updateSign(terrain);
                                if (TerrainManager.getInstance().getEconomy() != null && refund > 0) {
                                    TerrainManager.getInstance().getEconomy()
                                            .depositPlayer(player.getName(), refund);
                                    player.sendMessage(CC.translate("&7Terreno puesto a la venta. Reembolso: &f$"
                                            + (int) refund + "&7."));
                                } else {
                                    player.sendMessage(CC.translate("&7Terreno &f" + terrain.getId()
                                            + " &7puesto a la venta."));
                                }
                                player.closeInventory();
                            }));
                        }

                        // ── ✦ PERSONALIZAR (VIP) ─────────────────────────────────────
                        boolean hasAnyCustPerm =
                                player.hasPermission(ClaimsPermissions.TERRAIN_CUSTOMIZE_BIOME)
                                        || player.hasPermission(ClaimsPermissions.TERRAIN_CUSTOMIZE_FLOOR)
                                        || player.hasPermission(ClaimsPermissions.TERRAIN_CUSTOMIZE_WEATHER)
                                        || player.hasPermission(ClaimsPermissions.TERRAIN_CUSTOMIZE_RULES)
                                        || player.hasPermission(ClaimsPermissions.TERRAIN_CUSTOMIZE_TIME)
                                        || player.hasPermission(ClaimsPermissions.TERRAIN_CUSTOMIZE_EFFECTS)
                                        || isAdmin;

                        if ((isOwner || isAdmin) && terrain.isCommitted() && hasAnyCustPerm) {
                            ItemStack customizeBtn = new ItemStack(Material.NETHER_STAR);
                            ItemMeta cm = customizeBtn.getItemMeta();
                            cm.setDisplayName(CC.translate("&6&l✦ Personalizar Terreno"));
                            cm.setLore(Arrays.asList(
                                    CC.translate("&7Cambia bioma, suelo, clima,"),
                                    CC.translate("&7reglas de juego, tiempo y efectos"),
                                    CC.translate(""),
                                    CC.translate("&6[CLICK PARA PERSONALIZAR]")));
                            customizeBtn.setItemMeta(cm);
                            contents.set(3, 5, ClickableItem.of(customizeBtn, e ->
                                    TerrainCustomizeMenu.createMainMenu(terrain, player).open(player)));
                        } else if (isOwner && terrain.isCommitted() && !hasAnyCustPerm) {
                            ItemStack lockedBtn = new ItemStack(Material.REDSTONE);
                            ItemMeta lm = lockedBtn.getItemMeta();
                            lm.setDisplayName(CC.translate("&c&l✦ Personalizar &8(VIP)"));
                            lm.setLore(Arrays.asList(
                                    CC.translate("&7Cambia bioma, suelo, clima y más"),
                                    CC.translate("&c✗ Requiere rango VIP o superior")));
                            lockedBtn.setItemMeta(lm);
                            contents.set(3, 5, ClickableItem.empty(lockedBtn));
                        }

                        // ── Admin ───────────────────────────────────────────────────
                        if (isAdmin) {
                            ItemStack adminButton = new ItemStack(Material.COMMAND);
                            ItemMeta adminMeta = adminButton.getItemMeta();
                            adminMeta.setDisplayName(CC.translate("&c&lAcciones Admin"));
                            adminMeta.setLore(Arrays.asList(
                                    CC.translate("&7Eliminar o disolver terreno"),
                                    CC.translate(""),
                                    CC.translate("&c[CLICK PARA VER OPCIONES]")));
                            adminButton.setItemMeta(adminMeta);
                            contents.set(3, 6, ClickableItem.of(adminButton, e ->
                                    TerritoryAdminInfoMenu.createAdminInfoMenu(terrain, player).open(player)));
                        }

                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(3, 0, ClickableItem.of(backButton, e ->
                                TerritoryListMenu.createListMenu(1).open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {}
                })
                .size(4, 9)
                .title(CC.translate("&f&lTerreno: " + terrain.getId()))
                .build();
    }
}