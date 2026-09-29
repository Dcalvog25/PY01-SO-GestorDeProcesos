package com.minipc.t1sominipc.controller;

import com.minipc.t1sominipc.model.BCP;
import com.minipc.t1sominipc.model.CPU;
import com.minipc.t1sominipc.model.ConvertidorASM;
import com.minipc.t1sominipc.model.Disco;
import com.minipc.t1sominipc.model.Dispatcher;
import com.minipc.t1sominipc.model.Instruccion;
import com.minipc.t1sominipc.model.Memoria;
import com.minipc.t1sominipc.model.Pantalla;
import com.minipc.t1sominipc.model.Planificador;
import com.minipc.t1sominipc.view.MiniPCFrame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador principal de la aplicación MiniPC.
 */
public class ControllerMiniPC {

    private Memoria memoria;
    private BCP bcp; // proceso actualmente asignado a la CPU (puede ser null si no hay ninguno)
    private CPU cpu;
    private Pantalla pantalla;
    private ConvertidorASM parser;
    private MiniPCFrame vista;
    private Disco disco;
    private Planificador planificador;
    private Dispatcher dispatcher;

    // Historial de todos los BCP admitidos (se conservan aun después de "Finalizado" para mostrarlos en las tablas)
    private List<BCP> historialProcesos = new ArrayList<>();
    private boolean ejecutandoAutomatico = false; // true si "Ejecutar Todo" quedó pausado esperando teclado

    /*
        * Nombre: ControllerMiniPC
        *Entrada: MiniPCFrame vista
        *Salida: void
        *Descripción: Constructor del controlador.
     */
    public ControllerMiniPC(MiniPCFrame vista) {
        this.vista = vista;
        this.parser = new ConvertidorASM();
        inicializarMaquina(256, 64);
        registrarEventos();
        actualizarVista();
    }

    /*
        * Nombre: inicializarMaquina
        *Entrada: int tamanoRAM, int tamanoKernel
        *Salida: void
        *Descripción: Inicializa la máquina con los parámetros especificados.
     */
    private void inicializarMaquina(int tamanoRAM, int tamanoKernel) {
        memoria = new Memoria(tamanoRAM, tamanoKernel);
        bcp = null; // sin proceso en CPU hasta que se admita alguno
        pantalla = new Pantalla();
        cpu = new CPU(memoria, pantalla);
        disco = new Disco(512, 64); 
        planificador = new Planificador(memoria,disco);
        dispatcher = new Dispatcher();

        resetEntradaTeclado();
    }

    /*
        * Nombre: registrarEventos
        *Entrada: void
        *Salida: void
        *Descripción: Registra los eventos de la vista.
     */
    private void registrarEventos() {
        vista.getBtnCargarArchivo().addActionListener(e -> cargarArchivo());
        vista.getBtnPasoAPaso().addActionListener(e -> ejecutarPaso());
        vista.getBtnEjecutarTodo().addActionListener(e -> ejecutarTodo());
        vista.getBtnLimpiarReset().addActionListener(e -> limpiarTodo());
        vista.getBtnAplicarConfig().addActionListener(e -> aplicarConfiguracion());
        vista.getTxtEntradaTeclado().addActionListener(e -> procesarEntradaTeclado());
    }

    // ===================== CARGA: VALIDA, GUARDA EN DISCO Y ADMITE (FCFS) =====================

    /*
        * Nombre: cargarArchivo
        *Entrada: void
        *Salida: void
        *Descripción: Permite seleccionar uno o varios archivos .asm, valida cada uno,
        * los guarda en Disco y dispara la admisión de los que sean válidos.
     */
    private void cargarArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ASM", "asm"));
        chooser.setMultiSelectionEnabled(true);
        int resultado = chooser.showOpenDialog(vista);

        if (resultado != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] archivos = chooser.getSelectedFiles();
        List<String> nombresAceptados = new ArrayList<>();
        StringBuilder erroresGlobales = new StringBuilder();

        for (File archivo : archivos) {
            String error = validarYGuardarArchivo(archivo);
            if (error != null) {
                erroresGlobales.append(error).append("\n\n");
            } else {
                nombresAceptados.add(archivo.getName());
            }
        }

        if (erroresGlobales.length() > 0) {
            JOptionPane.showMessageDialog(vista, erroresGlobales.toString().trim(),
                    "Algunos archivos fueron rechazados", JOptionPane.WARNING_MESSAGE);
        }

        if (nombresAceptados.isEmpty()) {
            return;
        }

        actualizarTablaDisco();
        admitirProcesos(nombresAceptados);
    }

    /*
        * Nombre: validarYGuardarArchivo
        *Entrada: File archivo
        *Salida: String - null si se guardó correctamente, o un mensaje de error para mostrar al usuario
        *Descripción: Convierte el .asm a instrucciones, valida sintaxis/tamaño/duplicados y lo guarda en Disco.
     */
    private String validarYGuardarArchivo(File archivo) {
        List<String> lineas;
        try {
            lineas = Files.readAllLines(archivo.toPath());
        } catch (IOException ex) {
            return archivo.getName() + ": no se pudo leer (" + ex.getMessage() + ")";
        }

        List<Instruccion> programa = parser.convertirASM(lineas);

        if (parser.tieneErrores()) {
            return archivo.getName() + ":\n" + String.join("\n", parser.getErrores());
        }

        if (programa.isEmpty()) {
            return archivo.getName() + ": el archivo no contiene ninguna instrucción.";
        }

        int espacioMaximoUsuario = memoria.getTamanoTotal() - memoria.getInicioMemoriaUsuario();
        if (programa.size() > espacioMaximoUsuario) {
            return archivo.getName() + ": tiene " + programa.size() + " instrucciones, pero la memoria de "
                    + "usuario solo tiene " + espacioMaximoUsuario + " posiciones en total.";
        }

        String nombre = archivo.getName();
        if (disco.existeArchivo(nombre)) {
            return nombre + ": ya hay un archivo con ese nombre en el disco.";
        }

        if (!disco.guardarArchivo(nombre, programa)) {
            return nombre + ": no se pudo guardar en disco (sin espacio o índice lleno).";
        }

        return null;
    }

    /*
        * Nombre: admitirProcesos
        *Entrada: List<String> nombresArchivos
        *Salida: void
        *Descripción: Simula el tiempo de admisión (preparar memoria) para un lote de archivos
        * ya guardados en Disco, y luego los entrega al Planificador.
     */
    private void admitirProcesos(List<String> nombresArchivos) {
        vista.getBtnPasoAPaso().setEnabled(false);
        vista.getBtnEjecutarTodo().setEnabled(false);
        vista.getLblEstadoProceso().setText("Preparando memoria para " + nombresArchivos.size() + " proceso(s)...");

        Timer timerAdmision = new Timer(1200, e -> completarAdmision(nombresArchivos));
        timerAdmision.setRepeats(false);
        timerAdmision.start();
    }

    /*
        * Nombre: completarAdmision
        *Entrada: List<String> nombresArchivos
        *Salida: void
        *Descripción: Solicita al Planificador la admisión (FCFS) de cada archivo del lote.
        * Los que no quepan quedan en la cola de espera del Planificador. Si la CPU está libre,
        * despacha de inmediato al primer proceso "Preparado".
     */
    private void completarAdmision(List<String> nombresArchivos) {
        for (String nombre : nombresArchivos) {
            BCP admitido = planificador.solicitarAdmision(nombre);
            if (admitido != null) {
                historialProcesos.add(admitido);
            }
        }

        despacharSiguienteSiCorresponde();

        vista.getBtnPasoAPaso().setEnabled(true);
        vista.getBtnEjecutarTodo().setEnabled(true);

        actualizarVista();

        if (!planificador.getColaEspera().isEmpty()) {
            JOptionPane.showMessageDialog(vista,
                    planificador.getColaEspera().size() + " proceso(s) quedaron en cola de espera "
                    + "por falta de memoria disponible. Se admitirán automáticamente cuando otro proceso termine.",
                    "En espera", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /*
        * Nombre: despacharSiguienteSiCorresponde
        *Entrada: void
        *Salida: void
        *Descripción: Si la CPU no tiene un proceso en ejecución, hace el cambio de contexto
        * hacia el próximo proceso "Preparado" según el Planificador (FCFS).
     */
    private void despacharSiguienteSiCorresponde() {
        if (bcp == null || "Finalizado".equals(bcp.getEstado())) {
            BCP siguiente = planificador.siguienteProceso();
            if (siguiente != null) {
                dispatcher.cambiarContexto(cpu, bcp, siguiente);
                bcp = siguiente;
            }
        }
    }

    

    // ===================== EJECUCIÓN =====================

    /*
        * Nombre: ejecutarPaso
        *Entrada: void
        *Salida: void
        *Descripción: Ejecuta un paso: si la CPU está libre, primero hace el cambio de contexto
        * hacia el siguiente proceso "Preparado" (ese clic no ejecuta instrucción); si ya hay un
        * proceso en ejecución, corre una instrucción de él.
     */
    private void ejecutarPaso() {
        if (cpu.isEsperandoEntrada()) {
            avisarEsperandoTeclado();
            return;
        }

        if (bcp == null || "Finalizado".equals(bcp.getEstado())) {
            BCP siguiente = planificador.siguienteProceso();
            if (siguiente == null) {
                JOptionPane.showMessageDialog(vista, "No hay ningún proceso listo para ejecutar. Carga un archivo .asm",
                        "Sin proceso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            dispatcher.cambiarContexto(cpu, bcp, siguiente);
            bcp = siguiente;
            actualizarVista();
            return;
        }

        boolean continua = cpu.paso(); // internamente ya pone "Ejecutando" antes de correr
        actualizarVista();

        if(cpu.isEsperandoEntrada() ) {
            avisarEsperandoTeclado();
            return;
        }

        if (cpu.getUltimoError() != null) {
            JOptionPane.showMessageDialog(vista, cpu.getUltimoError(),
                    "Aviso de ejecución", JOptionPane.WARNING_MESSAGE);
        }

        if (!continua) {
            liberarYRegistrar(bcp);
            actualizarVista();
            if (!quedaTrabajoPendiente()) {
                finalizarEjecucion();
            }
        }
    }

    /*
        * Nombre: ejecutarTodo
        *Entrada: void
        *Salida: void
        *Descripción: Ejecuta, en orden FCFS, cada proceso admitido hasta su finalización,
        * despachando automáticamente al siguiente "Preparado" cuando el actual termina.
     */
    private void ejecutarTodo() {
        if (cpu.isEsperandoEntrada()) {
            avisarEsperandoTeclado();
            return;
        }

        if (bcp == null && historialProcesos.isEmpty() && planificador.getColaEspera().isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Primero carga un archivo .asm",
                    "Sin programa", JOptionPane.WARNING_MESSAGE);
            return;
        }

        while (true) {
            if (bcp == null || "Finalizado".equals(bcp.getEstado())) {
                BCP siguiente = planificador.siguienteProceso();
                if (siguiente == null) {
                    break; // no quedan procesos listos
                }
                dispatcher.cambiarContexto(cpu, bcp, siguiente);
                bcp = siguiente;
            }

            boolean continua = cpu.paso();

            if (cpu.isEsperandoEntrada()) {
                ejecutandoAutomatico = true;
                actualizarVista();
                avisarEsperandoTeclado();
                return;
            }

            if (!continua) {
                liberarYRegistrar(bcp);
            }
        }
        actualizarVista();
        finalizarEjecucion();
    }

    /*
        * Nombre: liberarYRegistrar
        *Entrada: BCP finalizado
        *Salida: void
        *Descripción: Avisa al Planificador que un proceso terminó (libera su cupo y admite
        * automáticamente al siguiente en cola de espera, si hay). Si se admitió uno nuevo,
        * se agrega al historial para que aparezca en las tablas.
     */
    private void liberarYRegistrar(BCP finalizado) {
        BCP admitidoDeEspera = planificador.liberarProceso(finalizado);
        if (admitidoDeEspera != null) {
            historialProcesos.add(admitidoDeEspera);
        }
    }

    /*
        * Nombre: quedaTrabajoPendiente
        *Entrada: void
        *Salida: boolean
        *Descripción: Indica si aún hay algo por ejecutar (proceso en CPU, listos, o en cola de espera).
     */
    private boolean quedaTrabajoPendiente() {
        if (bcp != null && !"Finalizado".equals(bcp.getEstado())) {
            return true;
        }
        if (planificador.siguienteProceso() != null) {
            return true;
        }
        return !planificador.getColaEspera().isEmpty();
    }

    // ===================== RESET Y CONFIGURACIÓN =====================

    /*
        * Nombre: limpiarTodo
        *Entrada: void
        *Salida: void
        *Descripción: Limpia toda la máquina y reinicia la interfaz.
     */
    private void limpiarTodo() {
        inicializarMaquina(memoria.getTamanoTotal(), memoria.getInicioMemoriaUsuario());
        historialProcesos = new ArrayList<>();
        ejecutandoAutomatico = false;

        vista.getModeloMemoria().setRowCount(0);
        vista.getModeloProcesos().setRowCount(0);
        vista.getLblPID().setText("PID --");

        vista.getBtnCargarArchivo().setEnabled(true);
        vista.getBtnPasoAPaso().setEnabled(true);
        vista.getBtnEjecutarTodo().setEnabled(true);
        vista.getBtnConfigurarMemoria().setEnabled(true);
        vista.getBtnLimpiarReset().setEnabled(true);

        actualizarVista();
        actualizarTablaDisco();
    }

    /*
        * Nombre: aplicarConfiguracion
        *Entrada: void
        *Salida: void
        *Descripción: Aplica la configuración de la memoria.
     */
    private void aplicarConfiguracion() {
        int nuevoTamano = (Integer) vista.getSpinnerTamanoRAM().getValue();
        int nuevoKernel = Math.max((int) Math.round(nuevoTamano * 0.25), 16); // mismo cálculo que usa la vista

        inicializarMaquina(nuevoTamano, nuevoKernel);
        historialProcesos = new ArrayList<>();
        ejecutandoAutomatico = false;

        vista.getModeloMemoria().setRowCount(0);
        vista.getModeloProcesos().setRowCount(0);
        vista.getLblPID().setText("PID --");

        vista.getBtnCargarArchivo().setEnabled(true);
        vista.getBtnPasoAPaso().setEnabled(true);
        vista.getBtnEjecutarTodo().setEnabled(true);
        vista.getBtnConfigurarMemoria().setEnabled(true);
        vista.getBtnLimpiarReset().setEnabled(true);

        actualizarVista();
        actualizarTablaDisco();
    }

    // ===================== EXTRAS =====================

    /*
        * Nombre: actualizarTablaMemoria
        *Entrada: void
        *Salida: void
        *Descripción: Actualiza la tabla de la memoria, mostrando el segmento de cada proceso admitido.
     */
    private void actualizarTablaMemoria() {
        DefaultTableModel modelo = vista.getModeloMemoria();
        modelo.setRowCount(0);

        int finKernel = memoria.getFinMemoriaKernel();
        int inicioUsuario = memoria.getInicioMemoriaUsuario();
        int finTotal = memoria.getTamanoTotal() - 1;

        // ===== Sección KERNEL: atributos de los BCP + huecos agrupados =====
        int pos = 0;
        while (pos <= finKernel) {
            String label = memoria.getLabel(pos);

            if (label != null && !label.isEmpty()) {
                modelo.addRow(new Object[]{String.valueOf(pos), label + " = " + memoria.leer(pos)});
                pos++;
            } else {
                int inicioLibre = pos;
                while (pos <= finKernel && (memoria.getLabel(pos) == null || memoria.getLabel(pos).isEmpty())) {
                    pos++;
                }
                int finLibre = pos - 1;
                modelo.addRow(new Object[]{rango(inicioLibre, finLibre), "Kernel Libre"});
            }
        }

        // ===== Sección USUARIO: instrucciones de cada proceso admitido, en orden de llegada =====
        int posUsuario = inicioUsuario;
        for (BCP proceso : historialProcesos) {
            int base = proceso.getBase();
            int finProceso = base + proceso.getTamano();
            for (int direccion = base; direccion < finProceso; direccion++) {
                Instruccion instr = memoria.leerInstruccion(direccion);
                String textoInstr = (instr != null) ? instr.getLineaOriginal() : "";
                modelo.addRow(new Object[]{direccion, "PID " + proceso.getPID() + ": " + textoInstr});
            }
            posUsuario = finProceso;
        }

        // RESTO USUARIO: espacio libre agrupado en una sola fila
        if (posUsuario <= finTotal) {
            modelo.addRow(new Object[]{rango(posUsuario, finTotal), "Usuario Libre"});
        }
    }

    /*
        * Nombre: actualizarTablaProcesos
        *Entrada: void
        *Salida: void
        *Descripción: Actualiza la tabla de la lista/cola de trabajos: todos los procesos admitidos
        * (con su estado actual) más los que siguen esperando memoria disponible.
     */
    private void actualizarTablaProcesos() {
        DefaultTableModel modelo = vista.getModeloProcesos();
        modelo.setRowCount(0);
        for (BCP proceso : historialProcesos) {
            modelo.addRow(new Object[]{"PID " + proceso.getPID(), proceso.getEstado()});
        }
        for (String nombreEnEspera : planificador.getColaEspera()) {
            modelo.addRow(new Object[]{"--", nombreEnEspera + " (en espera por memoria)"});
        }
    }

    /*
        * Nombre: actualizarConsola
        *Entrada: void
        *Salida: void
        *Descripción: Vuelca el contenido de la Pantalla en el área de consola.
     */
    private void actualizarConsola() {
        List<String> contenido = pantalla.getContenido();
        if (contenido.isEmpty()) {
            vista.getAreaConsola().setText(">> Sistema iniciado correctamente...\n");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String linea : contenido) {
            sb.append(">> ").append(linea).append("\n");
        }
        vista.getAreaConsola().setText(sb.toString());
    }

    /*
        * Nombre: procesarEntradaTeclado
        *Entrada: void
        *Salida: void
        *Descripción: Atiende el Enter en el campo de teclado para completar un INT 09H pendiente.
     */
    private void procesarEntradaTeclado() {
        if (!cpu.isEsperandoEntrada()) {
            return;
        }

        String texto = vista.getTxtEntradaTeclado().getText().trim();
        int valor;
        try {
            valor = Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "Ingresa un valor numérico entre 0 y 255.",
                    "Entrada inválida", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (valor < 0 || valor > 255) {
            JOptionPane.showMessageDialog(vista, "El valor debe estar entre 0 y 255.",
                    "Entrada inválida", JOptionPane.ERROR_MESSAGE);
            return;
        }

        cpu.recibirEntrada(valor);
        resetEntradaTeclado();
        actualizarVista();

        if (ejecutandoAutomatico) {
            ejecutandoAutomatico = false;
            ejecutarTodo();
        }
    }

    /*
        * Nombre: avisarEsperandoTeclado
        *Entrada: void
        *Salida: void
        *Descripción: Habilita el campo de teclado y avisa al usuario que hay un INT 09H pendiente.
     */
    private void avisarEsperandoTeclado() {
        vista.getTxtEntradaTeclado().setEnabled(true);
        vista.getTxtEntradaTeclado().requestFocusInWindow();
        JOptionPane.showMessageDialog(vista,
                "El programa está esperando un valor de teclado (INT 09H).\nIngresa un número entre 0 y 255 y presiona Enter.",
                "Esperando entrada", JOptionPane.WARNING_MESSAGE);
    }

    /*
        * Nombre: resetEntradaTeclado
        *Entrada: void
        *Salida: void
        *Descripción: Limpia y deshabilita el campo de teclado hasta que se necesite de nuevo.
     */
    private void resetEntradaTeclado() {
        vista.getTxtEntradaTeclado().setText("");
        vista.getTxtEntradaTeclado().setEnabled(false);
    }

    /*
        * Nombre: finalizarEjecucion
        *Entrada: void
        *Salida: void
        *Descripción: Restaura los botones cuando el programa terminó (INT 20H).
     */
    private void finalizarEjecucion() {
        vista.getBtnConfigurarMemoria().setEnabled(true);
        vista.getBtnCargarArchivo().setEnabled(true);
        resetEntradaTeclado();
        vista.getBtnPasoAPaso().setEnabled(false);
        vista.getBtnEjecutarTodo().setEnabled(false);
        JOptionPane.showMessageDialog(vista, "Todos los procesos admitidos finalizaron su ejecución",
                "Ejecución finalizada", JOptionPane.INFORMATION_MESSAGE);
    }

    /*
        * Nombre: actualizarVista
        *Entrada: void
        *Salida: void
        *Descripción: Actualiza la vista de la interfaz.
     */

    private void actualizarVista() {
        vista.getLblPC().setText(String.valueOf(cpu.getPC()));
        vista.getLblIR().setText(cpu.getIR());
        vista.getLblAC().setText(String.valueOf(cpu.getAC()));
        vista.getLblAX().setText(String.valueOf(cpu.getAX()));
        vista.getLblBX().setText(String.valueOf(cpu.getBX()));
        vista.getLblCX().setText(String.valueOf(cpu.getCX()));
        vista.getLblDX().setText(String.valueOf(cpu.getDX()));
        vista.getLblPID().setText(bcp != null ? "PID " + bcp.getPID() : "PID --");
        vista.getLblEstadoProceso().setText(bcp != null ? bcp.getEstado() : "Esperando archivo");

        actualizarTablaMemoria();
        actualizarTablaProcesos();
        actualizarConsola();
    }

    private void actualizarTablaDisco() {
        DefaultTableModel modelo = vista.getModeloDisco();
        modelo.setRowCount(0);

        int cantidad = disco.getCantidadArchivos();

        // Sección 1: índice (2 posiciones por archivo)
        for (int i = 0; i < cantidad; i++) {
            int pos = i * 2;
            modelo.addRow(new Object[]{pos + "-" + (pos + 1),
                    "Índice: " + disco.leerNombre(pos) + " -> dir " + disco.leerDato(pos)
                    + ", tam " + disco.leerDato(pos + 1)});
        }
        int inicioLibreIndice = cantidad * 2;
        int finIndice = disco.getInicioZonaProgramas() - 1;
        if (inicioLibreIndice <= finIndice) {
            modelo.addRow(new Object[]{rango(inicioLibreIndice, finIndice), "Índice libre"});
        }

        // Sección 2: programas
        for (int i = 0; i < cantidad; i++) {
            String nombre = disco.leerNombre(i * 2);
            int direccion = disco.leerDato(i * 2);
            int tamano = disco.leerDato(i * 2 + 1);
            for (int j = 0; j < tamano; j++) {
                modelo.addRow(new Object[]{direccion + j,
                        nombre + ": " + disco.leerInstruccion(direccion + j).getLineaOriginal()});
            }
        }
        int inicioLibreProg = disco.getSiguienteDireccionLibre();
        int finProg = disco.getInicioMemoriaVirtual() - 1;
        if (inicioLibreProg <= finProg) {
            modelo.addRow(new Object[]{rango(inicioLibreProg, finProg), "Programas libre"});
        }

        // Sección 3: memoria virtual reservada
        modelo.addRow(new Object[]{rango(disco.getInicioMemoriaVirtual(), disco.getTamanoTotal() - 1),
                "Memoria virtual (reservada)"});
    }

    private String rango(int inicio, int fin) {
        if (inicio == fin) {
            return String.valueOf(inicio);
        }
        return inicio + "-" + fin;
    }
}