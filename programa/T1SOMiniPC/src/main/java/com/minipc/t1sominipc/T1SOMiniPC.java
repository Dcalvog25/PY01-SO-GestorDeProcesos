package com.minipc.t1sominipc;

import com.minipc.t1sominipc.controller.ControllerMiniPC;
import com.minipc.t1sominipc.view.MiniPCFrame;

import javax.swing.*;

/*
 * Nombre: T1SOMiniPC
 * Descripción: Clase principal que inicia la aplicación del MiniPC.
 */
public class T1SOMiniPC {

    /*
        * Nombre: main
        * Descripción: Método principal que inicia la aplicación.
        * Entrada: String[] args
        * Salida: void
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MiniPCFrame vista = new MiniPCFrame();
            new ControllerMiniPC(vista); // conecta los botones con la lógica
            vista.setVisible(true);
        });
    }
}