package com.minipc.t1sominipc.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/*
 * Nombre: MiniPCFrame
 * Descripción: Ventana principal de la aplicación Mini PC Simulator.
 * Distribución: 
 * - Izquierda: BCP, Registros CPU y Cola de Procesos (pequeña).
 * - Centro: Memoria RAM (columna completa).
 * - Derecha: Almacenamiento/Disco (columna completa).
 */
public class MiniPCFrame extends JFrame {

    // Paleta de colores - azul oscuro
    private static final Color BG_DARK = new Color(11, 29, 51);
    private static final Color BG_CARD = new Color(16, 42, 71);
    private static final Color BG_BUTTON_PRIMARY = new Color(30, 92, 151);
    private static final Color BG_BUTTON_SECONDARY = new Color(18, 58, 94);
    private static final Color TEXT_LIGHT = new Color(232, 238, 247);
    private static final Color TEXT_MUTED = new Color(143, 166, 196);
    private static final Color ACCENT_GREEN = new Color(111, 207, 151);
    private static final Color ACCENT_RED = new Color(240, 166, 166);
    private static final Color BORDER_COLOR = new Color(30, 63, 95);
    private static final Color BG_RESALTADO = new Color(122, 98, 24);

    private int direccionResaltada = -1; // dir. de RAM de la instrucción en ejecución, -1 si no hay

    // Componentes de Control
    private JButton btnCargarArchivo;
    private JButton btnPasoAPaso;
    private JButton btnEjecutarTodo;
    private JButton btnConfigurarMemoria;
    private JButton btnLimpiarReset;
    private JButton btnEstadisticas;

    // Diálogo de configuración
    private JDialog dialogoConfigMemoria;
    private JSpinner spinnerTamanoRAM;
    private JLabel lblKernelCalculado;
    private JSpinner spinnerTamanoDisco;
    private JLabel lblMemoriaVirtualCalculada;
    private JButton btnAplicarConfig;

    // Tablas y Modelos
    private DefaultTableModel modeloProcesos;
    private DefaultTableModel modeloMemoria;
    private DefaultTableModel modeloDisco;
    private JTable tablaProcesos;
    private JTable tablaMemoria;
    private JTable tablaDisco;

    // Componentes de CPU y BCP
    private JLabel lblPID;
    private JLabel lblEstadoProceso;
    private JLabel lblPC, lblIR, lblAC, lblAX, lblBX, lblCX, lblDX;
    
    // Consola y Teclado
    private JTextArea areaConsola;
    private JTextField txtEntradaTeclado;
    

    public MiniPCFrame() {
        setTitle("Mini PC Simulator - Sistemas Operativos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800); 
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Barra Superior (Botones)
        add(crearBarraSuperior(), BorderLayout.NORTH);

        // 2. Panel Central (Las 3 columnas)
        JPanel panelCentral = new JPanel(new GridLayout(1, 3, 15, 0)); // 15px de separación entre columnas
        panelCentral.setBackground(BG_DARK);
        
        panelCentral.add(crearColumnaIzquierda());
        panelCentral.add(crearColumnaCentral());
        panelCentral.add(crearColumnaDerecha());
        
        add(panelCentral, BorderLayout.CENTER);

        // 3. Panel Inferior (Consola)
        add(crearPanelConsola(), BorderLayout.SOUTH);

        // Inicializar diálogo oculto
        crearDialogoConfigMemoria();
    }

    // ===================== BARRA SUPERIOR =====================
    private JPanel crearBarraSuperior() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(BG_DARK);

        btnCargarArchivo = crearBoton("Cargar Archivos", BG_BUTTON_PRIMARY, TEXT_LIGHT);
        btnEjecutarTodo = crearBoton("Ejecutar Automático", BG_BUTTON_SECONDARY, ACCENT_GREEN);
        btnPasoAPaso = crearBoton("Paso a Paso", BG_BUTTON_SECONDARY, TEXT_LIGHT);
        btnLimpiarReset = crearBoton("Limpiar / Reset", BG_BUTTON_SECONDARY, ACCENT_RED);
        btnEstadisticas = crearBoton("Ver Estadísticas", BG_BUTTON_SECONDARY, TEXT_LIGHT);
        btnConfigurarMemoria = crearBoton("Configurar Hardware", BG_BUTTON_SECONDARY, TEXT_LIGHT);

        panel.add(btnCargarArchivo);
        panel.add(btnEjecutarTodo);
        panel.add(btnPasoAPaso);
        panel.add(btnLimpiarReset);
        panel.add(btnEstadisticas);
        panel.add(btnConfigurarMemoria);

        btnConfigurarMemoria.addActionListener(e -> dialogoConfigMemoria.setVisible(true));

        return panel;
    }

    // ===================== COLUMNA IZQUIERDA (Info y Procesos) =====================
    private JPanel crearColumnaIzquierda() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(BG_DARK);

        // --- Contenedor Superior: BCP + Registros CPU ---
        JPanel panelInfoSuperior = new JPanel(new BorderLayout(0, 15));
        panelInfoSuperior.setBackground(BG_DARK);

        // 1. BCP Actual
        JPanel panelBCP = new JPanel();
        panelBCP.setLayout(new BoxLayout(panelBCP, BoxLayout.Y_AXIS));
        panelBCP.setBackground(BG_CARD);
        panelBCP.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        
        panelBCP.add(crearEtiquetaSeccion("BCP ACTUAL EN CPU"));
        panelBCP.add(Box.createVerticalStrut(10));
        
        lblPID = new JLabel("ID Proceso: --");
        lblPID.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblPID.setForeground(TEXT_LIGHT);
        
        lblEstadoProceso = new JLabel("Estado: Esperando...");
        lblEstadoProceso.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblEstadoProceso.setForeground(TEXT_MUTED);
        
        panelBCP.add(lblPID);
        panelBCP.add(Box.createVerticalStrut(6));
        panelBCP.add(lblEstadoProceso);
        
        // 2. Registros CPU (Movidos aquí)
        JPanel panelRegistros = new JPanel(new BorderLayout(0, 8));
        panelRegistros.setBackground(BG_DARK);
        panelRegistros.add(crearEtiquetaSeccion("REGISTROS CPU"), BorderLayout.NORTH);
        
        JPanel gridRegistros = new JPanel(new GridLayout(2, 4, 8, 8));
        gridRegistros.setBackground(BG_DARK);
        
        lblPC = crearValorRegistro(14); lblIR = crearValorRegistro(11);
        lblAC = crearValorRegistro(14); lblAX = crearValorRegistro(14);
        lblBX = crearValorRegistro(14); lblCX = crearValorRegistro(14);
        lblDX = crearValorRegistro(14);
        
        gridRegistros.add(crearTarjetaRegistro("PC", lblPC));
        gridRegistros.add(crearTarjetaRegistro("IR", lblIR));
        gridRegistros.add(crearTarjetaRegistro("AC", lblAC));
        gridRegistros.add(crearTarjetaRegistro("AX", lblAX));
        gridRegistros.add(crearTarjetaRegistro("BX", lblBX));
        gridRegistros.add(crearTarjetaRegistro("CX", lblCX));
        gridRegistros.add(crearTarjetaRegistro("DX", lblDX));
        
        panelRegistros.add(gridRegistros, BorderLayout.CENTER);

        // Agrupar BCP y Registros
        panelInfoSuperior.add(panelBCP, BorderLayout.NORTH);
        panelInfoSuperior.add(panelRegistros, BorderLayout.CENTER);

        // --- Contenedor Inferior: Tabla de Procesos ---
        JPanel panelProcesos = new JPanel(new BorderLayout(0, 8));
        panelProcesos.setBackground(BG_DARK);
        panelProcesos.add(crearEtiquetaSeccion("COLA DE TRABAJO / PROCESOS"), BorderLayout.NORTH);
        
        modeloProcesos = new DefaultTableModel(new Object[]{"ID Proceso", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaProcesos = crearTablaEstilizada(modeloProcesos);
        JScrollPane scrollProcesos = new JScrollPane(tablaProcesos);
        scrollProcesos.getViewport().setBackground(BG_CARD);
        scrollProcesos.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        // Forzar scrollbar para mantener diseño consistente
        scrollProcesos.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        panelProcesos.add(scrollProcesos, BorderLayout.CENTER);

        // Ensamblar la columna izquierda
        panel.add(panelInfoSuperior, BorderLayout.NORTH);
        panel.add(panelProcesos, BorderLayout.CENTER); // Toma el espacio restante (más pequeña)

        return panel;
    }

    // ===================== COLUMNA CENTRAL (RAM) =====================
    private JPanel crearColumnaCentral() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG_DARK);

        panel.add(crearEtiquetaSeccion("MEMORIA PRINCIPAL (RAM)"), BorderLayout.NORTH);
        
        modeloMemoria = new DefaultTableModel(new Object[]{"Pos", "Valor en Memoria"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaMemoria = crearTablaEstilizada(modeloMemoria);
        // Las filas de RAM tienen la posición como Integer; las de kernel como String
        tablaMemoria.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                    boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, sel, foco, fila, columna);
                Object pos = t.getValueAt(fila, 0);
                boolean resaltada = pos instanceof Integer && (Integer) pos == direccionResaltada;
                c.setBackground(resaltada ? BG_RESALTADO : BG_CARD);
                c.setForeground(TEXT_LIGHT);
                return c;
            }
        });
        JScrollPane scrollMemoria = new JScrollPane(tablaMemoria);
        scrollMemoria.getViewport().setBackground(BG_CARD);
        scrollMemoria.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollMemoria.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollMemoria, BorderLayout.CENTER);

        return panel;
    }

    // ===================== COLUMNA DERECHA (Disco) =====================
    private JPanel crearColumnaDerecha() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG_DARK);

        panel.add(crearEtiquetaSeccion("ALMACENAMIENTO (DISCO / VIRTUAL)"), BorderLayout.NORTH);
        
        modeloDisco = new DefaultTableModel(new Object[]{"Pos", "Valor en Disco"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaDisco = crearTablaEstilizada(modeloDisco);
        JScrollPane scrollDisco = new JScrollPane(tablaDisco);
        scrollDisco.getViewport().setBackground(BG_CARD);
        scrollDisco.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollDisco.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollDisco, BorderLayout.CENTER);

        return panel;
    }

    // ===================== PANEL INFERIOR (Consola) =====================
    private JPanel crearPanelConsola() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(BG_DARK);
        panel.setPreferredSize(new Dimension(0, 160));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        panel.add(crearEtiquetaSeccion("PANTALLA / CONSOLA (INT 10H / INT 09H)"), BorderLayout.NORTH);

        areaConsola = new JTextArea();
        areaConsola.setEditable(false);
        areaConsola.setBackground(Color.BLACK);
        areaConsola.setForeground(ACCENT_GREEN);
        areaConsola.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaConsola.setText(">> Sistema iniciado correctamente...\n");
        
        JScrollPane scrollConsola = new JScrollPane(areaConsola);
        scrollConsola.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        panel.add(scrollConsola, BorderLayout.CENTER);

        // Entrada de teclado
        JPanel panelEntrada = new JPanel(new BorderLayout(10, 0));
        panelEntrada.setBackground(BG_DARK);
        panelEntrada.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        JLabel lblEntrada = new JLabel(">> Ingresar valor (Teclado): ");
        lblEntrada.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblEntrada.setForeground(TEXT_LIGHT);
        
        txtEntradaTeclado = new JTextField();
        txtEntradaTeclado.setBackground(BG_CARD);
        txtEntradaTeclado.setForeground(TEXT_LIGHT);
        txtEntradaTeclado.setCaretColor(TEXT_LIGHT);
        txtEntradaTeclado.setFont(new Font("Monospaced", Font.BOLD, 14));
        txtEntradaTeclado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        
        panelEntrada.add(lblEntrada, BorderLayout.WEST);
        panelEntrada.add(txtEntradaTeclado, BorderLayout.CENTER);

        panel.add(panelEntrada, BorderLayout.SOUTH);

        return panel;
    }

    // ===================== VENTANA EMERGENTE DE CONFIGURACIÓN =====================
    private void crearDialogoConfigMemoria() {
        dialogoConfigMemoria = new JDialog(this, "Configurar Sistema", true);
        dialogoConfigMemoria.setSize(380, 420);
        dialogoConfigMemoria.setLocationRelativeTo(this);
        dialogoConfigMemoria.getContentPane().setBackground(BG_CARD);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titulo = new JLabel("Configuración de RAM y Disco");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 14));
        titulo.setForeground(TEXT_LIGHT);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTamano = crearEtiquetaCampo("Tamaño total de RAM");
        lblTamano.setAlignmentX(Component.LEFT_ALIGNMENT);
        spinnerTamanoRAM = new JSpinner(new SpinnerNumberModel(256, 256, 10000, 8));
        spinnerTamanoRAM.setAlignmentX(Component.LEFT_ALIGNMENT);
        spinnerTamanoRAM.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblKernelInfo = crearEtiquetaCampo("Espacio de kernel (automático)");
        lblKernelInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblKernelCalculado = new JLabel("--");
        lblKernelCalculado.setForeground(ACCENT_GREEN);
        lblKernelCalculado.setFont(new Font("Monospaced", Font.BOLD, 13));
        lblKernelCalculado.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDisco = crearEtiquetaCampo("Tamaño total del disco");
        lblDisco.setAlignmentX(Component.LEFT_ALIGNMENT);
        spinnerTamanoDisco = new JSpinner(new SpinnerNumberModel(512, 512, 100000, 64));
        spinnerTamanoDisco.setAlignmentX(Component.LEFT_ALIGNMENT);
        spinnerTamanoDisco.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblVirtualInfo = crearEtiquetaCampo("Memoria virtual (automática)");
        lblVirtualInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblMemoriaVirtualCalculada = new JLabel("--");
        lblMemoriaVirtualCalculada.setForeground(ACCENT_GREEN);
        lblMemoriaVirtualCalculada.setFont(new Font("Monospaced", Font.BOLD, 13));
        lblMemoriaVirtualCalculada.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnAplicarConfig = crearBoton("Aplicar configuración", BG_BUTTON_PRIMARY, TEXT_LIGHT);
        btnAplicarConfig.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnAplicarConfig.addActionListener(e -> dialogoConfigMemoria.setVisible(false));

        panel.add(titulo);
        panel.add(Box.createVerticalStrut(16));
        panel.add(lblTamano);
        panel.add(spinnerTamanoRAM);
        panel.add(Box.createVerticalStrut(12));
        panel.add(lblKernelInfo);
        panel.add(lblKernelCalculado);
        panel.add(Box.createVerticalStrut(16));
        panel.add(lblDisco);
        panel.add(spinnerTamanoDisco);
        panel.add(Box.createVerticalStrut(12));
        panel.add(lblVirtualInfo);
        panel.add(lblMemoriaVirtualCalculada);
        panel.add(Box.createVerticalStrut(18));
        panel.add(btnAplicarConfig);

        dialogoConfigMemoria.add(panel);
    }

    // ===================== HELPERS DE ESTILO =====================
    private JTable crearTablaEstilizada(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setBackground(BG_CARD);
        tabla.setForeground(TEXT_LIGHT);
        tabla.setGridColor(BORDER_COLOR);
        tabla.setRowHeight(24);
        tabla.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tabla.setSelectionBackground(BG_BUTTON_PRIMARY);
        tabla.setSelectionForeground(TEXT_LIGHT);
        tabla.getTableHeader().setBackground(BG_DARK);
        tabla.getTableHeader().setForeground(TEXT_MUTED);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tabla.setFillsViewportHeight(true);
        return tabla;
    }

    private JPanel crearTarjetaRegistro(String nombre, JLabel valor) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JLabel lblNombre = new JLabel(nombre);
        lblNombre.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblNombre.setForeground(TEXT_MUTED);

        panel.add(lblNombre);
        panel.add(valor);
        return panel;
    }

    private JLabel crearValorRegistro(int tamanoFuente) {
        JLabel lbl = new JLabel("0");
        lbl.setFont(new Font("Monospaced", Font.BOLD, tamanoFuente));
        lbl.setForeground(ACCENT_GREEN);
        return lbl;
    }

    private JLabel crearEtiquetaSeccion(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel crearEtiquetaCampo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }

    private JButton crearBoton(String texto, Color fondo, Color textoColor) {
        JButton btn = new JButton(texto);
        btn.setBackground(fondo);
        btn.setForeground(textoColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        return btn;
    }

    // ===================== GETTERS PARA EL CONTROLLER =====================
    public void setDireccionResaltada(int direccion) {
        direccionResaltada = direccion;
        for (int fila = 0; fila < modeloMemoria.getRowCount(); fila++) {
            Object pos = modeloMemoria.getValueAt(fila, 0);
            if (pos instanceof Integer && (Integer) pos == direccion) {
                tablaMemoria.scrollRectToVisible(tablaMemoria.getCellRect(fila, 0, true));
                break;
            }
        }
        tablaMemoria.repaint();
    }
    public JButton getBtnCargarArchivo() { 
        return btnCargarArchivo; 
    }
    public JButton getBtnPasoAPaso() { 
        return btnPasoAPaso; 
    }
    public JButton getBtnEjecutarTodo() { 
        return btnEjecutarTodo; 
    }
    public JButton getBtnLimpiarReset() { 
        return btnLimpiarReset; 
    }
    public JButton getBtnEstadisticas() { 
        return btnEstadisticas; 
    }
    public JButton getBtnConfigurarMemoria() { 
        return btnConfigurarMemoria; 
    }
    public JButton getBtnAplicarConfig() { 
        return btnAplicarConfig; 
    }
    
    public JSpinner getSpinnerTamanoRAM() { 
        return spinnerTamanoRAM; 
    }
    public JSpinner getSpinnerTamanoDisco() {
        return spinnerTamanoDisco;
    }
    public JLabel getLblKernelCalculado() {
        return lblKernelCalculado;
    }
    public JLabel getLblMemoriaVirtualCalculada() {
        return lblMemoriaVirtualCalculada;
    }
    
    public DefaultTableModel getModeloProcesos() { 
        return modeloProcesos; 
    }
    public DefaultTableModel getModeloMemoria() { 
        return modeloMemoria; 
    }
    public DefaultTableModel getModeloDisco() { 
        return modeloDisco; 
    }
    
    public JLabel getLblPID() { 
        return lblPID; 
    }
    public JLabel getLblEstadoProceso() { 
        return lblEstadoProceso; 
    }
    public JLabel getLblPC() { 
        return lblPC; 
    }
    public JLabel getLblIR() { 
        return lblIR; 
    }
    public JLabel getLblAC() { 
        return lblAC; 
    }
    public JLabel getLblAX() { 
        return lblAX; 
    }
    public JLabel getLblBX() { 
        return lblBX; 
    }
    public JLabel getLblCX() { 
        return lblCX; 
    }
    public JLabel getLblDX() { 
        return lblDX; 
    }
    
    public JTextArea getAreaConsola() { 
        return areaConsola; 
    }
    public JTextField getTxtEntradaTeclado() { 
        return txtEntradaTeclado; 
 }

    // ===================== PRUEBA RÁPIDA =====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MiniPCFrame frame = new MiniPCFrame();
            frame.setVisible(true);
        });
    }
}