package com.minipc.t1sominipc.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Nombre: Planificador
 * Descripción: Planificador FCFS. Decide el orden: lleva la Lista de Trabajo (todos los programas
 * cargados) y la Lista de Procesos (los que ya tienen BCP), y le pide al Dispatcher que ejecute
 * la admisión, el swap-in y la liberación.
 */
public class Planificador {

    private Memoria memoria;
    private Disco disco;
    private ListaTrabajo listaTrabajo;
    private ListaProcesos listaProcesos;
    private Dispatcher dispatcher;
    private List<String> esperandoLista; // ya están en Disco, esperan lugar en la Lista de Trabajo

    public Planificador(Memoria memoria, Disco disco) {
        this.memoria = memoria;
        this.disco = disco;
        this.listaProcesos = new ListaProcesos(memoria);
        this.listaTrabajo = new ListaTrabajo(memoria, listaProcesos.getFinZonaBCP() + 1);
        this.dispatcher = new Dispatcher();
        this.esperandoLista = new ArrayList<>();
    }

    /*
        * Nombre: getMaximoProcesos
        *Entrada: void
        *Salida: int
        *Descripción: Devuelve cuántos BCP caben en el kernel configurado (tope duro de 5).
     */
    public int getMaximoProcesos() {
        return listaProcesos.getMaximo();
    }

    /*
        * Nombre: registrarTrabajo
        * Descripción: Pasa a la Lista de Trabajo un programa ya guardado en Disco. Si la lista está
        * llena, el programa espera en Disco hasta que se libere un lugar.
        * Salida: false si no existe en Disco o ya estaba registrado.
     */
    public boolean registrarTrabajo(String nombreArchivo) {
        if (!disco.existeArchivo(nombreArchivo) || listaTrabajo.buscar(nombreArchivo) != -1
                || esperandoLista.contains(nombreArchivo)) {
            return false;
        }
        if (!listaTrabajo.agregar(nombreArchivo, disco.getTamanoArchivo(nombreArchivo),
                disco.getDireccionArchivo(nombreArchivo))) {
            esperandoLista.add(nombreArchivo);
        }
        return true;
    }

    // FCFS: los que esperaban en Disco entran a la Lista de Trabajo conforme se libera espacio.
    private void pasarEsperandoALista() {
        while (!esperandoLista.isEmpty() && listaTrabajo.hayEspacio()) {
            String nombre = esperandoLista.remove(0);
            listaTrabajo.agregar(nombre, disco.getTamanoArchivo(nombre), disco.getDireccionArchivo(nombre));
        }
    }

    public List<String> getEsperandoLista() {
        return esperandoLista;
    }

    /*
        * Nombre: admitirPendientes
        * Descripción: Recorre los trabajos sin BCP en orden de llegada y los admite (dos pasos)
        * hasta que no quepa otro BCP; el resto espera en la Lista de Trabajo.
        * Salida: los BCP creados.
     */
    public List<BCP> admitirPendientes() {
        List<BCP> nuevos = new ArrayList<>();
        for (String nombre : listaTrabajo.getNombresSinBCP()) {
            BCP bcp = dispatcher.admitir(nombre, listaTrabajo, listaProcesos, memoria, disco);
            if (bcp == null) {
                break; // FCFS: si el primero no tiene BCP, los demás tampoco pasan
            }
            nuevos.add(bcp);
        }
        return nuevos;
    }

    /*
        * Nombre: solicitarAdmision
        * Descripción: Registra un programa guardado en Disco y trata de admitirlo.
        * Salida: el BCP si se creó (puede quedar "Preparado" o "EnEspera" de RAM),
        * o null si sigue esperando espacio de BCP en la Lista de Trabajo.
     */
    public BCP solicitarAdmision(String nombreArchivo) {
        if (!registrarTrabajo(nombreArchivo)) {
            return null;
        }
        admitirPendientes();

        int pid = listaTrabajo.getPID(listaTrabajo.buscar(nombreArchivo));
        for (BCP bcp : listaProcesos.getTodos()) {
            if (bcp.getPID() == pid) {
                return bcp;
            }
        }
        return null;
    }

    /*
        * Nombre: liberarProceso
        * Descripción: Se llama cuando un proceso termina. Libera sus recursos, hace swap-in de
        * los procesos "EnEspera" que ya quepan en RAM y admite al siguiente trabajo sin BCP.
        * Salida: el BCP nuevo si se admitió un trabajo, o null.
     */
    public BCP liberarProceso(BCP finalizado) {
        dispatcher.liberar(finalizado, listaTrabajo, listaProcesos, memoria);
        pasarEsperandoALista();

        for (BCP bcp : listaProcesos.getTodos()) {
            dispatcher.swapIn(bcp, listaTrabajo, memoria, disco);
        }

        List<BCP> nuevos = admitirPendientes();
        if (nuevos.isEmpty()) {
            return null;
        }
        return nuevos.get(0);
    }

    /*
        * Nombre: siguienteProceso
        *Entrada: void
        *Salida: BCP
        *Descripción: Devuelve el primer proceso "Preparado" en orden de llegada (FCFS).
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
        *Descripción: Nombres de los trabajos que aún no tienen BCP (alias de la parte pendiente
        * de la Lista de Trabajo).
     */
    public List<String> getColaEspera() {
        return listaTrabajo.getNombresSinBCP();
    }

    public ListaTrabajo getListaTrabajo() {
        return listaTrabajo;
    }

    /*
        * Nombre: getListaProcesos
        *Entrada: void
        *Salida: ListaProcesos
        *Descripción: Devuelve los procesos que ya tienen BCP.
     */
    public ListaProcesos getListaProcesos() {
        return listaProcesos;
    }
}
