package com.minipc.t1sominipc.model;

import java.util.ArrayList;
import java.util.List;

public class Planificador {

    private Memoria memoria;
    private Disco disco;
    private ListaProcesos listaProcesos;
    private List<String> colaEspera; // nombres de archivos en Disco, esperando admisión
    private List<Integer> direccionesBCPLibres; // direcciones de kernel de BCP ya finalizados, listas para reutilizar
    private int siguienteDireccionBCPNueva; // próxima dirección de kernel no usada por un BCP
    private int siguientePID;

    public Planificador(Memoria memoria, Disco disco) {
        this.memoria = memoria;
        this.disco = disco;
        this.listaProcesos = new ListaProcesos();
        this.colaEspera = new ArrayList<>();
        this.direccionesBCPLibres = new ArrayList<>();
        this.siguienteDireccionBCPNueva = 0;
        this.siguientePID = 1;
    }

    /*
        * Nombre: getMaximoProcesos
        *Entrada: void
        *Salida: int
        *Descripción: Devuelve el número máximo de procesos que se pueden admitir en memoria.
     */

    public int getMaximoProcesos() {
        int espacioKernel = memoria.getFinMemoriaKernel() + 1;
        int caben = espacioKernel / BCP.getTamanoBCP();
        int min = 5;
        if(min > caben) {
            min = caben;
        }
        return min;
    }

    /*
        * Nombre: solicitarAdmision
        * Descripción: Intenta admitir un programa ya guardado en Disco.
        * Entrada: String nombreArchivo
        * Salida: Si no hay espacio (BCP o memoria de usuario), queda en cola de espera.
        * Devuelve el BCP si se admitió, o null si quedó esperando.
     */
    public BCP solicitarAdmision(String nombreArchivo) {
        List<Instruccion> programa = disco.leerArchivo(nombreArchivo);
        if (programa == null) {
            return null;
        }

        if (!hayDireccionDisponibleParaBCP()) {
            colaEspera.add(nombreArchivo);
            return null;
        }

        int baseUsuario = memoria.asignarBloque(programa.size());
        if (baseUsuario == -1) {
            colaEspera.add(nombreArchivo);
            return null;
        }

        return crearYAdmitir(nombreArchivo, programa, baseUsuario);
    }

    /*
        * Nombre: hayDireccionDisponibleParaBCP
        *Entrada: void
        *Salida: boolean
        *Descripción: Indica si hay una dirección de kernel libre (reciclada o nueva) para
        * guardar un BCP más, respetando el límite de getMaximoProcesos().
     */
    private boolean hayDireccionDisponibleParaBCP() {
        if (listaProcesos.getCantidad() >= getMaximoProcesos()) {
            return false;
        }
        if (!direccionesBCPLibres.isEmpty()) {
            return true;
        }
        return siguienteDireccionBCPNueva + BCP.getTamanoBCP() - 1 <= memoria.getFinMemoriaKernel();
    }

    /*
        * Nombre: obtenerDireccionParaNuevoBCP
        *Entrada: void
        *Salida: int
        *Descripción: Reutiliza la dirección de un BCP ya finalizado si hay alguna disponible;
        * si no, entrega la siguiente dirección de kernel nunca antes usada.
     */
    private int obtenerDireccionParaNuevoBCP() {
        if (!direccionesBCPLibres.isEmpty()) {
            return direccionesBCPLibres.remove(0);
        }
        int direccion = siguienteDireccionBCPNueva;
        siguienteDireccionBCPNueva += BCP.getTamanoBCP();
        return direccion;
    }

    /*
        * Nombre: crearYAdmitir
        *Entrada: String nombreArchivo, List<Instruccion> programa, int baseUsuario
        *Salida: BCP
        *Descripción: Crea un nuevo BCP para el programa y lo admite en la lista de procesos.
     */
    private BCP crearYAdmitir(String nombreArchivo, List<Instruccion> programa, int baseUsuario) {
        int direccionBase = obtenerDireccionParaNuevoBCP();
        memoria.cargarPrograma(programa, baseUsuario);

        BCP nuevoBcp = new BCP(memoria, direccionBase, siguientePID, baseUsuario, programa.size(), 0);
        siguientePID++;
        nuevoBcp.actualizarEstado("Preparado");
        listaProcesos.agregar(nuevoBcp);
        return nuevoBcp;
    }

    /*
        * Nombre: liberarProceso
        * Descripción: Se llama cuando un proceso termina. Intenta admitir
        * automáticamente al siguiente de la cola de espera, si hay alguno.
     */
    public BCP liberarProceso(BCP finalizado) {
        listaProcesos.eliminar(finalizado);
        direccionesBCPLibres.add(finalizado.getDireccionBase());

        if (colaEspera.isEmpty()) {
            return null;
        }

        String siguienteNombre = colaEspera.remove(0);
        return solicitarAdmision(siguienteNombre);
    }

    /*
        * Nombre: siguienteProceso
        *Entrada: void
        *Salida: BCP
        *Descripción: Devuelve el siguiente proceso en estado "Preparado".
     */
    public BCP siguienteProceso() {
        for (BCP bcp : listaProcesos.getTodos()) {
            if ("Preparado".equals(bcp.getEstado())) {
                return bcp;
            }
        }
        return null;
    }

    /*
        * Nombre: getColaEspera
        *Entrada: void
        *Salida: List<String>
        *Descripción: Devuelve la cola de espera de procesos.
     */

    public List<String> getColaEspera() {
        return colaEspera;
    }

    /*
        * Nombre: getListaProcesos
        *Entrada: void
        *Salida: ListaProcesos
        *Descripción: Devuelve la lista de procesos actualmente en el planificador.
     */
    public ListaProcesos getListaProcesos() {
        return listaProcesos;
    }
}