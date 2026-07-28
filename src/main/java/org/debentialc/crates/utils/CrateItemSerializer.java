package org.debentialc.crates.utils;

import net.minecraft.server.v1_7_R4.NBTCompressedStreamTools;
import net.minecraft.server.v1_7_R4.NBTReadLimiter;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class CrateItemSerializer {

    public static String itemToBase64(ItemStack item) {
        if (item == null || item.getTypeId() == 0) {
            return null;
        }
        try {
            net.minecraft.server.v1_7_R4.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
            NBTTagCompound compound = new NBTTagCompound();
            nmsStack.save(compound);
            byte[] data = NBTCompressedStreamTools.a(compound);
            return Base64.getEncoder().encodeToString(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack itemFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            NBTTagCompound compound = readCompound(data);
            if (compound == null) {
                return null;
            }
            net.minecraft.server.v1_7_R4.ItemStack nmsStack = net.minecraft.server.v1_7_R4.ItemStack.createStack(compound);
            return CraftItemStack.asBukkitCopy(nmsStack);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static NBTTagCompound readCompound(byte[] data) {
        try {
            return NBTCompressedStreamTools.a(data, new NBTReadLimiter(2097152L));
        } catch (Exception e) {
            try {
                return NBTCompressedStreamTools.a(new ByteArrayInputStream(data));
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }
}
