package main.java.com.aliprinsa.administracion.gusto.model;

import java.time.LocalDate;

public record Asignacion(
        int id,
        int equipoId, String codigoEquipo, String equipo,
        int empleadoId, String empleado, String departamento,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String observaciones) {

    public boolean vigente() {
        return fechaFin == null;
    }
}
