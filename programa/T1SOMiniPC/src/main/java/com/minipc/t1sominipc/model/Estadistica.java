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

    /*
     */
    public Estadistica(int pid, String nombre, int inicio, int fin, int duracion) {
        this.pid = pid;
        this.nombre = nombre;
        this.inicio = inicio;
        this.fin = fin;
        this.duracion = duracion;
    }



    /*
     * Nombre: getProceso
     * Descripción: Devuelve una cadena con el PID y el nombre del proceso.
     * Entrada: void
     * Salida: String - "PID <pid> - <nombre>"
     */
    public String getProceso() {
        return "PID " + pid + " - " + nombre;
    }

    /*
     * Nombre: getHoraInicio
     * Descripción: Devuelve la hora de inicio del proceso en formato "HH:MM".
     * Entrada: void
     * Salida: String - hora de inicio formateada.
     */
    public String getHoraInicio() {
        return formatear(inicio);
    }

    /*
     * Nombre: getHoraFin
     * Descripción: Devuelve la hora de fin del proceso en formato "HH:MM".
     * Entrada: void
     * Salida: String - hora de fin formateada.
     */
    public String getHoraFin() {
        return formatear(fin);
    }

    /*
     * Nombre: getDuracion
     * Descripción: Devuelve la duración del proceso en segundos de CPU.
     * Entrada: void
     * Salida: int - duración en segundos de CPU.
     */
    public int getDuracion() {
        return duracion;
    }

    /*
     * Nombre: formatear
     * Descripción: Convierte minutos desde medianoche a formato "HH:MM".
     * Entrada: int minutoDelDia
     * Salida: String - hora formateada.
     */
    private static String formatear(int minutoDelDia) {
        return String.format("%02d:%02d", minutoDelDia / 60, minutoDelDia % 60);
    }
}
