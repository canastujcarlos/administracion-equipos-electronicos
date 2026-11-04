package main.java.com.aliprinsa.administracion.gusto.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import main.java.com.aliprinsa.administracion.gusto.config.DataBaseConnection;
import main.java.com.aliprinsa.administracion.gusto.model.Asignacion;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;

public class AsignacionRepository {

    private static final String SELECT_BASE =
          "SELECT a.id, a.equipo_id, eq.codigo_inventario, CONCAT(m.nombre, ' ', eq.modelo) AS equipo, "
        + "       a.empleado_id, CONCAT(emp.nombres, ' ', emp.apellidos) AS empleado, "
        + "       d.nombre AS departamento, a.fecha_inicio, a.fecha_fin, a.observaciones "
        + "FROM asignaciones a "
        + "JOIN equipos eq ON eq.id = a.equipo_id "
        + "JOIN marcas m ON m.id = eq.marca_id "
        + "JOIN empleados emp ON emp.id = a.empleado_id "
        + "JOIN departamentos d ON d.id = emp.departamento_id ";

    /** vigente: true = solo vigentes, false = solo finalizadas, null = todas. */
    public List<Asignacion> listar(String texto, Boolean vigente) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append("AND (eq.codigo_inventario LIKE ? OR eq.modelo LIKE ? OR m.nombre LIKE ? "
                     + "OR CONCAT(emp.nombres, ' ', emp.apellidos) LIKE ? OR d.nombre LIKE ?) ");
            String like = "%" + texto.trim() + "%";
            for (int i = 0; i < 5; i++) params.add(like);
        }
        if (vigente != null) {
            sql.append(vigente ? "AND a.fecha_fin IS NULL " : "AND a.fecha_fin IS NOT NULL ");
        }
        sql.append("ORDER BY (a.fecha_fin IS NULL) DESC, a.fecha_inicio DESC, a.id DESC");

        List<Asignacion> lista = new ArrayList<>();
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Equipos operativos que no tienen asignación vigente. */
    public List<Catalogo> equiposDisponibles() throws SQLException {
        String sql = "SELECT eq.id, CONCAT(eq.codigo_inventario, ' · ', m.nombre, ' ', eq.modelo) AS nombre "
                   + "FROM equipos eq "
                   + "JOIN marcas m ON m.id = eq.marca_id "
                   + "JOIN estados_funcionales efu ON efu.id = eq.estado_funcional_id "
                   + "WHERE efu.nombre = 'Operativo' "
                   + "AND NOT EXISTS (SELECT 1 FROM asignaciones a WHERE a.equipo_id = eq.id AND a.fecha_fin IS NULL) "
                   + "ORDER BY eq.codigo_inventario";
        return catalogo(sql);
    }

    public List<Catalogo> empleadosActivos() throws SQLException {
        String sql = "SELECT id, CONCAT(nombres, ' ', apellidos) AS nombre "
                   + "FROM empleados WHERE activo = TRUE ORDER BY nombres, apellidos";
        return catalogo(sql);
    }

    public void insertar(int equipoId, int empleadoId, LocalDate inicio, String observaciones) throws SQLException {
        try (Connection cn = DataBaseConnection.getConnection()) {
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT 1 FROM asignaciones WHERE equipo_id = ? AND fecha_fin IS NULL")) {
                ps.setInt(1, equipoId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) throw new IllegalArgumentException("Ese equipo ya tiene una asignación vigente.");
                }
            }
            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO asignaciones (equipo_id, empleado_id, fecha_inicio, observaciones) VALUES (?,?,?,?)")) {
                ps.setInt(1, equipoId);
                ps.setInt(2, empleadoId);
                ps.setDate(3, Date.valueOf(inicio));
                ps.setString(4, observaciones);
                ps.executeUpdate();
            }
        }
    }

    /** Cierra la asignación hoy (o en su fecha de inicio si esta es futura). */
    public void finalizar(int asignacionId) throws SQLException {
        String sql = "UPDATE asignaciones SET fecha_fin = GREATEST(CURDATE(), fecha_inicio) "
                   + "WHERE id = ? AND fecha_fin IS NULL";
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, asignacionId);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("La asignación ya estaba finalizada.");
            }
        }
    }

    // ---------- auxiliares ----------

    private List<Catalogo> catalogo(String sql) throws SQLException {
        List<Catalogo> lista = new ArrayList<>();
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new Catalogo(rs.getInt("id"), rs.getString("nombre")));
        }
        return lista;
    }

    private Asignacion mapear(ResultSet rs) throws SQLException {
        Date fin = rs.getDate("fecha_fin");
        return new Asignacion(
                rs.getInt("id"),
                rs.getInt("equipo_id"), rs.getString("codigo_inventario"), rs.getString("equipo"),
                rs.getInt("empleado_id"), rs.getString("empleado"), rs.getString("departamento"),
                rs.getDate("fecha_inicio").toLocalDate(),
                fin == null ? null : fin.toLocalDate(),
                rs.getString("observaciones"));
    }
}