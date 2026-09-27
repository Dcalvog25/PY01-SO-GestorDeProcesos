package com.minipc.t1sominipc.model;

public class PilaBCP {

    private static final int tamanoFijo = 5;

    private Memoria memoria;
    private int direccionBase; // dirección física donde inician los 5 espacios
    private int cont;

    public PilaBCP(Memoria memoria, int direccionBase) {
        this.memoria = memoria;
        this.direccionBase = direccionBase;
        this.cont = -1;
    }

    public boolean push(int valor) {
        if (estaLlena()) {
            return false; // desbordamiento
        }
        cont++;
        memoria.escribir(direccionBase + cont, valor, "Pila");
        return true;
    }

    public Integer pop() {
        if (estaVacia()) {
            return null; // pila vacía: 
        }        
        int valor = memoria.leer(direccionBase + cont);
        memoria.escribir(direccionBase + cont, 0, "Pila Libre");
        cont--;
        return valor;
    }

    public boolean estaVacia() {
        if (cont == -1) {
            return true;
        }
        return false;
    }

    public boolean estaLlena() {
        if (cont == tamanoFijo - 1) {
            return true;
        }
        return false;
    }

    public static int getTamanoFijo() {
        return tamanoFijo;
    }

    public void reiniciar() {
        cont = -1;
        for (int i = 0; i < tamanoFijo; i++) {
            memoria.escribir(direccionBase + i, 0, "Pila Libre");
        }
    }
}