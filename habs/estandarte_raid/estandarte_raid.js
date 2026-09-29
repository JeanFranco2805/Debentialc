/*
 * Chispa Genki - hab de item para Debentialc (id interno: estandarte_raid)
 *
 * Donde va: plugins/Debentialc/scripts/estandarte_raid.js
 *           (el nombre del archivo tiene que ser igual al id del item)
 *           o desde el juego: /cimenu > item > Opciones Avanzadas > Scripts.
 *
 * Este archivo es ASCII a proposito: el plugin lo lee con la codificacion por defecto
 * del servidor, asi que los acentos de los mensajes van escritos como \u00e1, \u00f1...
 *
 * Que hace al dar clic derecho:
 *   - Solo funciona DURANTE UNA RAID y solo para quien sigue vivo en ella.
 *   - Restaura vida y ki de DBC a los participantes de esa misma raid que esten en un
 *     radio de 15 bloques (incluido quien lo usa): 20% de la vida maxima y 30% del ki
 *     maximo de cada uno.
 *   - No revive: a quien esta derribado (vida 0) no se le toca, y quien murio en la raid
 *     ya no cuenta como participante. Tampoco cura a nadie que este fuera de la raid.
 *   - Si nadie del radio necesita nada (todos con vida y ki completos), no gasta uso.
 *   - 3 usos por chispa; al gastarlos se apaga.
 *   - Enfriamiento COMPARTIDO: despues de un uso, nadie de esa raid puede usar otra
 *     chispa durante 30 s. Sin esto, varias chispas se encadenarian.
 *
 * Los porcentajes, el radio, los usos y el enfriamiento salen de CONFIG. Afectan al
 * equilibrio de las raids: conviene que los apruebe quien las disena.
 *
 * Como guarda los usos: en el NBT del item (clave estandarte_raid_usos), con el mismo
 * NbtHandler que usa el plugin para el id y el dueno. Este item no esta atado a un
 * dueno: lo puede usar quien lo tenga en la mano.
 */

var CONFIG = {
    USOS: 3,
    RADIO: 15,              // bloques alrededor de quien lo usa
    CURACION_VIDA: 0.20,    // fraccion de la vida maxima de cada aliado
    CURACION_KI: 0.30,      // fraccion del ki maximo de cada aliado
    COOLDOWN_MS: 30000,     // compartido por toda la raid
    AVISAR_USOS: 1,         // solo avisa los usos restantes cuando quedan estos o menos
    CLAVE_COOLDOWN: "estandarte_raid_cd",
    CLAVE_USOS: "estandarte_raid_usos"
};

var NBT_CLASS = "org.debentialc.customitems.tools.nbt.NbtHandler";
var RAID_SESIONES_CLASS = "org.debentialc.raids.managers.RaidSessionManager";
var NPC_API_CLASS = "noppes.npcs.api.AbstractNpcAPI";
var ESTADOS_TERMINADOS = ["COMPLETED", "FAILED"];

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

function numero(valor) {
    return valor == null ? null : Number(String(valor));
}

// Sesion de raid activa de quien usa la chispa, o null si no esta en una raid viva.
// Lanza una excepcion si no se puede consultar el sistema de raids: ese caso NO debe
// tratarse como "no hay raid" para que el fallo se vea.
function sesionDeRaid() {
    var metodo = buscarMetodo(cargar(RAID_SESIONES_CLASS), "getPlayerSession", ["java.util.UUID"]);
    if (metodo == null) throw "no se encontr\u00f3 RaidSessionManager.getPlayerSession(UUID)";
    var sesion = metodo.invoke(null, player.getUniqueId());
    if (sesion == null) return null;
    var estado = String(sesion.getStatus().name());
    if (ESTADOS_TERMINADOS.indexOf(estado) >= 0) return null;
    if (!sesion.getActivePlayers().contains(player.getUniqueId())) return null;
    return sesion;
}

function dbcDe(jugador) {
    var instancia = buscarMetodo(cargar(NPC_API_CLASS), "Instance", []).invoke(null);
    return instancia.getPlayer(jugador.getName()).getDBCPlayer();
}

// Lee un numero del addon de DBC; -1 si el metodo no existe o falla.
function leerDbc(dbc, metodo) {
    try {
        var valor = Number(dbc[metodo]());
        return isNaN(valor) ? -1 : valor;
    } catch (e) {
        return -1;
    }
}

// Estado de un participante: que puede recuperar y cuanto. null si no hay nada que dar.
function necesidad(jugador) {
    var dbc = dbcDe(jugador);
    if (dbc == null) return null;
    var vida = Number(dbc.getHP());                          // si falla, beneficiarios() lo avisa
    if (!(vida > 0)) return null;                            // derribado: no se revive
    var vidaMax = leerDbc(dbc, "getMaxBody");
    var ki = leerDbc(dbc, "getKi");
    var kiMax = leerDbc(dbc, "getMaxKi");
    var nuevaVida = vida;
    var nuevoKi = ki;
    if (vidaMax > 0 && vida < vidaMax) nuevaVida = Math.min(vidaMax, vida + Math.round(vidaMax * CONFIG.CURACION_VIDA));
    if (kiMax > 0 && ki >= 0 && ki < kiMax) nuevoKi = Math.min(kiMax, ki + Math.round(kiMax * CONFIG.CURACION_KI));
    if (nuevaVida === vida && nuevoKi === ki) return null;
    return { dbc: dbc, vida: nuevaVida, ki: nuevoKi, cambiaVida: nuevaVida !== vida, cambiaKi: nuevoKi !== ki };
}

// Participantes de la misma raid, dentro del radio, que necesitan algo.
function beneficiarios(sesion) {
    var lista = [];
    var cercanos = api.getNearbyPlayers(player.getLocation(), CONFIG.RADIO);
    for (var i = 0; i < cercanos.size(); i++) {
        var jugador = cercanos.get(i);
        if (!sesion.getActivePlayers().contains(jugador.getUniqueId())) continue;
        try {
            var falta = necesidad(jugador);
            if (falta != null) lista.push({ jugador: jugador, falta: falta });
        } catch (e) {
            api.warn("[estandarte_raid] No se pudo leer a " + jugador.getName() + ": " + e);
        }
    }
    return lista;
}

function aplicar(beneficiario) {
    var falta = beneficiario.falta;
    if (falta.cambiaVida) falta.dbc.setHP(Math.floor(falta.vida));
    if (falta.cambiaKi) falta.dbc.setKi(Math.floor(falta.ki));
}

// Enfriamiento compartido: se anota en todos los participantes conectados de la raid.
function anotarEnfriamiento(sesion, ahora) {
    var ids = sesion.getActivePlayers().toArray();
    for (var i = 0; i < ids.length; i++) {
        var conectado = server.getPlayer(ids[i]);
        if (conectado != null) api.setPlayerData(conectado, CONFIG.CLAVE_COOLDOWN, ahora);
    }
}

function anunciar(beneficiarios) {
    var pctVida = Math.round(CONFIG.CURACION_VIDA * 100);
    var pctKi = Math.round(CONFIG.CURACION_KI * 100);
    for (var i = 0; i < beneficiarios.length; i++) {
        var jugador = beneficiarios[i].jugador;
        var falta = beneficiarios[i].falta;
        var partes = [];
        if (falta.cambiaVida) partes.push("&a+" + pctVida + "% vida");
        if (falta.cambiaKi) partes.push("&b+" + pctKi + "% ki");
        var propio = String(jugador.getName()) === String(player.getName());
        var quien = propio ? "Compartiste la Chispa Genki" : "&f" + player.getName() + " &7comparti\u00f3 su Chispa Genki";
        api.sendMessage(jugador, "&a\u2726 &7" + quien + ": " + partes.join(" &7y "));
        api.playSound(jugador, "LEVEL_UP", 1.0, 1.4);
    }
    api.playSoundAt(player.getLocation(), "ORB_PICKUP", 1.0, 0.8);
}

// NbtHandler del plugin sobre una copia del item: se lee el contador y, al guardar,
// se reemplaza el item de la mano por la copia con el contador actualizado.
function nbtDe(stack) {
    var clase = cargar(NBT_CLASS);
    var constructores = clase.getConstructors();
    for (var i = 0; i < constructores.length; i++) {
        var params = constructores[i].getParameterTypes();
        if (params.length === 1 && String(params[0].getName()) === "org.bukkit.inventory.ItemStack") {
            return constructores[i].newInstance(stack);
        }
    }
    throw "no se encontr\u00f3 el constructor NbtHandler(ItemStack)";
}

function leerUsos(nbt) {
    return nbt.hasKey(CONFIG.CLAVE_USOS) ? nbt.getInteger(CONFIG.CLAVE_USOS) : 0;
}

// Cambia el item del slot en el tick siguiente: si se cambia en este mismo tick, el
// servidor vuelve a poner en la mano el item que habia antes del clic.
function reemplazarDespues(nuevo) {
    var inventario = player.getInventory();
    var slot = inventario.getHeldItemSlot();
    var tipo = item.getTypeId();
    var plugin = server.getPluginManager().getPlugin("Debentialc");
    server.getScheduler().runTaskLater(plugin, function () {
        var actual = inventario.getItem(slot);
        if (actual == null || actual.getTypeId() !== tipo) {
            api.warn("[estandarte_raid] " + player.getName() + " movio la chispa antes de guardar los usos");
            return;
        }
        inventario.setItem(slot, nuevo);
        player.updateInventory();
    }, 1);
}

function guardarUsos(nbt, usados) {
    nbt.setInteger(CONFIG.CLAVE_USOS, usados);
    reemplazarDespues(nbt.getItemStack());
}

function romper(nbt) {
    if (item.getAmount() > 1) {
        nbt.setInteger(CONFIG.CLAVE_USOS, 0);
        var resto = nbt.getItemStack();
        resto.setAmount(item.getAmount() - 1);
        reemplazarDespues(resto);
    } else {
        reemplazarDespues(null);
    }
    api.playSound(player, "ITEM_BREAK", 1.0, 1.0);
    api.sendMessage(player, "&c\u2726 La Chispa Genki se apag\u00f3 tras " + CONFIG.USOS + " usos.");
}

// El plugin solo ejecuta el script con un item en la mano, asi que no hace falta
// comprobar item/player contra null (en Rhino 1.7R4 eso llena la consola de avisos).
function main() {
    var sesion;
    try {
        sesion = sesionDeRaid();
    } catch (e) {
        api.error("[estandarte_raid] No se pudo consultar el sistema de raids; chispa bloqueada: " + e);
        api.sendMessage(player, "&c\u2717 La chispa fall\u00f3. Avisa a un admin.");
        return;
    }
    if (sesion == null) {
        api.sendMessage(player, "&7La chispa solo funciona durante una raid. &8(no se gast\u00f3 ning\u00fan uso)");
        return;
    }

    var ahora = new Date().getTime();
    var ultimoUso = numero(api.getPlayerData(player, CONFIG.CLAVE_COOLDOWN));
    if (ultimoUso != null) {
        var restante = CONFIG.COOLDOWN_MS - (ahora - ultimoUso);
        if (restante > 0) {
            api.sendMessage(player, "&7La chispa se est\u00e1 recuperando: &e" + Math.ceil(restante / 1000) + "s");
            return;
        }
    }

    var nbt;
    try {
        nbt = nbtDe(item);
    } catch (e2) {
        api.error("[estandarte_raid] No se pudo leer el NBT del item; chispa bloqueada: " + e2);
        api.sendMessage(player, "&c\u2717 La chispa fall\u00f3. Avisa a un admin.");
        return;
    }
    var usados = leerUsos(nbt);
    if (usados >= CONFIG.USOS) {
        romper(nbt);
        return;
    }

    var lista;
    try {
        lista = beneficiarios(sesion);
    } catch (e3) {
        api.error("[estandarte_raid] Error buscando a los participantes: " + e3);
        api.sendMessage(player, "&c\u2717 La chispa fall\u00f3. Avisa a un admin.");
        return;
    }
    if (lista.length === 0) {
        api.sendMessage(player, "&7Nadie a tu alrededor lo necesita. &8(no se gast\u00f3 ning\u00fan uso)");
        return;
    }

    for (var i = 0; i < lista.length; i++) {
        try {
            aplicar(lista[i]);
        } catch (e4) {
            api.warn("[estandarte_raid] No se pudo restaurar a " + lista[i].jugador.getName() + ": " + e4);
        }
    }
    anotarEnfriamiento(sesion, ahora);
    anunciar(lista);
    api.log("[estandarte_raid] " + player.getName() + " restaur\u00f3 a " + lista.length + " participante(s) de la sesi\u00f3n " + sesion.getSessionId());

    usados++;
    if (usados >= CONFIG.USOS) {
        romper(nbt);
    } else {
        guardarUsos(nbt, usados);
        var restantes = CONFIG.USOS - usados;
        if (restantes <= CONFIG.AVISAR_USOS) {
            api.sendMessage(player, "&6Chispa Genki &7\u00bb te " + (restantes === 1 ? "queda &e1 &7uso" : "quedan &e" + restantes + " &7usos"));
        }
    }
    player.updateInventory();
}

main();
