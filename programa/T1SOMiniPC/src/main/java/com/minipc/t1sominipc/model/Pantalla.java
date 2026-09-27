package com.minipc.t1sominipc.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Nombre: Pantalla
 * Descripción: Simula la pantalla de la mini computadora. Guarda los mensajes
 * que el programa ensamblador imprime con INT 10H.
 */
public class Pantalla {

    private List<String> buffer;

    public Pantalla() {
        buffer = new ArrayList<>();
    }

    public void imprimir(String mensaje) {
        buffer.add(mensaje);
    }

    public List<String> getContenido() {
        return buffer;
    }

    public void limpiar() {
        buffer.clear();
    }
}