package com.minipc.t1sominipc.model;

/*
 * Nombre: Despachador
 * Descripción: Realiza el cambio de contexto entre procesos. El Planificador
 * decide QUIÉN sigue; el Despachador ejecuta CÓMO se hace el cambio.
 */
public class Dispatcher {

    /*
        * Nombre: cambiarContexto
        * Descripción: Saca al proceso saliente de "Ejecutando" (si aplica) y
        * pone al entrante en la CPU, en estado "Ejecutando".
     */
    public void cambiarContexto(CPU cpu, BCP saliente, BCP entrante) {
        if (saliente != null && "Ejecutando".equals(saliente.getEstado())) {
            saliente.actualizarEstado("Preparado"); // vuelve a la cola, no terminó
        }

        entrante.actualizarEstado("Ejecutando");
        cpu.asignarProceso(entrante);
    }
}