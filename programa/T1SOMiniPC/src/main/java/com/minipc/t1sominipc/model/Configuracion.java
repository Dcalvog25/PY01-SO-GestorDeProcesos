package com.minipc.t1sominipc.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Nombre: Configuracion
 * Descripción: Tamaños de RAM y Disco, y los porcentajes que definen el kernel y la memoria
 * virtual. Se lee y se guarda en un archivo JSON, no queda en el código.
 */
public class Configuracion {

    private int memoriaRAM;
    private double porcentajeKernel;
    private int disco;
    private double porcentajeMemoriaVirtual;

    public Configuracion(int memoriaRAM, double porcentajeKernel, int disco, double porcentajeMemoriaVirtual) {
        this.memoriaRAM = memoriaRAM;
        this.porcentajeKernel = porcentajeKernel;
        this.disco = disco;
        this.porcentajeMemoriaVirtual = porcentajeMemoriaVirtual;
    }

    /*
        * Nombre: cargar
        * Descripción: Lee la configuración de un archivo JSON plano.
        * Salida: la configuración, o IOException si falta el archivo, una clave o un valor no es válido.
     */
    public static Configuracion cargar(Path ruta) throws IOException {
        String json = new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8);

        int ram = (int) leerNumero(json, "memoriaRAM");
        double kernel = leerNumero(json, "porcentajeKernel");
        int disco = (int) leerNumero(json, "disco");
        double virtual = leerNumero(json, "porcentajeMemoriaVirtual");

        if (ram <= 0 || disco <= 0 || kernel < 1 || kernel > 90 || virtual < 1 || virtual > 50) {
            throw new IOException("valores fuera de rango (RAM y disco > 0, kernel 1-90%, memoria virtual 1-50%)");
        }
        return new Configuracion(ram, kernel, disco, virtual);
    }

    public void guardar(Path ruta) throws IOException {
        String json = String.format(Locale.US,
                "{%n  \"memoriaRAM\": %d,%n  \"porcentajeKernel\": %s,%n  \"disco\": %d,%n  \"porcentajeMemoriaVirtual\": %s%n}%n",
                memoriaRAM, porcentajeKernel, disco, porcentajeMemoriaVirtual);
        Files.write(ruta, json.getBytes(StandardCharsets.UTF_8));
    }

    private static double leerNumero(String json, String clave) throws IOException {
        Matcher m = Pattern.compile("\"" + clave + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)").matcher(json);
        if (!m.find()) {
            throw new IOException("falta la clave \"" + clave + "\"");
        }
        return Double.parseDouble(m.group(1));
    }

    public int calcularKernel(int tamanoRAM) {
        return Math.max((int) Math.round(tamanoRAM * porcentajeKernel / 100.0), 16);
    }

    public int calcularMemoriaVirtual(int tamanoDisco) {
        return Math.max((int) Math.round(tamanoDisco * porcentajeMemoriaVirtual / 100.0), 1);
    }

    public int getMemoriaRAM() {
        return memoriaRAM;
    }

    public void setMemoriaRAM(int memoriaRAM) {
        this.memoriaRAM = memoriaRAM;
    }

    public int getDisco() {
        return disco;
    }

    public void setDisco(int disco) {
        this.disco = disco;
    }

    public double getPorcentajeKernel() {
        return porcentajeKernel;
    }

    public double getPorcentajeMemoriaVirtual() {
        return porcentajeMemoriaVirtual;
    }
}
