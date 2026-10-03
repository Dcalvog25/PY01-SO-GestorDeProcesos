package com.minipc.t1sominipc.model;
import java.util.List;
/*
 * Nombre: Instruccion
 * Descripción: Clase que representa una instrucción en el procesador.
 */

public class Instruccion {

    private String operador;
    private String reg1;
    private String reg2;
    private Integer valor;
    private List<Integer> parametros;
    private String lineaOriginal; 
    private String valorTexto; // solo se usa cuando MOV DX recibe algo entre comillas

    /*
        * Nombre: Instruccion
        * Descripción: Constructor de la clase Instruccion.
        * Entrada: String operador, String reg1, String reg2, Integer valor, List<Integer> parametros, String valorTexto, String lineaOriginal
        * Salida: void
     */
    public Instruccion(String operador, String reg1, String reg2, Integer valor, List<Integer> parametros, String valorTexto, String lineaOriginal) {
        this.operador = operador;
        this.reg1 = reg1;
        this.reg2 = reg2;
        this.valor = valor;
        this.parametros = parametros;
        this.lineaOriginal = lineaOriginal;
        this.valorTexto = valorTexto;
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
        * Nombre: getReg1
        * Descripción: Devuelve el primer registro de la instrucción.
        * Entrada: void
        * Salida: String
     */

    public String getReg1() {
        return reg1;
    }

    /*
        * Nombre: getReg2
        * Descripción: Devuelve el segundo registro de la instrucción.
        * Entrada: void
        * Salida: String
     */
    public String getReg2() {
        return reg2;
    }

    /*
        * Nombre: getParametros
        * Descripción: Devuelve la lista de parámetros de la instrucción.
        * Entrada: void
        * Salida: List<Integer>
     */
    public List<Integer> getParametros() {
        return parametros;
    }
    /*
        * Nombre: getValor
        * Descripción: Devuelve el valor de la instrucción.
        * Entrada: void
        * Salida: Integer
     */
    public Integer getValor() {
        return valor;
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
        * Nombre: getValorTexto
        * Descripción: Devuelve el valor de texto de la instrucción.
        * Entrada: void
        * Salida: String
     */
    public String getValorTexto() {
        return valorTexto;
    }

    /*
        * Nombre: getPeso
        * Descripción: Segundos (ticks) de CPU que dura la instrucción, según la tabla del enunciado.
        * Entrada: void
        * Salida: int
     */
    public int getPeso() {
        switch (operador) {
            case "LOAD":
            case "STORE":
            case "JMP":
            case "CMP":
            case "JE":
            case "JNE":
                return 2;
            case "ADD":
            case "SUB":
            case "PARAM":
                return 3;
            case "INT":
                if (valor != null && valor == 0x21) {
                    return 5;
                }
                if (valor != null && valor == 0x09) {
                    return 3;
                }
                return 2;
            default:
                return 1; // MOV, INC, DEC, SWAP, PUSH, POP
        }
    }
}
