package com.minipc.t1sominipc.controller;

import com.minipc.t1sominipc.model.BCP;
import com.minipc.t1sominipc.model.CPU;
import com.minipc.t1sominipc.model.ConvertidorASM;
import com.minipc.t1sominipc.model.Instruccion;
import com.minipc.t1sominipc.model.Memoria;
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
    private ConvertidorASM parser;
    private MiniPCFrame vista;

    private List<Instruccion> programaActual;
    private boolean procesoAdmitido = false; // true una vez que ya se escribió a RAM

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
        bcp = new BCP(memoria,  1, memoria.getInicioMemoriaUsuario());
        cpu = new CPU(memoria, bcp);
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

            actualizarTablaPrograma();
            bcp.actualizarEstado("Nuevo");
            vista.getLblPID().setText("PID 1");
            vista.getLblEstadoProceso().setText("Preparando memoria para el proceso...");

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
            cpu.cargarPrograma(programaActual);
            bcp.actualizarEstado("Listo");
            //actualizarTablaMemoria();
            procesoAdmitido = true;

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
            vista.getModeloPrograma().setRowCount(0);
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

        boolean continua = cpu.paso(); // internamente ya pone "Ejecutando" antes de correr
        actualizarVista();

        if (!continua) {
            vista.getBtnConfigurarMemoria().setEnabled(true);
            vista.getBtnCargarArchivo().setEnabled(true);
            JOptionPane.showMessageDialog(vista, "Programa terminado",
                    "Ejecución finalizada", JOptionPane.INFORMATION_MESSAGE);
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

        boolean continua = true;
        while (continua) {
            continua = cpu.paso();
        }
        actualizarVista();
        vista.getBtnConfigurarMemoria().setEnabled(true);
        vista.getBtnCargarArchivo().setEnabled(true);
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
        procesoAdmitido = false;

        vista.getModeloPrograma().setRowCount(0);
        vista.getModeloMemoria().setRowCount(0);
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
        int nuevoKernel = (Integer) vista.getSpinnerKernel().getValue();

        if (nuevoKernel >= nuevoTamano) {
            JOptionPane.showMessageDialog(vista,
                    "El espacio de kernel debe ser menor que el tamaño total de RAM",
                    "Configuración inválida", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if(nuevoKernel> (nuevoTamano-16)){
            JOptionPane.showMessageDialog(vista,
                    "El espacio de kernel debe dejar al menos 16 direcciones para el usuario",
                    "Configuración inválida", JOptionPane.ERROR_MESSAGE);
            return;
        }

        inicializarMaquina(nuevoTamano, nuevoKernel);
        programaActual = null;
        procesoAdmitido = false;

        vista.getModeloPrograma().setRowCount(0);
        vista.getModeloMemoria().setRowCount(0);
        vista.getLblPID().setText("PID --");
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
        return procesoAdmitido && !"Terminado".equals(bcp.getEstado());
    }

    /*
        * Nombre: actualizarTablaPrograma
        *Entrada: void
        *Salida: void
        *Descripción: Actualiza la tabla del programa.
     */
    private void actualizarTablaPrograma() {
        DefaultTableModel modelo = vista.getModeloPrograma();
        modelo.setRowCount(0);
        for (Instruccion instr : programaActual) {
            modelo.addRow(new Object[]{instr.getLineaOriginal(), formatearBinario(instr.aBinario())});
        }
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

        if(programaActual != null){ 
            int pos = 0;
            while (pos <= finKernel) {
                String label = memoria.getLabel(pos);

                if (label != null && !label.isEmpty()) {
                    modelo.addRow(new Object[]{String.valueOf(pos), label, memoria.leer(pos)});
                    pos++;
                } else {
                    int inicioLibre = pos;
                    while (pos <= finKernel && (memoria.getLabel(pos) == null || memoria.getLabel(pos).isEmpty())) {
                        pos++;
                    }
                    int finLibre = pos - 1;
                    String rango = (inicioLibre == finLibre)
                            ? String.valueOf(inicioLibre)
                            : inicioLibre + "-" + finLibre;
                    modelo.addRow(new Object[]{rango, "Kernel Libre", "-"});
                }
            }

        }
        

        // ===== Sección USUARIO: instrucciones cargadas =====
        if (programaActual != null) {
            int fin = inicioUsuario + programaActual.size();
            for (int direccion = inicioUsuario; direccion < fin; direccion++) {
                Instruccion instr = memoria.leerInstruccion(direccion);
                String textoInstr = (instr != null) ? instr.getLineaOriginal() : "";
                String valorMemoria = formatoValorMemoria(memoria.leer(direccion));
                modelo.addRow(new Object[]{direccion, textoInstr, valorMemoria});
            }
        }
    }

    /*
        * Nombre: actualizarVista
        *Entrada: void
        *Salida: void
        *Descripción: Actualiza la vista de la interfaz.
     */

    private void actualizarVista() {
        vista.getLblPC().setText(String.valueOf(cpu.getPC()));
        vista.getLblIR().setText(cpu.getIRBinario());
        vista.getLblAC().setText(String.valueOf(cpu.getAC()));
        vista.getLblAX().setText(String.valueOf(cpu.getAX()));
        vista.getLblBX().setText(String.valueOf(cpu.getBX()));
        vista.getLblCX().setText(String.valueOf(cpu.getCX()));
        vista.getLblDX().setText(String.valueOf(cpu.getDX()));
        vista.getLblEstadoProceso().setText(bcp.getEstado());

        actualizarTablaMemoria();
    }

    /*
        * Nombre: formatearBinario
        *Entrada: String binario
        *Salida: String
        *Descripción: Formatea un número binario para su visualización.
     */

    private String formatearBinario(String binario) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < binario.length(); i++) {
            sb.append(binario.charAt(i));
            if ((i == 3 || i == 7) && i != binario.length() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /*
        * Nombre: formatoValorMemoria
        *Entrada: int valor
        *Salida: String
        *Descripción: Formatea un valor de memoria para su visualización.
     */
    private String formatoValorMemoria(int valor) {
        String binario = String.format("%16s", Integer.toBinaryString(valor)).replace(' ', '0');
        return formatearBinario(binario);
    }
}