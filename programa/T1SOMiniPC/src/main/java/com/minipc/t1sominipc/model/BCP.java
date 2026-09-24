package com.minipc.t1sominipc.model;

/**
 * BCP (Base Control Process) representa el proceso en ejecución.
 */

public class BCP {

    private static final int POS_PID = 0;
    private static final int POS_ESTADO = 1;
    private static final int POS_PC = 2;
    private static final int POS_AC = 3;
    private static final int POS_AX = 4;
    private static final int POS_BX = 5;
    private static final int POS_CX = 6;
    private static final int POS_DX = 7;
    private static final int POS_BASE = 8;
    private static final int POS_CONTADOR = 9;
    private static final int TAM_BCP = 10; 

    private Memoria memoria;

    /*
     * El BCP se almacena en la memoria, en una posición específica.
     * Cada campo del BCP ocupa una posición en la memoria.
     */

    public BCP(Memoria memoria, int pid, int baseUsuario) {
        this.memoria = memoria;

        if(POS_CONTADOR > memoria.getFinMemoriaKernel()) {
            throw new IllegalArgumentException("La memoria es demasiado pequeña para almacenar el BCP.");
        }
        memoria.escribir(POS_PID, pid, "PID");
        memoria.escribir(POS_ESTADO, 0, "Estado"); // 0 = Nuevo
        memoria.escribir(POS_PC, baseUsuario, "PC");
        memoria.escribir(POS_AC, 0, "AC");
        memoria.escribir(POS_AX, 0, "AX");
        memoria.escribir(POS_BX, 0, "BX");
        memoria.escribir(POS_CX, 0, "CX");
        memoria.escribir(POS_DX, 0, "DX");
        memoria.escribir(POS_BASE, baseUsuario, "Base");
        memoria.escribir(POS_CONTADOR, 0, "Contador");
    }

    /*
        * Nombre: actualizarRegistros
        *Entrada: int pc, int ac, int ax, int bx, int cx, int dx
        *Salida: void
        *Descripción: Actualiza los registros del BCP en la memoria con los valores proporcionados.
     */
    public void actualizarRegistros(int pc, int ac, int ax, int bx, int cx, int dx) {
        memoria.escribir(POS_PC, pc, "PC");
        memoria.escribir(POS_AC, ac, "AC");
        memoria.escribir(POS_AX, ax, "AX");
        memoria.escribir(POS_BX, bx, "BX");
        memoria.escribir(POS_CX, cx, "CX");
        memoria.escribir(POS_DX, dx, "DX");
    }


    /*
        * Nombre: actualizarEstado
        *Entrada: String estado
        *Salida: void
        *Descripción: Actualiza el estado del BCP en la memoria con el valor proporcionado.
     */

    public void actualizarEstado(String estado) {
        memoria.escribir(POS_ESTADO, estadoACodigo(estado), "Estado");
    }

    /*
        * Nombre: avanzarContador
        *Entrada: void
        *Salida: void
        *Descripción: Incrementa el contador de instrucciones del BCP en la memoria.
     */
    public void avanzarContador() {
        int actual = memoria.leer(POS_CONTADOR);
        memoria.escribir(POS_CONTADOR, actual + 1, "Contador");
    }

    /*
        * Nombre: getContadorInstrucciones
        *Entrada: void
        *Salida: int
        *Descripción: Devuelve el valor del contador de instrucciones del BCP desde la memoria.
     */
    public int getContadorInstrucciones() {
        return memoria.leer(POS_CONTADOR);
    }

    /*
        * Nombre: getEstado
        *Entrada: void
        *Salida: String
        *Descripción: Devuelve el estado del BCP desde la memoria.
     */
    public String getEstado() {
        return codigoAEstado(memoria.leer(POS_ESTADO));
    }
   
    /*
        * Nombre: reiniciar
        *Entrada: int baseUsuario
        *Salida: void
        *Descripción: Reinicia el BCP en la memoria con los valores iniciales.
     */
    public void reiniciar(int baseUsuario) {
        memoria.escribir(POS_ESTADO, 1, "Estado");   // 1 = Listo directamente
        memoria.escribir(POS_PC, baseUsuario, "PC");
        memoria.escribir(POS_AC, 0, "AC");
        memoria.escribir(POS_AX, 0, "AX");
        memoria.escribir(POS_BX, 0, "BX");
        memoria.escribir(POS_CX, 0, "CX");
        memoria.escribir(POS_DX, 0, "DX");
        memoria.escribir(POS_BASE, baseUsuario, "Base");
        memoria.escribir(POS_CONTADOR, 0, "Contador");
    }

    /*
        * Nombre: estadoACodigo
        *Entrada: String estado
        *Salida: int
        *Descripción: Convierte un estado en su código correspondiente.
     */
    private int estadoACodigo(String estado) {
        switch (estado) {
            case "Nuevo": 
                return 0;
            case "Listo": 
                return 1;
            case "Ejecutando": 
                return 2;
            case "Terminado": 
                return 3;
            default: 
                throw new IllegalArgumentException("Estado no reconocido: " + estado);
        }
    }

    /*
        * Nombre: codigoAEstado
        *Entrada: int codigo
        *Salida: String
        *Descripción: Convierte un código en su estado correspondiente.
     */

    private String codigoAEstado(int codigo) {
        switch (codigo) {
            case 0: 
                return "Nuevo";
            case 1: 
                return "Listo";
            case 2: 
                return "Ejecutando";
            case 3: 
                return "Terminado";
            default: 
                return "Desconocido";
        }
    }
}