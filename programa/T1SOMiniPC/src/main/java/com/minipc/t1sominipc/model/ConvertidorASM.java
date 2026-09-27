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

            int argumentos = partes.length - 1;
            Instruccion instruccion = null;

            switch (operador) {
                case "LOAD":
                case "STORE":
                case "ADD":
                case "SUB":
                case "PUSH":
                case "POP":
                    instruccion = validarUnRegistro(operador, partes, argumentos, numeroLinea, linea);
                    break;

                case "INC":
                case "DEC":
                    instruccion = validarIncDec(operador, partes, argumentos, numeroLinea, linea);
                    break;

                case "MOV":
                    instruccion = validarMov(operador, partes, argumentos, numeroLinea, linea);
                    break;

                case "CMP":
                case "SWAP":
                    instruccion = validarDosRegistros(operador, partes, argumentos, numeroLinea, linea);
                    break;

                case "JMP":
                case "JE":
                case "JNE":
                    instruccion = validarSalto(operador, partes, argumentos, numeroLinea, linea);
                    break;

                case "PARAM":
                    instruccion = validarParam(operador, partes, argumentos, numeroLinea, linea);
                    break;

                case "INT":
                    instruccion = validarInt(operador, partes, argumentos, numeroLinea, linea);
                    break;
            }

            if (instruccion != null) {
                instrucciones.add(instruccion);
            }
        }

        return instrucciones;
    }

    /*
        * Nombre: validarUnRegistro
        *Descripción: Para LOAD, STORE, ADD, SUB, PUSH, POP - todos esperan un solo registro.
        * Entrada: String operador, String[] partes, int argumentos, int numeroLinea, String linea
        * Salida: Instruccion - objeto Instruccion válido o null si hay error
     */
    private Instruccion validarUnRegistro(String operador, String[] partes, int argumentos, int numeroLinea, String linea) {
        if (argumentos != 1) {
            registrarError(numeroLinea, linea, operador + " espera 1 argumento (un registro)");
            return null;
        }

        String registro = partes[1].toUpperCase();
        if (!registroValido(registro)) {
            registrarError(numeroLinea, linea, "Registro no reconocido: '" + partes[1] + "'");
            return null;
        }

        return new Instruccion(operador, registro, null, null, null, linea);
    }

    /*
        * Nombre: validarIncDec
        *Descripción: INC y DEC pueden ir solos (actúan sobre AC) o con un registro.
        * Entrada: String operador, String[] partes, int argumentos, int numeroLinea, String linea
        * Salida: Instruccion - objeto Instruccion válido o null si hay error   
     */
    private Instruccion validarIncDec(String operador, String[] partes, int argumentos, int numeroLinea, String linea) {
        if (argumentos == 0) {
            return new Instruccion(operador, null, null, null, null, linea);
        }

        if (argumentos == 1) {
            String registro = partes[1].toUpperCase();
            if (!registroValido(registro)) {
                registrarError(numeroLinea, linea, "Registro no reconocido: '" + partes[1] + "'");
                return null;
            }
            return new Instruccion(operador, registro, null, null, null, linea);
        }

        registrarError(numeroLinea, linea, operador + " espera 0 o 1 argumento");
        return null;
    }

    /*
        * Nombre: validarMov
        * Descripción: MOV puede ser reg,reg o reg,valor. Se revisa si el segundo argumento es un registro.
        * Entrada: String operador, String[] partes, int argumentos, int numeroLinea, String linea
        * Salida: Instruccion - objeto Instruccion válido o null si hay error
     */
    private Instruccion validarMov(String operador, String[] partes, int argumentos, int numeroLinea, String linea) {
        if (argumentos != 2) {
            registrarError(numeroLinea, linea, "MOV espera 2 argumentos");
            return null;
        }

        String reg1 = partes[1].toUpperCase();
        if (!registroValido(reg1)) {
            registrarError(numeroLinea, linea, "Registro no reconocido: '" + partes[1] + "'");
            return null;
        }

        String segundo = partes[2].toUpperCase();

        if (registroValido(segundo)) {
            return new Instruccion(operador, reg1, segundo, null, null, linea);
        }

        try {
            int valor = Integer.parseInt(partes[2]);
            if (!valorEnRango(valor)) {
                registrarError(numeroLinea, linea, "Valor fuera de rango (-127 a 127): " + valor);
                return null;
            }
            return new Instruccion(operador, reg1, null, valor, null, linea);
        } catch (NumberFormatException e) {
            registrarError(numeroLinea, linea, "Segundo argumento inválido: '" + partes[2] + "'");
            return null;
        }
    }

    /*
        * Nombre: validarDosRegistros
        * Descripción: Para CMP y SWAP - ambos esperan dos registros.
        * Entrada: String operador, String[] partes, int argumentos, int numeroLinea, String linea
        * Salida: Instruccion - objeto Instruccion válido o null si hay error
     */
    private Instruccion validarDosRegistros(String operador, String[] partes, int argumentos, int numeroLinea, String linea) {
        if (argumentos != 2) {
            registrarError(numeroLinea, linea, operador + " espera 2 registros");
            return null;
        }

        String reg1 = partes[1].toUpperCase();
        String reg2 = partes[2].toUpperCase();

        if (!registroValido(reg1) || !registroValido(reg2)) {
            registrarError(numeroLinea, linea, "Registro no reconocido en: " + linea);
            return null;
        }

        return new Instruccion(operador, reg1, reg2, null, null, linea);
    }

    /*
        * Nombre: validarSalto
        * Descripción: Para JMP, JE, JNE - esperan un desplazamiento numérico con signo.
        * Entrada: String operador, String[] partes, int argumentos, int numeroLinea, String linea
        * Salida: Instruccion - objeto Instruccion válido o null si hay error
     */
    private Instruccion validarSalto(String operador, String[] partes, int argumentos, int numeroLinea, String linea) {
        if (argumentos != 1) {
            registrarError(numeroLinea, linea, operador + " espera 1 argumento (desplazamiento)");
            return null;
        }

        try {
            int desplazamiento = Integer.parseInt(partes[1]);
            if (!valorEnRango(desplazamiento)) {
                registrarError(numeroLinea, linea, "Desplazamiento fuera de rango (-127 a 127): " + desplazamiento);
                return null;
            }
            return new Instruccion(operador, null, null, desplazamiento, null, linea);
        } catch (NumberFormatException e) {
            registrarError(numeroLinea, linea, "Desplazamiento no numérico: '" + partes[1] + "'");
            return null;
        }
    }

    /*
        * Nombre: validarParam
        * Descripción: PARAM acepta de 1 a 3 valores numéricos, se guardan en la lista de parámetros.
        * Entrada: String operador, String[] partes, int argumentos, int numeroLinea, String linea
        * Salida: Instruccion - objeto Instruccion válido o null si hay error
     */
    private Instruccion validarParam(String operador, String[] partes, int argumentos, int numeroLinea, String linea) {
        if (argumentos < 1 || argumentos > 3) {
            registrarError(numeroLinea, linea, "PARAM acepta entre 1 y 3 valores");
            return null;
        }

        List<Integer> parametros = new ArrayList<>();

        for (int i = 1; i <= argumentos; i++) {
            try {
                int valor = Integer.parseInt(partes[i]);
                if (!valorEnRango(valor)) {
                    registrarError(numeroLinea, linea, "Valor fuera de rango (-127 a 127): " + valor);
                    return null;
                }
                parametros.add(valor); 
            } catch (NumberFormatException e) {
                registrarError(numeroLinea, linea, "Valor no numérico en PARAM: '" + partes[i] + "'");
                return null;
            }
        }

        return new Instruccion(operador, null, null, null, parametros, linea);
    }

    /*
        * Nombre: validarInt
        * Descripción: INT espera un código en hexadecimal (ej. 20H, 10H). Se convierte a decimal.
        * Entrada: String operador, String[] partes, int argumentos, int numeroLinea, String linea
        * Salida: Instruccion - objeto Instruccion válido o null si hay error
     */
    private Instruccion validarInt(String operador, String[] partes, int argumentos, int numeroLinea, String linea) {
        if (argumentos != 1) {
            registrarError(numeroLinea, linea, "INT espera 1 argumento (código en hexadecimal)");
            return null;
        }

        String codigoTexto = partes[1].toUpperCase().replace("H", "");

        try {
            int codigo = Integer.parseInt(codigoTexto, 16);
            return new Instruccion(operador, null, null, codigo, null, linea);
        } catch (NumberFormatException e) {
            registrarError(numeroLinea, linea, "Código de interrupción inválido: '" + partes[1] + "'");
            return null;
        }
    }

    /*
        * Nombre: operadorValido
        * Descripción: Verifica si el operador está dentro del conjunto completo de operadores válidos.
        * Entrada: String operador
        * Salida: boolean - true si es válido, false en caso contrario
     */
    private boolean operadorValido(String operador) {
        if (operador.matches("LOAD|STORE|MOV|SUB|ADD|INC|DEC|SWAP|CMP|JMP|JE|JNE|PARAM|PUSH|POP|INT")) {
            return true;
        }
        return false;
    }

    /*
        * Nombre: registroValido
        * Descripción: Verifica si el registro es uno de los registros válidos (AX, BX, CX, DX).
        * Entrada: String registro
        * Salida: boolean - true si es válido, false en caso contrario
     */
    private boolean registroValido(String registro) {
        if (registro.matches("AX|BX|CX|DX")) {
            return true;
        }
        return false;
    }

    /*
        * Nombre: valorEnRango
        * Descripción: Verifica si un valor numérico está dentro del rango permitido (-127 a 127).
        * Entrada: int valor
        * Salida: boolean - true si está en rango, false en caso contrario
     */

    private boolean valorEnRango(int valor) {
        if (valor < -127 || valor > 127) {
            return false;
        }
        return true;
    }

    /*
        * Nombre: registrarError
        * Descripción: Registra un error de conversión de ASM.
        * Entrada: int numeroLinea, String lineaOriginal, String motivo
        * Salida: void
     */

    private void registrarError(int numeroLinea, String lineaOriginal, String motivo) {
        errores.add("Línea " + numeroLinea + ": \"" + lineaOriginal.trim() + "\" → " + motivo);
    }

    /*
        * Nombre: getErrores
        * Descripción: Devuelve la lista de errores registrados durante la conversión de ASM.
        * Entrada: void
        * Salida: List<String> - lista de errores
     */

    public List<String> getErrores() {
        return errores;
    }

    /*
        * Nombre: tieneErrores
        * Descripción: Verifica si hay errores registrados durante la conversión de ASM.
        * Entrada: void
        * Salida: boolean - true si hay errores, false en caso contrario
     */
    
    public boolean tieneErrores() {
        if (errores == null || errores.isEmpty()) {
            return false;
        }
        return true;
    }
}