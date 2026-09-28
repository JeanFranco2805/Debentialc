/*
 * Baculo Sagrado - hab de item para Debentialc
 *
 * Donde va: plugins/Debentialc/scripts/baculo_sagrado.js
 *           (el nombre del archivo tiene que ser igual al id del item)
 *           o desde el juego: /cimenu > item > Opciones Avanzadas > Scripts, pegando
 *           la URL de este archivo en GitHub.
 *
 * Este archivo es ASCII a proposito: el plugin lo lee con la codificacion por defecto
 * del servidor, asi que los acentos de los mensajes van escritos como \u00e1, \u00f1...
 * para que no salgan como '?' en servidores sin UTF-8.
 *
 * Que hace al dar clic derecho:
 *   - Barrido de 360 grados que empuja y dana a los NPCs de CustomNPCs cercanos cuya
 *     faccion sea agresiva o neutral con el dueno, esten peleando con el o no.
 *     Los amistosos no se tocan.
 *   - Dano por NPC: 20% del ultimo golpe cuerpo a cuerpo que el dueno le dio a un NPC.
 *     DBC no expone un "dano de melee" que se pueda leer, asi que se mide de los golpes
 *     reales (incluye forma, release, kaioken, items...). El ultimo golpe usado queda
 *     guardado en el baculo, asi que solo hay que pegarle a un NPC antes del primer uso.
 *   - 10 usos por baculo; al gastarlos se rompe. Un clic sin enemigos cerca no gasta uso.
 *   - Solo responde a su dueno: el que recibio el item con /ci give (ownerOnly: true).
 *
 * Como guarda los usos: en el NBT del item (clave baculo_sagrado_usos), con el mismo
 * NbtHandler que usa el plugin para el id y el dueno. Sirve con cualquier material,
 * incluidos items de mod como el Baston Magico de DBC. Si el item tiene durabilidad
 * propia, activa "Irrompible" en /cimenu para que no se gaste al pegar.
 */

var CONFIG = {
    USOS: 10,
    RADIO: 6,               // bloques alrededor del jugador
    FUERZA: 1.6,            // empuje horizontal
    ALTURA: 0.45,           // cuanto los levanta
    PORCENTAJE_DANO: 0.20,  // fraccion del ultimo golpe del dueno
    COOLDOWN_MS: 3000,      // evita que un solo clic gaste dos usos
    CLAVE_COOLDOWN: "baculo_sagrado_cd",
    CLAVE_GOLPE: "baculo_sagrado_golpe",
    CLAVE_BARRIENDO: "baculo_sagrado_barriendo",
    CLAVE_USOS: "baculo_sagrado_usos"
};

var TAGGING_CLASS = "org.debentialc.customitems.tools.nbt.CustomItemTagging";
var NBT_CLASS = "org.debentialc.customitems.tools.nbt.NbtHandler";
var NPC_BASE_CLASS = "noppes.npcs.entity.EntityNPCInterface";
var EVENTO_DANO = "EntityDamageByEntityEvent";

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

// Usa el mismo chequeo de dueno que el plugin (CustomItemTagging). Si algo falla,
// el baculo queda bloqueado para todos en lugar de quedar libre para cualquiera.
function esDueno() {
    try {
        var tagging = api.getClass().getClassLoader().loadClass(TAGGING_CLASS);
        var hasOwner = buscarMetodo(tagging, "hasOwner", ["org.bukkit.inventory.ItemStack"]);
        var isOwner = buscarMetodo(tagging, "isOwner", ["org.bukkit.inventory.ItemStack", "org.bukkit.entity.Player"]);
        if (hasOwner == null || isOwner == null) {
            api.error("[baculo_sagrado] No se encontr\u00f3 hasOwner/isOwner en " + TAGGING_CLASS + "; b\u00e1culo bloqueado.");
            return false;
        }
        if (String(hasOwner.invoke(null, item)) !== "true") return false;
        return String(isOwner.invoke(null, item, player)) === "true";
    } catch (e) {
        api.error("[baculo_sagrado] Error verificando el due\u00f1o: " + e);
        return false;
    }
}

// Devuelve la entidad de CustomNPC (EntityNPCInterface) o null si no es un NPC.
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

// Faccion agresiva o neutral con el dueno, este o no peleando con el.
function esEnemigo(npc, yo) {
    var faccion = npc.faction;
    if (faccion == null) return false;
    return faccion.isAggressiveToPlayer(yo) || faccion.isNeutralToPlayer(yo);
}

function numero(valor) {
    return valor == null ? null : Number(String(valor));
}

// Registra una sola vez el listener que mide los golpes cuerpo a cuerpo de los
// jugadores contra NPCs. Ignora el dano del propio barrido para no medirse a si mismo.
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

function barrido(danoPorNpc) {
    var centro = player.getLocation();
    var mirada = centro.getDirection();
    var yo = player.getHandle();
    var cercanas = api.getNearbyEntities(centro, CONFIG.RADIO);
    var golpeados = 0;

    api.setPlayerData(player, CONFIG.CLAVE_BARRIENDO, true);
    try {
        for (var i = 0; i < cercanas.size(); i++) {
            var entidad = cercanas.get(i);
            var npc = handleNpc(entidad);
            if (npc == null || !esEnemigo(npc, yo)) continue;

            var direccion = entidad.getLocation().toVector().subtract(centro.toVector()).setY(0);
            if (direccion.lengthSquared() < 0.01) direccion = mirada.clone().setY(0);
            if (direccion.lengthSquared() < 0.01) continue;

            var empuje = direccion.normalize().multiply(CONFIG.FUERZA).setY(CONFIG.ALTURA);
            entidad.setVelocity(empuje);
            if (danoPorNpc > 0) entidad.damage(danoPorNpc, player);
            golpeados++;
        }
    } finally {
        api.removePlayerData(player, CONFIG.CLAVE_BARRIENDO);
    }

    if (golpeados > 0) {
        api.playSoundAt(centro, "ENDERDRAGON_WING", 1.0, 1.2);
        api.playSoundAt(centro, "IRONGOLEM_THROW", 1.0, 0.8);
    }
    return golpeados;
}

// NbtHandler del plugin sobre una copia del item: se lee el contador y, al guardar,
// se reemplaza el item de la mano por la copia con el contador actualizado.
function nbtDe(stack) {
    var clase = api.getClass().getClassLoader().loadClass(NBT_CLASS);
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

// Cambia el item del slot en el tick siguiente. Si se cambia en este mismo tick, los items
// que tienen accion al clic derecho (espadas como el Baston Magico, arcos...) lo pisan:
// despues del evento el servidor vuelve a poner en la mano el item que habia antes.
function reemplazarDespues(nuevo) {
    var inventario = player.getInventory();
    var slot = inventario.getHeldItemSlot();
    var tipo = item.getTypeId();
    var plugin = server.getPluginManager().getPlugin("Debentialc");
    server.getScheduler().runTaskLater(plugin, function () {
        var actual = inventario.getItem(slot);
        if (actual == null || actual.getTypeId() !== tipo) {
            api.warn("[baculo_sagrado] " + player.getName() + " movio el baculo antes de guardar los usos");
            return;
        }
        inventario.setItem(slot, nuevo);
        player.updateInventory();
    }, 1);
}

function guardarUsos(nbt, usados, golpe) {
    nbt.setInteger(CONFIG.CLAVE_USOS, usados);
    nbt.setLong(CONFIG.CLAVE_GOLPE, Math.round(golpe));
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
    api.sendMessage(player, "&c\u2726 El B\u00e1culo Sagrado se rompi\u00f3 tras " + CONFIG.USOS + " usos.");
}

// El plugin solo ejecuta el script con un item en la mano, asi que no hace falta
// comprobar item/player contra null (en Rhino 1.7R4 eso llena la consola de avisos).
function main() {
    if (!esDueno()) {
        api.sendMessage(player, "&c\u2717 Este b\u00e1culo no te responde. Solo su due\u00f1o puede usarlo.");
        api.playSound(player, "VILLAGER_NO", 1.0, 1.0);
        return;
    }

    registrarMedidor();

    var ahora = new Date().getTime();
    var ultimoUso = numero(api.getPlayerData(player, CONFIG.CLAVE_COOLDOWN));
    if (ultimoUso != null) {
        var restante = CONFIG.COOLDOWN_MS - (ahora - ultimoUso);
        if (restante > 0) {
            api.sendMessage(player, "&7El b\u00e1culo se est\u00e1 recuperando: &e" + Math.ceil(restante / 1000) + "s");
            return;
        }
    }

    var nbt;
    try {
        nbt = nbtDe(item);
    } catch (e) {
        api.error("[baculo_sagrado] No se pudo leer el NBT del item; b\u00e1culo bloqueado: " + e);
        api.sendMessage(player, "&c\u2717 El b\u00e1culo fall\u00f3. Avisa a un admin.");
        return;
    }
    var usados = leerUsos(nbt);
    if (usados >= CONFIG.USOS) {
        romper(nbt);
        return;
    }

    // Golpe medido en esta sesion; si no hay (p. ej. tras un reinicio), el guardado en el baculo.
    var golpe = numero(api.getPlayerData(player, CONFIG.CLAVE_GOLPE));
    if (golpe == null && nbt.hasKey(CONFIG.CLAVE_GOLPE)) golpe = nbt.getLong(CONFIG.CLAVE_GOLPE);
    if (golpe == null || golpe <= 0) {
        api.sendMessage(player, "&7Golpea una vez a un NPC antes del primer uso: el b\u00e1culo usa tu golpe para calcular su da\u00f1o. &8(no se gast\u00f3 ning\u00fan uso)");
        return;
    }
    var danoPorNpc = golpe * CONFIG.PORCENTAJE_DANO;

    var golpeados = barrido(danoPorNpc);
    if (golpeados === 0) {
        api.sendMessage(player, "&7No hay enemigos cerca. &8(no se gast\u00f3 ning\u00fan uso)");
        return;
    }

    api.setPlayerData(player, CONFIG.CLAVE_COOLDOWN, ahora);
    usados++;

    api.sendMessage(player, "&6B\u00e1culo Sagrado &7\u00bb &e" + golpeados + " &7enemigo(s), &c" + Math.round(danoPorNpc)
            + " &7de da\u00f1o c/u &8(" + Math.round(CONFIG.PORCENTAJE_DANO * 100) + "% de tu golpe de " + Math.round(golpe) + ")");
    if (usados >= CONFIG.USOS) {
        romper(nbt);
    } else {
        guardarUsos(nbt, usados, golpe);
        api.sendMessage(player, "&7Usos restantes: &e" + (CONFIG.USOS - usados) + "&7/" + CONFIG.USOS);
    }
    player.updateInventory();
}

main();
