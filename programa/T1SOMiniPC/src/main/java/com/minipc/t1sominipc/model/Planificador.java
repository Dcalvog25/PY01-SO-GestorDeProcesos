package com.minipc.t1sominipc.model;

import java.util.List;

/*
 * Nombre: Planificador
 * Descripción: Planificador de procesos FCFS (First Come, First Served).
 * Admite procesos nuevos, calcula dónde vive cada BCP en el kernel,
 * y decide cuál proceso preparado le toca ejecutar a la CPU.
 */
public class Planificador {

    private Memoria memoria;
    private ListaProcesos listaProcesos;
    private int siguientePID;

    public Planificador(Memoria memoria) {
        this.memoria = memoria;
        this.listaProcesos = new ListaProcesos();
        this.siguientePID = 1;
    }

    /*
        * Nombre: getMaximoProcesos
        * Descripción: Cuántos BCP caben de verdad en el kernel configurado,
        * limitado a 5 según el enunciado, pero nunca más de lo que hay espacio real.
     */
    public int getMaximoProcesos() {
        int espacioKernel = memoria.getFinMemoriaKernel() + 1;
        int caben = espacioKernel / BCP.getTamanoBCP();
        return Math.min(caben, 5);
    }

    /*
        * Nombre: admitirProceso
        * Descripción: Crea el BCP para un programa nuevo y lo agrega a la lista.
        * Devuelve null si ya no hay espacio para más procesos.
     */
    public BCP admitirProceso(List<Instruccion> programa, int baseUsuario) {
        if (listaProcesos.estaLlena(getMaximoProcesos())) {
            return null;
        }

        int direccionBase = listaProcesos.getCantidad() * BCP.getTamanoBCP();
        BCP nuevoBcp = new BCP(memoria, direccionBase, siguientePID, baseUsuario, programa.size(), 0);
        siguientePID++;

        nuevoBcp.actualizarEstado("Preparado");
        listaProcesos.agregar(nuevoBcp);
        return nuevoBcp;
    }

    /*
        * Nombre: siguienteProceso
        * Descripción: FCFS - el primer proceso en estado "Preparado", en orden de llegada.
     */
    public BCP siguienteProceso() {
        for (BCP bcp : listaProcesos.getTodos()) {
            if ("Preparado".equals(bcp.getEstado())) {
                return bcp;
            }
        }
        return null;
    }

    public ListaProcesos getListaProcesos() {
        return listaProcesos;
    }
}