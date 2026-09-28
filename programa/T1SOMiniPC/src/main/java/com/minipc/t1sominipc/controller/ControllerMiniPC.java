package com.minipc.t1sominipc.controller;

import com.minipc.t1sominipc.model.BCP;
import com.minipc.t1sominipc.model.CPU;
import com.minipc.t1sominipc.model.ConvertidorASM;
import com.minipc.t1sominipc.model.Disco;
import com.minipc.t1sominipc.model.Instruccion;
import com.minipc.t1sominipc.model.Memoria;
import com.minipc.t1sominipc.model.Pantalla;
import com.minipc.t1sominipc.view.MiniPCFrame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Controlador principal de la aplicación MiniPC.
 */
public class ControllerMiniPC {

    private Memoria memoria;
    private BCP bcp;
    private CPU cpu;
    private Pantalla pantalla;
    private ConvertidorASM parser;
    private MiniPCFrame vista;
    private Disco disco;
    private String nombreProgramaActual;

    private List<Instruccion> programaActual;
    private boolean procesoAdmitido = false; // true una vez que ya se escribió a RAM
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
       // bcp = new BCP(memoria,  1, memoria.getInicioMemoriaUsuario());
        bcp = null; // inicializa bcp como null hasta que se cargue un programa
        pantalla = new Pantalla();
        cpu = new CPU(memoria, pantalla);
        disco = new Disco(512, 64); // por ahora, estos números pasan a un archivo de configuración luego
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

    // ===================== CARGA: SOLO RECONOCE, NO TOCA MEMORIA =====================

    /*
        * Nombre: cargarArchivo
        *Entrada: void
        *Salida: void
        *Descripción: Carga un archivo ASM, lo convierte a instrucciones y maneja errores de validación.
     */
    private void cargarArchivo() {
        if (procesoHayQueResetear()) {
            JOptionPane.showMessageDialog(vista,
                    "Hay un proceso activo. Da clic en 'Limpiar / Reset' antes de cargar otro archivo.",
                    "Proceso en curso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ASM", "asm"));
        int resultado = chooser.showOpenDialog(vista);

        if (resultado != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = chooser.getSelectedFile();

        try {
            List<String> lineas = Files.readAllLines(archivo.toPath());
            programaActual = parser.convertirASM(lineas);

            if (parser.tieneErrores()) {
                String mensaje = "ARCHIVO NO ES PERMITIDO POR ERRORES\n\n"
                        + String.join("\n", parser.getErrores());
                JOptionPane.showMessageDialog(vista, mensaje,
                        "Archivo rechazado", JOptionPane.ERROR_MESSAGE);
                programaActual = null;
                return; // no se carga nada
            }

            if (programaActual.isEmpty()) {
                JOptionPane.showMessageDialog(vista,
                        "El archivo no contiene ninguna instrucción.",
                        "Archivo vacío", JOptionPane.ERROR_MESSAGE);
                programaActual = null;
                return;
            }

            int espacioDisponible = memoria.getTamanoTotal() - memoria.getInicioMemoriaUsuario();
            if (programaActual.size() > espacioDisponible) {
                JOptionPane.showMessageDialog(vista,
                        "El programa tiene " + programaActual.size() + " instrucciones, pero solo hay "
                        + espacioDisponible + " posiciones de memoria de usuario disponibles.",
                        "Programa demasiado grande", JOptionPane.ERROR_MESSAGE);
                programaActual = null;
                return;
            }

            String nombre = archivo.getName();

            if (disco.existeArchivo(nombre)) {
                JOptionPane.showMessageDialog(vista,
                        "Ya hay un archivo llamado '" + nombre + "' en el disco. Usa 'Limpiar / Reset' o cambia el nombre del archivo.",
                        "Archivo repetido", JOptionPane.WARNING_MESSAGE);
                programaActual = null;
                return;
            }

            if (!disco.guardarArchivo(nombre, programaActual)) {
                JOptionPane.showMessageDialog(vista,
                        "No se pudo guardar en disco (sin espacio o índice lleno).",
                        "Disco lleno", JOptionPane.ERROR_MESSAGE);
                programaActual = null;
                return;
            }

            nombreProgramaActual = nombre;
            //bcp.actualizarEstado("Nuevo");
            vista.getLblPID().setText("PID 1");
            vista.getLblEstadoProceso().setText("Preparando memoria para el proceso...");
            actualizarTablaDisco();
            actualizarTablaProcesos();

            deshabilitarTodosLosBotones();

            Timer timerAdmision = new Timer(1200, e -> completarAdmision());
            timerAdmision.setRepeats(false);
            timerAdmision.start();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(vista,
                    "No se pudo leer el archivo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /*
        * Nombre: completarAdmision
        *Entrada: void
        *Salida: void
        *Descripción: Completa el proceso de admisión del programa.
     */
    private void completarAdmision() {
        try {
            List<Instruccion> desdeDisco = disco.leerArchivo(nombreProgramaActual);
            programaActual = desdeDisco;

            bcp = new BCP(memoria, 0, 1, memoria.getInicioMemoriaUsuario(), desdeDisco.size(), 0);
            cpu.asignarProceso(bcp); // en vez de que CPU ya lo tuviera fijo
            cpu.cargarPrograma(desdeDisco);
            bcp.actualizarEstado("Preparado");
            procesoAdmitido = true;
            //actualizarTablaMemoria();


            vista.getBtnPasoAPaso().setEnabled(true);
            vista.getBtnEjecutarTodo().setEnabled(true);
            vista.getBtnLimpiarReset().setEnabled(true);

            actualizarVista();

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(vista,
                    "No se pudo cargar el programa a memoria: " + ex.getMessage(),
                    "Error de admisión", JOptionPane.ERROR_MESSAGE);

            // Revertir todo para que el usuario pueda intentar de nuevo
            programaActual = null;
            vista.getLblPID().setText("PID --");
            vista.getLblEstadoProceso().setText("Esperando archivo");

            vista.getBtnCargarArchivo().setEnabled(true);
            vista.getBtnConfigurarMemoria().setEnabled(true);
            vista.getBtnLimpiarReset().setEnabled(true);
            
        }
    }

    /*
        * Nombre: deshabilitarTodosLosBotones
        *Entrada: void
        *Salida: void
        *Descripción: Deshabilita todos los botones de la interfaz.
     */
    private void deshabilitarTodosLosBotones() {
        vista.getBtnCargarArchivo().setEnabled(false);
        vista.getBtnPasoAPaso().setEnabled(false);
        vista.getBtnEjecutarTodo().setEnabled(false);
        vista.getBtnConfigurarMemoria().setEnabled(false);
        vista.getBtnLimpiarReset().setEnabled(false);
    }

    

    // ===================== EJECUCIÓN =====================

    /*
        * Nombre: ejecutarPaso
        *Entrada: void
        *Salida: void
        *Descripción: Ejecuta un paso del programa.
     */
    private void ejecutarPaso() {
        if (programaActual == null) {
            JOptionPane.showMessageDialog(vista, "Primero carga un archivo .asm",
                    "Sin programa", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (cpu.isEsperandoEntrada()) {
            avisarEsperandoTeclado();
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

        if (!continua && !cpu.isEsperandoEntrada()) {
            finalizarEjecucion();
        }
    }

    /*
        * Nombre: ejecutarTodo
        *Entrada: void
        *Salida: void
        *Descripción: Ejecuta todo el programa.
     */
    private void ejecutarTodo() {
        if (programaActual == null) {
            JOptionPane.showMessageDialog(vista, "Primero carga un archivo .asm",
                    "Sin programa", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (cpu.isEsperandoEntrada()) {
            avisarEsperandoTeclado();
            return;
        }

        boolean continua = true;
        while (continua) {
            continua = cpu.paso();
            if (cpu.isEsperandoEntrada()) {
                ejecutandoAutomatico = true;
                actualizarVista();
                avisarEsperandoTeclado();
                return;
            }
        }
        actualizarVista();
        finalizarEjecucion();
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
        programaActual = null;
        nombreProgramaActual = null;
        procesoAdmitido = false;
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
        programaActual = null;
        nombreProgramaActual = null;
        procesoAdmitido = false;
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
    }

    // ===================== EXTRAS =====================

    /*
        * Nombre: procesoHayQueResetear
        *Entrada: void
        *Salida: boolean
        *Descripción: Verifica si el proceso actual necesita ser reiniciado.
     */
    private boolean procesoHayQueResetear() {
        return procesoAdmitido && bcp != null && !"Finalizado".equals(bcp.getEstado());
    }
    /*
        * Nombre: actualizarTablaMemoria
        *Entrada: void
        *Salida: void
        *Descripción: Actualiza la tabla de la memoria.
     */
    private void actualizarTablaMemoria() {
        DefaultTableModel modelo = vista.getModeloMemoria();
        modelo.setRowCount(0);

        int finKernel = memoria.getFinMemoriaKernel();
        int inicioUsuario = memoria.getInicioMemoriaUsuario();
        int finTotal = memoria.getTamanoTotal() - 1;

        // ===== Sección KERNEL: atributos del BCP + huecos agrupados =====
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
                String rango = (inicioLibre == finLibre) ? String.valueOf(inicioLibre) : inicioLibre + "-" + finLibre;
                modelo.addRow(new Object[]{rango, "Kernel Libre"});
            }
        }

        // ===== Sección USUARIO: instrucciones cargadas =====
        int posUsuario = inicioUsuario;
        if (procesoAdmitido && programaActual != null) {
            int finPrograma = inicioUsuario + programaActual.size();
            for (int direccion = inicioUsuario; direccion < finPrograma; direccion++) {
                Instruccion instr = memoria.leerInstruccion(direccion);
                String textoInstr = (instr != null) ? instr.getLineaOriginal() : "";
                modelo.addRow(new Object[]{direccion, textoInstr});
            }
            posUsuario = finPrograma;
        }

        // RESTO USUARIO: espacio libre agrupado en una sola fila
        if (posUsuario <= finTotal) {
            String rango = (posUsuario == finTotal) ? String.valueOf(posUsuario) : posUsuario + "-" + finTotal;
            modelo.addRow(new Object[]{rango, "Usuario Libre"});
        }
    }

    /*
        * Nombre: actualizarTablaProcesos
        *Entrada: void
        *Salida: void
        *Descripción: Actualiza la tabla de la cola de trabajo (por ahora un único proceso).
     */
    private void actualizarTablaProcesos() {
        DefaultTableModel modelo = vista.getModeloProcesos();
        modelo.setRowCount(0);
        if (programaActual != null && bcp != null) {
            modelo.addRow(new Object[]{"PID 1", bcp.getEstado()});
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
        JOptionPane.showMessageDialog(vista, "Programa terminado",
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