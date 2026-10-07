# Tarea 1: Principios de Sistemas Operativos

## Mini PC - Gestor de Procesos

**Estudiante:** David Calvo Garcia 2024122451  
**Curso:** IC-6600 Principios de Sistemas Operativos  
**Tecnologia:** Java y Swing  
**IDE:** NetBeans  
**Estado del proyecto:** Excelente

### Video de demostracion

[Ver el video de la aplicacion en ejecucion](https://youtu.be/8e_jOAPWHno)

## Descripcion del proyecto

Mini PC es un simulador de una computadora y de los componentes basicos de un sistema operativo. La aplicacion permite cargar uno o varios programas escritos en un mini ensamblador, validarlos, almacenarlos en disco y ejecutarlos mediante una CPU simulada.

El proyecto representa el ciclo **fetch-decode-execute** y permite observar en tiempo real la CPU, los registros, la memoria principal, los BCP, la cola de procesos, el disco, la memoria virtual, la pantalla y los estados de los procesos.

La implementacion se basa en los conceptos estudiados en *Operating Systems: Internals and Design Principles*, de William Stallings.

## Objetivos alcanzados

| Objetivo | Estado | Implementacion |
|---|:---:|---|
| Interfaz grafica para monitorear el sistema | Alcanzado | Interfaz Swing con paneles de CPU, registros, memoria, procesos, disco y consola. |
| Carga de uno o varios archivos `.asm` | Alcanzado | Selector de archivos con soporte para multiples programas. |
| Validacion de sintaxis | Alcanzado | `ConvertidorASM` valida operadores, registros, argumentos, valores y codigos de interrupcion. |
| CPU y ciclo de ejecucion | Alcanzado | Registros AX, BX, CX, DX, AC, PC e IR; ejecucion paso a paso y automatica. |
| Pesos de las instrucciones | Alcanzado | Cada instruccion consume los ticks definidos en el enunciado. |
| BCP (Bloque de Control de Proceso) | Alcanzado | El BCP almacena PID, estado, registros, PC, base, tamano, pila, tiempos y archivos abiertos. |
| Estados de los procesos | Alcanzado | Se muestran estados como Preparado, Ejecutando, EnEspera, Suspendido y Finalizado. |
| Lista de trabajos | Alcanzado | Los trabajos se administran en el area de kernel y se liberan al crear su BCP. |
| Planificador FCFS | Alcanzado | Los procesos se admiten y ejecutan en orden de llegada. |
| Dispatcher y cambios de contexto | Alcanzado | El Dispatcher guarda y restaura el contexto de los procesos. |
| Administracion de memoria | Alcanzado | La RAM se divide en kernel y usuario, con asignacion y liberacion de bloques. |
| Memoria virtual | Alcanzado | Los procesos que no caben en RAM esperan en el area virtual del disco. |
| Interrupcion INT 09H | Alcanzado | Solicita un valor numerico del teclado y lo guarda en DX. |
| Interrupcion INT 10H | Alcanzado | Imprime el contenido de DX en la pantalla simulada y permanece EnEspera durante sus ticks. |
| Interrupcion INT 21H | Alcanzado | Permite crear, abrir, leer, escribir y eliminar archivos. |
| Proteccion de memoria | Alcanzado | Se valida que los saltos y el PC permanezcan dentro del espacio del proceso. |
| Proteccion de pila | Alcanzado | Se detectan overflow y underflow de la pila de cinco posiciones. |
| Mensajes de seguridad | Alcanzado | Los errores de seguridad se imprimen en pantalla como `Segmentation Fault`. |
| Estadisticas de procesos | Alcanzado | Se registra PID, proceso, hora de inicio, hora de finalizacion y duracion. |
| Configuracion externa | Alcanzado | RAM, disco, kernel y memoria virtual se configuran desde `config.json`. |

## Requisitos

- JDK 26, de acuerdo con la version configurada en `pom.xml`.
- NetBeans.
- Maven, si se desea compilar desde la terminal.

## Como ejecutar el proyecto

1. Abrir NetBeans.
2. Seleccionar **File > Open Project** y abrir la carpeta `programa/T1SOMiniPC`.
3. Verificar que el proyecto use un JDK compatible.
4. Ejecutar la clase `com.minipc.t1sominipc.T1SOMiniPC`.
5. Presionar **Cargar Archivos** y seleccionar uno o varios archivos `.asm`.
6. Usar **Paso a Paso** para avanzar un tick de CPU por vez o **Ejecutar Automatico** para ejecutar los procesos hasta finalizar.

Desde Maven, el proyecto puede compilarse con:

```bash
mvn clean package
```

## Configuracion

La configuracion se encuentra en `programa/T1SOMiniPC/config.json`:

```json
{
	"memoriaRAM": 256,
	"porcentajeKernel": 25,
	"disco": 512,
	"porcentajeMemoriaVirtual": 12.5
}
```

La aplicacion calcula automaticamente el tamano del kernel y de la memoria virtual a partir de estos porcentajes.

## Instrucciones soportadas

| Instruccion | Ejemplo | Descripcion |
|---|---|---|
| `LOAD` | `LOAD AX` | Carga un registro en AC. |
| `STORE` | `STORE BX` | Guarda AC en un registro. |
| `MOV` | `MOV AX, 5` | Mueve un valor o registro a otro registro. |
| `MOV` texto | `MOV DX, "datos.txt"` | Guarda una cadena en DX para operaciones de archivos o pantalla. |
| `ADD` | `ADD BX` | Suma el registro a AC. |
| `SUB` | `SUB BX` | Resta el registro a AC. |
| `INC` | `INC AX` | Incrementa AC o un registro. |
| `DEC` | `DEC AX` | Decrementa AC o un registro. |
| `SWAP` | `SWAP AX, BX` | Intercambia dos registros. |
| `CMP` | `CMP AX, BX` | Compara dos registros y actualiza la bandera cero. |
| `JMP` | `JMP +2` | Salta a una posicion relativa. |
| `JE` / `JNE` | `JE -1` | Salta dependiendo de la bandera cero. |
| `PARAM` | `PARAM 5, 10` | Guarda parametros numericos en la pila. |
| `PUSH` | `PUSH AX` | Inserta un registro en la pila. |
| `POP` | `POP AX` | Extrae un valor de la pila. |
| `INT 09H` | `INT 09H` | Lee un valor numerico del teclado y lo guarda en DX. |
| `INT 10H` | `INT 10H` | Imprime DX en la pantalla. |
| `INT 20H` | `INT 20H` | Finaliza el proceso. |
| `INT 21H` | `INT 21H` | Ejecuta operaciones de archivos usando AH y DX. |

Registros validos: `AX`, `BX`, `CX` y `DX`.

### Ejemplo de programa

```asm
MOV DX, "Hola Mini PC"
INT 10H
MOV AX, 5
MOV BX, 3
LOAD AX
ADD BX
STORE CX
INT 20H
```

## Interrupcion INT 21H

El registro `AH` determina la operacion y `DX` contiene el nombre del archivo cuando corresponde:

| AH | Operacion |
|---|---|
| `3CH` | Crear archivo |
| `3DH` | Abrir archivo |
| `4DH` | Leer archivo |
| `40H` | Escribir archivo |
| `41H` | Eliminar archivo |

Las interrupciones de entrada/salida se muestran en estado **EnEspera** durante todos los ticks de su peso. `INT 10H` consume 2 ticks y `INT 21H` consume 5 ticks.

## Proteccion y seguridad

El simulador incluye las siguientes validaciones:

- **Salto invalido:** un `JMP`, `JE` o `JNE` no puede salir del rango asignado al proceso.
- **Proteccion de memoria:** el PC no puede invadir la memoria de otro proceso.
- **Stack overflow:** se detecta cuando la pila supera sus cinco posiciones.
- **Stack underflow:** se detecta cuando se intenta hacer `POP` con la pila vacia.
- **Segmentation Fault:** los errores de seguridad se muestran en la pantalla simulada y el proceso se finaliza.

## Estructura del proyecto

```text
programa/T1SOMiniPC/
├── config.json
├── pom.xml
└── src/
		├── main/java/com/minipc/t1sominipc/
		│   ├── T1SOMiniPC.java
		│   ├── controller/
		│   │   └── ControllerMiniPC.java
		│   ├── model/
		│   │   ├── BCP.java
		│   │   ├── CPU.java
		│   │   ├── ConvertidorASM.java
		│   │   ├── Configuracion.java
		│   │   ├── Disco.java
		│   │   ├── Dispatcher.java
		│   │   ├── Estadistica.java
		│   │   ├── Instruccion.java
		│   │   ├── ListaProcesos.java
		│   │   ├── ListaTrabajo.java
		│   │   ├── Memoria.java
		│   │   ├── Pantalla.java
		│   │   ├── PilaBCP.java
		│   │   ├── Planificador.java
		│   │   └── TablaArchivosKernel.java
		│   └── view/
		│       └── MiniPCFrame.java
		└── test/java/
```

### Responsabilidad de los componentes principales

- `CPU`: ejecuta instrucciones, administra registros, ticks, interrupciones y validaciones de seguridad.
- `BCP`: representa el estado de cada proceso y almacena su contexto de ejecucion.
- `Memoria`: administra las zonas de kernel y usuario de la RAM.
- `Disco`: almacena los programas, el indice de archivos y la memoria virtual.
- `ListaTrabajo`: administra los trabajos pendientes en el kernel.
- `ListaProcesos`: mantiene los procesos que ya tienen BCP.
- `Planificador`: coordina la admision y el orden FCFS.
- `Dispatcher`: realiza admision, liberacion, swap y cambios de contexto.
- `ControllerMiniPC`: conecta la interfaz con el modelo y actualiza las tablas.
- `MiniPCFrame`: contiene la interfaz grafica Swing.

## Estados de los procesos

El ciclo general de un proceso es:

```text
Nuevo -> Preparado -> Ejecutando -> Finalizado
												 |
												 +-> EnEspera (interrupciones y entrada)
												 +-> Suspendido (espera de memoria RAM)
```

Los procesos que no caben inicialmente en la memoria principal permanecen en memoria virtual hasta que se libera espacio.


## Autor

David Calvo Garcia - 2024122451
