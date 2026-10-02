package com.minipc.t1sominipc.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Nombre: ListaProcesos
 * Descripción: Solo los procesos que ya tienen BCP. Es dueña de la zona de BCP del kernel
 * (dirección 0 en adelante, en bloques de BCP.getTamanoBCP()) y encadena los BCP entre sí
 * mediante su campo siguienteBCP (dirección en memoria).
 */
public class ListaProcesos {

    private static final int TOPE_PROCESOS = 5;

    private Memoria memoria;
    private List<BCP> procesos;
    private boolean[] espacioOcupado; // un espacio por cada BCP que cabe en el kernel
    private int siguientePID;

    public ListaProcesos(Memoria memoria) {
        this.memoria = memoria;
        this.procesos = new ArrayList<>();
        this.espacioOcupado = new boolean[calcularMaximo(memoria)];
        this.siguientePID = 1;
    }

    // Un cuarto del kernel se reserva para la Lista de Trabajo; el resto se divide en BCP.
    private static int calcularMaximo(Memoria memoria) {
        int kernel = memoria.getFinMemoriaKernel() + 1;
        int paraBCP = kernel - kernel / 4;
        return Math.min(TOPE_PROCESOS, paraBCP / BCP.getTamanoBCP());
    }

    /*
        * Nombre: getMaximo
        * Descripción: Cuántos BCP caben en el kernel configurado (tope duro de 5).
     */
    public int getMaximo() {
        return espacioOcupado.length;
    }

    /*
        * Nombre: getFinZonaBCP
        * Descripción: Última dirección de kernel usada por la zona de BCP.
     */
    public int getFinZonaBCP() {
        return getMaximo() * BCP.getTamanoBCP() - 1;
    }

    /*
        * Nombre: hayEspacioParaBCP
        * Descripción: Paso (a) de la admisión: ¿cabe un BCP más en el kernel?
     */
    public boolean hayEspacioParaBCP() {
        return procesos.size() < getMaximo();
    }

    /*
        * Nombre: crearBCP
        * Descripción: Crea un BCP en el primer espacio libre de kernel, en estado "Nuevo" y sin
        * dirección en RAM todavía (Base = -1). Devuelve null si no cabe.
     */
    public BCP crearBCP(int tamanoPrograma) {
        for (int i = 0; i < espacioOcupado.length; i++) {
            if (!espacioOcupado[i]) {
                espacioOcupado[i] = true;
                BCP bcp = new BCP(memoria, i * BCP.getTamanoBCP(), siguientePID, -1, tamanoPrograma, 0);
                siguientePID++;
                procesos.add(bcp);
                reenlazar();
                return bcp;
            }
        }
        return null;
    }

    /*
        * Nombre: eliminar
        * Descripción: Quita un BCP de la lista y libera su espacio de kernel.
     */
    public void eliminar(BCP bcp) {
        if (procesos.remove(bcp)) {
            espacioOcupado[bcp.getDireccionBase() / BCP.getTamanoBCP()] = false;
            reenlazar();
        }
    }

    // Cada BCP apunta a la dirección del siguiente; el último apunta a -1.
    private void reenlazar() {
        for (int i = 0; i < procesos.size(); i++) {
            int siguiente = -1;
            if (i + 1 < procesos.size()) {
                siguiente = procesos.get(i + 1).getDireccionBase();
            }
            procesos.get(i).setSiguienteBCP(siguiente);
        }
    }

    /*
        * Nombre: getTodos
        * Descripción: Devuelve todos los BCP en la lista, en orden de llegada.
     */
    public List<BCP> getTodos() {
        return procesos;
    }

    public int getCantidad() {
        return procesos.size();
    }

    public boolean estaLlena(int maximoProcesos) {
        if(procesos.size() >= maximoProcesos) {
            return true;
        }
        return false;
    }

}
