package com.minipc.t1sominipc.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/*
 * Nombre: Estadistica
 * Descripción: Resumen de un proceso ya terminado. Se copia del BCP antes de liberarlo,
 * porque su espacio de kernel se reutiliza.
 */
public class Estadistica {

    private static final DateTimeFormatter HORA_MINUTO =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private int pid;
    private String nombre;
    private long inicio;  
    private long fin;    
    private int duracion; // segundos de CPU (ticks + espera de teclado)

    public Estadistica(int pid, String nombre, long inicio, long fin, int duracion) {
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
        return HORA_MINUTO.format(Instant.ofEpochSecond(inicio));
    }

    public String getHoraFin() {
        return HORA_MINUTO.format(Instant.ofEpochSecond(fin));
    }

    public int getDuracion() {
        return duracion;
    }
}
