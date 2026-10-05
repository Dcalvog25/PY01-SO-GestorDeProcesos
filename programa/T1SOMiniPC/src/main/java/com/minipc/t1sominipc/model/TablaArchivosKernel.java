package com.minipc.t1sominipc.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Nombre: TablaArchivosKernel
 * Descripción: Archivos abiertos de cada proceso. El BCP solo guarda un entero (el ID de su lista,
 * 0 = ninguna); las listas de nombres viven aquí porque la memoria es un int[].
 */
public class TablaArchivosKernel {

    private List<List<String>> listas = new ArrayList<>(); // el ID de una lista es su posición + 1

    public int crearLista() {
        listas.add(new ArrayList<>());
        return listas.size();
    }

    /*
        * Nombre: abrir
        * Descripción: Agrega el archivo al final de la lista; si ya estaba abierto, solo lo mueve
        * al final para que sea el último abierto.
     */
    public void abrir(int id, String nombre) {
        List<String> lista = listas.get(id - 1);
        lista.remove(nombre);
        lista.add(nombre);
    }

    /*
        * Nombre: getAbiertos
        * Descripción: Nombres de los archivos abiertos de la lista con ese ID, del más antiguo al último abierto.
     */
    public List<String> getAbiertos(int id) {
        if (id <= 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(listas.get(id - 1));
    }

    public String ultimoAbierto(int id) {
        if (id <= 0 || listas.get(id - 1).isEmpty()) {
            return null;
        }
        List<String> lista = listas.get(id - 1);
        return lista.get(lista.size() - 1);
    }

    // Un archivo eliminado del disco deja de estar abierto en cualquier proceso.
    public void cerrarEnTodas(String nombre) {
        for (List<String> lista : listas) {
            lista.remove(nombre);
        }
    }

    public void liberar(int id) {
        if (id > 0) {
            listas.get(id - 1).clear();
        }
    }
}
