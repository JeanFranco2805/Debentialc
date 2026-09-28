// ============================================================
// PRUEBA de las Munequeras Zenkai (solo diagnostico).
// No cura, no pega y no cambia nada: solo muestra datos.
//
// Donde va: Scripter de CustomNPC+ > clic derecho al aire >
// "Players" > pestana NUEVA (+) > lenguaje ECMAScript.
// Uso: escribe !zenkai en el chat y deja que un NPC te pegue.
// Cuando terminemos, borra esta pestana.
// ============================================================

var COMANDO = "!zenkai";
var ID_TIMER = 469201;      // id unico para no chocar con otros scripts
var GOLPES_A_MOSTRAR = 3;

var pedidos = {};           // nombre -> true si pidio el reporte
var activos = {};           // nombre -> golpes que faltan por mostrar
var ultimaVida = {};        // nombre -> vida DBC en el ultimo tick

function avisar(p, texto) {
    p.sendMessage("&6[Zenkai-prueba] &f" + texto);
    print(p.getName() + ": " + texto);
}

function llamar(objeto, metodo) {
    try {
        if (typeof objeto[metodo] !== "function") return "no existe";
        return String(objeto[metodo]());
    } catch (e) {
        return "error: " + e;
    }
}

function metodosDe(objeto, patron) {
    var metodos = objeto.getClass().getMethods();
    var vistos = {};
    var lista = [];
    for (var i = 0; i < metodos.length; i++) {
        var nombre = String(metodos[i].getName());
        if (patron.test(nombre) && !vistos[nombre]) {
            vistos[nombre] = true;
            lista.push(nombre);
        }
    }
    return lista.sort().join(", ");
}

function describirItem(it) {
    var tag = it.getItemNbt().getCompound("tag");
    var id = tag.has("debentialc_id") ? String(tag.getString("debentialc_id")) : "-";
    var claves = Java.from(tag.getKeys()).join(",");
    return it.getName() + " | " + it.getDisplayName() + "&e | id=" + id + " | nbt=" + (claves || "-");
}

function reporte(p) {
    var dbc = p.getDBCPlayer();
    if (dbc == null) {
        avisar(p, "&cgetDBCPlayer() devolvio null");
        return false;
    }
    avisar(p, "Clase DBC: &7" + dbc.getClass().getName());
    avisar(p, "Vida DBC: &a" + dbc.getHP());
    avisar(p, "getMaxBody: &a" + llamar(dbc, "getMaxBody") + "&f  getMaxHP: &a" + llamar(dbc, "getMaxHP"));
    avisar(p, "Metodos: &7" + metodosDe(dbc, /max|percent/i));

    var inventario = dbc.getInventory();
    avisar(p, "Items en el inventario DBC: &a" + (inventario.length - 36));
    for (var ranura = 0; ranura <= 20; ranura++) {
        var it = dbc.getItem(10 - ranura, true);
        if (it != null) avisar(p, "Ranura " + ranura + ": &e" + describirItem(it));
    }

    try {
        var Bukkit = Java.type("org.bukkit.Bukkit");
        var plugin = Bukkit.getPluginManager().getPlugin("Debentialc");
        avisar(p, "Bukkit: &aOK&f  Debentialc: " + (plugin == null ? "&cno encontrado" : "&a" + plugin.getDescription().getVersion()));
    } catch (e) {
        avisar(p, "Bukkit: &cno accesible &7(" + e + ")");
    }
    return true;
}

function chat(event) {
    if (String(event.message).trim().toLowerCase() !== COMANDO) return;
    event.setCanceled(true);
    pedidos[event.player.getName()] = true;
}

function tick(event) {
    var p = event.player;
    var nombre = p.getName();
    try {
        if (pedidos[nombre]) {
            pedidos[nombre] = false;
            if (reporte(p)) {
                activos[nombre] = GOLPES_A_MOSTRAR;
                avisar(p, "Ahora deja que un NPC te pegue " + GOLPES_A_MOSTRAR + " veces.");
            }
        }
        if (activos[nombre] > 0) ultimaVida[nombre] = p.getDBCPlayer().getHP();
    } catch (e) {
        avisar(p, "&cError: " + e);
    }
}

function damaged(event) {
    var p = event.player;
    var nombre = p.getName();
    if (!(activos[nombre] > 0)) return;
    activos[nombre]--;
    try {
        avisar(p, "Golpe: dano=" + event.damage + "  vida antes=" + ultimaVida[nombre] + "  vida en el golpe=&a" + p.getDBCPlayer().getHP());
        p.getTimers().forceStart(ID_TIMER, 1, false);
    } catch (e) {
        avisar(p, "&cError en el golpe: " + e);
    }
}

function timer(event) {
    if (Number(event.id) !== ID_TIMER) return;
    var p = event.player;
    try {
        avisar(p, "  1 tick despues: vida=&a" + p.getDBCPlayer().getHP());
    } catch (e) {
        avisar(p, "&cError en el timer: " + e);
    }
}
