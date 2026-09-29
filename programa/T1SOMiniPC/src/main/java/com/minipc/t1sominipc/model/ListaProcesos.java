package com.minipc.t1sominipc.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Nombre: ListaProcesos
 * Descripción: Mantiene los BCP admitidos, encadenados entre sí mediante
 * su campo siguienteBCP (dirección en memoria), tal como pide el enunciado.
 */
public class ListaProcesos {

    private List<BCP> procesos;

    public ListaProcesos() {
        procesos = new ArrayList<>();
    }

    /*
        * Nombre: agregar
        * Descripción: Agrega un BCP a la lista y actualiza el enlace del anterior.
        * Entrada: BCP bcp
        * Salida: void
     */
    public void agregar(BCP bcp) {
        if (!procesos.isEmpty()) {
            BCP ultimo = procesos.get(procesos.size() - 1);
            ultimo.setSiguienteBCP(bcp.getDireccionBase());
        }
        procesos.add(bcp);
    }

    /*
        * Nombre: eliminar
        * Descripción: Elimina un BCP de la lista.
        * Entrada: BCP bcp
        * Salida: void
     */

    public void eliminar(BCP bcp) {
        procesos.remove(bcp);
    }

    /*
        * Nombre: getTodos
        * Descripción: Devuelve todos los BCP en la lista.
        * Entrada: void
        * Salida: List<BCP>
     */
    public List<BCP> getTodos() {
        return procesos;
    }

    /*
        * Nombre: getCantidad
        * Descripción: Devuelve la cantidad de BCP en la lista.
        * Entrada: void
        * Salida: int
     */
    public int getCantidad() {
        return procesos.size();
    }

    /*
        * Nombre: estaLlena
        * Descripción: Indica si la lista ha alcanzado el número máximo de procesos.
        * Entrada: int maximoProcesos
        * Salida: boolean
     */

    public boolean estaLlena(int maximoProcesos) {
        if(procesos.size() >= maximoProcesos) {
            return true;
        }
        return false;
    }

}