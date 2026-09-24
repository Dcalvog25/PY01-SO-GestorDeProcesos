package com.minipc.t1sominipc.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Nombre: ConvertidorASM
 * Descripción: Clase que convierte código ensamblador en instrucciones.
 */
public class ConvertidorASM {

   
    private List<String> errores;

    /*
        * Nombre: convertirASM
        *Entrada: List<String> lineasASM
        *Salida: List<Instruccion>
        *Descripción: Convierte una lista de líneas de código ensamblador en una lista de instrucciones.
     */
    public List<Instruccion> convertirASM(List<String> lineasASM) {
        errores = new ArrayList<>();
        List<Instruccion> instrucciones = new ArrayList<>();
        int numeroLinea = 0;

        for (String linea : lineasASM) {
            numeroLinea++;
            String lineaLimpia = linea.trim();

            if (lineaLimpia.isEmpty()) {
                continue; 
            }

            String[] partes = lineaLimpia.split("[,\\s]+");
            String operador = partes[0].toUpperCase();

            if (!operadorValido(operador)) {
                registrarError(numeroLinea, linea, "Operador no reconocido: '" + partes[0] + "'");
                continue;
            }

            // Estructura esperada según el operador: MOV lleva registro + valor, el resto solo registro
            int argumentosEsperados = operador.equals("MOV") ? 2 : 1;
            int argumentosRecibidos = partes.length - 1;

            if (argumentosRecibidos != argumentosEsperados) {
                registrarError(numeroLinea, linea, operador + " espera " + argumentosEsperados
                        + " argumento(s), pero la línea tiene " + argumentosRecibidos);
                continue;
            }

            String registro = partes[1].toUpperCase();
            if (!registroValido(registro)) {
                registrarError(numeroLinea, linea, "Registro no reconocido: '" + partes[1] + "'");
                continue;
            }

            int valorDireccion = 0;
            if (argumentosEsperados == 2) {
                try {
                    valorDireccion = Integer.parseInt(partes[2]);
                } catch (NumberFormatException e) {
                    registrarError(numeroLinea, linea, "Valor no numérico: '" + partes[2] + "'");
                    continue;
                }

                if (!valorEnRango(valorDireccion)) {
                    registrarError(numeroLinea, linea, "Valor fuera de rango (-127 a 127): " + valorDireccion);
                    continue;
                }
            }
            Instruccion instruccion = new Instruccion(operador, registro, valorDireccion, linea);
            instrucciones.add(instruccion);
        }

        return instrucciones;
    }

    /*
        * Nombre: operadorValido
        *Entrada: String operador
        *Salida: boolean
        *Descripción: Verifica si un operador es válido dentro del conjunto de operadores válidos.
     */
    private boolean operadorValido(String operador) {
        if(operador.matches("LOAD|STORE|MOV|SUB|ADD")) {
            return true;
        }
        return false;
    }

    /*
        * Nombre: registroValido
        *Entrada: String registro
        *Salida: boolean
        *Descripción: Verifica si un registro es válido dentro del conjunto de registros válidos.
     */

    private boolean registroValido(String registro) {
        if(registro.matches("AX|BX|CX|DX")) {
            return true;
        }
        return false;
    }

    /*
        * Nombre: valorEnRango
        *Entrada: int valor
        *Salida: boolean
        *Descripción: Verifica si un valor está dentro del rango permitido (-127 a 127).
     */

    private boolean valorEnRango(int valor) {
        if(valor < -127 || valor > 127) {
            return false;
        }
        return true;
    }

    /*
        * Nombre: registrarError
        *Entrada: int numeroLinea, String lineaOriginal, String motivo
        *Salida: void
        *Descripción: Registra un error de conversión con detalles sobre la línea y el motivo.
     */
    private void registrarError(int numeroLinea, String lineaOriginal, String motivo) {
        errores.add("Línea " + numeroLinea + ": \"" + lineaOriginal.trim() + "\" → " + motivo);
    }

    /*
        * Nombre: getErrores
        *Entrada: void
        *Salida: List<String>
        *Descripción: Devuelve la lista de errores registrados.
     */
    public List<String> getErrores() {
        return errores;
    }

    /*
        * Nombre: tieneErrores
        *Entrada: void
        *Salida: boolean
        *Descripción: Indica si hubo errores durante la conversión.
     */

    public boolean tieneErrores() {
        if(errores == null || errores.isEmpty()) {
            return false;
        }
        return true;
    }
}