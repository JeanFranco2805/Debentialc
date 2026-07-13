package org.debentialc;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import lombok.Getter;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.event.INpcEvent;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.debentialc.boosters.core.BoosterModule;
import org.debentialc.boosters.core.BoosterSettings;
import org.debentialc.boosters.core.BoosterUtils;
import org.debentialc.boosters.managers.GlobalBoosterManager;
import org.debentialc.boosters.managers.PersonalBoosterManager;
import org.debentialc.boosters.models.PersonalBooster;
import org.debentialc.boosters.placeholders.PlaceholderModule;
import org.debentialc.claims.ClaimsModule;
import org.debentialc.claims.managers.TerrainCustomizeManager;
import org.debentialc.claims.storage.TerrainStorage;
import org.debentialc.customitems.tools.ci.CustomManager;
import org.debentialc.customitems.tools.fragments.FragmentBonusIntegration;
import org.debentialc.customitems.tools.storage.CustomArmorStorage;
import org.debentialc.raids.events.NPCDeathListener;
import org.debentialc.raids.managers.RaidStorageManager;
import org.debentialc.rebirths.RebirthModule;
import org.debentialc.rebirths.managers.RebirthBlockManager;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.rebirths.storage.RebirthStorage;
import org.debentialc.service.ClassesRegistration;
import org.debentialc.service.commands.CommandFramework;

import java.io.File;

import static org.debentialc.customitems.tools.ci.CustomManager.effectsTask;
import static org.debentialc.customitems.tools.config.DBCConfigManager.loadAllConfigs;

@Getter
public class Main extends JavaPlugin {

    private final CommandFramework commandFramework = new CommandFramework(this);

    private final ClassesRegistration classesRegistration = new ClassesRegistration();

    static {
        String ruta1 = System.getProperty("user.dir") + File.separator + "plugins";
        File file = new File(ruta1, "Debentialc");
        file.mkdir();
    }

    public static Main instance;

    @Override
    public void onEnable() {
        instance = this;
        System.out.println("Plugin successfully enabled");
        System.out.println("Version: 1.1.5 ");
        System.out.println("By DelawareX");

        classesRegistration.loadCommands("org.debentialc.customitems.commands");
        classesRegistration.loadCommands("org.debentialc.raids.commands");
        classesRegistration.loadCommands("org.debentialc.boosters.commands");
        classesRegistration.loadCommands("org.debentialc.claims.commands");
        classesRegistration.loadCommands("org.debentialc.rebirths.commands");

        classesRegistration.loadListeners("org.debentialc.customitems.events");
        classesRegistration.loadListeners("org.debentialc.boosters.events");
        classesRegistration.loadListeners("org.debentialc.raids.events");
        classesRegistration.loadListeners("org.debentialc.claims.events");
        classesRegistration.loadListeners("org.debentialc.rebirths.events");

        CustomManager.armorTask();
        effectsTask();
        CustomArmorStorage.getInstance().initialLoad();
        loadAllConfigs();
        armorTask();
        startTerrainEffectsTask();

        BoosterModule.initialize(this);
        PlaceholderModule.initialize(this);
        ClaimsModule.initialize(this);
        RebirthModule.initialize(this);

        registerCustomNPCsEvents();

        RaidStorageManager.loadAllRaids();
        System.out.println("[Raids] Sistema de raids inicializado");
    }
    /**
     * Tarea periódica que aplica efectos ambientales y tiempo de terreno a los jugadores.
     * Se ejecuta cada 5 segundos (100 ticks).
     *
     * CAMBIO: ahora también llama a applyTimeToPlayer para que el tiempo sea
     * individual por jugador según el terreno donde se encuentren.
     */
    private void startTerrainEffectsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    TerrainCustomizeManager.applyEffectToPlayer(player);
                    TerrainCustomizeManager.applyTimeToPlayer(player);
                }
            }
        }.runTaskTimer(this, 20L, 180L);
    }
    /**
     * Registra los eventos de CustomNPCs
     * Debe ejecutarse después de que el servidor esté completamente iniciado
     */
    private void registerCustomNPCsEvents() {
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!noppes.npcs.api.AbstractNpcAPI.IsAvailable()) {
                getLogger().warning("[Raids] CustomNPCs no está disponible. NPCDeathListener no será registrado.");
                getLogger().warning("[Raids] Asegúrate de que CustomNPCs esté instalado.");
                return;
            }

            try {
                noppes.npcs.api.AbstractNpcAPI api = noppes.npcs.api.AbstractNpcAPI.Instance();

                if (api == null) {
                    getLogger().severe("[Raids] No se pudo obtener la instancia de la API de CustomNPCs!");
                    return;
                }

                api.events().register(new org.debentialc.raids.events.NPCDeathListener());

                getLogger().info("[Raids] NPCDeathListener registrado correctamente en CustomNPCs");

            } catch (Exception e) {
                getLogger().severe("[Raids] Error al registrar NPCDeathListener:");
                e.printStackTrace();
            }
        }, 20L);
    }

    public static void armorTask() {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player onlinePlayer : Main.instance.getServer().getOnlinePlayers()) {
                    FragmentBonusIntegration.applyFragmentBonuses(onlinePlayer);
                }
            }
        };
        runnable.runTaskTimer(Main.instance, 1L, 1L);
    }

    public static void callDeathEvent(INpcEvent.DiedEvent event) {
        NPCDeathListener npcDeathListener = new NPCDeathListener();
        npcDeathListener.onNpcDie(event);
    }

    /**
     * Devuelve el nivel de rebirth local que un jugador tiene desbloqueado en un bloque.
     * El conteo se reinicia en cada bloque (bloque 1: 1-10, bloque 2: 1-10, etc.).
     *
     * @param playerName Nombre del jugador (puede estar online u offline)
     * @param blockId    ID del bloque
     * @return Nivel de rebirth desbloqueado dentro del bloque (0 si no tiene ninguno)
     */
    public static int getPlayerRebirthLevelInBlock(String playerName, int blockId) {
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);

        java.util.UUID uuid;
        if (onlinePlayer != null) {
            uuid = onlinePlayer.getUniqueId();
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
                return 0;
            }
            uuid = offlinePlayer.getUniqueId();
        }

        int globalLevel = RebirthStorage.getInstance().loadPlayerRebirthLevel(uuid);
        return RebirthBlockManager.getInstance().getLocalRebirthLevel(globalLevel, blockId);
    }

    @Override
    public void onDisable() {
        BoosterModule.shutdown();
        PlaceholderModule.shutdown();
    }

    public static void exampleUsage() {
        Player player = Bukkit.getPlayer("PlayerName");
        if (player == null) return;

        double globalMult = GlobalBoosterManager.getCurrentMultiplier();
        double personalMult = PersonalBoosterManager.getActiveMultiplier(player.getUniqueId());
        double combined = BoosterUtils.calculateCombinedMultiplier(player.getUniqueId());

        player.sendMessage("Global: " + BoosterUtils.formatMultiplier(globalMult));
        player.sendMessage("Personal: " + BoosterUtils.formatMultiplier(personalMult));
        player.sendMessage("Combined: " + BoosterUtils.formatMultiplier(combined));

        boolean hasGlobal = GlobalBoosterManager.isBoosterActive();
        PersonalBooster active = PersonalBoosterManager.getActiveBooster(player.getUniqueId());

        if (hasGlobal) {
            player.sendMessage("Booster global activo: " +
                    BoosterUtils.formatPercentage(globalMult));
        }

        if (active != null) {
            player.sendMessage("Booster personal activo: " +
                    BoosterUtils.formatPercentage(active.getMultiplier()));
            player.sendMessage("Nivel: " + active.getLevelName());
            player.sendMessage("Tiempo restante: " +
                    BoosterUtils.formatTime(active.getActivationTimeRemaining(900)));
        }
    }

    public static void customizeExample() {
        BoosterSettings.setPersonalBoosterMultiplier(1, 0.15);
        BoosterSettings.setPersonalBoosterMultiplier(2, 0.30);
        BoosterSettings.setPersonalBoosterMultiplier(3, 0.60);
        BoosterSettings.setPersonalBoosterMultiplier(4, 1.20);
        BoosterSettings.setPersonalBoosterMultiplier(5, 2.50);

        BoosterSettings.setRankMultiplier("admin", 3.0);
        BoosterSettings.setRankMultiplier("moderator", 2.0);
        BoosterSettings.setRankMultiplier("vip", 1.5);

        BoosterSettings.setGlobalBoosterDuration(7200);
        BoosterSettings.setPersonalBoosterDuration(1800);
    }

    public static void activateBoosterExample() {
        GlobalBoosterManager.activateBooster(2.5, "AdminName");

        Bukkit.broadcastMessage("§6§lBooster Activado: 2.5x durante 1 hora");

        Bukkit.getScheduler().scheduleSyncDelayedTask(
                Bukkit.getPluginManager().getPlugin("Debentialc"),
                () -> GlobalBoosterManager.deactivateBooster(),
                3600 * 20
        );
    }

    public static void addPersonalBoosterExample() {
        Player player = Bukkit.getPlayer("PlayerName");
        if (player == null) return;

        int level = 4;
        double multiplier = BoosterSettings.getPersonalBoosterMultiplier(level);
        PersonalBooster booster = new PersonalBooster(player.getUniqueId(), level, multiplier);

        PersonalBoosterManager.addBooster(booster);
        player.sendMessage("§aHas recibido un booster personal nivel " + level);
    }

    public static void applyBoosterInCalculation(Player player, double baseValue) {
        double multiplier = BoosterUtils.calculateCombinedMultiplier(player.getUniqueId());
        double result = baseValue * multiplier;

        player.sendMessage("§aValor base: " + baseValue);
        player.sendMessage("§eMultiplicador: " + BoosterUtils.formatMultiplier(multiplier));
        player.sendMessage("§6Resultado: " + result);
    }
    public void lossPower(ICustomNpc<?> npc){
        Location location = new Location ( Main.instance.getServer ( ).getWorld ( "world" ), npc.getX ( ), npc.getY ( ), npc.getZ ( ) );
        Faction faction = BoardColl.get ( ).getFactionAt ( PS.valueOf ( location ) );
        for (MPlayer mPlayer : faction.getMPlayers()) {
            mPlayer.setPower(mPlayer.getPower() - 1);
        }
    }
    public void lossPower(ICustomNpc<?> npc, int power){
        Location location = new Location ( Main.instance.getServer ( ).getWorld ( "world" ), npc.getX ( ), npc.getY ( ), npc.getZ ( ) );
        Faction faction = BoardColl.get ( ).getFactionAt ( PS.valueOf ( location ) );
        for (MPlayer mPlayer : faction.getMPlayers()) {
            mPlayer.setPower(mPlayer.getPower() - power);
        }
    }
    public void unclaim ( ICustomNpc<?> npc ) {
        Location location = new Location ( Main.instance.getServer ( ).getWorld ( "world" ), npc.getX ( ), npc.getY ( ), npc.getZ ( ) );
        Faction faction = BoardColl.get ( ).getFactionAt ( PS.valueOf ( location ) );
        int chunkX = location.getChunk ( ).getX ( );
        int chunkZ = location.getChunk ( ).getZ ( );
        PS ps = PS.valueOf ( location.getWorld ( ).getName ( ), chunkX, chunkZ );
        Bukkit.broadcastMessage ( "§cSe desclaimó el chunk en X:" + chunkX + " Z:" + chunkZ + " de la facción " + faction.getName ( ) );
        Faction wilderness = FactionColl.get ( ).getNone ( );
        BoardColl.get ( ).setFactionAt ( ps, wilderness );
    }
    public void unclaim(ICustomNpc<?> npc, int radio) {
        World world = Main.instance.getServer().getWorld("world");
        Location location = new Location(world, npc.getX(), npc.getY(), npc.getZ());

        // Facción dueña del chunk actual
        Faction faction = BoardColl.get().getFactionAt(PS.valueOf(location));
        if (faction == null) return;

        // Chunk central
        int centerX = location.getChunk().getX();
        int centerZ = location.getChunk().getZ();

        // Wilderness (sin dueño)
        Faction wilderness = FactionColl.get().getNone();

        int unclaimed = 0;

        for (int dx = -radio; dx <= radio; dx++) {
            for (int dz = -radio; dz <= radio; dz++) {
                if (dx * dx + dz * dz > radio * radio) continue;

                int chunkX = centerX + dx;
                int chunkZ = centerZ + dz;

                PS ps = PS.valueOf(world.getName(), chunkX, chunkZ);
                Faction current = BoardColl.get().getFactionAt(ps);

                if (current != null && current.equals(faction)) {
                    BoardColl.get().setFactionAt(ps, wilderness);
                    unclaimed++;
                }
            }
        }

        Bukkit.broadcastMessage("§cSe desclaimaron §e" + unclaimed + " §cchunks en radio de " + radio + " de la facción " + faction.getName());
    }
    public String getPlayerFactionName ( Player player ) {
        MPlayer mPlayer = MPlayer.get ( player );
        Faction faction = mPlayer.getFaction ( );
        if (faction == null) return null;
        return faction.getName ( );
    }

    public String getPlayerAtFactionLoc ( Player player ) {
        Faction faction2 = BoardColl.get ( ).getFactionAt ( PS.valueOf ( player.getLocation ( ) ) );
        if (faction2 == null) return null;
        return faction2.getName ( );
    }

    public String getTopLandFaction () {
        return FactionColl.get ( ).getAll ( ).stream ( ).reduce ( ( a, b ) -> {
            if (a.getLandCount ( ) > b.getLandCount ( )) return a;
            else return b;
        } ).orElse ( new Faction ( ) ).getName ( );
    }

    public boolean hasAccessFaction ( String name ) {
        Player player = Bukkit.getPlayer ( name );
        MPlayer mPlayer = MPlayer.get ( player );
        Faction faction = mPlayer.getFaction ( );
        long alliesCount = FactionColl.get ( ).getAll ( ).stream ( )
                .filter ( e -> e.getRelationTo ( faction ) == Rel.ALLY )
                .count ( );

        if (faction == null) return false;
        Faction faction2 = BoardColl.get ( ).getFactionAt ( PS.valueOf ( player.getLocation ( ) ) );
        if (faction2 != null) {
            if (faction.getName ( ).equalsIgnoreCase ( faction2.getName ( ) )
                    && !faction.getName ( ).contains ( "Wilderness" )) {
                return true;
            }
        }
        return false;
    }
}