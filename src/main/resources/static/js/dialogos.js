document.addEventListener("click", (event) => {
    const boton = event.target.closest("[data-dialogo]");
    if (!boton) return;

    const siguiente = document.getElementById(boton.dataset.dialogo);
    if (!(siguiente instanceof HTMLDialogElement)) return;

    boton.closest("dialog")?.close();
    siguiente.showModal();
});

const pendiente = document.querySelector("dialog[data-abrir]");
if (pendiente instanceof HTMLDialogElement) {
    pendiente.addEventListener("close", () => {
        history.replaceState(null, "", location.pathname);
    }, { once: true });
    pendiente.showModal();
}
