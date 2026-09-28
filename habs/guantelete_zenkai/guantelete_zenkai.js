/*
 * Guantelete Zenkai - hab de item para Debentialc
 *
 * Donde va: plugins/Debentialc/scripts/guantelete_zenkai.js
 *           (el nombre del archivo tiene que ser igual al id del item)
 *
 * Este archivo es ASCII a proposito: el plugin lo lee con la codificacion por defecto
 * del servidor, asi que los acentos de los mensajes van escritos como escapes unicode.
 *
 * Como funciona:
 *   - Clic derecho: prepara o desactiva el Zenkai. Prepararlo no gasta usos.
 *   - Mientras esta preparado, un vigilante revisa tu vida de DBC cada medio segundo.
 *     Si baja del 20% de tu vida maxima, el Zenkai se activa solo: te cura el 30% de la
 *     vida maxima, hace un barrido que empuja y dana a los NPCs enemigos cercanos con el
 *     50% de tu ultimo golpe, y gasta un uso.
 *   - Despues de activarse queda en enfriamiento y hay que volver a prepararlo.
 *   - El guantelete tiene que estar en tu inventario (en cualquier casilla) cuando tu
 *     vida baja; si no lo llevas encima, el Zenkai se pierde.
 *   - Solo responde a su dueno.
 *
 * Vida maxima: CustomNPC no la expone. Si el addon de DBC tiene getMaxBody() se usa esa;
 * si no, se usa la vida mas alta que el guantelete te vio desde que lo preparaste.
 *
 * Los usos se guardan en el NBT del guantelete, como en el Baculo Sagrado.
 */

var CONFIG = {
    ID_ITEM: "guantelete_zenkai",
    USOS: 5,                    // zenkais por guantelete
    UMBRAL_VIDA: 0.20,          // se activa al bajar de esta fraccion de tu vida maxima
    CURACION: 0.30,             // cura esta fraccion de tu vida maxima
    VIDA_MIN_PREPARAR: 0.50,    // no se puede preparar con menos vida que esto (si se conoce la maxima)
    RADIO: 5,                   // bloques del barrido
    PORCENTAJE_DANO: 0.50,      // fraccion de tu ultimo golpe que recibe cada enemigo
    FUERZA: 1.8,                // empuje horizontal del barrido
    ALTURA: 0.6,                // cuanto levanta a los NPCs
    ENFRIAMIENTO_MS: 300000,    // 5 minutos entre zenkais
    REVISAR_CADA_TICKS: 10,     // 10 ticks = medio segundo
    RADIO_ANUNCIO: 30,          // jugadores a esta distancia ven el anuncio del Zenkai
    CLAVE_ARMADO: "guantelete_zenkai_armado",
    CLAVE_REF: "guantelete_zenkai_ref",
    CLAVE_CD: "guantelete_zenkai_cd",
    CLAVE_GOLPE: "guantelete_zenkai_golpe",
    CLAVE_BARRIENDO: "guantelete_zenkai_barriendo",
    CLAVE_USOS: "guantelete_zenkai_usos"
};

var VERSION = "2026-09-28 zenkai-1";
var TAGGING_CLASS = "org.debentialc.customitems.tools.nbt.CustomItemTagging";
var NBT_CLASS = "org.debentialc.customitems.tools.nbt.NbtHandler";
var NPC_API_CLASS = "noppes.npcs.api.AbstractNpcAPI";
var NPC_BASE_CLASS = "noppes.npcs.entity.EntityNPCInterface";
var FUENTE_NPC_CLASS = "noppes.npcs.NpcDamageSource";
var FUENTE_SCRIPT_CLASS = "noppes.npcs.scripted.ScriptDamageSource";
var RAID_NPCS_CLASS = "org.debentialc.raids.managers.NPCSpawnManager";
var RAID_SESIONES_CLASS = "org.debentialc.raids.managers.RaidSessionManager";
var EFECTO_CLASS = "org.bukkit.EntityEffect";
var TIPO_DANO = "guantelete_zenkai";
var EVENTO_DANO = "EntityDamageByEntityEvent";
var EVENTO_SALIDA = "PlayerQuitEvent";

// ---------------------------------------------------------------------------
// Utilidades de reflexion (las clases se cargan con el cargador del plugin)
// ---------------------------------------------------------------------------

function cargar(nombreClase) {
    return api.getClass().getClassLoader().loadClass(nombreClase);
}

function buscarMetodo(clase, nombre, tipos) {
    var metodos = clase.getMethods();
    for (var i = 0; i < metodos.length; i++) {
        if (String(metodos[i].getName()) !== nombre) continue;
        var params = metodos[i].getParameterTypes();
        if (params.length !== tipos.length) continue;
        var coincide = true;
        for (var j = 0; j < params.length; j++) {
            if (String(params[j].getName()) !== tipos[j]) {
                coincide = false;
                break;
            }
        }
        if (coincide) return metodos[i];
    }
    return null;
}

function construir(nombreClase, nParams, args) {
    var constructores = cargar(nombreClase).getConstructors();
    for (var i = 0; i < constructores.length; i++) {
        if (constructores[i].getParameterTypes().length === nParams) {
            return constructores[i].newInstance(args);
        }
    }
    throw "no se encontr\u00f3 el constructor de " + nombreClase;
}

function numero(valor) {
    return valor == null ? null : Number(String(valor));
}

function enteroJava(n) {
    return buscarMetodo(cargar("java.lang.Integer"), "valueOf", ["java.lang.String"]).invoke(null, String(n));
}

// ---------------------------------------------------------------------------
// Dueno, NBT y DBC
// ---------------------------------------------------------------------------

// Mismo chequeo de dueno que el plugin. Si falla, bloquea en vez de dejarlo libre.
function esDuenoDe(stack, jugador) {
    try {
        var tagging = cargar(TAGGING_CLASS);
        var hasOwner = buscarMetodo(tagging, "hasOwner", ["org.bukkit.inventory.ItemStack"]);
        var isOwner = buscarMetodo(tagging, "isOwner", ["org.bukkit.inventory.ItemStack", "org.bukkit.entity.Player"]);
        if (hasOwner == null || isOwner == null) {
            api.error("[guantelete_zenkai] No se encontr\u00f3 hasOwner/isOwner en " + TAGGING_CLASS);
            return false;
        }
        if (String(hasOwner.invoke(null, stack)) !== "true") return false;
        return String(isOwner.invoke(null, stack, jugador)) === "true";
    } catch (e) {
        api.error("[guantelete_zenkai] Error verificando el due\u00f1o: " + e);
        return false;
    }
}

function nbtDe(stack) {
    var constructores = cargar(NBT_CLASS).getConstructors();
    for (var i = 0; i < constructores.length; i++) {
        var params = constructores[i].getParameterTypes();
        if (params.length === 1 && String(params[0].getName()) === "org.bukkit.inventory.ItemStack") {
            return constructores[i].newInstance(stack);
        }
    }
    throw "no se encontr\u00f3 el constructor NbtHandler(ItemStack)";
}

function dbcDe(jugador) {
    var instancia = buscarMetodo(cargar(NPC_API_CLASS), "Instance", []).invoke(null);
    return instancia.getPlayer(jugador.getName()).getDBCPlayer();
}

// Vida maxima de DBC si el addon la expone; -1 si no.
function vidaMaximaDBC(dbc) {
    try {
        var maxima = Number(dbc.getMaxBody());
        return maxima > 0 ? maxima : -1;
    } catch (e) {
        return -1;
    }
}

// Busca el guantelete del jugador en su inventario (cualquier casilla).
function buscarGuantelete(jugador) {
    var inventario = jugador.getInventory();
    for (var i = 0; i < inventario.getSize(); i++) {
        var stack = inventario.getItem(i);
        if (stack == null) continue;
        try {
            var nbt = nbtDe(stack);
            if (String(nbt.getString("debentialc_id")) !== CONFIG.ID_ITEM) continue;
            if (!esDuenoDe(stack, jugador)) continue;
            return { slot: i, stack: stack, nbt: nbt };
        } catch (e) {
            // Item sin NBT legible: no es el guantelete
        }
    }
    return null;
}

// ---------------------------------------------------------------------------
// Barrido (mismo sistema que el Baculo Sagrado)
// ---------------------------------------------------------------------------

function handleNpc(entidad) {
    try {
        var handle = entidad.getHandle();
        var clase = handle.getClass();
        while (clase != null) {
            if (String(clase.getName()) === NPC_BASE_CLASS) return handle;
            clase = clase.getSuperclass();
        }
    } catch (e) {
        // Entidades sin handle de NMS: no son NPCs
    }
    return null;
}

function esEnemigo(npc, yo) {
    var faccion = npc.faction;
    if (faccion == null) return false;
    return faccion.isAggressiveToPlayer(yo) || faccion.isNeutralToPlayer(yo);
}

// Misma regla que el sistema de raids: a un NPC de raid solo lo danan sus participantes.
function permitidoPorRaids(entidad, jugador) {
    try {
        var oleada = buscarMetodo(cargar(RAID_NPCS_CLASS), "getWaveIdForNpc", ["int"]).invoke(null, enteroJava(entidad.getEntityId()));
        if (oleada == null) return true;
        var id = String(oleada);
        var sesion = buscarMetodo(cargar(RAID_SESIONES_CLASS), "getSessionById", ["java.lang.String"]).invoke(null, id.substring(0, id.lastIndexOf("_wave_")));
        if (sesion == null) return false;
        return sesion.getActivePlayers().contains(jugador.getUniqueId());
    } catch (e) {
        api.warn("[guantelete_zenkai] No se pudo revisar el sistema de raids: " + e);
        return true;
    }
}

function efectoGolpe(entidad) {
    try {
        entidad.playEffect(cargar(EFECTO_CLASS).getField("HURT").get(null));
    } catch (e) {
        // Solo visual
    }
}

function fuenteDe(yo) {
    try {
        return construir(FUENTE_SCRIPT_CLASS, 1, [construir(FUENTE_NPC_CLASS, 2, [TIPO_DANO, yo])]);
    } catch (e) {
        api.warn("[guantelete_zenkai] No se pudo crear la fuente de da\u00f1o de CustomNPC: " + e);
        return null;
    }
}

// Golpe no letal: se resta directo de la vida (DBC convierte en golpe completo cualquier
// dano que venga del jugador). Golpe letal: con la fuente del dueno, para que cuente como suyo.
function danar(entidad, npc, cantidad, fuente) {
    var vida = entidad.getHealth();
    if (cantidad < vida) {
        entidad.setHealth(vida - cantidad);
        efectoGolpe(entidad);
        return;
    }
    var scriptNpc = npc.wrappedNPC;
    if (fuente != null && scriptNpc != null) {
        scriptNpc.hurt(cantidad, fuente);
    } else {
        entidad.damage(cantidad);
    }
}

function barrido(jugador, danoPorNpc) {
    var centro = jugador.getLocation();
    var mirada = centro.getDirection();
    var yo = jugador.getHandle();
    var cercanas = api.getNearbyEntities(centro, CONFIG.RADIO);
    var fuente = danoPorNpc > 0 ? fuenteDe(yo) : null;
    var golpeados = 0;

    api.setPlayerData(jugador, CONFIG.CLAVE_BARRIENDO, true);
    try {
        for (var i = 0; i < cercanas.size(); i++) {
            var entidad = cercanas.get(i);
            var npc = handleNpc(entidad);
            if (npc == null || !esEnemigo(npc, yo)) continue;
            if (!permitidoPorRaids(entidad, jugador)) continue;

            var direccion = entidad.getLocation().toVector().subtract(centro.toVector()).setY(0);
            if (direccion.lengthSquared() < 0.01) direccion = mirada.clone().setY(0);
            if (direccion.lengthSquared() < 0.01) continue;
            entidad.setVelocity(direccion.normalize().multiply(CONFIG.FUERZA).setY(CONFIG.ALTURA));
            if (danoPorNpc > 0) danar(entidad, npc, danoPorNpc, fuente);
            golpeados++;
        }
    } finally {
        api.removePlayerData(jugador, CONFIG.CLAVE_BARRIENDO);
    }
    return golpeados;
}

// ---------------------------------------------------------------------------
// Zenkai
// ---------------------------------------------------------------------------

function desarmar(jugador) {
    api.removePlayerData(jugador, CONFIG.CLAVE_ARMADO);
    api.removePlayerData(jugador, CONFIG.CLAVE_REF);
}

function zenkai(jugador, dbc, vida, referencia) {
    desarmar(jugador);
    var hallado = buscarGuantelete(jugador);
    if (hallado == null) {
        api.sendMessage(jugador, "&c\u2717 No llevabas el Guantelete Zenkai encima: el Zenkai se perdi\u00f3.");
        return;
    }

    var nuevaVida = Math.min(referencia, vida + referencia * CONFIG.CURACION);
    dbc.setHP(Math.round(nuevaVida));

    var golpe = numero(api.getPlayerData(jugador, CONFIG.CLAVE_GOLPE));
    if (golpe == null && hallado.nbt.hasKey(CONFIG.CLAVE_GOLPE)) golpe = hallado.nbt.getLong(CONFIG.CLAVE_GOLPE);
    var danoPorNpc = golpe != null && golpe > 0 ? golpe * CONFIG.PORCENTAJE_DANO : 0;
    barrido(jugador, danoPorNpc);

    var lugar = jugador.getLocation();
    api.strikeLightningEffect(lugar);
    api.playSoundAt(lugar, "ENDERDRAGON_GROWL", 1.0, 1.0);
    api.playSoundAt(lugar, "EXPLODE", 1.0, 0.8);
    api.sendMessage(jugador, "&6&l\u00a1ZENKAI! &eTu poder despierta al borde de la derrota.");
    var testigos = api.getNearbyPlayers(lugar, CONFIG.RADIO_ANUNCIO);
    for (var i = 0; i < testigos.size(); i++) {
        var testigo = testigos.get(i);
        if (!testigo.equals(jugador)) {
            api.sendMessage(testigo, "&6&l\u00a1ZENKAI! &e" + jugador.getName() + " despert\u00f3 su poder al borde de la derrota.");
        }
    }

    var inventario = jugador.getInventory();
    var usados = (hallado.nbt.hasKey(CONFIG.CLAVE_USOS) ? hallado.nbt.getInteger(CONFIG.CLAVE_USOS) : 0) + 1;
    if (usados >= CONFIG.USOS) {
        if (hallado.stack.getAmount() > 1) {
            hallado.nbt.setInteger(CONFIG.CLAVE_USOS, 0);
            var resto = hallado.nbt.getItemStack();
            resto.setAmount(hallado.stack.getAmount() - 1);
            inventario.setItem(hallado.slot, resto);
        } else {
            inventario.setItem(hallado.slot, null);
        }
        api.playSound(jugador, "ITEM_BREAK", 1.0, 1.0);
        api.sendMessage(jugador, "&c\u2726 El Guantelete Zenkai se rompi\u00f3 tras " + CONFIG.USOS + " zenkais.");
    } else {
        hallado.nbt.setInteger(CONFIG.CLAVE_USOS, usados);
        if (golpe != null && golpe > 0) hallado.nbt.setLong(CONFIG.CLAVE_GOLPE, Math.round(golpe));
        inventario.setItem(hallado.slot, hallado.nbt.getItemStack());
        var restantes = CONFIG.USOS - usados;
        api.sendMessage(jugador, "&6Guantelete Zenkai &7\u00bb te " + (restantes === 1 ? "queda &e1 &7zenkai" : "quedan &e" + restantes + " &7zenkais"));
    }
    jugador.updateInventory();
    api.setPlayerData(jugador, CONFIG.CLAVE_CD, new Date().getTime());
}

function revisar(jugador) {
    var dbc = dbcDe(jugador);
    var vida = Number(dbc.getHP());
    if (vida <= 0) return;
    var maxima = vidaMaximaDBC(dbc);
    var referencia = maxima;
    if (referencia <= 0) {
        var vista = numero(api.getPlayerData(jugador, CONFIG.CLAVE_REF));
        referencia = Math.max(vista == null ? 0 : vista, vida);
        api.setPlayerData(jugador, CONFIG.CLAVE_REF, referencia);
    }
    if (vida < referencia * CONFIG.UMBRAL_VIDA) zenkai(jugador, dbc, vida, referencia);
}

function revisarJugadores() {
    var jugadores = server.getOnlinePlayers();
    var lista = typeof jugadores.length === "number" ? jugadores : jugadores.toArray();
    for (var i = 0; i < lista.length; i++) {
        var jugador = lista[i];
        if (String(api.getPlayerData(jugador, CONFIG.CLAVE_ARMADO)) !== "true") continue;
        try {
            revisar(jugador);
        } catch (e) {
            api.warn("[guantelete_zenkai] Error revisando a " + jugador.getName() + ": " + e);
        }
    }
}

// El vigilante se crea una sola vez por carga del script. Tras /sadmin reload, el plugin
// quita los listeners de la carga anterior; el vigilante viejo lo detecta y se detiene.
function iniciarVigilante() {
    if (api.isEventRegistered(EVENTO_SALIDA)) return;
    api.log("[guantelete_zenkai] Script cargado, versi\u00f3n " + VERSION);
    api.on(EVENTO_SALIDA, function (evento) {
        desarmar(evento.getPlayer());
    });
    var plugin = server.getPluginManager().getPlugin("Debentialc");
    var tarea = null;
    tarea = server.getScheduler().runTaskTimer(plugin, function () {
        if (!api.isEventRegistered(EVENTO_SALIDA)) {
            if (tarea != null) tarea.cancel();
            return;
        }
        revisarJugadores();
    }, CONFIG.REVISAR_CADA_TICKS, CONFIG.REVISAR_CADA_TICKS);
}

// Mide los golpes cuerpo a cuerpo de los jugadores contra NPCs, igual que el baculo.
function registrarMedidor() {
    if (api.isEventRegistered(EVENTO_DANO)) return;
    api.on(EVENTO_DANO, function (evento) {
        if (evento.isCancelled()) return;
        var atacante = evento.getDamager();
        if (String(atacante.getType()) !== "PLAYER") return;
        if (handleNpc(evento.getEntity()) == null) return;
        if (String(api.getPlayerData(atacante, CONFIG.CLAVE_BARRIENDO)) === "true") return;
        var dano = evento.getDamage();
        if (dano > 0) api.setPlayerData(atacante, CONFIG.CLAVE_GOLPE, dano);
    }, "MONITOR");
}

// ---------------------------------------------------------------------------
// Clic derecho: preparar o desactivar
// ---------------------------------------------------------------------------

// El plugin solo ejecuta el script con un item en la mano, asi que no hace falta
// comprobar item/player contra null (en Rhino 1.7R4 eso llena la consola de avisos).
function main() {
    if (!esDuenoDe(item, player)) {
        api.sendMessage(player, "&c\u2717 Este guantelete no te responde. Solo su due\u00f1o puede usarlo.");
        api.playSound(player, "VILLAGER_NO", 1.0, 1.0);
        return;
    }

    registrarMedidor();
    iniciarVigilante();

    if (String(api.getPlayerData(player, CONFIG.CLAVE_ARMADO)) === "true") {
        desarmar(player);
        api.sendMessage(player, "&6Guantelete Zenkai &7\u00bb desactivado.");
        return;
    }

    var ultimo = numero(api.getPlayerData(player, CONFIG.CLAVE_CD));
    var ahora = new Date().getTime();
    if (ultimo != null && ahora - ultimo < CONFIG.ENFRIAMIENTO_MS) {
        var restante = Math.ceil((CONFIG.ENFRIAMIENTO_MS - (ahora - ultimo)) / 1000);
        api.sendMessage(player, "&7Tu cuerpo a\u00fan se recupera del \u00faltimo Zenkai: &e" + Math.floor(restante / 60) + "m " + (restante % 60) + "s");
        return;
    }

    var dbc;
    try {
        dbc = dbcDe(player);
    } catch (e) {
        api.error("[guantelete_zenkai] No se pudo leer la vida de DBC: " + e);
        api.sendMessage(player, "&c\u2717 El guantelete fall\u00f3. Avisa a un admin.");
        return;
    }
    var vida = Number(dbc.getHP());
    var maxima = vidaMaximaDBC(dbc);
    if (maxima > 0 && vida < maxima * CONFIG.VIDA_MIN_PREPARAR) {
        api.sendMessage(player, "&7Necesitas al menos el &e" + Math.round(CONFIG.VIDA_MIN_PREPARAR * 100) + "% &7de tu vida para preparar el Zenkai.");
        return;
    }

    api.setPlayerData(player, CONFIG.CLAVE_ARMADO, true);
    api.setPlayerData(player, CONFIG.CLAVE_REF, maxima > 0 ? maxima : vida);
    api.playSound(player, "FIRE_IGNITE", 1.0, 0.5);
    api.sendMessage(player, "&6Guantelete Zenkai &7\u00bb preparado. Si tu vida baja del &e" + Math.round(CONFIG.UMBRAL_VIDA * 100)
            + "%&7, despertar\u00e1s tu poder. &8(clic derecho para desactivar)");
}

main();
