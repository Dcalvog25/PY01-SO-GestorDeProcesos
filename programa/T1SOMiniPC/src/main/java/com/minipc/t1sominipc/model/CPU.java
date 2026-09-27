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
    private String IR; // Instruction Register - ahora es texto, ya no hay binario
    private int AC; // Accumulator

    private boolean banderaIgual; // resultado de la última comparación (CMP), la usan JE/JNE

    private Memoria memoria;
    private BCP bcp;

    private String ultimoError; // mensaje de error controlado

    private int programaActualTamano; // tamaño del programa cargado en memoria

    /*
        * Nombre: CPU
        * Descripcion: Constructor de la CPU.
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
        * Descripcion: Inicializa todos los registros de la CPU a sus valores por defecto.
        * Entrada: void
        * Salida: void
     */

    private void inicializarRegistros() {
        AX = 0;
        BX = 0;
        CX = 0;
        DX = 0;
        this.PC = memoria.getInicioMemoriaUsuario();
        this.IR = "";
        this.AC = 0;
        this.banderaIgual = false;
        this.ultimoError = null;
    }

    /*
        * Nombre: cargarPrograma
        * Descripcion: Carga un programa en la memoria y reinicia los registros de la CPU.
        * Entrada: List<Instruccion> programa
        * Salida: void
     */
    public void cargarPrograma(List<Instruccion> programa) {
        memoria.limpiarMemoriaUsuario();
        memoria.cargarPrograma(programa);
        programaActualTamano = programa.size();
        inicializarRegistros();
        bcp.reiniciar(memoria.getInicioMemoriaUsuario());
    }

    /*
        * Nombre: paso
        * Descripcion: Ejecuta un paso de la CPU (una instrucción).
        * Entrada: void
        * Salida: boolean - false si ya no hay más instrucciones o el programa terminó
     */
    public boolean paso() {
        ultimoError = null;

        Instruccion actual = memoria.leerInstruccion(PC);

        if (actual == null) {
            bcp.actualizarEstado("Terminado");
            return false;
        }

        IR = actual.getLineaOriginal();
        bcp.actualizarEstado("Ejecutando");

        boolean salto = ejecutar(actual);

        if (!salto) {
            PC++;
        }

        bcp.avanzarContador();
        bcp.actualizarRegistros(PC, AC, AX, BX, CX, DX);

        if (esFinDePrograma(actual)) {
            bcp.actualizarEstado("Terminado");
            return false;
        }

        return true;
    }

    /*
        * Nombre: esFinDePrograma
        * Descripcion: Verifica si la instrucción actual indica el fin del programa.
        * Entrada: Instruccion instruccion
        * Salida: boolean - true si es el fin del programa, false en caso contrario
     */

    private boolean esFinDePrograma(Instruccion instruccion) {
        if (instruccion.getOperador().equals("INT") && instruccion.getValor() == 0x20) {
            return true;
        }
        return false;
    }

    /*
        * Nombre: ejecutar
        * Descripcion: Ejecuta una instrucción.
        * Entrada: Instruccion instruccion
        * Salida: boolean - true si la instrucción ya movió el PC ella misma (saltos), false si falta el PC++ normal
     */
    public boolean ejecutar(Instruccion instruccion) {
        String operador = instruccion.getOperador();
        String reg1 = instruccion.getReg1();
        String reg2 = instruccion.getReg2();
        Integer valor = instruccion.getValor();

        switch (operador) {
            case "LOAD":
                AC = getRegistro(reg1);
                return false;

            case "STORE":
                setRegistro(reg1, AC);
                return false;

            case "MOV":
                if (reg2 != null) {
                    setRegistro(reg1, getRegistro(reg2));
                } else {
                    setRegistro(reg1, valor);
                }
                return false;

            case "ADD":
                AC += getRegistro(reg1);
                return false;

            case "SUB":
                AC -= getRegistro(reg1);
                return false;

            case "INC":
                if (reg1 == null) {
                    AC++;
                } else {
                    setRegistro(reg1, getRegistro(reg1) + 1);
                }
                return false;

            case "DEC":
                if (reg1 == null) {
                    AC--;
                } else {
                    setRegistro(reg1, getRegistro(reg1) - 1);
                }
                return false;

            case "SWAP":
                int temporal = getRegistro(reg1);
                setRegistro(reg1, getRegistro(reg2));
                setRegistro(reg2, temporal);
                return false;

            case "CMP":
                banderaIgual = (getRegistro(reg1) == getRegistro(reg2));
                return false;

            case "JMP":
                return realizarSalto(valor);

            case "JE":
                if (banderaIgual) {
                    return realizarSalto(valor);
                }
                return false;

            case "JNE":
                if (!banderaIgual) {
                    return realizarSalto(valor);
                }
                return false;

            /* 
            case "PUSH":
                boolean cupoPush = bcp.getPila().push(getRegistro(reg1));
                if (!cupoPush) {
                    ultimoError = "Desbordamiento de pila al hacer PUSH " + reg1;
                }
                return false;

            case "POP":
                Integer valorSacado = bcp.getPila().pop();
                if (valorSacado == null) {
                    ultimoError = "La pila está vacía, no se puede hacer POP";
                } else {
                    setRegistro(reg1, valorSacado);
                }
                return false;

            case "PARAM":
                for (int p : instruccion.getParametros()) {
                    boolean cupoParam = bcp.getPila().push(p);
                    if (!cupoParam) {
                        ultimoError = "Desbordamiento de pila al ejecutar PARAM";
                        break;
                    }
                }
                return false;
            */
            case "INT":
                ejecutarInterrupcion(valor);
                return false;

            default:
                throw new IllegalArgumentException("Operador no reconocido: " + operador);
        }
    }

    /*
        * Nombre: realizarSalto
        * Descripción: Realiza un salto a la dirección especificada si está dentro del rango del programa cargado.
        * Entrada: int direccionDestino
        * Salida: boolean - true si el salto fue exitoso, false si está fuera de rango
    */

    private boolean realizarSalto(int direccionDestino) {
        int inicio = memoria.getInicioMemoriaUsuario();
        int fin = inicio + programaActualTamano - 1; // necesitas guardar el tamaño del programa cargado

        if (direccionDestino < inicio || direccionDestino > fin) {
            ultimoError = "Salto fuera de rango del programa: intentó ir a la posición " + direccionDestino;
            return false; // no saltamos, dejamos que PC++ siga su curso normal
        }

        PC = direccionDestino;
        return true;
    }

    /*
        * Nombre: ejecutarInterrupcion
        * Descripción: Maneja las interrupciones. Por ahora solo INT 20H (fin de programa)
        * está resuelta aquí; el resto (teclado, pantalla, archivos) se conecta en el
        * paso de ManejadorInterrupciones más adelante.
     */
    private void ejecutarInterrupcion(int codigo) {
        if (codigo == 0x20) {
            return; // el fin de programa se detecta en esFinDePrograma()
        }
        ultimoError = "Interrupción INT " + Integer.toHexString(codigo).toUpperCase()
                + "H todavía no está implementada";
    }

    /*
        * Nombre: getRegistro
        * Descripcion: Obtiene el valor de un registro específico.
        * Entrada: String registro
        * Salida: int - valor del registro
     */
    private int getRegistro(String registro) {
        switch (registro) {
            case "AX": return AX;
            case "BX": return BX;
            case "CX": return CX;
            case "DX": return DX;
            default: throw new IllegalArgumentException("Registro no reconocido: " + registro);
        }
    }

    /*
        * Nombre: setRegistro
        * Descripcion: Establece el valor de un registro específico.
        * Entrada: String registro, int valor
        * Salida: void
     */

    private void setRegistro(String registro, int valor) {
        switch (registro) {
            case "AX": AX = valor; break;
            case "BX": BX = valor; break;
            case "CX": CX = valor; break;
            case "DX": DX = valor; break;
            default: throw new IllegalArgumentException("Registro no reconocido: " + registro);
        }
    }

    /*
        * Nombre: getAX, getBX, getCX, getDX, getPC, getIR, getAC, getUltimoError
        * Descripcion: Obtiene el valor de los registros y del último error.
        * Entrada: void
        * Salida: int o String según corresponda
    */

    public int getAX() { 
        return AX; 
    }
    public int getBX() { 
        return BX; 
    }
    public int getCX() { 
        return CX; 
    }
    public int getDX() { 
        return DX; 
    }
    public int getPC() { 
        return PC; 
    }
    public String getIR() { 
        return IR; 
    }
    public int getAC() { 
        return AC; 
    }
    public String getUltimoError() { 
        return ultimoError; 
    }
}