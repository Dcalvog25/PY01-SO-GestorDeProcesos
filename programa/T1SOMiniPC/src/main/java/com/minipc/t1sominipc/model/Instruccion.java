package com.minipc.t1sominipc.model;

/*
 * Nombre: Instruccion
 * Descripción: Clase que representa una instrucción en el procesador.
 */

public class Instruccion {

    private String operador;
    private String registro;
    private int valorDireccion;
    private String lineaOriginal; 

    /*
        * Nombre: Instruccion
        * Descripción: Constructor de la clase Instruccion.
        * Entrada: String operador, String registro, int valorDireccion, String lineaOriginal
        * Salida: void
     */
    public Instruccion(String operador, String registro, int valorDireccion, String lineaOriginal) {
        this.operador = operador;
        this.registro = registro;
        this.valorDireccion = valorDireccion;
        this.lineaOriginal = lineaOriginal;
    }

    /*
        * Nombre: getOperador
        * Descripción: Devuelve el operador de la instrucción.
        * Entrada: void
        * Salida: String
     */

    public String getOperador() {
        return operador;
    }

    /*
        * Nombre: getRegistro
        * Descripción: Devuelve el registro de la instrucción.
        * Entrada: void
        * Salida: String
     */

    public String getRegistro() {
        return registro;
    }

    /*
        * Nombre: getValorDireccion
        * Descripción: Devuelve el valor de la dirección de la instrucción.
        * Entrada: void
        * Salida: int
     */
    public int getValorDireccion() {
        return valorDireccion;
    }

    /*
        * Nombre: getLineaOriginal
        * Descripción: Devuelve la línea original de la instrucción.
        * Entrada: void
        * Salida: String
     */

    public String getLineaOriginal() {
        return lineaOriginal;
    }

    /*
        * Nombre: aBinario
        * Descripción: Convierte la instrucción a su representación binaria.
        * Entrada: void
        * Salida: String
     */
    public String aBinario() {
        String operacion = obtenerCodigoOperacion(operador);
        String registroBinario = obtenerCodigoRegistro(registro);
        String valorBinario = valorABinario(valorDireccion);
        return operacion + registroBinario + valorBinario;
    }
    
    /*
        * Nombre: valorABinario
        * Descripción: Convierte un valor entero a su representación binaria de 8 bits.
        * Entrada: int valor
        * Salida: String
     */

    private String valorABinario(int valor) {
        int signo = 0;
        if (valor < 0) {
            signo = 1;
        }
        int magnitud = Math.abs(valor);
        String binario7bits = String.format("%7s", Integer.toBinaryString(magnitud)).replace(' ', '0');
        return signo + binario7bits;
    }

    /*
        * Nombre: obtenerCodigoOperacion
        * Descripción: Obtiene el código de operación para un operador dado.
        * Entrada: String operador
        * Salida: String
     */

    public String obtenerCodigoOperacion(String operador) {
        switch (operador) {
            case "LOAD":
                return "0001";
            case "STORE":
                return "0010";
            case "MOV":
                return "0011";
            case "SUB":
                return "0100";
            case "ADD":
                return "0101";
            default:
                throw new IllegalArgumentException("Operador no reconocido: " + operador);
        }
    }

    /*
        * Nombre: obtenerCodigoRegistro
        * Descripción: Obtiene el código de registro para un registro dado.
        * Entrada: String registro
        * Salida: String
     */

    public String obtenerCodigoRegistro(String registro) {
        switch (registro) {
            case "AX":
                return "0001";
            case "BX":
                return "0010";
            case "CX":
                return "0011";
            case "DX":
                return "0100";
            default:
                throw new IllegalArgumentException("Operador no reconocido: " + registro);
        }
    }
}
