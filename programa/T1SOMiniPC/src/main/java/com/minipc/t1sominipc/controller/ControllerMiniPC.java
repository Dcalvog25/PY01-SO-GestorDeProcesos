package com.minipc.t1sominipc.controller;

import com.minipc.t1sominipc.model.BCP;
import com.minipc.t1sominipc.model.CPU;
import com.minipc.t1sominipc.model.ConvertidorASM;
import com.minipc.t1sominipc.model.Disco;
import com.minipc.t1sominipc.model.Dispatcher;
import com.minipc.t1sominipc.model.Instruccion;
import com.minipc.t1sominipc.model.ListaProcesos;
import com.minipc.t1sominipc.model.ListaTrabajo;
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

    // "PID n - archivo" de los procesos ya terminados; su BCP se libera, solo se muestran en la tabla
    private List<String> finalizados = new ArrayList<>();
    private boolean ejecutandoAutomatico = false; // true si "Ejecutar Todo" quedó pausado esperando teclado
    private Timer timerAuto; // un tick de CPU por segundo en modo automático

    /*
        * Nombre: ControllerMiniPC
        *Entrada: MiniPCFrame vista
        *Salida: void
        *Descripción: Constructor del controlador.
     */
    public ControllerMiniPC(MiniPCFrame vista) {
        this.vista = vista;
        this.parser = new ConvertidorASM();
        inicializarMaquina(256, calcularKernel(256));
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
        actualizarVista(); // ya hay trabajos en la Lista de Trabajo: bloquea cargar/configurar
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

        planificador.registrarTrabajo(nombre);
        return null;
    }

    /*
        * Nombre: admitirProcesos
        *Entrada: List<String> nombresArchivos
        *Salida: void
        *Descripción: Simula el tiempo de admisión (preparar memoria) para un lote de archivos
        * ya guardados en Disco y registrados en la Lista de Trabajo, y luego pide su admisión.
     */
    private void admitirProcesos(List<String> nombresArchivos) {
        vista.getBtnPasoAPaso().setEnabled(false);
        vista.getBtnEjecutarTodo().setEnabled(false);
        vista.getLblEstadoProceso().setText("Preparando memoria para " + nombresArchivos.size() + " proceso(s)...");

        Timer timerAdmision = new Timer(1200, e -> completarAdmision());
        timerAdmision.setRepeats(false);
        timerAdmision.start();
    }

    /*
        * Nombre: completarAdmision
        *Entrada: void
        *Salida: void
        *Descripción: Pide al Planificador admitir los trabajos pendientes (dos pasos: BCP en kernel,
        * luego RAM). Si la CPU está libre, despacha al primer proceso "Preparado" y avisa de los
        * trabajos que quedaron esperando.
     */
    private void completarAdmision() {
        planificador.admitirPendientes();

        despacharSiguienteSiCorresponde();

        vista.getBtnPasoAPaso().setEnabled(true);
        vista.getBtnEjecutarTodo().setEnabled(true);

        actualizarVista();

        int sinBCP = planificador.getColaEspera().size();
        int sinRAM = 0;
        for (BCP proceso : planificador.getListaProcesos().getTodos()) {
            if ("EnEspera".equals(proceso.getEstado())) {
                sinRAM++;
            }
        }

        StringBuilder aviso = new StringBuilder();
        int enDisco = planificador.getEsperandoLista().size();
        if (enDisco > 0) {
            aviso.append(enDisco).append(" programa(s) esperan en Disco: la Lista de Trabajo del kernel está llena.\n");
        }
        if (sinBCP > 0) {
            aviso.append(sinBCP).append(" programa(s) siguen en la Lista de Trabajo: no cabe otro BCP en el kernel "
                    + "(máximo ").append(planificador.getMaximoProcesos()).append(" procesos).\n");
        }
        if (sinRAM > 0) {
            aviso.append(sinRAM).append(" proceso(s) tienen BCP pero no caben en RAM: esperan en la memoria virtual del disco.\n");
        }
        if (aviso.length() > 0) {
            aviso.append("Se admitirán automáticamente cuando termine un proceso.");
            JOptionPane.showMessageDialog(vista, aviso.toString(), "En espera", JOptionPane.INFORMATION_MESSAGE);
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
            if (siguiente != null && dispatcher.cambiarContexto(cpu, bcp, siguiente)) {
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
            if (dispatcher.cambiarContexto(cpu, bcp, siguiente)) {
                bcp = siguiente;
            }
            actualizarVista();
            return;
        }

        boolean continua = cpu.tick(); // 1 clic = 1 segundo; una instrucción dura su peso en ticks
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
        *Descripción: Ejecuta en orden FCFS cada proceso admitido hasta su finalización, con un
        * tick por segundo (una instrucción dura su peso en segundos), despachando al siguiente
        * "Preparado" cuando el actual termina.
     */
    private void ejecutarTodo() {
        if (cpu.isEsperandoEntrada()) {
            avisarEsperandoTeclado();
            return;
        }

        if (bcp == null && planificador.getListaTrabajo().getCantidad() == 0) {
            JOptionPane.showMessageDialog(vista, "Primero carga un archivo .asm",
                    "Sin programa", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ejecutandoAutomatico = true;
        vista.getBtnPasoAPaso().setEnabled(false);
        vista.getBtnEjecutarTodo().setEnabled(false);

        timerAuto = new Timer(1000, e -> tickAutomatico());
        timerAuto.start();
    }

    /*
        * Nombre: tickAutomatico
        *Entrada: void
        *Salida: void
        *Descripción: Un segundo del modo automático: despacha al siguiente proceso (ese tick es el
        * cambio de contexto) o avanza un tick de la instrucción en curso.
     */
    private void tickAutomatico() {
        if (bcp == null) {
            BCP siguiente = planificador.siguienteProceso();
            if (siguiente == null || !dispatcher.cambiarContexto(cpu, null, siguiente)) {
                terminarAutomatico();
                return;
            }
            bcp = siguiente;
            actualizarVista();
            return;
        }

        boolean continua = cpu.tick();

        if (cpu.isEsperandoEntrada()) {
            detenerAutomatico();
            actualizarVista();
            avisarEsperandoTeclado();
            return;
        }

        String error = cpu.getUltimoError();
        if (!continua) {
            liberarYRegistrar(bcp);
        }
        actualizarVista();

        boolean fin = !continua && !quedaTrabajoPendiente();
        if (error != null) {
            timerAuto.stop(); // el diálogo modal no debe dejar correr más ticks
            JOptionPane.showMessageDialog(vista, error, "Aviso de ejecución", JOptionPane.WARNING_MESSAGE);
            if (!fin) {
                timerAuto.start();
            }
        }
        if (fin) {
            terminarAutomatico();
        }
    }

    private void detenerAutomatico() {
        if (timerAuto != null) {
            timerAuto.stop();
        }
        vista.getBtnPasoAPaso().setEnabled(true);
        vista.getBtnEjecutarTodo().setEnabled(true);
    }

    private void terminarAutomatico() {
        detenerAutomatico();
        ejecutandoAutomatico = false;
        actualizarVista();
        finalizarEjecucion();
    }

    /*
        * Nombre: liberarYRegistrar
        *Entrada: BCP finalizado
        *Salida: void
        *Descripción: Avisa al Planificador que un proceso terminó: se libera su RAM, su BCP y su
        * entrada de la Lista de Trabajo, y se admite lo que estaba esperando. La CPU queda libre.
     */
    private void liberarYRegistrar(BCP finalizado) {
        ListaTrabajo trabajos = planificador.getListaTrabajo();
        int indice = trabajos.buscarPorPID(finalizado.getPID());
        finalizados.add("PID " + finalizado.getPID() + " - " + trabajos.getNombre(indice));

        planificador.liberarProceso(finalizado);
        bcp = null; // su espacio de BCP pudo reutilizarse de inmediato por otro proceso
    }

    /*
        * Nombre: quedaTrabajoPendiente
        *Entrada: void
        *Salida: boolean
        *Descripción: Indica si aún hay programas en la Lista de Trabajo (sin terminar).
     */
    private boolean quedaTrabajoPendiente() {
        return planificador.getListaTrabajo().getCantidad() > 0 || !planificador.getEsperandoLista().isEmpty();
    }

    // ===================== RESET Y CONFIGURACIÓN =====================

    /*
        * Nombre: limpiarTodo
        *Entrada: void
        *Salida: void
        *Descripción: Limpia toda la máquina y reinicia la interfaz.
     */
    private void limpiarTodo() {
        detenerAutomatico();
        inicializarMaquina(memoria.getTamanoTotal(), memoria.getInicioMemoriaUsuario());
        finalizados = new ArrayList<>();
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
        int nuevoKernel = calcularKernel(nuevoTamano);

        inicializarMaquina(nuevoTamano, nuevoKernel);
        finalizados = new ArrayList<>();
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
        *Descripción: Actualiza la tabla de la memoria: zona de BCP y Lista de Trabajo del kernel,
        * y los programas que están en RAM.
     */
    private void actualizarTablaMemoria() {
        DefaultTableModel modelo = vista.getModeloMemoria();
        modelo.setRowCount(0);

        ListaProcesos procesos = planificador.getListaProcesos();
        ListaTrabajo trabajos = planificador.getListaTrabajo();
        int tamBCP = BCP.getTamanoBCP();
        int tamEntrada = ListaTrabajo.getTamanoEntrada();

        // ===== KERNEL: un espacio por cada BCP que cabe =====
        for (int espacio = 0; espacio < procesos.getMaximo(); espacio++) {
            int inicio = espacio * tamBCP;
            if (buscarBCPEnDireccion(inicio) == null) {
                modelo.addRow(new Object[]{rango(inicio, inicio + tamBCP - 1), "BCP libre"});
                continue;
            }
            for (int pos = inicio; pos < inicio + tamBCP; pos++) {
                modelo.addRow(new Object[]{String.valueOf(pos), memoria.getLabel(pos) + " = " + memoria.leer(pos)});
            }
        }

        // ===== KERNEL: Lista de Trabajo =====
        for (int i = 0; i < trabajos.getCantidad(); i++) {
            int inicio = trabajos.getInicio() + i * tamEntrada;
            for (int pos = inicio; pos < inicio + tamEntrada; pos++) {
                String texto = memoria.getLabel(pos) + " = " + memoria.leer(pos);
                if (pos == inicio) {
                    texto += " (" + trabajos.getNombre(i) + ")";
                }
                modelo.addRow(new Object[]{String.valueOf(pos), texto});
            }
        }
        int inicioLibreTrabajo = trabajos.getInicio() + trabajos.getCantidad() * tamEntrada;
        if (inicioLibreTrabajo <= memoria.getFinMemoriaKernel()) {
            modelo.addRow(new Object[]{rango(inicioLibreTrabajo, memoria.getFinMemoriaKernel()), "Kernel Libre (Lista de Trabajo)"});
        }

        // ===== USUARIO: instrucciones en RAM y huecos libres agrupados =====
        int finTotal = memoria.getTamanoTotal() - 1;
        int posUsuario = memoria.getInicioMemoriaUsuario();
        while (posUsuario <= finTotal) {
            Instruccion instr = memoria.leerInstruccion(posUsuario);
            if (instr != null) {
                BCP dueno = buscarBCPDuenoDeRAM(posUsuario);
                String prefijo = (dueno != null) ? "PID " + dueno.getPID() + ": " : "";
                modelo.addRow(new Object[]{posUsuario, prefijo + instr.getLineaOriginal()});
                posUsuario++;
            } else {
                int inicioLibre = posUsuario;
                while (posUsuario <= finTotal && memoria.leerInstruccion(posUsuario) == null) {
                    posUsuario++;
                }
                modelo.addRow(new Object[]{rango(inicioLibre, posUsuario - 1), "Usuario Libre"});
            }
        }

        // La instrucción en curso del proceso que está en CPU se pinta resaltada
        vista.setDireccionResaltada((bcp != null && bcp.getBase() != -1) ? bcp.getPC() : -1);
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
        for (String terminado : finalizados) {
            modelo.addRow(new Object[]{terminado, "Finalizado"});
        }

        ListaTrabajo trabajos = planificador.getListaTrabajo();
        for (int i = 0; i < trabajos.getCantidad(); i++) {
            int pid = trabajos.getPID(i);
            BCP proceso = buscarBCPPorPID(pid);
            if (proceso == null) {
                modelo.addRow(new Object[]{"-- " + trabajos.getNombre(i), "Sin BCP (espera espacio de BCP)"});
            } else {
                modelo.addRow(new Object[]{"PID " + pid + " - " + trabajos.getNombre(i), proceso.getEstado()});
            }
        }
        for (String enDisco : planificador.getEsperandoLista()) {
            modelo.addRow(new Object[]{"-- " + enDisco, "En Disco (espera lugar en Lista de Trabajo)"});
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
        String estado = (bcp != null) ? bcp.getEstado() : "Esperando archivo";
        if (bcp != null && cpu.getTicksRestantes() > 0) {
            estado += " - tick " + (cpu.getPesoActual() - cpu.getTicksRestantes()) + "/" + cpu.getPesoActual();
        }
        vista.getLblEstadoProceso().setText(estado);

        actualizarTablaMemoria();
        actualizarTablaProcesos();
        actualizarTablaDisco();
        actualizarConsola();
        actualizarBotonesBloqueo();
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

        // Sección 3: memoria virtual (programas que esperan RAM)
        int pos = disco.getInicioMemoriaVirtual();
        int finVirtual = disco.getTamanoTotal() - 1;
        while (pos <= finVirtual) {
            String nombre = disco.leerNombre(pos);
            if (nombre != null) {
                int tamano = Math.max(disco.leerDato(pos), 1);
                for (int j = 0; j < tamano; j++) {
                    modelo.addRow(new Object[]{pos + j,
                            "Virtual " + nombre + ": " + disco.leerInstruccion(pos + j).getLineaOriginal()});
                }
                pos += tamano;
            } else {
                int inicioLibre = pos;
                while (pos <= finVirtual && disco.leerNombre(pos) == null) {
                    pos++;
                }
                modelo.addRow(new Object[]{rango(inicioLibre, pos - 1), "Memoria virtual libre"});
            }
        }
    }

    // El kernel es el 25% de la RAM: ahí caben 5 BCP y la Lista de Trabajo.
    private int calcularKernel(int tamanoRAM) {
        return Math.max((int) Math.round(tamanoRAM * 0.25), 16);
    }

    // Con programas sin terminar no se puede cargar más ni cambiar la memoria.
    private void actualizarBotonesBloqueo() {
        boolean hayTrabajo = quedaTrabajoPendiente();
        vista.getBtnCargarArchivo().setEnabled(!hayTrabajo);
        vista.getBtnConfigurarMemoria().setEnabled(!hayTrabajo);
    }

    private BCP buscarBCPPorPID(int pid) {
        for (BCP proceso : planificador.getListaProcesos().getTodos()) {
            if (proceso.getPID() == pid) {
                return proceso;
            }
        }
        return null;
    }

    private BCP buscarBCPEnDireccion(int direccionKernel) {
        for (BCP proceso : planificador.getListaProcesos().getTodos()) {
            if (proceso.getDireccionBase() == direccionKernel) {
                return proceso;
            }
        }
        return null;
    }

    private BCP buscarBCPDuenoDeRAM(int direccion) {
        for (BCP proceso : planificador.getListaProcesos().getTodos()) {
            if (proceso.getBase() != -1 && direccion >= proceso.getBase()
                    && direccion < proceso.getBase() + proceso.getTamano()) {
                return proceso;
            }
        }
        return null;
    }

    private String rango(int inicio, int fin) {
        if (inicio == fin) {
            return String.valueOf(inicio);
        }
        return inicio + "-" + fin;
    }
}