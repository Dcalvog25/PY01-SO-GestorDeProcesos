/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.minipc.t1sominipc.model;

import java.util.List;

/**
 *
 * @author garci
 */

/*
 * Nombre: Memoria
 * Descripción: Clase que representa la memoria del procesador.
 */
public class Memoria {
    
    private int[] memoria;
    private String[] memoriaLabels;
    private int tamanoTotal;
    private int finMemoriaKernel;
    private int inicioMemoriaUsuario;
    private Instruccion[] instrucciones;
    
    /*
        * Nombre: Memoria
        * Descripción: Constructor de la clase Memoria.
        * Entrada: void
        * Salida: void
     */
    public Memoria() {
        
        this(256,64);
    }

    /*
        * Nombre: Memoria
        * Descripción: Constructor de la clase Memoria.
        * Entrada: int tamanoTotal, int tamanoKernel
        * Salida: void
     */
    
    public Memoria(int tamanoTotal, int tamanoKernel) {
        this.tamanoTotal = tamanoTotal;
        this.finMemoriaKernel = tamanoKernel - 1;
        this.inicioMemoriaUsuario = tamanoKernel;
        this.memoria = new int[tamanoTotal];
        this.memoriaLabels = new String[tamanoTotal];
        this.instrucciones = new Instruccion[tamanoTotal];
        
        for (int i = 0; i < tamanoTotal; i++) {
            memoria[i] = 0; 
            memoriaLabels[i] = "";
        }
        
    }

    /*
        * Nombre: getTamanoTotal
        *Entrada: void
        *Salida: int
        *Descripción: Devuelve el tamaño total de la memoria.
     */

    public int getTamanoTotal() {
        return tamanoTotal;
    }

    /*
        * Nombre: getFinMemoriaKernel
        *Entrada: void
        *Salida: int
        *Descripción: Devuelve la posición final de la memoria del kernel.
     */

    public int getFinMemoriaKernel() {
        return finMemoriaKernel;
    }

    /*
        * Nombre: getInicioMemoriaUsuario
        *Entrada: void
        *Salida: int
        *Descripción: Devuelve la posición inicial de la memoria de usuario.
     */

    public int getInicioMemoriaUsuario() {
        return inicioMemoriaUsuario;
    }

    /*
        * Nombre: getMemoria
        *Entrada: void
        *Salida: int[]
        *Descripción: Devuelve el array de la memoria.
     */
    public int[] getMemoria() {
        return memoria;
    }

    /*
        * Nombre: getMemoriaLabels
        *Entrada: void
        *Salida: String[]
        *Descripción: Devuelve el array de labels de la memoria.
     */

    public String[] getMemoriaLabels() {
        return memoriaLabels;
    }


    /*
        * Nombre: direccionValida
        *Entrada: int direccion
        *Salida: boolean
        *Descripción: Verifica si una dirección es válida.
     */
    public boolean direccionValida(int direccion) {
        if (direccion < 0 || direccion >= tamanoTotal) {
            return false;
        }
        return true;
    }

    /*
        * Nombre: direccionValidaUsuario
        *Entrada: int direccion
        *Salida: boolean
        *Descripción: Verifica si una dirección es válida para la memoria de usuario.
     */
    public boolean direccionValidaUsuario(int direccion) {
        if (direccion < inicioMemoriaUsuario || direccion >= tamanoTotal) {
            return false;
        }
        return true;
    }

    /*
        * Nombre: limpiarMemoria
        *Entrada: void
        *Salida: void
        *Descripción: Limpia toda la memoria, estableciendo todos los valores a 0 y labels a vacío.
     */

    public void limpiarMemoria() {
        for (int i = 0; i < tamanoTotal; i++) {
            memoria[i] = 0; 
            memoriaLabels[i] = "";
            instrucciones[i] = null;
        }
    }

    /*
        * Nombre: limpiarMemoriaUsuario
        *Entrada: void
        *Salida: void
        *Descripción: Limpia la memoria de usuario, estableciendo todos los valores a 0 y labels a vacío.
     */
    public void limpiarMemoriaUsuario() {
        for (int i = inicioMemoriaUsuario; i < tamanoTotal; i++) {
            memoria[i] = 0;
            memoriaLabels[i] = "";
            instrucciones[i] = null;
        }
    }

    public int leer(int direccion) {
        if (!direccionValida(direccion)) {
            return 0; // Dirección inválida, devolver 0
        }
        return memoria[direccion];
    }
    
    /*
        * Nombre: escribir
        *Entrada: int direccion, int valor, String label
        *Salida: boolean
        *Descripción: Escribe un valor en una dirección de memoria.
     */
    public boolean escribir(int direccion, int valor, String label) {
        if (!direccionValida(direccion)) {
            return false; // Dirección inválida
        }
        memoria[direccion] = valor;
        memoriaLabels[direccion] = label;
        return true;
    }

    /*
        * Nombre: getLabel
        *Entrada: int direccion
        *Salida: String
        *Descripción: Devuelve el label asociado a una dirección de memoria.
     */
    public String getLabel(int direccion) {
        if (!direccionValida(direccion)) {
            return "";
        }
        return memoriaLabels[direccion];
    }
    
    /*
        * Nombre: cargarPrograma
        *Entrada: List<Instruccion> programa
        *Salida: void
        *Descripción: Carga un programa en la memoria.
     */
    public void cargarPrograma(List<Instruccion> programa) {

        if(programa.size() > (tamanoTotal - inicioMemoriaUsuario)){
            throw new IllegalArgumentException("El programa es demasiado grande para la memoria de usuario.");
        }
        int direccion = inicioMemoriaUsuario;
        for (Instruccion instr : programa) {
            if (!direccionValidaUsuario(direccion)) {
                break;
            }
            int valorBinario = Integer.parseInt(instr.aBinario(), 2);
            escribir(direccion, valorBinario, instr.getLineaOriginal());
            instrucciones[direccion] = instr;
            direccion++;
        }
    }

    /*
        * Nombre: leerInstruccion
        *Entrada: int direccion
        *Salida: Instruccion
        *Descripción: Lee una instrucción de una dirección de memoria.
     */
    public Instruccion leerInstruccion(int direccion) {
        if (!direccionValida(direccion)) {
            return null; // Dirección inválida
        }
        return instrucciones[direccion];
    }
}
