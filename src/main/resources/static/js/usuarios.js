const buscar = document.getElementById("buscar-usuario");
const cantidad = document.getElementById("cantidad-usuarios");
const filas = Array.from(document.querySelectorAll("[data-cuenta]"));
const anterior = document.getElementById("usuarios-anterior");
const siguiente = document.getElementById("usuarios-siguiente");
let pagina = 1;

function actualizarUsuarios() {
    const consulta = buscar.value.trim().toLocaleLowerCase("es");
    const coincidencias = filas.filter(fila => Array.from(fila.cells).slice(0, 3).some(
        celda => celda.textContent.toLocaleLowerCase("es").includes(consulta)
    ));
    const limite = Number(cantidad.value);
    const paginas = Math.max(1, Math.ceil(coincidencias.length / limite));
    pagina = Math.min(pagina, paginas);
    const inicio = (pagina - 1) * limite;
    filas.forEach(fila => { fila.hidden = true; });
    coincidencias.slice(inicio, inicio + limite).forEach(fila => { fila.hidden = false; });
    document.getElementById("resumen-usuarios").textContent = coincidencias.length
        ? `Mostrando ${inicio + 1} a ${Math.min(inicio + limite, coincidencias.length)} de ${coincidencias.length} usuarios`
        : "No se encontraron usuarios.";
    document.getElementById("pagina-usuarios").textContent = `${pagina} / ${paginas}`;
    anterior.disabled = pagina === 1;
    siguiente.disabled = pagina === paginas;
}

for (const control of [buscar, cantidad]) {
    control.addEventListener("input", () => {
        pagina = 1;
        actualizarUsuarios();
    });
}
anterior.addEventListener("click", () => { pagina--; actualizarUsuarios(); });
siguiente.addEventListener("click", () => { pagina++; actualizarUsuarios(); });
actualizarUsuarios();
