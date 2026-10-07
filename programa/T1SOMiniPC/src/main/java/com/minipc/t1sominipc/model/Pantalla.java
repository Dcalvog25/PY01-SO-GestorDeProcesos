package com.minipc.t1sominipc.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Nombre: Pantalla
 * Descripción: Simula la pantalla de la mini computadora. Guarda los mensajes
 * que el programa ensamblador imprime con INT 10H.
 */
public class Pantalla {

    /*
        * Nombre: buffer
        * Descripción: Lista que almacena los mensajes impresos en la pantalla.
     */
    private List<String> buffer;

    /*
        * Nombre: Pantalla
        * Descripción: Constructor de la clase Pantalla. Inicializa el buffer de mensajes.
        * Entrada: void
        * Salida: void
     */
    public Pantalla() {
        buffer = new ArrayList<>();
    }
    
    public void imprimir(String mensaje) {
        buffer.add(mensaje);
    }

    /*
        * Nombre: getContenido
        * Descripción: Devuelve el contenido actual de la pantalla.
        * Entrada: void
        * Salida: List<String> - lista de mensajes en la pantalla.
     */
    public List<String> getContenido() {
        return buffer;
    }
    /*
        * Nombre: limpiar
        * Descripción: Limpia el contenido de la pantalla.
        * Entrada: void
        * Salida: void
     */
    public void limpiar() {
        buffer.clear();
    }
}