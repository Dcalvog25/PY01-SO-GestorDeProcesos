package com.minipc.t1sominipc.model;

import java.time.LocalTime;

/**
 * BCP (Bloque de Control de Proceso). Cada instancia vive en su propio
 * segmento de memoria de kernel, indicado por direccionBase.
 */
public class BCP {

    private static final int PID = 0;
    private static final int ESTADO = 1;
    private static final int PC = 2;
    private static final int AC = 3;
    private static final int AX = 4;
    private static final int BX = 5;
    private static final int CX = 6;
    private static final int DX = 7;
    private static final int BASE = 8;
    private static final int TAMANO = 9;       // 
    private static final int PRIORIDAD = 10;   // 
    private static final int TIEMPO_INICIO = 11;   // 
    private static final int TIEMPO_EMPLEADO = 12; // 
    private static final int ARCHIVOS_ABIERTOS = 13; // ID de su lista en TablaArchivosKernel, 0 = ninguna
    private static final int SIGUIENTE_BCP = 14;   // 
    private static final int BANDERAS = 15;             
    private static final int CPU_ID = 16;          // CPU donde se ejecuta, 0 = ninguna
    private static final int PILA = 17;

    private static final int BANDERA_ZERO = 1;
        

    private static final int TAM_BCP = PILA + PilaBCP.getTamanoFijo(); // 17 + 5 = 22

    private Memoria memoria;
    private int direccionBase;
    private PilaBCP pila;

    public BCP(Memoria memoria, int direccionBase, int pid, int baseUsuario, int tamanoPrograma, int prioridad) {
        this.memoria = memoria;
        this.direccionBase = direccionBase;

        if (direccionBase + TAM_BCP - 1 > memoria.getFinMemoriaKernel()) {
            throw new IllegalArgumentException("El BCP no cabe en el segmento de kernel.");
        }

        memoria.escribir(direccionBase + PID, pid, "PID");
        memoria.escribir(direccionBase + ESTADO, 0, "Estado"); // 0 = Nuevo
        memoria.escribir(direccionBase + PC, baseUsuario, "PC");
        memoria.escribir(direccionBase + AC, 0, "AC");
        memoria.escribir(direccionBase + AX, 0, "AX");
        memoria.escribir(direccionBase + BX, 0, "BX");
        memoria.escribir(direccionBase + CX, 0, "CX");
        memoria.escribir(direccionBase + DX, 0, "DX");
        memoria.escribir(direccionBase + BASE, baseUsuario, "Base");
        memoria.escribir(direccionBase + TAMANO, tamanoPrograma, "Tamano");
        memoria.escribir(direccionBase + PRIORIDAD, prioridad, "Prioridad");
        memoria.escribir(direccionBase + TIEMPO_INICIO, -1, "TiempoInicio");
        memoria.escribir(direccionBase + TIEMPO_EMPLEADO, 0, "TiempoEmpleado");
        memoria.escribir(direccionBase + ARCHIVOS_ABIERTOS, 0, "ArchivosAbiertos");
        memoria.escribir(direccionBase + SIGUIENTE_BCP, -1, "SiguienteBCP");
        memoria.escribir(direccionBase + BANDERAS, 0, "PSW");
        memoria.escribir(direccionBase + CPU_ID, 0, "CPU");

        this.pila = new PilaBCP(memoria, direccionBase + PILA);
    }

    public PilaBCP getPila() {
        return pila;
    }

    /*
        * Nombre: asignarBase
        * Descripción: Se llama cuando el programa entra a RAM (admisión o swap-in).
        * Mientras el proceso no está en RAM, Base y PC valen -1.
     */
    public void asignarBase(int baseUsuario) {
        memoria.escribir(direccionBase + BASE, baseUsuario, "Base");
        memoria.escribir(direccionBase + PC, baseUsuario, "PC");
    }

    public int getDireccionBase() {
        return direccionBase;
    }

    public static int getTamanoBCP() {
        return TAM_BCP;
    }

    public void actualizarRegistros(int pc, int ac, int ax, int bx, int cx, int dx) {
        memoria.escribir(direccionBase + PC, pc, "PC");
        memoria.escribir(direccionBase + AC, ac, "AC");
        memoria.escribir(direccionBase + AX, ax, "AX");
        memoria.escribir(direccionBase + BX, bx, "BX");
        memoria.escribir(direccionBase + CX, cx, "CX");
        memoria.escribir(direccionBase + DX, dx, "DX");
    }

    public int getPC() { 
        return memoria.leer(direccionBase + PC); 
    }
    public int getAC() { 
        return memoria.leer(direccionBase + AC); 
    }
    public int getAX() { 
        return memoria.leer(direccionBase + AX); 
    }
    public int getBX() { 
        return memoria.leer(direccionBase + BX); 
    }
    public int getCX() { 
        return memoria.leer(direccionBase + CX); 
    }
    public int getDX() { 
        return memoria.leer(direccionBase + DX); 
    }
    public int getBase() { 
        return memoria.leer(direccionBase + BASE); 
    }
    public int getTamano() { 
        return memoria.leer(direccionBase + TAMANO); 
    }
    public int getPrioridad() { 
        return memoria.leer(direccionBase + PRIORIDAD); 
    }
    public int getPID() { 
        return memoria.leer(direccionBase + PID); 
    }

    public void actualizarEstado(String estado) {
        memoria.escribir(direccionBase + ESTADO, estadoACodigo(estado), "Estado");
    }

    public String getEstado() {
        return codigoAEstado(memoria.leer(direccionBase + ESTADO));
    }

    /*
        * Nombre: setBanderas
        * Descripción: Guarda el resultado de una comparación (CMP) en el PSW del proceso.
     */
    public void setBanderas(boolean zero) {
        int psw = 0;
        if (zero) {
            psw |= BANDERA_ZERO;
        }
        memoria.escribir(direccionBase + BANDERAS, psw, "PSW");
    }

    public boolean isZero() {
        return (memoria.leer(direccionBase + BANDERAS) & BANDERA_ZERO) != 0;
    }

    public void setCPU(int idCpu) {
        memoria.escribir(direccionBase + CPU_ID, idCpu, "CPU");
    }

    public int getCPU() {
        return memoria.leer(direccionBase + CPU_ID);
    }

    /* 
    public void avanzarContador() {
        int actual = memoria.leer(direccionBase + CONTADOR);
        memoria.escribir(direccionBase + CONTADOR, actual + 1, "Contador");
    }

    public int getContadorInstrucciones() {
        return memoria.leer(direccionBase + CONTADOR);
    }
    */

    public void setSiguienteBCP(int direccion) {
        memoria.escribir(direccionBase + SIGUIENTE_BCP, direccion, "SiguienteBCP");
    }

    public int getSiguienteBCP() {
        return memoria.leer(direccionBase + SIGUIENTE_BCP);
    }

    public static int minutoDelDia() {
        LocalTime ahora = LocalTime.now();
        return ahora.getHour() * 60 + ahora.getMinute();
    }

    public void registrarInicio(int minutoDelDia) {
        memoria.escribir(direccionBase + TIEMPO_INICIO, minutoDelDia, "TiempoInicio");
    }

    public int getIdArchivosAbiertos() {
        return memoria.leer(direccionBase + ARCHIVOS_ABIERTOS);
    }

    public void setIdArchivosAbiertos(int id) {
        memoria.escribir(direccionBase + ARCHIVOS_ABIERTOS, id, "ArchivosAbiertos");
    }

    public void registrarTiempoEmpleado(long tiempoTotal) {
        memoria.escribir(direccionBase + TIEMPO_EMPLEADO, (int) tiempoTotal, "TiempoEmpleado");
    }

    public int getTiempoInicio() { 
        return memoria.leer(direccionBase + TIEMPO_INICIO); 
    }
    public int getTiempoEmpleado() { 
        return memoria.leer(direccionBase + TIEMPO_EMPLEADO); 
    }

    private int estadoACodigo(String estado) {
        switch (estado) {
            case "Nuevo": return 0;
            case "Preparado": return 1;
            case "Ejecutando": return 2;
            case "Suspendido": return 3;
            case "EnEspera": return 4;
            case "Finalizado": return 5;
            default: throw new IllegalArgumentException("Estado no reconocido: " + estado);
        }
    }

    private String codigoAEstado(int codigo) {
        switch (codigo) {
            case 0: return "Nuevo";
            case 1: return "Preparado";
            case 2: return "Ejecutando";
            case 3: return "Suspendido";
            case 4: return "EnEspera";
            case 5: return "Finalizado";
            default: return "Desconocido";
        }
    }
}