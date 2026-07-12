package org.debentialc;

import lombok.Getter;
import noppes.npcs.api.event.INpcEvent;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
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
}