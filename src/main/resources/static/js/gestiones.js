const formulario = document.querySelector(".gestion-form");
const { tipo, serie, reemplazo, material, cantidad } = formulario.elements;
const campoReemplazo = formulario.querySelector('[data-campo="reemplazo"]');
const suministros = formulario.querySelector('[data-campo="suministros"]');
const ayuda = formulario.querySelector("[data-ayuda-serie]");

function actualizarCampos() {
    const salida = ["INSTALACION", "ENCOMIENDA"].includes(tipo.value);
    const cambio = tipo.value === "CAMBIO";
    serie.required = tipo.value === "INSTALACION";
    if (salida) serie.setAttribute("list", "series-disponibles");
    else serie.removeAttribute("list");
    campoReemplazo.hidden = reemplazo.disabled = !cambio;
    reemplazo.required = cambio;
    suministros.hidden = suministros.disabled = !salida;
    cantidad.required = salida && material.value.trim() !== "";
    ayuda.textContent = salida
        ? "Se sugieren equipos disponibles en tu bodega. Puedes separar varias series con comas."
        : "Para un equipo antiguo, escribe su serie o deja el campo vacío si no es legible.";
}

tipo.addEventListener("change", actualizarCampos);
material.addEventListener("input", actualizarCampos);
