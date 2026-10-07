package com.minipc.t1sominipc.model;

import java.lang.reflect.Constructor;
import java.util.List;

/* 
    * Nombre: CPU
    * Descripción: Clase que representa la unidad de procesamiento central (CPU) del sistema.
 */

public class CPU {

    private static final int ID = 1; // única CPU del sistema

    private int AX;
    private int BX;
    private int CX;
    private int DX;

    private int PC; // Program Counter
    private String IR; // Instruction Register - ahora es texto, ya no hay binario
    private int AC; // Accumulator

    private boolean esperandoEntrada;

    private Memoria memoria;
    private BCP bcp;
    private Pantalla pantalla;
    private Disco disco;
    private TablaArchivosKernel tablaArchivos;

    private String ultimoError; // mensaje de error controlado

    private int programaActualTamano; // tamaño del programa cargado en memoria

    private String dxTexto; // el "modo texto" de DX, separado del DX numérico

    private int ticksRestantes; // ticks que faltan para terminar la instrucción en curso (0 = ninguna)
    private int pesoActual;     // peso total de la instrucción en curso
    private long inicioEsperaTeclado; // ms en que empezó el INT 09H actual
    private boolean ejecutandoInterrupcion; // flag que indica si una interrupción (INT 10H, INT 21H) está en progreso

    /*
        * Nombre: CPU
        * Descripcion: Constructor de la CPU.
        * Entrada: Memoria memoria, Pantalla pantalla, Disco disco, TablaArchivosKernel tablaArchivos
        * Salida: void
     */
    public CPU(Memoria memoria, Pantalla pantalla, Disco disco, TablaArchivosKernel tablaArchivos) {
        this.memoria = memoria;
        this.bcp = bcp;
        this.pantalla = pantalla;
        this.disco = disco;
        this.tablaArchivos = tablaArchivos;
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
        this.ultimoError = null;
        this.ejecutandoInterrupcion = false;
    }

    /*
        * Nombre: cargarPrograma
        * Descripcion: Carga un programa en la memoria y reinicia los registros de la CPU.
        * Entrada: List<Instruccion> programa
        * Salida: void
    
    public void cargarPrograma(List<Instruccion> programa) {
        memoria.limpiarMemoriaUsuario();
        memoria.cargarPrograma(programa);
        programaActualTamano = programa.size();
        inicializarRegistros();
        //bcp.reiniciar(memoria.getInicioMemoriaUsuario());
    }

    */ 

    /*
        * Nombre: asignarProceso
        * Descripcion: Asigna un nuevo proceso (BCP) a la CPU, actualizando los registros con los valores del BCP.
        * Entrada: BCP nuevoBcp
        * Salida: void
     */
    public void asignarProceso(BCP nuevoBcp) {
        this.bcp = nuevoBcp;
        nuevoBcp.setCPU(ID);
        this.AX = nuevoBcp.getAX();
        this.BX = nuevoBcp.getBX();
        this.CX = nuevoBcp.getCX();
        this.DX = nuevoBcp.getDX();
        this.AC = nuevoBcp.getAC();
        this.PC = nuevoBcp.getPC();
        this.programaActualTamano = nuevoBcp.getTamano();
        this.IR = "";
        this.ultimoError = null;
        this.ticksRestantes = 0;
        this.pesoActual = 0;
        this.dxTexto = null;
    }

    /*
        * Nombre: tick
        * Descripción: Un segundo de CPU. Una instrucción de peso N consume N ticks y sus efectos
        * se aplican en el último (llama a paso()). Mientras tanto el PC sigue en esa instrucción.
        * Salida: igual que paso().
     */
    public boolean tick() {
        if (esperandoEntrada) {
            return true;
        }

        if (ticksRestantes == 0) {
            Instruccion actual = memoria.leerInstruccion(PC);
            if (actual == null) {
                return paso(); // paso() finaliza el proceso
            }
            ultimoError = null;
            IR = actual.getLineaOriginal();
            pesoActual = actual.getPeso();
            ticksRestantes = pesoActual;

            // El estado se fija desde el primer tick para que se vea durante TODA la instrucción,
            // no solo en el último tick (cuando paso() realmente la ejecuta).
            if (requiereEsperaVisible(actual)) {
                ejecutandoInterrupcion = true;
                bcp.actualizarEstado("EnEspera");
            } else {
                ejecutandoInterrupcion = false;
                bcp.actualizarEstado("Ejecutando");
            }
        }

        ticksRestantes--;
        bcp.registrarTiempoEmpleado(bcp.getTiempoEmpleado() + 1);

        if (ticksRestantes > 0) {
            return true;
        }
        return paso();
    }

    public int getTicksRestantes() {
        return ticksRestantes;
    }

    public int getPesoActual() {
        return pesoActual;
    }

    /*
        * Nombre: paso
        * Descripcion: Ejecuta un paso de la CPU (una instrucción).
        * Entrada: void
        * Salida: boolean - false si ya no hay más instrucciones o el programa terminó
     */
    public boolean paso() {

        if(esperandoEntrada) {
            return true;
        }

        if ("Finalizado".equals(bcp.getEstado())) {
            return false;
        }

        ultimoError = null;

        Instruccion actual = memoria.leerInstruccion(PC);

        if (actual == null) {
            bcp.actualizarEstado("Finalizado");
            return false;
        }

        IR = actual.getLineaOriginal();
        // El estado (Ejecutando / EnEspera) ya lo fijó tick() desde el primer tick de esta instrucción.

        boolean salto = ejecutar(actual);

         if (esperandoEntrada) {
            return true; // la instrucción quedó a medias, esperando INT 09H
        }

        // Si la ejecución generó un error de seguridad (salto fuera de rango, pila overflow, etc),
        // el proceso ya fue marcado como Finalizado en ejecutar(), así que retornamos false
        if ("Finalizado".equals(bcp.getEstado())) {
            return false;
        }

        if (esFinDePrograma(actual)) {
            bcp.actualizarRegistros(PC, AC, AX, BX, CX, DX);
            bcp.actualizarEstado("Finalizado");
            return false;
        }

        // Si una interrupción acaba de completarse, volver a estado "Ejecutando"
        if (ejecutandoInterrupcion) {
            ejecutandoInterrupcion = false;
            bcp.actualizarEstado("Ejecutando");
        }

        if (!salto) {
            PC++;
        }

        // Protección de memoria: si el programa no terminó con INT 20H y su PC se
        // salió del espacio que se le asignó, se finaliza para no invadir memoria de otro proceso.
        int inicioPrograma = bcp.getBase();
        int finPrograma = inicioPrograma + programaActualTamano; // exclusivo
        if (PC < inicioPrograma || PC >= finPrograma) {
            ultimoError = "El proceso no terminó con INT 20H y se salió de su espacio de memoria; "
                    + "se finalizó forzosamente para proteger a los demás procesos.";
            pantalla.imprimir("\n[Segmentation Fault - Memory Protection Violation]");
            bcp.actualizarEstado("Finalizado");
            bcp.actualizarRegistros(PC, AC, AX, BX, CX, DX);
            return false;
        }

        //bcp.avanzarContador();
        bcp.actualizarRegistros(PC, AC, AX, BX, CX, DX);

        /* 
        if (esFinDePrograma(actual)) {
            bcp.actualizarEstado("Finalizado");
            return false;
        }
        */
        return true;
    }

    /*
        * Nombre: recibirEntrada
        * Descripción: El Controller llama esto cuando el usuario presiona Enter
        * en el panel de pantalla, completando la instrucción INT 09H que quedó pausada.
     */
    public void recibirEntrada(int valor) {
        if (!esperandoEntrada) {
            return;
        }
        DX = valor;
        dxTexto = null;
        pantalla.imprimir(String.valueOf(valor));
        // La espera del teclado cuenta como tiempo del proceso hasta el Enter válido
        long esperaMs = System.currentTimeMillis() - inicioEsperaTeclado;
        bcp.registrarTiempoEmpleado(bcp.getTiempoEmpleado() + (int) Math.ceil(esperaMs / 1000.0));
        bcp.actualizarEstado("Ejecutando");
        esperandoEntrada = false;

        PC++;
        //bcp.avanzarContador();
        bcp.actualizarRegistros(PC, AC, AX, BX, CX, DX);
    }

    public boolean isEsperandoEntrada() {
        return esperandoEntrada;
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
        * Nombre: requiereEsperaVisible
        * Descripción: Indica si la instrucción debe mostrar "EnEspera" durante TODOS sus ticks
        * (INT 10H imprimir, INT 21H archivos), desde el primer tick en que se obtiene.
        * Entrada: Instruccion instruccion
        * Salida: boolean
     */
    private boolean requiereEsperaVisible(Instruccion instruccion) {
        if (!"INT".equals(instruccion.getOperador()) || instruccion.getValor() == null) {
            return false;
        }
        int codigo = instruccion.getValor();
        return codigo == 0x10 || codigo == 0x21;
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
                if (instruccion.getValorTexto() != null) {
                    dxTexto = instruccion.getValorTexto(); // MOV DX, "prog1"
                } else if (reg2 != null) {
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
                int diferencia = getRegistro(reg1) - getRegistro(reg2);
                bcp.setBanderas(diferencia == 0);
                return false;

            case "JMP":
                return realizarSalto(PC + valor);

            case "JE":
                if (bcp.isZero()) {
                    return realizarSalto(PC + valor);
                }
                return false;

            case "JNE":
                if (!bcp.isZero()) {
                    return realizarSalto(PC + valor);
                }
                return false;

            
            case "PUSH":
                boolean cupoPush = bcp.getPila().push(getRegistro(reg1));
                if (!cupoPush) {
                    ultimoError = "Desbordamiento de pila al hacer PUSH " + reg1;
                    pantalla.imprimir("\n[Segmentation Fault - Stack Overflow en PUSH]");
                    bcp.actualizarEstado("Finalizado");
                    bcp.actualizarRegistros(PC, AC, AX, BX, CX, DX);
                    return false;
                }
                return false;

            case "POP":
                Integer valorSacado = bcp.getPila().pop();
                if (valorSacado == null) {
                    ultimoError = "La pila está vacía, no se puede hacer POP";
                    pantalla.imprimir("\n[Segmentation Fault - Stack Underflow en POP]");
                    bcp.actualizarEstado("Finalizado");
                    bcp.actualizarRegistros(PC, AC, AX, BX, CX, DX);
                } else {
                    setRegistro(reg1, valorSacado);
                }
                return false;

            case "PARAM":
                for (int p : instruccion.getParametros()) {
                    boolean cupoParam = bcp.getPila().push(p);
                    if (!cupoParam) {
                        ultimoError = "Desbordamiento de pila al ejecutar PARAM";
                        pantalla.imprimir("\n[Segmentation Fault - Stack Overflow en PARAM]");
                        bcp.actualizarEstado("Finalizado");
                        bcp.actualizarRegistros(PC, AC, AX, BX, CX, DX);
                        return false;
                    }
                }
                return false;

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
        * Si el salto está fuera de rango, marca el proceso como Finalizado con un ultimoError explicativo.
        * Entrada: int direccionDestino
        * Salida: boolean - true si el salto fue exitoso, false en caso contrario
    */

    private boolean realizarSalto(int direccionDestino) {
        int inicio = bcp.getBase();
        int fin = inicio + programaActualTamano - 1;

        if (direccionDestino < inicio || direccionDestino > fin) {
            ultimoError = "Salto fuera de rango del programa: intentó ir a la posición " + direccionDestino 
                    + " (rango válido: " + inicio + " a " + fin + ")";
            pantalla.imprimir("\n[Segmentation Fault - Invalid Jump Address]");
            bcp.actualizarEstado("Finalizado");
            bcp.actualizarRegistros(PC, AC, AX, BX, CX, DX);
            return false;
        }

        PC = direccionDestino;
        return true;
    }
    /*
        * Nombre: ejecutarInterrupcion
        * Descripción: Ejecuta las interrupciones del sistema.
        * INT 20H termina el programa (se detecta en esFinDePrograma).
        * INT 10H imprime DX en pantalla (marca breve "EnEspera" durante la operación).
        * INT 09H pausa la ejecución esperando entrada del teclado (queda en "EnEspera").
        * INT 21H maneja archivos (marca breve "EnEspera" durante la operación).
     */
    private void ejecutarInterrupcion(int codigo) {
        if (codigo == 0x20) {
            return;
        }
        if (codigo == 0x10) {
            // Estado "EnEspera" ya se fijó desde el primer tick en tick(); solo imprime.
            pantalla.imprimir(dxTexto != null ? dxTexto : String.valueOf(DX));
            return;
        }
        if (codigo == 0x09) {
            pantalla.imprimir(">> Ingresar valor:");
            bcp.actualizarEstado("EnEspera");
            inicioEsperaTeclado = System.currentTimeMillis();
            esperandoEntrada = true;
            return;
        }
        if (codigo == 0x21) {
            // Estado "EnEspera" ya se fijó desde el primer tick en tick(); solo ejecuta la operación.
            llamadaArchivos();
            return;
        }
        ultimoError = "Interrupción INT " + Integer.toHexString(codigo).toUpperCase() + "H todavía no está implementada";
    }

    /*
        * Nombre: llamadaArchivos
        * Descripción: INT 21H. AH elige la función (3Ch crear, 3Dh abrir, 4Dh leer, 40h escribir,
        * 41h eliminar), el nombre del archivo viene en DX (texto) y el contenido pasa por AL.
        * Los errores quedan en ultimoError.
     */
    private void llamadaArchivos() {
        int funcion = getRegistro("AH");
        switch (funcion) {
            case 0x3C:
                crearArchivo();
                break;
            case 0x3D:
                abrirArchivo();
                break;
            case 0x4D:
                leerArchivo();
                break;
            case 0x40:
                escribirArchivo();
                break;
            case 0x41:
                eliminarArchivo();
                break;
            default:
                ultimoError = "INT 21H: función AH = " + Integer.toHexString(funcion).toUpperCase()
                        + "H no válida (usa 3Ch, 3Dh, 4Dh, 40h o 41h)";
        }
    }

    // Devuelve el nombre guardado en DX, o null (con error) si DX no tiene texto.
    private String nombreEnDX() {
        if (dxTexto == null || dxTexto.trim().isEmpty()) {
            ultimoError = "INT 21H: DX no tiene un nombre de archivo (usa MOV DX, \"archivo.txt\")";
            return null;
        }
        return dxTexto;
    }

    // Devuelve el último archivo abierto del proceso, o null (con error) si no tiene ninguno.
    private String ultimoArchivoAbierto() {
        String nombre = tablaArchivos.ultimoAbierto(bcp.getIdArchivosAbiertos());
        if (nombre == null) {
            ultimoError = "Error de sistema: el proceso no tiene ningún archivo abierto (abre uno con AH = 3Dh)";
        }
        return nombre;
    }

    private void crearArchivo() {
        String nombre = nombreEnDX();
        if (nombre == null) {
            return;
        }
        if (disco.existeArchivo(nombre)) {
            ultimoError = "INT 21H: ya existe el archivo '" + nombre + "'";
        } else if (!disco.crearArchivoDatos(nombre)) {
            ultimoError = "INT 21H: no se pudo crear '" + nombre + "' (disco o índice lleno)";
        }
    }

    private void abrirArchivo() {
        String nombre = nombreEnDX();
        if (nombre == null) {
            return;
        }
        if (!disco.existeArchivo(nombre)) {
            ultimoError = "INT 21H: el archivo '" + nombre + "' no existe en el disco";
            return;
        }
        int id = bcp.getIdArchivosAbiertos();
        if (id == 0) { // primera vez que este proceso abre un archivo
            id = tablaArchivos.crearLista();
            bcp.setIdArchivosAbiertos(id);
        }
        tablaArchivos.abrir(id, nombre);
    }

    private void leerArchivo() {
        String nombre = ultimoArchivoAbierto();
        if (nombre != null) {
            setRegistro("AL", disco.leerDatoArchivo(nombre));
        }
        pantalla.imprimir("Valor leído de AL: " + String.valueOf(getRegistro("AL")));
    }

    private void escribirArchivo() {
        String nombre = ultimoArchivoAbierto();
        if (nombre != null) {
            disco.escribirDatoArchivo(nombre, getRegistro("AL"));
        }
    }

    private void eliminarArchivo() {
        String nombre = nombreEnDX();
        if (nombre == null) {
            return;
        }
        if (!disco.existeArchivo(nombre)) {
            ultimoError = "INT 21H: el archivo '" + nombre + "' no existe en el disco";
        } else if (!disco.esArchivoDatos(nombre)) {
            ultimoError = "INT 21H: '" + nombre + "' es un programa, solo se pueden eliminar archivos de datos";
        } else {
            disco.eliminarArchivo(nombre);
            tablaArchivos.cerrarEnTodas(nombre);
        }
    }

    /*
        * Nombre: getRegistro
        * Descripcion: Obtiene el valor de un registro específico.
        * Entrada: String registro
        * Salida: int - valor del registro
     */
    private int getRegistro(String registro) {
        switch (registro) {
            case "AX": 
                return AX;
            case "BX": 
                return BX;
            case "CX":
                return CX;
            case "DX": 
                return DX;
            case "AH": // AH y AL son las mitades de AX
                return AX / 256;
            case "AL":
                return AX % 256;
            default: 
                throw new IllegalArgumentException("Registro no reconocido: " + registro);
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
                dxTexto = null; // DX guarda un número o un texto, no ambos
                break;
            case "AH":
                AX = (valor * 256) + getRegistro("AL");
                break;
            case "AL":
                AX = (getRegistro("AH") * 256) + (valor % 256);
                break;
            default: 
                throw new IllegalArgumentException("Registro no reconocido: " + registro);
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
    public String getDxTexto() {
        return dxTexto;
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