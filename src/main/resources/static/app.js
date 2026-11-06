// ValidaSQL: página única. Envía el esquema y las consultas a la API y dibuja una tarjeta por sentencia.
// No usa ningún framework y construye el DOM con textContent, así que el SQL del usuario nunca se
// interpreta como HTML.
"use strict";

const ETIQUETAS_ESTADO = {
    VALIDA: "Válida",
    CON_ADVERTENCIAS: "Con advertencias",
    CON_ERRORES: "Con errores",
};

const entradaEsquema = document.getElementById("esquema");
const entradaConsultas = document.getElementById("consultas");
const botonAnalizar = document.getElementById("analizar");
const contenedorEstado = document.getElementById("estado");
const contenedorResumen = document.getElementById("resumen");
const contenedorResultados = document.getElementById("resultados");

let contadorTarjetas = 0;

/** Crea un elemento con atributos y contenido: el texto se agrega siempre como texto plano. */
function el(etiqueta, atributos = {}, ...hijos) {
    const nodo = document.createElement(etiqueta);
    for (const [nombre, valor] of Object.entries(atributos)) {
        if (valor === false || valor === null || valor === undefined) continue;
        if (nombre === "clase") nodo.className = valor;
        else nodo.setAttribute(nombre, valor === true ? "" : valor);
    }
    for (const hijo of hijos.flat()) {
        nodo.append(hijo instanceof Node ? hijo : document.createTextNode(String(hijo)));
    }
    return nodo;
}

function mostrarEstado(texto, esError = false) {
    contenedorEstado.hidden = false;
    contenedorEstado.textContent = texto;
    contenedorEstado.classList.toggle("error", esError);
}

// ------------------------------------------------------------------ posición del cursor

/** Muestra «Línea L, columna C» del cursor, con la misma cuenta desde 1 que usan los diagnósticos. */
function actualizarPosicion(entrada, salida) {
    const antes = entrada.value.slice(0, entrada.selectionStart);
    const lineas = antes.split("\n");
    salida.textContent = `Línea ${lineas.length}, columna ${lineas[lineas.length - 1].length + 1}`;
}

function vincularPosicion(entrada, idSalida) {
    const salida = document.getElementById(idSalida);
    for (const evento of ["input", "click", "keyup", "focus"]) {
        entrada.addEventListener(evento, () => actualizarPosicion(entrada, salida));
    }
}

/** Lleva el cursor del editor a (línea, columna), contadas desde 1. */
function irA(entrada, linea, columna) {
    const lineas = entrada.value.split("\n");
    let desplazamiento = 0;
    for (let i = 0; i < linea - 1 && i < lineas.length; i++) desplazamiento += lineas[i].length + 1;
    desplazamiento += columna - 1;
    entrada.focus();
    entrada.setSelectionRange(desplazamiento, desplazamiento + 1);
}

// ------------------------------------------------------------------ dibujo de resultados

function dibujarDiagnosticos(sentencia, entradaOrigen) {
    if (sentencia.diagnosticos.length === 0) {
        return el("p", { clase: "valida" }, "Sentencia válida. Sin errores ni advertencias.");
    }
    return el("ul", { clase: "diagnosticos" }, sentencia.diagnosticos.map((d) => {
        const esAdvertencia = d.severidad === "ADVERTENCIA";
        const posicion = el("button", {
            type: "button",
            clase: "posicion-diag",
            title: "Ir a esta posición en el editor",
        }, `${d.linea}:${d.columna}`);
        posicion.addEventListener("click", () => irA(entradaOrigen, d.linea, d.columna));
        // El texto completo trae el prefijo «[M02] Error semántico (línea 1, columna 8). »; en pantalla
        // se muestra solo el mensaje, porque el código y la posición ya van aparte.
        const mensaje = d.texto.replace(/^\[[A-Z]\d+\] [^(]+\(línea \d+, columna \d+\)\.\s*/, "");
        return el("li", { clase: "diagnostico" },
            el("span", { clase: `codigo ${esAdvertencia ? "advertencia" : "error"}` }, d.codigo),
            posicion,
            el("span", { clase: "mensaje" }, mensaje));
    }));
}

function dibujarTokens(sentencia) {
    if (sentencia.tokens.length === 0) return el("p", { clase: "vacio" }, "Sin tokens.");
    return el("table", {},
        el("thead", {}, el("tr", {},
            el("th", { clase: "num" }, "N°"), el("th", {}, "Token"), el("th", {}, "Lexema"),
            el("th", { clase: "num" }, "Línea"), el("th", { clase: "num" }, "Columna"))),
        el("tbody", {}, sentencia.tokens.map((t, i) => el("tr", {},
            el("td", { clase: "num" }, i + 1), el("td", { clase: "mono" }, t.tipo),
            el("td", { clase: "mono" }, t.lexema),
            el("td", { clase: "num" }, t.linea), el("td", { clase: "num" }, t.columna)))));
}

function dibujarArbol(sentencia) {
    if (!sentencia.arbol) {
        return el("p", { clase: "vacio" }, "No hay árbol: la sentencia tiene errores léxicos o sintácticos.");
    }
    return el("pre", { clase: "arbol" }, sentencia.arbol);
}

function dibujarTraza(sentencia) {
    if (sentencia.traza.length === 0) {
        return el("p", { clase: "vacio" }, "No hay traza: la sentencia no llegó al análisis sintáctico.");
    }
    return el("table", {},
        el("thead", {}, el("tr", {},
            el("th", { clase: "num" }, "Paso"), el("th", {}, "No terminal"), el("th", {}, "Token"),
            el("th", {}, "Producción elegida"), el("th", {}, "Motivo"))),
        el("tbody", {}, sentencia.traza.map((p) => el("tr", {},
            el("td", { clase: "num" }, p.paso), el("td", { clase: "mono" }, p.noTerminal),
            el("td", { clase: "mono" }, p.token), el("td", { clase: "mono" }, p.produccion),
            el("td", { clase: `motivo ${p.motivo}` }, p.motivo)))));
}

/** Tarjeta de una sentencia con sus cuatro pestañas: Resultado, Tokens, Árbol y Traza LL(1). */
function dibujarTarjeta(sentencia, etiqueta, entradaOrigen) {
    const id = `tarjeta-${++contadorTarjetas}`;
    const paneles = [
        ["Resultado", dibujarDiagnosticos(sentencia, entradaOrigen)],
        ["Tokens", dibujarTokens(sentencia)],
        ["Árbol", dibujarArbol(sentencia)],
        ["Traza LL(1)", dibujarTraza(sentencia)],
    ];

    const lista = el("div", { clase: "pestanas", role: "tablist", "aria-label": "Fases del análisis" });
    const nodosPanel = [];
    const botones = paneles.map(([nombre, contenido], i) => {
        const boton = el("button", {
            type: "button", role: "tab", id: `${id}-pestana-${i}`,
            "aria-controls": `${id}-panel-${i}`, "aria-selected": i === 0 ? "true" : "false",
            tabindex: i === 0 ? "0" : "-1",
        }, nombre);
        const panel = el("div", {
            role: "tabpanel", id: `${id}-panel-${i}`, "aria-labelledby": `${id}-pestana-${i}`,
            hidden: i !== 0,
        }, contenido);
        nodosPanel.push(panel);
        return boton;
    });

    function seleccionar(indice) {
        botones.forEach((b, i) => {
            b.setAttribute("aria-selected", String(i === indice));
            b.tabIndex = i === indice ? 0 : -1;
            nodosPanel[i].hidden = i !== indice;
        });
        botones[indice].focus();
    }
    botones.forEach((boton, i) => {
        boton.addEventListener("click", () => seleccionar(i));
        boton.addEventListener("keydown", (evento) => {
            if (evento.key === "ArrowRight") { evento.preventDefault(); seleccionar((i + 1) % botones.length); }
            if (evento.key === "ArrowLeft") { evento.preventDefault(); seleccionar((i - 1 + botones.length) % botones.length); }
        });
        lista.append(boton);
    });

    return el("article", { clase: "tarjeta" },
        el("div", { clase: "tarjeta-cabecera" },
            el("h3", {}, `${etiqueta} ${sentencia.numero}`),
            el("span", { clase: "linea" }, `línea ${sentencia.linea}`),
            el("span", { clase: `insignia-estado ${sentencia.estado}` }, ETIQUETAS_ESTADO[sentencia.estado])),
        el("pre", { clase: "sentencia-texto" }, sentencia.texto),
        lista,
        nodosPanel);
}

function dibujarResumen(resumen) {
    const datos = [
        ["Tablas cargadas", resumen.tablas],
        ["Sentencias", resumen.sentencias],
        ["Válidas", resumen.validas],
        ["Con advertencias", resumen.conAdvertencias],
        ["Con errores", resumen.conErrores],
    ];
    contenedorResumen.replaceChildren(...datos.map(([nombre, valor]) =>
        el("span", {}, `${nombre}: `, el("strong", {}, valor))));
    contenedorResumen.hidden = false;
}

function dibujarResultado(resultado) {
    dibujarResumen(resultado.resumen);

    const problemasEsquema = resultado.esquema.filter((s) => s.diagnosticos.length > 0);
    const nodos = [];
    if (problemasEsquema.length > 0) {
        nodos.push(el("h2", { clase: "titulo-seccion" }, "Esquema con problemas"));
        problemasEsquema.forEach((s) => nodos.push(dibujarTarjeta(s, "Esquema", entradaEsquema)));
    }
    if (resultado.consultas.length > 0) {
        nodos.push(el("h2", { clase: "titulo-seccion" }, "Consultas"));
        resultado.consultas.forEach((s) => nodos.push(dibujarTarjeta(s, "Sentencia", entradaConsultas)));
    }
    contenedorResultados.replaceChildren(...nodos);

    if (resultado.consultas.length === 0) {
        mostrarEstado("No hay consultas para analizar.");
    } else {
        contenedorEstado.hidden = true;
    }
}

// ------------------------------------------------------------------ llamadas a la API

async function analizar() {
    botonAnalizar.disabled = true;
    mostrarEstado("Analizando...");
    try {
        const respuesta = await fetch("/api/analizar", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ esquema: entradaEsquema.value, consultas: entradaConsultas.value }),
        });
        if (!respuesta.ok) throw new Error(`La API respondió ${respuesta.status}`);
        dibujarResultado(await respuesta.json());
    } catch (error) {
        contenedorResumen.hidden = true;
        contenedorResultados.replaceChildren();
        mostrarEstado(`No se pudo analizar: ${error.message}`, true);
    } finally {
        botonAnalizar.disabled = false;
    }
}

async function cargarEjemplo() {
    try {
        const respuesta = await fetch("/api/esquema-ejemplo");
        if (!respuesta.ok) throw new Error(`La API respondió ${respuesta.status}`);
        const ejemplo = await respuesta.json();
        entradaEsquema.value = ejemplo.esquema;
        entradaConsultas.value = ejemplo.consultas;
        await analizar();
    } catch (error) {
        mostrarEstado(`No se pudo cargar el ejemplo: ${error.message}`, true);
    }
}

function limpiar() {
    entradaEsquema.value = "";
    entradaConsultas.value = "";
    contenedorResumen.hidden = true;
    contenedorResultados.replaceChildren();
    mostrarEstado("Escribe un esquema y unas consultas, o carga el ejemplo, y pulsa Analizar.");
    entradaEsquema.focus();
}

botonAnalizar.addEventListener("click", analizar);
document.getElementById("ejemplo").addEventListener("click", cargarEjemplo);
document.getElementById("limpiar").addEventListener("click", limpiar);
for (const entrada of [entradaEsquema, entradaConsultas]) {
    entrada.addEventListener("keydown", (evento) => {
        if (evento.key === "Enter" && (evento.ctrlKey || evento.metaKey)) {
            evento.preventDefault();
            analizar();
        }
    });
}
vincularPosicion(entradaEsquema, "posicion-esquema");
vincularPosicion(entradaConsultas, "posicion-consultas");
