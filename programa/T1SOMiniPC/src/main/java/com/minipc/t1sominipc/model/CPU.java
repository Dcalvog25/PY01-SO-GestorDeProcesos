package com.minipc.t1sominipc.model;

import java.util.List;

 /* 
    * Nombre: CPU
    * Descripción: Clase que representa la unidad de procesamiento central (CPU) del sistema.
 */

public class CPU {

    private int AX;
    private int BX;
    private int CX;
    private int DX;

    private int PC; // Program Counter
    private int IR; // Instruction Register
    private int AC; // Accumulator

   

    private Memoria memoria;
    private BCP bcp;

    /*
        * Nombre: CPU
        * Descripción: Constructor de la clase CPU.
        * Entrada: Memoria memoria, BCP bcp
        * Salida: void
     */
    public CPU(Memoria memoria, BCP bcp) {
        this.memoria = memoria;
        this.bcp = bcp;
        inicializarRegistros();
    }

    /*
        * Nombre: inicializarRegistros
        * Descripción: Inicializa los registros de la CPU.
        * Entrada: void
        * Salida: void
     */
    private void inicializarRegistros() {
        AX = 0;
        BX = 0;
        CX = 0;
        DX = 0;
        this.PC = memoria.getInicioMemoriaUsuario();
        this.IR = 0;
        this.AC = 0;
    }

    /*
        * Nombre: cargarPrograma
        * Descripción: Carga un programa en la memoria y reinicia los registros.
        * Entrada: List<Instruccion> programa
        * Salida: void
     */
    
    public void cargarPrograma(List<Instruccion> programa) {
        memoria.limpiarMemoriaUsuario();
        memoria.cargarPrograma(programa);
        inicializarRegistros();
        bcp.reiniciar(memoria.getInicioMemoriaUsuario());
    }

    /*
        * Nombre: paso
        * Descripción: Ejecuta un paso de la CPU.
        * Entrada: void
        * Salida: boolean
     */
    public boolean paso() {
        Instruccion actual = memoria.leerInstruccion(PC); 

        if (actual == null) {
            bcp.actualizarEstado("Terminado");
            return false;
        }

        IR = Integer.parseInt(actual.aBinario(), 2);
        ejecutar(actual);
        PC++;
        bcp.actualizarEstado("Ejecutando");
      
        bcp.avanzarContador();
        bcp.actualizarRegistros(PC, AC, AX, BX, CX, DX);
        return true;
    }

    /*
        * Nombre: ejecutar
        * Descripción: Ejecuta una instrucción.
        * Entrada: Instruccion instruccion
        * Salida: void
     */
    public void ejecutar(Instruccion instruccion) {
        String registro = instruccion.getRegistro();
        switch(instruccion.getOperador()) {
            case "LOAD":
                AC = getRegistro(registro);
                break;
            case "STORE":
                setRegistro(registro, AC);
                break;
            case "MOV":
                setRegistro(registro, instruccion.getValorDireccion());
                break;
            case "SUB":
                AC -= getRegistro(registro);
                break;
            case "ADD":
                AC += getRegistro(registro);
                break;
            default:
                throw new IllegalArgumentException("Operador no reconocido: " + instruccion.getOperador());
        }   
    }

    /*
        * Nombre: getRegistro
        * Descripción: Devuelve el valor de un registro.
        * Entrada: String registro
        * Salida: int
     */

    private int getRegistro(String registro) {
        switch(registro) {
            case "AX": return AX;
            case "BX": return BX;
            case "CX": return CX;
            case "DX": return DX;
            default: throw new IllegalArgumentException("Registro no reconocido: " + registro);
        }
    }


    /*
        * Nombre: setRegistro
        * Descripción: Establece el valor de un registro.
        * Entrada: String registro, int valor
        * Salida: void
     */
    private void setRegistro(String registro, int valor) {
        switch(registro) {
            case "AX": 
                AX = valor; 
                break;
            case "BX": 
                BX = valor; 
                break;
            case "CX": 
                CX = valor; 
                break;
            case "DX": 
                DX = valor; 
                break;
            default: throw new IllegalArgumentException("Registro no reconocido: " + registro);
        }
    }

    /*
        * Nombre: getAX
        * Descripción: Devuelve el valor del registro AX.
        * Entrada: void
        * Salida: int
     */
    public int getAX() {
        return AX;
    }

    /*
        * Nombre: getBX
        * Descripción: Devuelve el valor del registro BX.
        * Entrada: void
        * Salida: int
     */
    public int getBX() {
        return BX;
    }

    /*
        * Nombre: getCX
        * Descripción: Devuelve el valor del registro CX.
        * Entrada: void
        * Salida: int
     */
    public int getCX() {
        return CX;
    }

    /*
        * Nombre: getDX
        * Descripción: Devuelve el valor del registro DX.
        * Entrada: void
        * Salida: int
     */
    public int getDX() {
        return DX;
    }

    /*
        * Nombre: getPC
        * Descripción: Devuelve el valor del registro PC.
        * Entrada: void
        * Salida: int
     */
    public int getPC() {
        return PC;
    }

    /*
        * Nombre: getIR
        * Descripción: Devuelve el valor del registro IR.
        * Entrada: void
        * Salida: int
     */

    public int getIR() {
        return IR;
    }

    /*
        * Nombre: getAC
        * Descripción: Devuelve el valor del registro AC.
        * Entrada: void
        * Salida: int
     */
    public int getAC() {
        return AC;
    }

    /*
        * Nombre: getIRBinario
        * Descripción: Devuelve el valor del registro IR en formato binario.
        * Entrada: void
        * Salida: String
     */
    public String getIRBinario() {
        return String.format("%16s", Integer.toBinaryString(IR)).replace(' ', '0');
    }


    
}
