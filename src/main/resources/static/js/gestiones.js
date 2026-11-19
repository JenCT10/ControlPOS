const formulario = document.querySelector(".gestion-form");
const { tipo, comercio, reemplazo, material, cantidad } = formulario.elements;
const serie = formulario.querySelector('input[name="serie"]');
const disponibles = formulario.querySelector('select[name="serie"]');
const campoSerie = formulario.querySelector('[data-campo="serie"]');
const campoDisponibles = formulario.querySelector('[data-campo="disponibles"]');
const campoReemplazo = formulario.querySelector('[data-campo="reemplazo"]');
const suministros = formulario.querySelector('[data-campo="suministros"]');
const ayuda = formulario.querySelector("[data-ayuda-serie]");

function actualizarCampos() {
    const salida = ["INSTALACION", "ENCOMIENDA"].includes(tipo.value);
    const cambio = tipo.value === "CAMBIO";
    const mostrarDisponibles = salida && comercio.value !== "";
    campoDisponibles.hidden = !mostrarDisponibles;
    disponibles.disabled = !mostrarDisponibles;
    disponibles.required = tipo.value === "INSTALACION";
    campoSerie.hidden = salida;
    serie.disabled = salida;
    serie.required = false;
    campoReemplazo.hidden = reemplazo.disabled = !cambio;
    reemplazo.required = cambio;
    suministros.hidden = suministros.disabled = !salida;
    cantidad.required = salida && material.value.trim() !== "";
    ayuda.textContent = "En retiros y cambios puedes indicar una serie antigua o dejarla vacía si no es legible.";
}

tipo.addEventListener("change", actualizarCampos);
comercio.addEventListener("change", actualizarCampos);
material.addEventListener("input", actualizarCampos);
actualizarCampos();
