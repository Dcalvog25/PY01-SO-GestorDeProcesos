package com.minipc.t1sominipc.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Nombre: ListaTrabajo
 * Descripción: Todos los programas cargados, tengan o no BCP. Vive en el kernel de Memoria,
 * en la zona que queda después de los BCP. Cada entrada ocupa tam_entrada posiciones reales
 * del arreglo int[], el nombre (texto) va en un arreglo paralelo, igual que en Disco.
 */
public class ListaTrabajo {

    private static final int NOMBRE = 0;    // guarda el número de trabajo; el texto está en nombres[]
    private static final int TAMANO = 1;
    private static final int DIR_DISCO = 2;
    private static final int PID = 3;       // 0 = todavía sin BCP

    private static final int TAM_ENTRADA = 4;

    private Memoria memoria;
    private String[] nombres; // mismo tamaño que la memoria, se consulta con la misma dirección
    private int inicio;       // primera dirección de kernel de esta lista
    private int fin;          // última dirección de kernel de esta lista
    private int cantidad;
    private int siguienteNumero;

    public ListaTrabajo(Memoria memoria, int inicio) {
        this.memoria = memoria;
        this.inicio = inicio;
        this.fin = memoria.getFinMemoriaKernel();
        this.nombres = new String[memoria.getTamanoTotal()];
        this.cantidad = 0;
        this.siguienteNumero = 1;
    }

    /*
        * Nombre: agregar
        * Descripción: Agrega un trabajo al final de la lista (orden FCFS) sin BCP asignado.
        * Salida: false si no queda espacio de kernel para otra entrada.
        * Entrada: String nombre, int tamano, int direccionDisco
     */
    public boolean agregar(String nombre, int tamano, int direccionDisco) {
        if (!hayEspacio() || buscar(nombre) != -1) {
            return false;
        }
        int dir = direccionDeEntrada(cantidad);
        escribirEntrada(dir, siguienteNumero, nombre, tamano, direccionDisco, 0);
        siguienteNumero++;
        cantidad++;
        return true;
    }

    /*
        * Nombre: eliminar
        * Descripción: Quita un trabajo y compacta la lista, como Disco.eliminarArchivo.
        * Entrada: String nombre
        * Salida: void
     */
    public void eliminar(String nombre) {
        int indice = buscar(nombre);
        if (indice == -1) {
            return;
        }
        for (int i = indice; i < cantidad - 1; i++) {
            int destino = direccionDeEntrada(i);
            int origen = direccionDeEntrada(i + 1);
            escribirEntrada(destino, memoria.leer(origen + NOMBRE), nombres[origen],
                    memoria.leer(origen + TAMANO), memoria.leer(origen + DIR_DISCO), memoria.leer(origen + PID));
        }
        limpiarEntrada(direccionDeEntrada(cantidad - 1));
        cantidad--;
    }

    /*
        * Nombre: hayEspacio
        * Descripción: Comprueba si hay espacio de kernel disponible para otra entrada.
        * Entrada: void
        * Salida: boolean - true si hay espacio, false en caso contrario.
     */
    public boolean hayEspacio() {
        return direccionDeEntrada(cantidad) + TAM_ENTRADA - 1 <= fin;
    }
    /*
        * Nombre: getCapacidad
        * Descripción: Devuelve la capacidad máxima de la lista de trabajos en entradas de kernel.
        * Entrada: void
        * Salida: int - capacidad máxima de la lista.
     */
    public int getCapacidad() {
        return (fin - inicio + 1) / TAM_ENTRADA;
    }

    /*
        * Nombre: getCantidad
        * Descripción: Devuelve la cantidad de trabajos actualmente en la lista.
        * Entrada: void
        * Salida: int - número de trabajos en la lista.
     */
    public int getCantidad() {
        return cantidad;
    }

    /*
        * Nombre: buscar
        * Descripción: Busca un trabajo por su nombre y devuelve su índice en la lista.
        * Entrada: String nombre
        * Salida: int - índice del trabajo, o -1 si no se encuentra.
     */
    public int buscar(String nombre) {
        for (int i = 0; i < cantidad; i++) {
            if (nombres[direccionDeEntrada(i)].equals(nombre)) {
                return i;
            }
        }
        return -1;
    }

    /*
        * Nombre: buscarPorPID
        * Descripción: Busca un trabajo por su PID y devuelve su índice en la lista.
        * Entrada: int pid
        * Salida: int - índice del trabajo, o -1 si no se encuentra.
     */
    public int buscarPorPID(int pid) {
        for (int i = 0; i < cantidad; i++) {
            if (getPID(i) == pid) {
                return i;
            }
        }
        return -1;
    }

    /*
        * Nombre: getNombre
        * Descripción: Devuelve el nombre del trabajo en el índice especificado.
        * Entrada: int indice
        * Salida: String - nombre del trabajo.
     */
    public String getNombre(int indice) {
        return nombres[direccionDeEntrada(indice)];
    }

    /*
        * Nombre: getTamano
        * Descripción: Devuelve el tamaño del trabajo en el índice especificado.
        * Entrada: int indice
        * Salida: int - tamaño del trabajo.
     */
    public int getTamano(int indice) {
        return memoria.leer(direccionDeEntrada(indice) + TAMANO);
    }

    /*
        * Nombre: getDireccionDisco
        * Descripción: Devuelve la dirección en disco del trabajo en el índice especificado.
        * Entrada: int indice
        * Salida: int - dirección en disco del trabajo.
     */
    public int getDireccionDisco(int indice) {
        return memoria.leer(direccionDeEntrada(indice) + DIR_DISCO);
    }

    /*
        * Nombre: getPID
        * Descripción: Devuelve el PID del trabajo en el índice especificado.
        * Entrada: int indice
        * Salida: int - PID del trabajo.
     */
    public int getPID(int indice) {
        return memoria.leer(direccionDeEntrada(indice) + PID);
    }

    /*
        * Nombre: setPID
        * Descripción: Establece el PID del trabajo en el índice especificado.
        * Entrada: int indice, int pid
        * Salida: void
     */
    public void setPID(int indice, int pid) {
        memoria.escribir(direccionDeEntrada(indice) + PID, pid, "Trabajo.PID");
    }

    /*
        * Nombre: getNombresSinBCP
        * Descripción: Nombres de los trabajos que aún no tienen BCP, en orden de llegada.
        * Entrada: void
        * Salida: List<String> - lista de nombres de los trabajos sin BCP.
     */
    public List<String> getNombresSinBCP() {
        List<String> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            if (getPID(i) == 0) {
                lista.add(getNombre(i));
            }
        }
        return lista;
    }

    /*
        * Nombre: getInicio
        * Descripción: Devuelve la dirección de inicio de la lista de trabajos.
        * Entrada: void
        * Salida: int - dirección de inicio.
     */
    public int getInicio() {
        return inicio;
    }

    /*
        * Nombre: getFin
        * Descripción: Devuelve la dirección de fin de la lista de trabajos.
        * Entrada: void
        * Salida: int - dirección de fin.
     */
    public int getFin() {
        return fin;
    }

    /*
        * Nombre: getTamanoEntrada
        * Descripción: Devuelve el tamaño de cada entrada en la lista de trabajos.
        * Entrada: void
        * Salida: int - tamaño de cada entrada.
     */

    public static int getTamanoEntrada() {
        return TAM_ENTRADA;
    }

    /*
        * Nombre: direccionDeEntrada
        * Descripción: Calcula la dirección de memoria de la entrada en el índice especificado.
        * Entrada: int indice
        * Salida: int - dirección de la entrada.
     */ 
    private int direccionDeEntrada(int indice) {
        return inicio + indice * TAM_ENTRADA;
    }

    /*
        * Nombre: escribirEntrada
        * Descripción: Escribe los datos de una entrada en la memoria.
        * Entrada: int dir, int numero, String nombre, int tamano, int direccionDisco, int pid
        * Salida: void
     */
    private void escribirEntrada(int dir, int numero, String nombre, int tamano, int direccionDisco, int pid) {
        memoria.escribir(dir + NOMBRE, numero, "Trabajo.Nombre");
        memoria.escribir(dir + TAMANO, tamano, "Trabajo.Tamano");
        memoria.escribir(dir + DIR_DISCO, direccionDisco, "Trabajo.DirDisco");
        memoria.escribir(dir + PID, pid, "Trabajo.PID");
        nombres[dir] = nombre;
    }


    /*
        * Nombre: limpiarEntrada
        * Descripción: Limpia los datos de una entrada en la memoria.
        * Entrada: int dir
        * Salida: void
     */
    private void limpiarEntrada(int dir) {
        for (int i = 0; i < TAM_ENTRADA; i++) {
            memoria.escribir(dir + i, 0, "");
        }
        nombres[dir] = null;
    }
}
