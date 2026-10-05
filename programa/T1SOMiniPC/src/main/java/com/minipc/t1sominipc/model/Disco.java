package com.minipc.t1sominipc.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Nombre: Disco
 * Descripción: Simula el almacenamiento secundario. Un solo espacio de
 * direcciones (0 a tamanoTotal-1), dividido en 3 secciones: Índice, Programas
 * y Memoria virtual — igual que Memoria separa Kernel y Usuario en un solo arreglo.
 */
public class Disco {

    private int[] datos;              // arreglo real y único del disco: aquí vive el índice (dirección + tamaño)
    private Instruccion[] contenido;  // mismo tamaño, misma dirección
    private String[] nombresArchivo;  //  mismo tamaño, misma dirección
    private boolean[] ocupadaVirtual; // mismo tamaño; solo se usa dentro de la zona de memoria virtual

    private int tamanoTotal;
    private int tamanoIndice;
    private int tamanoMemoriaVirtual;
    private int inicioZonaProgramas;
    private int inicioMemoriaVirtual;

    private int cantidadArchivos;
    private int capacidadMaximaArchivos;
    private int siguienteDireccionLibre;

    public Disco(int tamanoTotal, int tamanoMemoriaVirtual) {
        this.tamanoTotal = tamanoTotal;
        this.tamanoMemoriaVirtual = tamanoMemoriaVirtual;

        this.tamanoIndice = Math.max((int) Math.round(tamanoTotal * 0.10), 4);
        this.capacidadMaximaArchivos = tamanoIndice / 2;

        this.inicioZonaProgramas = tamanoIndice;
        this.inicioMemoriaVirtual = tamanoTotal - tamanoMemoriaVirtual;

        // Los tres arreglos miden lo mismo, y se consultan con la misma dirección igual que Memoria
        this.datos = new int[tamanoTotal];
        this.contenido = new Instruccion[tamanoTotal];
        this.nombresArchivo = new String[tamanoTotal];
        this.ocupadaVirtual = new boolean[tamanoTotal];

        this.siguienteDireccionLibre = inicioZonaProgramas;
        this.cantidadArchivos = 0;
    }

    /*
     * Nombre: guardarArchivo
     * Descripción: Guarda un archivo en el disco.
     * Entrada: nombre del archivo y lista de instrucciones del programa.
     * Salida: true si se guardó correctamente, false si no se pudo guardar.
     */
    
    public boolean guardarArchivo(String nombre, List<Instruccion> programa) {
        if (buscarPorNombre(nombre) != -1) {
            return false;
        }
        if (cantidadArchivos >= capacidadMaximaArchivos) {
            return false;
        }
        if (siguienteDireccionLibre + programa.size() > inicioMemoriaVirtual) {
            return false;
        }

        int direccion = siguienteDireccionLibre;
        int posIndice = cantidadArchivos * 2;

        // El nombre se guarda en la misma dirección donde vive la entrada del índice
        datos[posIndice] = direccion;
        datos[posIndice + 1] = programa.size();
        nombresArchivo[posIndice] = nombre;
        cantidadArchivos++;

        for (Instruccion instr : programa) {
            contenido[siguienteDireccionLibre] = instr;
            siguienteDireccionLibre++;
        }

        return true;
    }

    /*
     * Nombre: leerArchivo
     * Descripción: Lee un archivo del disco por su nombre.
     * Entrada: nombre del archivo a leer.
     * Salida: lista de instrucciones del archivo o null si no se encuentra.
     */
    public List<Instruccion> leerArchivo(String nombre) {
        int posIndice = buscarPorNombre(nombre);
        if (posIndice == -1) {
            return null;
        }

        int direccion = datos[posIndice];
        int tamano = datos[posIndice + 1];

        List<Instruccion> programa = new ArrayList<>();
        for (int i = 0; i < tamano; i++) {
            programa.add(contenido[direccion + i]);
        }
        return programa;
    }

    /*
     * Nombre: eliminarArchivo
     * Descripción: Elimina un archivo del disco por su nombre.
     * Entrada: nombre del archivo a eliminar.
     * Salida: true si se eliminó correctamente, false si no se encontró.
     */
    public boolean eliminarArchivo(String nombre) {
        int posEliminar = buscarPorNombre(nombre);
        if (posEliminar == -1) {
            return false;
        }

        int indiceEliminado = posEliminar / 2;
        int dirArchivo = datos[posEliminar];
        int tamArchivo = datos[posEliminar + 1];
        if (contenido[dirArchivo] == null) { // archivo de datos: libera su celda
            datos[dirArchivo] = 0;
            if (dirArchivo + tamArchivo == siguienteDireccionLibre) {
                siguienteDireccionLibre -= tamArchivo;
            }
        }
        for (int i = indiceEliminado; i < cantidadArchivos - 1; i++) {
            datos[i * 2] = datos[(i + 1) * 2];
            datos[i * 2 + 1] = datos[(i + 1) * 2 + 1];
            nombresArchivo[i * 2] = nombresArchivo[(i + 1) * 2];
        }
        cantidadArchivos--;
        return true;
    }

    /*
     * Nombre: listarArchivos
     * Descripción: Devuelve una lista con los nombres de todos los archivos almacenados en el disco.
     * Entrada: ninguna.
     * Salida: lista de nombres de archivos.
     */
    public List<String> listarArchivos() {
        List<String> lista = new ArrayList<>();
        for (int i = 0; i < cantidadArchivos; i++) {
            lista.add(nombresArchivo[i * 2]);
        }
        return lista;
    }

    /*
     * Nombre: buscarPorNombre
        *Descripción: Busca un archivo por su nombre y devuelve la posición del índice correspondiente.
        *Entrada: nombre del archivo a buscar.
        *Salida: posición del índice correspondiente o -1 si no se encuentra.
     */
    private int buscarPorNombre(String nombre) {
        for (int i = 0; i < cantidadArchivos; i++) {
            int posIndice = i * 2;
            if (nombresArchivo[posIndice].equals(nombre)) {
                return posIndice;
            }
        }
        return -1;
    }

    /*
     * Métodos de acceso a las propiedades del disco
     */
    public int getTamanoIndice() { 
        return tamanoIndice; 
    }
    public int getInicioZonaProgramas() { 
        return inicioZonaProgramas; 
    }
    public int getInicioMemoriaVirtual() { 
        return inicioMemoriaVirtual; 
    }
    public int getEspacioLibre() { 
        return inicioMemoriaVirtual - siguienteDireccionLibre; 
    }

    public int getTamanoTotal() {
    return tamanoTotal;
    }

    public int getCantidadArchivos() {
        return cantidadArchivos;
    }

    public int getSiguienteDireccionLibre() {
        return siguienteDireccionLibre;
    }

    public int leerDato(int direccion) {
        return datos[direccion];
    }

    public String leerNombre(int direccion) {
        return nombresArchivo[direccion];
    }

    public Instruccion leerInstruccion(int direccion) {
        return contenido[direccion];
    }

    public boolean existeArchivo(String nombre) {
        if(buscarPorNombre(nombre) != -1) {
            return true;
        }
        return false;
    }

    /*
     * Nombre: crearArchivoDatos
     * Descripción: INT 21H 3Ch. Crea un archivo de una sola celda (un número, inicia en 0) en elíndice
     * y en la zona de programas. Las instrucciones de un programa nunca son null, así se distinguen.
     * Salida: false si ya existe, el índice está lleno o no hay espacio.
     */
    public boolean crearArchivoDatos(String nombre) {
        if (buscarPorNombre(nombre) != -1 || cantidadArchivos >= capacidadMaximaArchivos
                || siguienteDireccionLibre + 1 > inicioMemoriaVirtual) {
            return false;
        }
        int direccion = siguienteDireccionLibre;
        int posIndice = cantidadArchivos * 2;

        datos[posIndice] = direccion;
        datos[posIndice + 1] = 1;
        nombresArchivo[posIndice] = nombre;
        datos[direccion] = 0;
        contenido[direccion] = null;

        cantidadArchivos++;
        siguienteDireccionLibre++;
        return true;
    }

    public boolean esArchivoDatos(String nombre) {
        int posIndice = buscarPorNombre(nombre);
        return posIndice != -1 && contenido[datos[posIndice]] == null;
    }

    public int leerDatoArchivo(String nombre) {
        int posIndice = buscarPorNombre(nombre);
        if (posIndice == -1) {
            return 0;
        }
        return datos[datos[posIndice]];
    }

    public void escribirDatoArchivo(String nombre, int valor) {
        int posIndice = buscarPorNombre(nombre);
        if (posIndice != -1) {
            datos[datos[posIndice]] = valor;
        }
    }

    /*
     * Nombre: getDireccionArchivo / getTamanoArchivo
     * Descripción: Leen del índice la dirección de disco y el tamaño de un archivo, o -1 si no existe.
     */
    public int getDireccionArchivo(String nombre) {
        int posIndice = buscarPorNombre(nombre);
        if (posIndice == -1) {
            return -1;
        }
        return datos[posIndice];
    }

    public int getTamanoArchivo(String nombre) {
        int posIndice = buscarPorNombre(nombre);
        if (posIndice == -1) {
            return -1;
        }
        return datos[posIndice + 1];
    }

    /*
     * Nombre: guardarEnMemoriaVirtual
     * Descripción: Swap-out. Copia un programa a la zona de memoria virtual (primer hueco que quepa).
     * El nombre y el tamaño quedan en la primera dirección del bloque.
     * Salida: true si cupo, false si la memoria virtual no tiene un hueco suficiente.
     */
    public boolean guardarEnMemoriaVirtual(String nombre, List<Instruccion> programa) {
        int tamano = programa.size();
        int libresSeguidas = 0;
        for (int i = inicioMemoriaVirtual; i < tamanoTotal; i++) {
            if (ocupadaVirtual[i]) {
                libresSeguidas = 0;
            } else {
                libresSeguidas++;
            }
            if (tamano > 0 && libresSeguidas == tamano) {
                int base = i - tamano + 1;
                datos[base] = tamano;
                nombresArchivo[base] = nombre;
                for (int j = 0; j < tamano; j++) {
                    contenido[base + j] = programa.get(j);
                    ocupadaVirtual[base + j] = true;
                }
                return true;
            }
        }
        return false;
    }

    /*
     * Nombre: leerDeMemoriaVirtual
     * Descripción: Lee un programa que está en la memoria virtual. Devuelve null si no está.
     */
    public List<Instruccion> leerDeMemoriaVirtual(String nombre) {
        int base = buscarEnMemoriaVirtual(nombre);
        if (base == -1) {
            return null;
        }
        List<Instruccion> programa = new ArrayList<>();
        for (int i = 0; i < datos[base]; i++) {
            programa.add(contenido[base + i]);
        }
        return programa;
    }

    /*
     * Nombre: liberarMemoriaVirtual
     * Descripción: Swap-in terminado: libera el bloque que ocupaba el programa en la memoria virtual.
     */
    public void liberarMemoriaVirtual(String nombre) {
        int base = buscarEnMemoriaVirtual(nombre);
        if (base == -1) {
            return;
        }
        int tamano = datos[base];
        for (int i = base; i < base + tamano; i++) {
            ocupadaVirtual[i] = false;
            contenido[i] = null;
        }
        datos[base] = 0;
        nombresArchivo[base] = null;
    }

    public boolean estaEnMemoriaVirtual(String nombre) {
        return buscarEnMemoriaVirtual(nombre) != -1;
    }

    private int buscarEnMemoriaVirtual(String nombre) {
        for (int i = inicioMemoriaVirtual; i < tamanoTotal; i++) {
            if (nombre.equals(nombresArchivo[i])) {
                return i;
            }
        }
        return -1;
    }
    
    
}