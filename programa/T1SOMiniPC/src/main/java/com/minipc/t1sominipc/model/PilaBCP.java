package com.minipc.t1sominipc.model;

public class PilaBCP {

    private static final int tamanoFijo = 5;

    private Memoria memoria;
    private int direccionBase; // dirección física donde inician los 5 espacios
    private int cont;

    /*
        * Nombre: PilaBCP
        * Descripción: Constructor de la clase PilaBCP. Inicializa la pila en la memoria.
        * Entrada: Memoria memoria, int direccionBase
        * Salida: void
     */

    public PilaBCP(Memoria memoria, int direccionBase) {
        this.memoria = memoria;
        this.direccionBase = direccionBase;
        this.cont = -1;

        for (int i = 0; i < tamanoFijo; i++) {
            memoria.escribir(direccionBase + i, 0, "Pila Libre");
        }
    }

    /*
        * Nombre: push
        * Descripción: Inserta un valor en la pila.
        * Entrada: int valor
        * Salida: boolean - true si la operación fue exitosa, false si la pila está llena.
     */
    public boolean push(int valor) {
        if (estaLlena()) {
            return false; // desbordamiento
        }
        cont++;
        memoria.escribir(direccionBase + cont, valor, "Pila");
        return true;
    }

    /*
        * Nombre: pop
        * Descripción: Elimina y devuelve el valor superior de la pila.
        * Entrada: void
        * Salida: Integer - valor eliminado, o null si la pila está vacía.
     */
    public Integer pop() {
        if (estaVacia()) {
            return null; // pila vacía: 
        }        
        int valor = memoria.leer(direccionBase + cont);
        memoria.escribir(direccionBase + cont, 0, "Pila Libre");
        cont--;
        return valor;
    }

    /*
        * Nombre: estaVacia
        * Descripción: Comprueba si la pila está vacía.
        * Entrada: void
        * Salida: boolean - true si la pila está vacía, false en caso contrario.
     */
    public boolean estaVacia() {
        if (cont == -1) {
            return true;
        }
        return false;
    }

    /*
        * Nombre: estaLlena
        * Descripción: Comprueba si la pila está llena.
        * Entrada: void
        * Salida: boolean - true si la pila está llena, false en caso contrario.
     */
    public boolean estaLlena() {
        if (cont == tamanoFijo - 1) {
            return true;
        }
        return false;
    }

    /*
        * Nombre: getTamanoFijo
        * Descripción: Devuelve el tamaño fijo de la pila.
        * Entrada: void
        * Salida: int - tamaño fijo de la pila.
     */
    public static int getTamanoFijo() {
        return tamanoFijo;
    }

    /*
        * Nombre: reiniciar
        * Descripción: Reinicia la pila, vaciándola y marcando todos los espacios como libres en la memoria.
        * Entrada: void
        * Salida: void
     */
    public void reiniciar() {
        cont = -1;
        for (int i = 0; i < tamanoFijo; i++) {
            memoria.escribir(direccionBase + i, 0, "Pila Libre");
        }
    }
}