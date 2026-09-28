/*
 * Báculo Sagrado — hab de item para Debentialc
 *
 * Dónde va: plugins/Debentialc/scripts/baculo_sagrado.js
 *           (el nombre del archivo tiene que ser igual al id del item)
 *
 * Qué hace al dar clic derecho:
 *   - Barrido de 360° que empuja y daña a los NPCs de CustomNPCs cercanos que sean
 *     enemigos del dueño: los de facción agresiva con él, y los de facción neutral
 *     que lo estén atacando. Los amistosos y los neutrales tranquilos no se tocan.
 *   - Daño por NPC: 20% del último golpe cuerpo a cuerpo que el dueño le dio a un NPC.
 *     DBC no expone un "daño de melee" que se pueda leer, así que se mide de los golpes
 *     reales (incluye forma, release, kaioken, items...). Después de cada reinicio hay
 *     que golpear un NPC una vez antes de usar el báculo.
 *   - 10 usos por báculo; al gastarlos se rompe. Un clic sin enemigos cerca no gasta uso.
 *   - Solo responde a su dueño: el que recibió el item con /ci give (ownerOnly: true).
 *
 * Cómo guarda los usos: en el valor de daño del item (item.setDurability). No toca
 * lore ni NBT, así no se pierden el id del item ni el tag de dueño. Por eso el
 * material tiene que ser uno sin durabilidad propia (la vara de blaze, id 369).
 */

var CONFIG = {
    USOS: 10,
    RADIO: 6,               // bloques alrededor del jugador
    FUERZA: 1.6,            // empuje horizontal
    ALTURA: 0.45,           // cuánto los levanta
    PORCENTAJE_DANO: 0.20,  // fracción del último golpe del dueño
    COOLDOWN_MS: 3000,      // evita que un solo clic gaste dos usos
    CLAVE_COOLDOWN: "baculo_sagrado_cd",
    CLAVE_GOLPE: "baculo_sagrado_golpe",
    CLAVE_BARRIENDO: "baculo_sagrado_barriendo"
};

var TAGGING_CLASS = "org.debentialc.customitems.tools.nbt.CustomItemTagging";
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

// Usa el mismo chequeo de dueño que el plugin (CustomItemTagging). Si algo falla,
// el báculo queda bloqueado para todos en lugar de quedar libre para cualquiera.
function esDueno() {
    try {
        var tagging = api.getClass().getClassLoader().loadClass(TAGGING_CLASS);
        var hasOwner = buscarMetodo(tagging, "hasOwner", ["org.bukkit.inventory.ItemStack"]);
        var isOwner = buscarMetodo(tagging, "isOwner", ["org.bukkit.inventory.ItemStack", "org.bukkit.entity.Player"]);
        if (hasOwner == null || isOwner == null) {
            api.error("[baculo_sagrado] No se encontró hasOwner/isOwner en " + TAGGING_CLASS + "; báculo bloqueado.");
            return false;
        }
        if (String(hasOwner.invoke(null, item)) !== "true") return false;
        return String(isOwner.invoke(null, item, player)) === "true";
    } catch (e) {
        api.error("[baculo_sagrado] Error verificando el dueño: " + e);
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

// Agresivo con el dueño según su facción, o neutral y atacándolo ahora mismo.
function esEnemigo(npc, yo) {
    var faccion = npc.faction;
    if (faccion == null) return false;
    if (faccion.isAggressiveToPlayer(yo)) return true;
    if (!faccion.isNeutralToPlayer(yo)) return false;

    var scriptNpc = npc.wrappedNPC;
    if (scriptNpc == null) return false;
    var objetivo = scriptNpc.getAttackTarget();
    return objetivo != null && objetivo.getMCEntity().equals(yo);
}

function numero(valor) {
    return valor == null ? null : Number(String(valor));
}

// Registra una sola vez el listener que mide los golpes cuerpo a cuerpo de los
// jugadores contra NPCs. Ignora el daño del propio barrido para no medirse a sí mismo.
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

function romper() {
    if (item.getAmount() > 1) {
        item.setAmount(item.getAmount() - 1);
        item.setDurability(0);
    } else {
        player.setItemInHand(null);
    }
    api.playSound(player, "ITEM_BREAK", 1.0, 1.0);
    api.sendMessage(player, "&c✦ El Báculo Sagrado se rompió tras " + CONFIG.USOS + " usos.");
}

// El plugin solo ejecuta el script con un item en la mano, así que no hace falta
// comprobar item/player contra null (en Rhino 1.7R4 eso llena la consola de avisos).
function main() {
    if (!esDueno()) {
        api.sendMessage(player, "&c✗ Este báculo no te responde. Solo su dueño puede usarlo.");
        api.playSound(player, "VILLAGER_NO", 1.0, 1.0);
        return;
    }

    registrarMedidor();

    var ahora = new Date().getTime();
    var ultimoUso = numero(api.getPlayerData(player, CONFIG.CLAVE_COOLDOWN));
    if (ultimoUso != null) {
        var restante = CONFIG.COOLDOWN_MS - (ahora - ultimoUso);
        if (restante > 0) {
            api.sendMessage(player, "&7El báculo se está recuperando: &e" + Math.ceil(restante / 1000) + "s");
            return;
        }
    }

    var usados = Number(item.getDurability());
    if (usados >= CONFIG.USOS) {
        romper();
        return;
    }

    var golpe = numero(api.getPlayerData(player, CONFIG.CLAVE_GOLPE));
    if (golpe == null) {
        api.sendMessage(player, "&7Golpea primero a un NPC: el báculo usa tu golpe para calcular su daño. &8(no se gastó ningún uso)");
        return;
    }
    var danoPorNpc = golpe * CONFIG.PORCENTAJE_DANO;

    var golpeados = barrido(danoPorNpc);
    if (golpeados === 0) {
        api.sendMessage(player, "&7No hay enemigos cerca. &8(no se gastó ningún uso)");
        return;
    }

    api.setPlayerData(player, CONFIG.CLAVE_COOLDOWN, ahora);
    usados++;

    api.sendMessage(player, "&6Báculo Sagrado &7» &e" + golpeados + " &7enemigo(s), &c" + Math.round(danoPorNpc)
            + " &7de daño c/u &8(" + Math.round(CONFIG.PORCENTAJE_DANO * 100) + "% de tu golpe de " + Math.round(golpe) + ")");
    if (usados >= CONFIG.USOS) {
        romper();
    } else {
        item.setDurability(usados);
        api.sendMessage(player, "&7Usos restantes: &e" + (CONFIG.USOS - usados) + "&7/" + CONFIG.USOS);
    }
    player.updateInventory();
}

main();
