package org.debentialc.customitems.commands;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.service.CC;
import org.debentialc.customitems.tools.ci.CustomArmor;
import org.debentialc.customitems.tools.nbt.NbtHandler;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;
import org.debentialc.customitems.tools.permissions.Permissions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterItem extends BaseCommand {
    public static ConcurrentHashMap<String, CustomArmor> items = new ConcurrentHashMap<>();
    private Player player = null;

    /*
     * 1. (Crear item) /ca create <id>
     * 2. (Eliminar item) /ca delete <id>
     * 3. (Agregar lore) /ca lore <id> <linea> <lore>
     * 4. (Agregar percentage) /ca percentage <id> <value> <stat>
     * 5. (Adicionar suma) /ca plus <id> <value> <stat>
     * 6. (Restar) /ca less <id> <value> <stat>
     * 7. (Listar) /ca l <page>
     *
     * */
    @Command(name = "customarmor", aliases = {"customarmor", "ca"}, permission =
            Permissions.COMMAND + "ci")
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        player = command.getPlayer();
        if (command.length() < 2)
            player.sendMessage(CC.translate("&cMake sure you type the command correctly, or use /help to get more information"));
        try {
            String arg0 = command.getArgs(0);
            String arg1 = command.getArgs(1);
            switch (arg0) {
                case "create":
                    create(arg1);
                    break;
                case "remove":
                case "delete":
                    delete(arg1);
                    break;
                case "percentage":
                    addPercentage(arg1, command.getArgs(3), Double.parseDouble(command.getArgs(2)));
                    break;
                case "plus":
                    plus(arg1, command.getArgs(3), Double.parseDouble(command.getArgs(2)));
                    break;
                case "less":
                    less(arg1, command.getArgs(3), Double.parseDouble(command.getArgs(2)));
                    break;
                case "addline":
                    List<String> lore;
                    StringBuilder text = new StringBuilder();
                    for (int i = 1; i < command.getArgs().length; i++) {
                        text.append(command.getArgs(i));
                    }
                    CustomArmor ca = toItemCustom(player.getItemInHand());
                    ItemStack item = player.getItemInHand();
                    ItemMeta meta = item.getItemMeta();
                    lore = meta.getLore();
                    if(lore == null)
                        lore =  new ArrayList<>();
                    lore.add(CC.translate(text.toString()));
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    ca.setLore(lore);
                    player.setItemInHand(item);
                    player.sendMessage(CC.translate("&aItem lore updated successfully"));

                    items.put(ca.getId(),ca);
                    break;
                case "setline":
                    StringBuilder loreline = new StringBuilder();
                    int line = command.getArgs(1).length();
                    for (int i = 2; i < command.getArgs().length; i++) {
                        loreline.append(CC.translate(command.getArgs(i)));
                    }
                    //Armor Custom Lore
                    ca = toItemCustom(player.getItemInHand());
                    lore = ca.getLore();
                    try {
                        lore.set((line - 1), CC.translate(loreline.toString()));
                        player.sendMessage(CC.translate("&aItem lore updated successfully"));
                    } catch (Exception e) {
                        player.sendMessage(CC.translate("&cInvalid line number!"));
                    }
                    ca.setLore(lore);
                    //ItemStack Lore
                    item = player.getItemInHand();
                    meta = item.getItemMeta();
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    //Update in player inventory
                    player.setItemInHand(item);

                    items.put(ca.getId(),ca);

                    break;
                case "l":
                case "list":
                    sendList(player, Integer.parseInt(command.getArgs(1)));
                    break;
                case "rename":
                    text = new StringBuilder();
                    for (int i = 1; i < command.getArgs().length; i++) {
                        text.append(command.getArgs(i));
                    }
                    ca = toItemCustom(player.getItemInHand());
                    ca.setDisplayName(CC.translate(text.toString()));
                    item = player.getItemInHand();
                    meta = item.getItemMeta();
                    meta.setDisplayName(CC.translate(text.toString()));
                    item.setItemMeta(meta);
                    player.setItemInHand(item);

                    items.put(ca.getId(),ca);
                    break;
                case "help":
                case "effect":
                    ca = toItemCustom(player.getItemInHand());
                    String effect = command.getArgs(1).toUpperCase();
                    HashMap<String, Double> effects = ca.getEffects();
                    switch (effect){
                        case "HEALTHREGEN":
                            effects.put("HEALTHREGEN", Double.parseDouble(command.getArgs(2)));
                            break;
                        case "KIREGEN":
                            effects.put("KIREGEN", Double.parseDouble(command.getArgs(2)));
                            break;
                        case "STAMINAREGEN":
                            effects.put("STAMINAREGEN", Double.parseDouble(command.getArgs(2)));
                            break;
                    }
                    break;
                default:
                    sendHelp(player);
                    break;
            }
        } catch (ArrayIndexOutOfBoundsException exception) {
            sendHelp(player);
        }
    }

    public void sendHelp(Player player) {
        player.sendMessage(CC.translate("&8&l&m--------------------------------------"));
        player.sendMessage(CC.translate("&b&lCustom Armor - Help Menu "));
        player.sendMessage(CC.translate("&8&l&m--------------------------------------"));
        player.sendMessage(CC.translate("&fUse &a/ca <subcommand> &fto manage custom armors."));
        player.sendMessage(" ");
        player.sendMessage(CC.translate("&e/ca create <id> &7- Create a custom item from your hand."));
        player.sendMessage(CC.translate("&e/ca delete <id> &7- Delete a registered item."));
        player.sendMessage(CC.translate("&e/ca addline <text> &7- Add lore."));
        player.sendMessage(CC.translate("&e/ca setline <line> <text> &7- Set or modify lore line."));
        player.sendMessage(CC.translate("&e/ca rename <text> &7- Modify displayname"));

        player.sendMessage(CC.translate("&e/ca percentage <id> <value> <stat> &7- Multiplier bonus (*)."));
        player.sendMessage(CC.translate("&e/ca plus <id> <value> <stat> &7- Additive bonus (+)."));
        player.sendMessage(CC.translate("&e/ca less <id> <value> <stat> &7- Subtractive bonus (-)."));
        player.sendMessage(CC.translate("&e/ca effect <effect> <value> &7- Regenerates a % of an attribute for the player (%)."));
        player.sendMessage(" ");
        player.sendMessage(CC.translate("&7Available stats: &fstr&7, &fcon&7, &fdex&7, &fwill&7, &fmnd"));
        player.sendMessage(CC.translate("&8&l&m--------------------------------------"));
    }

    public void create(String id) {
        if (items.containsKey(id)) {
            player.sendMessage(CC.translate("&cAn item with this ID is already registered."));
            return;
        }

        ItemStack item = player.getItemInHand();
        if (item == null || item.getTypeId() == 0) {
            player.sendMessage(CC.translate("&cYou must hold an item in your hand to create a CustomItem."));
            return;
        }

        if (item.getItemMeta() == null) {
            player.sendMessage(CC.translate("&cThis item does not have metadata (name/lore)."));
            return;
        }

        items.put(id, new CustomArmor()
                .setId(id)
                .setMaterial(item.getTypeId())
                .setLore(item.getItemMeta().getLore())
                .setDisplayName(item.getItemMeta().getDisplayName()));
        player.sendMessage(CC.translate("&aItem created successfully"));
    }

    public void addPercentage(String id, String stat, double value) {
        items.put(id, items.get(id).setOperation("*", stat)
                .setBonusStat(stat, value));
        player.sendMessage(CC.translate("&aBonus correctly applied to the armor set"));
    }

    public void plus(String id, String stat, double value) {
        items.put(id, items.get(id).setOperation("+", stat).setBonusStat(stat, value));
        player.sendMessage(CC.translate("&aBonus correctly applied to the armor set"));
    }

    public void less(String id, String stat, double value) {
        items.put(id, items.get(id).setOperation("-", stat).setBonusStat(stat, value));
        player.sendMessage(CC.translate("&aBonus correctly applied to the armor set"));
    }

    private void delete(String arg0) {
        items.remove(arg0);
        player.sendMessage(CC.translate("&aItem removed successfully"));
    }

    public boolean isCustom(ItemStack item) {
        if (item == null || item.getTypeId() == 0) return false;
        NbtHandler nbt = new NbtHandler(item);
        if (nbt.hasNBT() && nbt.hasKey("debentialc_id") && "armor".equals(nbt.getString("debentialc_type"))) {
            return items.containsKey(nbt.getString("debentialc_id"));
        }
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!meta.hasLore() || meta.getLore() == null) return false;
        if (!meta.hasDisplayName() || meta.getDisplayName() == null) return false;
        return items.containsValue(
                new CustomArmor()
                        .setMaterial(item.getTypeId())
                        .setLore(meta.getLore())
                        .setDisplayName(meta.getDisplayName())
        );
    }


    public ItemStack toItemStack(CustomArmor item) {
        ItemStack itemStack = new ItemStack(item.getMaterial());
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(item.getDisplayName());
        meta.setLore(item.getLore());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public void sendList(Player player, int page) {
        int pageSize = 7;
        List<String> keys = new ArrayList<>(items.keySet());

        if (keys.isEmpty()) {
            player.sendMessage(CC.translate("&cNo custom items registered."));
            return;
        }

        int totalPages = (int) Math.ceil((double) keys.size() / pageSize);

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, keys.size());

        player.sendMessage(CC.translate("&8&l&m--------------------------------------"));
        player.sendMessage(CC.translate("&b&lRegistered Custom Items"));
        player.sendMessage(CC.translate("&7Page &a" + page + " &7/ &a" + totalPages));
        player.sendMessage(CC.translate("&8&l&m--------------------------------------"));

        for (int i = start; i < end; i++) {
            String id = keys.get(i);
            CustomArmor ci = items.get(id);

            player.sendMessage(CC.translate("&e" + id + " &7- &f" + ci.getDisplayName()));
        }

        player.sendMessage(CC.translate("&8&l&m--------------------------------------"));
    }


    public CustomArmor toItemCustom(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) return null;

        NbtHandler nbt = new NbtHandler(itemStack);
        if (nbt.hasNBT() && nbt.hasKey("debentialc_id") && "armor".equals(nbt.getString("debentialc_type"))) {
            CustomArmor byId = items.get(nbt.getString("debentialc_id"));
            if (byId != null) return byId;
        }

        return items.values().stream()
                .filter(armor -> armor.matchesItem(itemStack))
                .findFirst()
                .orElse(null);
    }
}
