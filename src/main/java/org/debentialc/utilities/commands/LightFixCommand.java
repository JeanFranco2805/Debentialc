package org.debentialc.utilities.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class LightFixCommand extends BaseCommand {

    private NmsResolver resolver;

    @Command(name = "fixlight", aliases = {"lightfix", "cleanlight"}, permission = "dbcplugin.utilities.fixlight", inGameOnly = true)
    public void onCommand(CommandArgs command) {
        Player player = command.getPlayer();
        int chunkRadius = 5;
        if (command.length() >= 1) {
            try {
                chunkRadius = Integer.parseInt(command.getArgs(0));
                if (chunkRadius < 1) chunkRadius = 1;
                if (chunkRadius > 50) chunkRadius = 50;
            } catch (NumberFormatException e) {
                player.sendMessage(CC.translate("&7[&cFixLight&7] &fRadio invalido. Usa un numero entero."));
                return;
            }
        }

        Plugin lightCleaner = Bukkit.getPluginManager().getPlugin("LightCleaner");
        if (lightCleaner != null && lightCleaner.isEnabled()) {
            player.sendMessage(CC.translate("&7[&aFixLight&7] &fLimpiando iluminacion con LightCleaner..."));
            if (!Bukkit.dispatchCommand(player, "cleanlight " + chunkRadius)) {
                player.sendMessage(CC.translate("&7[&cFixLight&7] &fNo se pudo ejecutar LightCleaner."));
            }
            return;
        }

        if (resolver == null) {
            resolver = new NmsResolver();
        }
        if (!resolver.isReady()) {
            player.sendMessage(CC.translate("&7[&cFixLight&7] &fNo se pudo preparar el sistema de luz para este servidor."));
            return;
        }

        player.sendMessage(CC.translate("&7[&aFixLight&7] &fRecalculando iluminacion en un radio de &7" + chunkRadius + " &fchunks..."));

        int centerX = player.getLocation().getChunk().getX();
        int centerZ = player.getLocation().getChunk().getZ();
        World world = player.getWorld();

        int loaded = 0;
        List<ChunkCoord> coords = new ArrayList<>();
        for (int dx = -(chunkRadius + 1); dx <= chunkRadius + 1; dx++) {
            for (int dz = -(chunkRadius + 1); dz <= chunkRadius + 1; dz++) {
                int cx = centerX + dx;
                int cz = centerZ + dz;
                try {
                    world.loadChunk(cx, cz);
                    loaded++;
                } catch (Exception ignored) {
                }
                if (dx >= -chunkRadius && dx <= chunkRadius && dz >= -chunkRadius && dz <= chunkRadius) {
                    coords.add(new ChunkCoord(cx, cz));
                }
            }
        }

        int processed = 0;
        int failed = 0;
        for (ChunkCoord coord : coords) {
            if (resolver.initChunkLighting(world, coord.x, coord.z)) {
                processed++;
            } else {
                failed++;
            }
        }

        int skyPropagated = 0;
        for (ChunkCoord coord : coords) {
            skyPropagated += resolver.propagateSkyLight(world, coord.x, coord.z);
        }

        int blockEmitters = 0;
        for (ChunkCoord coord : coords) {
            blockEmitters += resolver.propagateBlockLight(world, coord.x, coord.z);
        }

        int refreshed = 0;
        for (ChunkCoord coord : coords) {
            if (resolver.refreshChunk(world, coord.x, coord.z)) {
                refreshed++;
            }
        }

        player.sendMessage(CC.translate("&7[&aFixLight&7] &fChunks cargados: &7" + loaded + "&f. Inicializados: &7" + processed + "&f. Fallidos: &7" + failed));
        player.sendMessage(CC.translate("&7[&aFixLight&7] &fColumnas de luz de cielo: &7" + skyPropagated + "&f. Emisores de luz de bloque: &7" + blockEmitters + "&f. Chunks reenviados: &7" + refreshed));
    }

    private static class ChunkCoord {
        final int x;
        final int z;

        ChunkCoord(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    /**
     * Reflection resolver that works for both CraftBukkit NMS and Forge/Cauldron
     * by looking at method signatures instead of obfuscated names.
     */
    private static class NmsResolver {
        private boolean ready = false;

        private Class<?> enumSkyBlockClass;
        private Object skyLightType;
        private Object blockLightType;

        private Method worldRecalcLightMethod;
        private Method chunkInitLightingMethod;
        private Method chunkGetSectionsMethod;
        private Method chunkSectionGetEmittedLightMethod;
        private Method worldNotifyMethod;
        private Method worldGetPlayerChunkMapMethod;
        private Method playerChunkMapFlagDirtyMethod;
        private Class<?> packetClass;
        private Constructor<?> packetChunkConstructor;
        private Field playerConnectionField;
        private Method sendPacketMethod;

        NmsResolver() {
            try {
                prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean isReady() {
            return ready;
        }

        private void prepare() throws Exception {
            // Try CraftBukkit NMS first.
            Class<?> worldClass = tryClass("net.minecraft.server.v1_7_R4.World");
            Class<?> chunkClass = tryClass("net.minecraft.server.v1_7_R4.Chunk");
            Class<?> chunkSectionClass = tryClass("net.minecraft.server.v1_7_R4.ChunkSection");
            Class<?> enumSkyBlockClassLocal = tryClass("net.minecraft.server.v1_7_R4.EnumSkyBlock");
            Class<?> packetChunkClass = tryClass("net.minecraft.server.v1_7_R4.PacketPlayOutMapChunk");
            Class<?> entityPlayerClass = tryClass("net.minecraft.server.v1_7_R4.EntityPlayer");
            Class<?> playerConnectionClass = tryClass("net.minecraft.server.v1_7_R4.PlayerConnection");
            Class<?> packetClass = tryClass("net.minecraft.server.v1_7_R4.Packet");
            Class<?> playerChunkMapClass = tryClass("net.minecraft.server.v1_7_R4.PlayerChunkMap");

            // Try Forge/Cauldron naming.
            if (worldClass == null) worldClass = tryClass("net.minecraft.world.World");
            if (chunkClass == null) chunkClass = tryClass("net.minecraft.world.chunk.Chunk");
            if (chunkSectionClass == null) chunkSectionClass = tryClass("net.minecraft.world.chunk.ChunkSection");
            if (enumSkyBlockClassLocal == null) enumSkyBlockClassLocal = tryClass("net.minecraft.world.EnumSkyBlock");
            if (packetChunkClass == null) packetChunkClass = tryClass("net.minecraft.network.play.server.S21PacketChunkData");
            if (packetChunkClass == null) packetChunkClass = tryClass("net.minecraft.network.play.server.PacketPlayOutMapChunk");
            if (entityPlayerClass == null) entityPlayerClass = tryClass("net.minecraft.entity.player.EntityPlayerMP");
            if (entityPlayerClass == null) entityPlayerClass = tryClass("net.minecraft.entity.player.EntityPlayer");
            if (playerConnectionClass == null) playerConnectionClass = tryClass("net.minecraft.network.NetHandlerPlayServer");
            if (packetClass == null) packetClass = tryClass("net.minecraft.network.Packet");
            if (playerChunkMapClass == null) playerChunkMapClass = tryClass("net.minecraft.world.gen.ChunkProviderServer");

            if (worldClass == null || chunkClass == null || enumSkyBlockClassLocal == null) {
                throw new IllegalStateException("No se pudieron encontrar las clases NMS/World");
            }

            this.enumSkyBlockClass = enumSkyBlockClassLocal;
            resolveEnumSkyBlock();

            // World method: (EnumSkyBlock, int, int, int) -> boolean
            worldRecalcLightMethod = findMethodByParams(worldClass, boolean.class, enumSkyBlockClassLocal, int.class, int.class, int.class);
            if (worldRecalcLightMethod == null) {
                throw new IllegalStateException("No se encontro el metodo de recalcular luz en World");
            }

            // Chunk.initLighting () -> void
            chunkInitLightingMethod = findMethodByParams(chunkClass, void.class);
            if (chunkInitLightingMethod == null) {
                throw new IllegalStateException("No se encontro Chunk.initLighting");
            }

            // Chunk.getSections () -> ChunkSection[] or Object[]
            for (Method m : chunkClass.getDeclaredMethods()) {
                if (m.getParameterTypes().length == 0 && m.getReturnType().isArray()) {
                    chunkGetSectionsMethod = m;
                    break;
                }
            }
            if (chunkGetSectionsMethod == null) {
                throw new IllegalStateException("No se encontro Chunk.getSections");
            }

            // ChunkSection.getEmittedLight(int,int,int) -> int
            if (chunkSectionClass != null) {
                chunkSectionGetEmittedLightMethod = findMethodByParams(chunkSectionClass, int.class, int.class, int.class, int.class);
            }

            // World.notify (int,int,int) -> void
            worldNotifyMethod = findMethodByParams(worldClass, void.class, int.class, int.class, int.class);

            // PlayerChunkMap.flagDirty(int,int,int) -> void
            if (playerChunkMapClass != null) {
                playerChunkMapFlagDirtyMethod = findMethodByParams(playerChunkMapClass, void.class, int.class, int.class, int.class);
            }
            // World.getPlayerChunkMap() -> PlayerChunkMap
            if (playerChunkMapClass != null) {
                worldGetPlayerChunkMapMethod = findMethodByReturnType(worldClass, playerChunkMapClass);
            }

            // Packet chunk constructor: (Chunk, boolean, int)
            // Resolve the packet base class from the chunk packet type.
            if (packetChunkClass != null) {
                try {
                    packetChunkConstructor = packetChunkClass.getConstructor(chunkClass, boolean.class, int.class);
                } catch (Exception ignored) {
                    packetChunkConstructor = null;
                }
                if (packetChunkConstructor == null) {
                    try {
                        packetChunkConstructor = packetChunkClass.getConstructor(chunkClass, boolean.class, int.class, int.class);
                    } catch (Exception ignored) {
                        packetChunkConstructor = null;
                    }
                }
                if (packetChunkConstructor != null) {
                    Class<?> superClass = packetChunkClass.getSuperclass();
                    if (superClass != null && superClass != Object.class) {
                        packetClass = superClass;
                    }
                }
            }

            // EntityPlayer connection field & sendPacket
            if (entityPlayerClass != null && playerConnectionClass != null && packetClass != null) {
                for (Class<?> entityType : new Class<?>[]{entityPlayerClass, entityPlayerClass.getSuperclass(), entityPlayerClass.getSuperclass() != null ? entityPlayerClass.getSuperclass().getSuperclass() : null}) {
                    if (entityType == null) continue;
                    for (Field f : entityType.getDeclaredFields()) {
                        if (f.getType() == playerConnectionClass || playerConnectionClass.isAssignableFrom(f.getType())) {
                            playerConnectionField = f;
                            playerConnectionField.setAccessible(true);
                            break;
                        }
                    }
                    if (playerConnectionField != null) break;
                }
                sendPacketMethod = findMethodByParams(playerConnectionClass, void.class, packetClass);
            }

            ready = true;
        }

        private void resolveEnumSkyBlock() {
            Object[] constants = enumSkyBlockClass.getEnumConstants();
            if (constants == null || constants.length == 0) {
                throw new IllegalStateException("EnumSkyBlock sin valores");
            }
            for (Object constant : constants) {
                int base = getEnumBaseLight(constant);
                String name = constant.toString();
                if (skyLightType == null && (base == 15 || name.contains("SKY") || name.contains("Sky") || name.contains("sky"))) {
                    skyLightType = constant;
                }
                if (blockLightType == null && (base == 0 || name.contains("BLOCK") || name.contains("Block") || name.contains("block"))) {
                    blockLightType = constant;
                }
            }
            if (skyLightType == null && constants.length > 0) skyLightType = constants[0];
            if (blockLightType == null && constants.length > 1) blockLightType = constants[1];
            if (blockLightType == null && constants.length == 1) blockLightType = constants[0];
        }

        private int getEnumBaseLight(Object enumConstant) {
            for (Field f : enumConstant.getClass().getDeclaredFields()) {
                if (f.getType() == int.class) {
                    try {
                        f.setAccessible(true);
                        return f.getInt(enumConstant);
                    } catch (Exception ignored) {
                    }
                }
            }
            return -1;
        }

        private Class<?> tryClass(String name) {
            try {
                return Class.forName(name);
            } catch (Exception ignored) {
                return null;
            }
        }

        private Method findMethodByParams(Class<?> clazz, Class<?> returnType, Class<?>... params) {
            Method found = findMethodByParamsInClass(clazz, returnType, params);
            if (found != null) return found;
            Class<?> current = clazz.getSuperclass();
            while (current != null && current != Object.class) {
                found = findMethodByParamsInClass(current, returnType, params);
                if (found != null) return found;
                current = current.getSuperclass();
            }
            return null;
        }

        private Method findMethodByParamsInClass(Class<?> clazz, Class<?> returnType, Class<?>... params) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getReturnType() != returnType) continue;
                if (m.getParameterTypes().length != params.length) continue;
                Class<?>[] mp = m.getParameterTypes();
                boolean ok = true;
                for (int i = 0; i < params.length; i++) {
                    if (!params[i].isAssignableFrom(mp[i]) && mp[i] != params[i]) {
                        ok = false;
                        break;
                    }
                }
                if (ok) return m;
            }
            return null;
        }

        private Method findMethodByReturnType(Class<?> clazz, Class<?> returnType) {
            Method found = findMethodByReturnTypeInClass(clazz, returnType);
            if (found != null) return found;
            Class<?> current = clazz.getSuperclass();
            while (current != null && current != Object.class) {
                found = findMethodByReturnTypeInClass(current, returnType);
                if (found != null) return found;
                current = current.getSuperclass();
            }
            return null;
        }

        private Method findMethodByReturnTypeInClass(Class<?> clazz, Class<?> returnType) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getReturnType() == returnType || returnType.isAssignableFrom(m.getReturnType())) {
                    return m;
                }
            }
            return null;
        }

        private Object getWorldHandle(World world) {
            try {
                return world.getClass().getMethod("getHandle").invoke(world);
            } catch (Exception e) {
                return null;
            }
        }

        private Object getChunkHandle(org.bukkit.Chunk chunk) {
            try {
                return chunk.getClass().getMethod("getHandle").invoke(chunk);
            } catch (Exception e) {
                return null;
            }
        }

        private Object getPlayerHandle(Player player) {
            try {
                return player.getClass().getMethod("getHandle").invoke(player);
            } catch (Exception e) {
                return null;
            }
        }

        boolean initChunkLighting(World world, int cx, int cz) {
            try {
                org.bukkit.Chunk bukkitChunk = world.getChunkAt(cx, cz);
                if (bukkitChunk == null || !bukkitChunk.isLoaded()) return false;
                Object nmsChunk = getChunkHandle(bukkitChunk);
                if (nmsChunk == null) return false;
                chunkInitLightingMethod.invoke(nmsChunk);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        int propagateSkyLight(World world, int cx, int cz) {
            int propagated = 0;
            try {
                Object nmsWorld = getWorldHandle(world);
                if (nmsWorld == null) return 0;
                org.bukkit.Chunk bukkitChunk = world.getChunkAt(cx, cz);
                if (bukkitChunk == null || !bukkitChunk.isLoaded()) return 0;
                int baseX = cx << 4;
                int baseZ = cz << 4;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        Boolean result = (Boolean) worldRecalcLightMethod.invoke(nmsWorld, skyLightType, baseX + x, 255, baseZ + z);
                        if (result != null && result) propagated++;
                    }
                }
                // Propagate edges to neighbours.
                int[][] sides = { { -1, 8 }, { 16, 8 }, { 8, -1 }, { 8, 16 } };
                for (int[] side : sides) {
                    for (int y = 0; y < 256; y += 16) {
                        worldRecalcLightMethod.invoke(nmsWorld, skyLightType, baseX + side[0], y, baseZ + side[1]);
                    }
                }
            } catch (Exception e) {
                // ignore single failures
            }
            return propagated;
        }

        int propagateBlockLight(World world, int cx, int cz) {
            int emitters = 0;
            try {
                Object nmsWorld = getWorldHandle(world);
                if (nmsWorld == null) return 0;
                org.bukkit.Chunk bukkitChunk = world.getChunkAt(cx, cz);
                if (bukkitChunk == null || !bukkitChunk.isLoaded()) return 0;
                Object nmsChunk = getChunkHandle(bukkitChunk);
                if (nmsChunk == null) return 0;
                Object[] sections = (Object[]) chunkGetSectionsMethod.invoke(nmsChunk);
                if (sections == null) return 0;
                int baseX = cx << 4;
                int baseZ = cz << 4;
                for (int si = 0; si < sections.length; si++) {
                    Object section = sections[si];
                    if (section == null || chunkSectionGetEmittedLightMethod == null) continue;
                    int baseY = si << 4;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                int emitted = (Integer) chunkSectionGetEmittedLightMethod.invoke(section, x, y, z);
                                if (emitted > 0) {
                                    emitters++;
                                    worldRecalcLightMethod.invoke(nmsWorld, blockLightType, baseX + x, baseY + y, baseZ + z);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return emitters;
        }

        boolean refreshChunk(World world, int cx, int cz) {
            try {
                org.bukkit.Chunk bukkitChunk = world.getChunkAt(cx, cz);
                if (bukkitChunk == null || !bukkitChunk.isLoaded()) return false;
                Object nmsChunk = getChunkHandle(bukkitChunk);
                if (nmsChunk == null) return false;
                Object nmsWorld = getWorldHandle(world);
                if (nmsWorld == null) return false;

                int baseX = cx << 4;
                int baseZ = cz << 4;

                if (worldGetPlayerChunkMapMethod != null && playerChunkMapFlagDirtyMethod != null) {
                    Object playerChunkMap = worldGetPlayerChunkMapMethod.invoke(nmsWorld);
                    if (playerChunkMap != null) {
                        for (int y = 0; y < 256; y += 16) {
                            playerChunkMapFlagDirtyMethod.invoke(playerChunkMap, baseX + 8, y, baseZ + 8);
                        }
                    }
                }

                Object packet = null;
                if (packetChunkConstructor != null && playerConnectionField != null && sendPacketMethod != null) {
                    try {
                        Class<?>[] ctorParams = packetChunkConstructor.getParameterTypes();
                        if (ctorParams.length == 3) {
                            packet = packetChunkConstructor.newInstance(nmsChunk, true, 0xffff);
                        } else if (ctorParams.length == 4) {
                            packet = packetChunkConstructor.newInstance(nmsChunk, true, 0xffff, 0);
                        }
                        if (packet != null && !packetClass.isAssignableFrom(packet.getClass())) {
                            Bukkit.getLogger().log(Level.WARNING, "[FixLight] Tipo de paquete no asignable a {0}: {1}", new Object[]{packetClass.getName(), packet.getClass().getName()});
                            packet = null;
                        }
                    } catch (Exception ex) {
                        Bukkit.getLogger().log(Level.WARNING, "[FixLight] No se pudo crear el paquete de chunk: " + ex.getClass().getName() + ": " + ex.getMessage());
                    }
                }

                if (packet != null) {
                    for (Player online : world.getPlayers()) {
                        try {
                            Object nmsPlayer = getPlayerHandle(online);
                            if (nmsPlayer == null) continue;
                            Object connection = playerConnectionField.get(nmsPlayer);
                            if (connection == null) continue;
                            if (!sendPacketMethod.getDeclaringClass().isAssignableFrom(connection.getClass())) {
                                Bukkit.getLogger().log(Level.WARNING, "[FixLight] La conexion no es asignable a {0}: {1}", new Object[]{sendPacketMethod.getDeclaringClass().getName(), connection.getClass().getName()});
                                continue;
                            }
                            sendPacketMethod.invoke(connection, packet);
                        } catch (Exception ex) {
                            Bukkit.getLogger().log(Level.WARNING, "[FixLight] Fallo enviando paquete al jugador " + online.getName() + ": " + ex.getClass().getName() + ": " + ex.getMessage());
                            if (ex.getCause() != null) {
                                Bukkit.getLogger().log(Level.WARNING, "[FixLight] Causa: " + ex.getCause().getClass().getName() + ": " + ex.getCause().getMessage());
                            }
                        }
                    }
                }

                world.refreshChunk(cx, cz);
                return true;
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "[FixLight] Error al refrescar chunk " + cx + "," + cz + ": " + e.getClass().getName() + ": " + e.getMessage());
                return false;
            }
        }
    }
}
