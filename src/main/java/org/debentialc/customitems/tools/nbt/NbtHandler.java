package org.debentialc.customitems.tools.nbt;

import net.minecraft.server.v1_7_R4.*;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NbtHandler {

    private final net.minecraft.server.v1_7_R4.ItemStack item;
    private NBTTagCompound compound;

    public NbtHandler(ItemStack item) {
        net.minecraft.server.v1_7_R4.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
        this.item = nmsStack;
        if (nmsStack.getTag() != null) {
            this.compound = nmsStack.getTag();
        } else {
            this.compound = null;
        }
    }

    public NBTTagCompound getCompound() {
        return compound;
    }

    public boolean hasNBT() {
        return item != null && item.hasTag();
    }

    public boolean isEmpty() {
        return compound == null || compound.isEmpty();
    }

    public void setCompoundFromString(String comp) {
        try {
            NBTTagCompound nbt = getCompoundFromString(comp);
            item.setTag(nbt);
            this.compound = nbt;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setString(String key, String value) {
        if (compound == null) {
            compound = new NBTTagCompound();
        }
        compound.setString(key, value);
        item.setTag(compound);
    }

    public void setInteger(String key, int value) {
        if (compound == null) {
            compound = new NBTTagCompound();
        }
        compound.setInt(key, value);
        item.setTag(compound);
    }

    public void setBoolean(String key, boolean value) {
        if (compound == null) {
            compound = new NBTTagCompound();
        }
        compound.setBoolean(key, value);
        item.setTag(compound);
    }

    public void setShort(String key, short value) {
        if (compound == null) {
            compound = new NBTTagCompound();
        }
        compound.setShort(key, value);
        item.setTag(compound);
    }

    public void setCompound(String key, NBTTagCompound compound) {
        if (this.compound == null) {
            this.compound = new NBTTagCompound();
        }
        this.compound.set(key, compound);
        this.item.setTag(this.compound);
    }

    public void changeDamage(int damage) {
        if (this.compound == null) {
            this.compound = new NBTTagCompound();
        }
        NBTTagList modifiers = new NBTTagList();
        NBTTagCompound damageTag = new NBTTagCompound();
        damageTag.set("AttributeName", new NBTTagString("generic.attackDamage"));
        damageTag.set("Name", new NBTTagString("generic.attackDamage"));
        damageTag.set("Amount", new NBTTagInt(damage));
        damageTag.set("Operation", new NBTTagInt(0));
        damageTag.set("UUIDMost", new NBTTagInt(item.hashCode()));
        damageTag.set("UUIDLeast", new NBTTagInt(item.hashCode()));
        damageTag.set("Slot", new NBTTagString("mainhand"));
        modifiers.add(damageTag);
        this.compound.set("AttributeModifiers", modifiers);
        this.item.setTag(this.compound);
    }

    public String getString(String key) {
        if (compound == null) return "";
        return compound.getString(key);
    }

    public int getInteger(String key) {
        if (compound == null) return 0;
        return compound.getInt(key);
    }

    public boolean getBoolean(String key) {
        if (compound == null) return false;
        return compound.getBoolean(key);
    }

    public boolean hasKey(String key) {
        if (compound == null) return false;
        return compound.hasKey(key);
    }

    public ItemStack getItemStack() {
        return CraftItemStack.asBukkitCopy(item);
    }

    public boolean containsCompound(String key) {
        if (compound == null) return false;
        return compound.getCompound(key) != null;
    }

    public static NBTTagCompound getCompoundFromString(String sNBT) {
        return (NBTTagCompound) MojangsonParser.parse(sNBT);
    }

    public static String serializeItemStack(ItemStack item) {
        if (item == null || item.getTypeId() == 0) return null;
        net.minecraft.server.v1_7_R4.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound compound = new NBTTagCompound();
        nmsStack.save(compound);
        return compound.toString();
    }

    public static ItemStack deserializeItemStack(String nbtString) {
        if (nbtString == null || nbtString.isEmpty()) return null;
        try {
            NBTTagCompound compound = getCompoundFromString(nbtString);
            net.minecraft.server.v1_7_R4.ItemStack nmsStack = net.minecraft.server.v1_7_R4.ItemStack.createStack(compound);
            return CraftItemStack.asBukkitCopy(nmsStack);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}