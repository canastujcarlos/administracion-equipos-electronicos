package main.java.com.aliprinsa.administracion.gusto.model;

import java.time.LocalDate;

/** Los nombres (codigoEquipo, equipo, realizadoPor) solo se llenan al LEER; al guardar se usan los ids. */
public record Mantenimiento(
        Integer id,
        int equipoId, String codigoEquipo, String equipo,
        LocalDate fecha,
        String tipo,
        String descripcion,
        int realizadoPorId, String realizadoPor) {
}