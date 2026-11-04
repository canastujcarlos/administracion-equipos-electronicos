package main.java.com.aliprinsa.administracion.gusto.model;

import java.time.LocalDate;

/**
 * Equipo del inventario (equivale a una fila de la plantilla de levantamiento).
 * Los nombres (tipo, marca, sede, responsable...) solo se llenan al LEER;
 * al guardar se usan unicamente los ids.
 */
public record Equipo(
        Integer id,
        String codigoInventario,
        int tipoId, String tipo,
        int marcaId, String marca,
        String modelo,
        String numeroSerie,
        String hostname,
        String macAddress,
        String ipAddress,
        Integer ubicacionId, String ubicacion,
        String sede,                       // solo lectura: sale de la ubicacion
        int estadoFisicoId, String estadoFisico,
        int estadoFuncionalId, String estadoFuncional,
        boolean requiereAtencion,
        LocalDate fechaLevantamiento,
        LocalDate fechaCompra,
        LocalDate garantiaHasta,
        String accesorios,
        String observaciones,
        String responsable,                // solo lectura: asignacion vigente
        String departamento,               // solo lectura: del responsable
        String levantadoPor,               // solo lectura
        DatosComputo computo) {            // null si no es equipo de computo / no se capturo

    /**
     * Constructor con la firma ANTERIOR (21 campos), para que el formulario actual
     * siga compilando mientras migramos paso a paso. Se puede borrar cuando el
     * formulario nuevo (paso 2) use el constructor completo.
     */
    public Equipo(Integer id, String codigoInventario,
                  int tipoId, String tipo, int marcaId, String marca,
                  String modelo, String numeroSerie, String hostname, String macAddress,
                  Integer ubicacionId, String ubicacion,
                  int estadoFisicoId, String estadoFisico,
                  int estadoFuncionalId, String estadoFuncional,
                  LocalDate fechaCompra, LocalDate garantiaHasta,
                  String accesorios, String observaciones, String responsable) {
        this(id, codigoInventario, tipoId, tipo, marcaId, marca, modelo, numeroSerie, hostname, macAddress,
             null, ubicacionId, ubicacion, null,
             estadoFisicoId, estadoFisico, estadoFuncionalId, estadoFuncional,
             false, null, fechaCompra, garantiaHasta,
             accesorios, observaciones, responsable, null, null, null);
    }
}