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
     */
    public void agregar(BCP bcp) {
        if (!procesos.isEmpty()) {
            BCP ultimo = procesos.get(procesos.size() - 1);
            ultimo.setSiguienteBCP(bcp.getDireccionBase());
        }
        procesos.add(bcp);
    }

    public void eliminar(BCP bcp) {
        procesos.remove(bcp);
    }

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