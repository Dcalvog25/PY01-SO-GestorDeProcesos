package com.minipc.t1sominipc.model;

/*
 * Nombre: Estadistica
 * Descripción: Resumen de un proceso ya terminado. Se copia del BCP antes de liberarlo,
 * porque su espacio de kernel se reutiliza. Las horas son minutos desde medianoche.
 */
public class Estadistica {

    private int pid;
    private String nombre;
    private int inicio;
    private int fin;
    private int duracion; // segundos de CPU (ticks + espera de teclado)

    public Estadistica(int pid, String nombre, int inicio, int fin, int duracion) {
        this.pid = pid;
        this.nombre = nombre;
        this.inicio = inicio;
        this.fin = fin;
        this.duracion = duracion;
    }

    public String getProceso() {
        return "PID " + pid + " - " + nombre;
    }

    public String getHoraInicio() {
        return formatear(inicio);
    }

    public String getHoraFin() {
        return formatear(fin);
    }

    public int getDuracion() {
        return duracion;
    }

    private static String formatear(int minutoDelDia) {
        return String.format("%02d:%02d", minutoDelDia / 60, minutoDelDia % 60);
    }
}
