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

    public boolean hayEspacio() {
        return direccionDeEntrada(cantidad) + TAM_ENTRADA - 1 <= fin;
    }

    public int getCapacidad() {
        return (fin - inicio + 1) / TAM_ENTRADA;
    }

    public int getCantidad() {
        return cantidad;
    }

    public int buscar(String nombre) {
        for (int i = 0; i < cantidad; i++) {
            if (nombres[direccionDeEntrada(i)].equals(nombre)) {
                return i;
            }
        }
        return -1;
    }

    public int buscarPorPID(int pid) {
        for (int i = 0; i < cantidad; i++) {
            if (getPID(i) == pid) {
                return i;
            }
        }
        return -1;
    }

    public String getNombre(int indice) {
        return nombres[direccionDeEntrada(indice)];
    }

    public int getTamano(int indice) {
        return memoria.leer(direccionDeEntrada(indice) + TAMANO);
    }

    public int getDireccionDisco(int indice) {
        return memoria.leer(direccionDeEntrada(indice) + DIR_DISCO);
    }

    public int getPID(int indice) {
        return memoria.leer(direccionDeEntrada(indice) + PID);
    }

    public void setPID(int indice, int pid) {
        memoria.escribir(direccionDeEntrada(indice) + PID, pid, "Trabajo.PID");
    }

    /*
        * Nombre: getNombresSinBCP
        * Descripción: Nombres de los trabajos que aún no tienen BCP, en orden de llegada.
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

    public int getInicio() {
        return inicio;
    }

    public int getFin() {
        return fin;
    }

    public static int getTamanoEntrada() {
        return TAM_ENTRADA;
    }

    private int direccionDeEntrada(int indice) {
        return inicio + indice * TAM_ENTRADA;
    }

    private void escribirEntrada(int dir, int numero, String nombre, int tamano, int direccionDisco, int pid) {
        memoria.escribir(dir + NOMBRE, numero, "Trabajo.Nombre");
        memoria.escribir(dir + TAMANO, tamano, "Trabajo.Tamano");
        memoria.escribir(dir + DIR_DISCO, direccionDisco, "Trabajo.DirDisco");
        memoria.escribir(dir + PID, pid, "Trabajo.PID");
        nombres[dir] = nombre;
    }

    private void limpiarEntrada(int dir) {
        for (int i = 0; i < TAM_ENTRADA; i++) {
            memoria.escribir(dir + i, 0, "");
        }
        nombres[dir] = null;
    }
}
