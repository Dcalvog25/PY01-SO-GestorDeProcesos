package com.minipc.t1sominipc.model;

import java.util.List;

/*
 * Nombre: Dispatcher
 * Descripción: Ejecuta las acciones sobre los procesos: la admisión en dos pasos, el swap-in /
 * swap-out contra la memoria virtual del Disco, la liberación al terminar y el cambio de
 * contexto hacia la CPU. El Planificador decide quien y cuando, el Dispatcher hace el como.
 *
 */
public class Dispatcher {

    /*
        * Nombre: admitir
        * Descripción: Admisión de un trabajo en dos pasos secuenciales.
        * El trabajo se elimina de la Lista de Trabajo una vez que se crea su BCP.
        * Entrada: nombre del trabajo, listas de trabajos y procesos, memoria y disco.
        * Salida: el BCP creado, o null si falló 
     */
    public BCP admitir(String nombre, ListaTrabajo trabajos, ListaProcesos procesos, Memoria memoria, Disco disco) {
        int indice = trabajos.buscar(nombre);
        if (indice == -1 || trabajos.getPID(indice) != 0) {
            return null; // no existe o ya tiene BCP
        }

        // espacio de BCP en el kernel
        if (!procesos.hayEspacioParaBCP()) {
            return null;
        }
        BCP bcp = procesos.crearBCP(trabajos.getTamano(indice));
        bcp.setNombreArchivo(nombre); // guardar el nombre en el BCP
        trabajos.setPID(indice, bcp.getPID());
        
        // Eliminar el trabajo de la Lista de Trabajo una vez que tiene BCP asignado
        // Esto libera espacio para que otros trabajos entren a la lista
        trabajos.eliminar(nombre);

        // espacio del programa en RAM
        List<Instruccion> programa = disco.leerArchivo(nombre);
        int base = memoria.asignarBloque(programa.size());
        if (base == -1) {
            swapOut(nombre, programa, disco);
            bcp.actualizarEstado("Suspendido");
        } else {
            memoria.cargarPrograma(programa, base);
            bcp.asignarBase(base);
            bcp.actualizarEstado("Preparado");
        }
        return bcp;
    }

    /*
        * Nombre: swapOut
        * Descripción: Deja el programa en la memoria virtual del Disco. Si tampoco cabe ahí,
        * sigue disponible en su archivo del Disco y el swap-in lo leerá de allí.
     */
    private void swapOut(String nombre, List<Instruccion> programa, Disco disco) {
        disco.guardarEnMemoriaVirtual(nombre, programa);
    }

    /*
        * Nombre: swapIn
        * Descripción: Si hay RAM para un proceso "EnEspera", lo carga y pasa a "Preparado".
        * Salida: true si se hizo el swap-in.
     */
    public boolean swapIn(BCP bcp, ListaTrabajo trabajos, Memoria memoria, Disco disco) {
        if (!"EnEspera".equals(bcp.getEstado())) {
            return false;
        }
        String nombre = trabajos.getNombre(trabajos.buscarPorPID(bcp.getPID()));

        List<Instruccion> programa = disco.leerDeMemoriaVirtual(nombre);
        if (programa == null) {
            programa = disco.leerArchivo(nombre);
        }

        int base = memoria.asignarBloque(programa.size());
        if (base == -1) {
            return false;
        }
        memoria.cargarPrograma(programa, base);
        disco.liberarMemoriaVirtual(nombre);
        bcp.asignarBase(base);
        bcp.actualizarEstado("Preparado");
        return true;
    }

    /*
        * Nombre: liberar
        * Descripción: Un proceso terminó: devuelve su RAM y su espacio de BCP.
        * El trabajo ya fue eliminado de ListaTrabajo cuando se admitió (ver admitir()),
        * así que solo se libera la RAM y el BCP.
     */
    public void liberar(BCP bcp, ListaTrabajo trabajos, ListaProcesos procesos, Memoria memoria) {
        if (bcp.getBase() != -1) {
            memoria.liberarBloque(bcp.getBase(), bcp.getTamano());
        }
        // El trabajo ya fue eliminado durante la admisión, no hay que eliminarlo aquí
        procesos.eliminar(bcp);
    }

    /*
        * Nombre: cambiarContexto
        * Descripción: Pone al proceso entrante en la CPU, en estado "Ejecutando". Con FCFS no hay
        * apropiación: si el saliente sigue "Ejecutando" (incluso esperando INT 09H) no se cambia.
        * Salida: true si se hizo el cambio.
     */
    public boolean cambiarContexto(CPU cpu, BCP saliente, BCP entrante) {
        if (saliente != null && "Ejecutando".equals(saliente.getEstado())) {
            return false;
        }
        if (entrante == null || !"Preparado".equals(entrante.getEstado())) {
            return false;
        }

        entrante.actualizarEstado("Ejecutando");
        if (entrante.getTiempoInicio() == -1) {
            entrante.registrarInicio(BCP.minutoDelDia());
        }
        cpu.asignarProceso(entrante);
        return true;
    }
}
