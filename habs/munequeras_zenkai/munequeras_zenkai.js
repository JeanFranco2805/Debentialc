// ============================================================
// HAB: "Quitarse las pesas" - Munequeras y tobilleras Zenkai
//
// Script GLOBAL DE JUGADORES de CustomNPC+ (Nashorn / ECMAScript).
// No es un script de /sadmin: esos solo corren con clic derecho y
// esta hab es pasiva.
//
// Donde va: Scripter > clic derecho al aire > "Players" > pestana
// NUEVA (+) > lenguaje ECMAScript > pegar > guardar.
//
// Que hace: con las munequeras PUESTAS en el inventario DBC (no en
// la mochila ni en las ranuras cosmeticas), cuando la vida DBC baja
// de UMBRAL (10%) "se caen las pesas":
//   - recuperas CURACION (15%) de tu vida maxima,
//   - +25% de STR y DEX durante 30 s,
//   - una onda empuja a los NPC enemigos (agresivos o neutrales),
//   - una sola vez cada 10 minutos. El enfriamiento se guarda en el
//     jugador: sobrevive a relogs y reinicios.
//
// Limites conocidos:
//   - No revive. Si un golpe te lleva de mas del 10% a 0, DBC te
//     derriba antes de que este script pueda mirar.
//   - Necesita leer la vida MAXIMA de DBC (getMaxBody u otro
//     accesor parecido). Si no lo encuentra no hace NADA y lo dice
//     en la consola del Scripter (una linea por minuto).
//
// Para probarla sin estar a punto de morir: sube UMBRAL a 0.95 y
// baja ENFRIAMIENTO_MS a 10000. Con DEPURAR en true la consola dice
// por que si o por que no se activa. Deja los valores originales
// cuando termines.
// ============================================================

var CONFIG = {
    ID_ITEM: "munequeras_zenkai",   // el id que le pusiste con /ci create
    UMBRAL: 0.10,                   // se activa con MENOS de este % de vida
    CURACION: 0.15,                 // % de la vida maxima que recupera
    BONUS: 1.25,                    // multiplicador de STR y DEX
    DURACION_BONUS_TICKS: 600,      // 600 ticks = 30 s
    ENFRIAMIENTO_MS: 600000,        // 10 minutos
    RADIO: 5,                       // radio de la onda, en bloques
    EMPUJE: 1.4,
    ALTURA: 0.45,
    RADIO_ANUNCIO: 30,              // quien esta cerca ve el aviso
    RANURAS: 8,                     // ranuras normales del inventario DBC que se revisan
    REVISAR_EQUIPO_MS: 2000,        // sin munequeras, no volver a mirar antes de esto
    ID_BONUS: "zenkai_pesas",
    ID_TIMER_REVISAR: 469211,       // ids de temporizador propios de esta hab
    ID_TIMER_BONUS: 469212,
    CLAVE_LISTO: "zenkai_pesas_listo",
    DEPURAR: false
};

var TIPO_JUGADOR = 1;               // EntityType.PLAYER
var TIPO_NPC = 2;                   // EntityType.NPC
var ACCESORES_MAXIMO = ["getMaxBody", "getMaxHP", "getMaxHealth", "getBodyMax"];

var accesorMaximo = null;           // el accesor que funciono
var sinEquipoDesde = {};            // nombre -> cuando se vio sin munequeras
var ultimoAviso = {};               // limita las lineas de consola

function ahora() {
    return new Date().getTime();
}

function numero(valor) {
    var n = Number(valor);
    return isNaN(n) ? -1 : n;
}

// Una linea de consola como maximo cada `ms` por jugador y tema.
function avisarConsola(nombre, tema, texto, ms) {
    var clave = nombre + "|" + tema;
    var t = ahora();
    if (ultimoAviso[clave] && t - ultimoAviso[clave] < ms) return;
    ultimoAviso[clave] = t;
    print("[munequeras_zenkai] " + nombre + ": " + texto);
}

function traza(nombre, texto) {
    if (CONFIG.DEPURAR) avisarConsola(nombre, "traza", texto, 2000);
}

// Un error sin atrapar hace que CustomNPC apague TODA la pestana para
// todos los jugadores. Por eso todo pasa por aqui.
function seguro(jugador, accion) {
    var nombre = "?";
    try {
        nombre = String(jugador.getName());
        accion(jugador, nombre);
    } catch (e) {
        avisarConsola(nombre, "error", "error: " + e, 60000);
    }
}

function vidaMaxima(dbc) {
    var nombres = accesorMaximo === null ? ACCESORES_MAXIMO : [accesorMaximo];
    for (var i = 0; i < nombres.length; i++) {
        if (typeof dbc[nombres[i]] !== "function") continue;
        var valor = numero(dbc[nombres[i]]());
        if (valor > 0) {
            accesorMaximo = nombres[i];
            return valor;
        }
    }
    return -1;
}

// Solo las ranuras NORMALES (vanity = false): las cosmeticas no cuentan.
function tienePuestas(dbc) {
    for (var ranura = 0; ranura < CONFIG.RANURAS; ranura++) {
        var pieza = dbc.getItem(ranura, false);
        if (pieza == null) continue;
        var tag = pieza.getNbt();
        if (tag.has("debentialc_id") && String(tag.getString("debentialc_id")) === CONFIG.ID_ITEM) return true;
    }
    return false;
}

// Leer el inventario DBC serializa al jugador entero: solo se hace
// cuando ya esta bajo el umbral, y sin munequeras no se repite en 2 s.
function equipoPuesto(nombre, dbc) {
    var t = ahora();
    if (sinEquipoDesde[nombre] && t - sinEquipoDesde[nombre] < CONFIG.REVISAR_EQUIPO_MS) return false;
    if (tienePuestas(dbc)) {
        delete sinEquipoDesde[nombre];
        return true;
    }
    sinEquipoDesde[nombre] = t;
    return false;
}

function listoEn(jugador) {
    var guardado = jugador.getStoredData(CONFIG.CLAVE_LISTO);
    return guardado == null ? 0 : numero(guardado);
}

function esEnemigo(npc, jugador) {
    var faccion = npc.getFaction();
    if (faccion == null) return false;
    return faccion.isAggressiveToPlayer(jugador) || faccion.isNeutralToPlayer(jugador);
}

function onda(jugador) {
    var x = jugador.getX();
    var y = jugador.getY();
    var z = jugador.getZ();
    var cerca = jugador.getSurroundingEntities(CONFIG.RADIO, TIPO_NPC);
    var empujados = 0;
    for (var i = 0; i < cerca.length; i++) {
        try {
            var npc = cerca[i];
            var dx = npc.getX() - x;
            var dy = npc.getY() - y;
            var dz = npc.getZ() - z;
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) > CONFIG.RADIO) continue;
            if (!esEnemigo(npc, jugador)) continue;
            var plano = Math.sqrt(dx * dx + dz * dz);
            if (plano < 0.01) {
                dx = 1;
                dz = 0;
                plano = 1;
            }
            npc.setMotion(dx / plano * CONFIG.EMPUJE, CONFIG.ALTURA, dz / plano * CONFIG.EMPUJE);
            empujados++;
        } catch (e) {
            avisarConsola("npc", "onda", "no pude empujar a un NPC: " + e, 60000);
        }
    }
    return empujados;
}

function efectos(jugador) {
    var mundo = jugador.getWorld();
    mundo.spawnParticle("hugeexplosion", jugador.getX(), jugador.getY() + 1, jugador.getZ(), 0, 0, 0, 0, 1);
    jugador.playSound("mob.enderdragon.growl", 0.8, 1.4);
    jugador.playSound("random.explode", 0.7, 0.8);
}

function anunciar(jugador, nombre) {
    var porcentaje = Math.round((CONFIG.BONUS - 1) * 100);
    var segundos = Math.round(CONFIG.DURACION_BONUS_TICKS / 20);
    jugador.sendMessage("&6&l\u00a1Las pesas caen! &eTu Zenkai despierta: &a+" + porcentaje + "% &efuerza y destreza durante &a" + segundos + " s&e.");
    var cerca = jugador.getSurroundingEntities(CONFIG.RADIO_ANUNCIO, TIPO_JUGADOR);
    for (var i = 0; i < cerca.length; i++) {
        cerca[i].sendMessage("&6" + nombre + " &esolt\u00f3 sus pesas: \u00a1Zenkai!");
    }
}

function quitarBonus(dbc) {
    dbc.removeBonusAttribute("str", CONFIG.ID_BONUS);
    dbc.removeBonusAttribute("dex", CONFIG.ID_BONUS);
}

function activar(jugador, nombre, dbc, vida, maxima) {
    // Primero el enfriamiento: si algo falla despues, no se repite en cada tick.
    jugador.setStoredData(CONFIG.CLAVE_LISTO, ahora() + CONFIG.ENFRIAMIENTO_MS);
    var nueva = Math.floor(Math.min(maxima, vida + Math.round(maxima * CONFIG.CURACION)));
    dbc.setHP(nueva);
    dbc.addBonusAttribute("str", CONFIG.ID_BONUS, "*", CONFIG.BONUS);
    dbc.addBonusAttribute("dex", CONFIG.ID_BONUS, "*", CONFIG.BONUS);
    jugador.getTimers().forceStart(CONFIG.ID_TIMER_BONUS, CONFIG.DURACION_BONUS_TICKS, false);

    // Lo cosmetico y la onda nunca pueden estropear la curacion.
    var empujados = 0;
    try { empujados = onda(jugador); } catch (e) { avisarConsola(nombre, "onda", "onda: " + e, 60000); }
    try { efectos(jugador); } catch (e2) { avisarConsola(nombre, "efectos", "efectos: " + e2, 60000); }
    try { anunciar(jugador, nombre); } catch (e3) { avisarConsola(nombre, "aviso", "aviso: " + e3, 60000); }
    print("[munequeras_zenkai] " + nombre + " activo las pesas: vida " + vida + " -> " + nueva + " de " + maxima + ", empujados " + empujados);
}

function revisar(jugador, nombre) {
    var dbc = jugador.getDBCPlayer();
    if (dbc == null) return;
    var vida = numero(dbc.getHP());
    if (vida <= 0) return;                                   // derribado o muerto: ya decidio DBC
    var maxima = vidaMaxima(dbc);
    if (maxima <= 0) {
        avisarConsola("todos", "sinmax", "no encuentro la vida maxima de DBC (probe " + ACCESORES_MAXIMO.join(", ") + "): la hab no hace nada", 60000);
        return;
    }
    if (vida >= maxima * CONFIG.UMBRAL) return;
    traza(nombre, "vida " + vida + "/" + maxima + " (" + Math.round(vida / maxima * 1000) / 10 + "%): bajo el umbral");
    if (ahora() < listoEn(jugador)) {
        traza(nombre, "en enfriamiento");
        return;
    }
    if (!equipoPuesto(nombre, dbc)) {
        traza(nombre, "sin las munequeras en las ranuras normales 0-" + (CONFIG.RANURAS - 1));
        return;
    }
    activar(jugador, nombre, dbc, vida, maxima);
}

function terminarBonus(jugador, nombre) {
    var dbc = jugador.getDBCPlayer();
    if (dbc == null) return;
    quitarBonus(dbc);
    jugador.sendMessage("&7Las pesas vuelven a pesar: se acab\u00f3 el Zenkai.");
}

// ---- eventos de CustomNPC+ ----

function tick(event) {
    seguro(event.player, revisar);
}

// DBC resta la vida al terminar el golpe: se mira un tick despues.
function damaged(event) {
    seguro(event.player, function (jugador, nombre) {
        if (jugador.getDBCPlayer() == null) return;
        jugador.getTimers().forceStart(CONFIG.ID_TIMER_REVISAR, 1, false);
    });
}

function timer(event) {
    var id = Number(event.id);
    if (id === CONFIG.ID_TIMER_REVISAR) seguro(event.player, revisar);
    else if (id === CONFIG.ID_TIMER_BONUS) seguro(event.player, terminarBonus);
}

// Si el servidor se apago con el bonus puesto, el bonus sigue en el
// jugador para siempre: al entrar se quita salvo que su temporizador
// (que tambien se guarda) siga corriendo.
function login(event) {
    seguro(event.player, function (jugador, nombre) {
        if (jugador.getTimers().has(CONFIG.ID_TIMER_BONUS)) return;
        var dbc = jugador.getDBCPlayer();
        if (dbc != null) quitarBonus(dbc);
    });
}
