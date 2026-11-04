package main.java.com.aliprinsa.administracion.gusto.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import main.java.com.aliprinsa.administracion.gusto.config.DataBaseConnection;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.Mantenimiento;

public class MantenimientoRepository {

    private static final String SELECT_BASE =
          "SELECT mt.id, mt.equipo_id, eq.codigo_inventario, CONCAT(m.nombre, ' ', eq.modelo) AS equipo, "
        + "       mt.fecha, mt.tipo, mt.descripcion, mt.realizado_por_id, "
        + "       CONCAT(emp.nombres, ' ', emp.apellidos) AS realizado_por "
        + "FROM mantenimientos mt "
        + "JOIN equipos eq ON eq.id = mt.equipo_id "
        + "JOIN marcas m ON m.id = eq.marca_id "
        + "JOIN empleados emp ON emp.id = mt.realizado_por_id ";

    // ---------- Consultas ----------

    /** tipo: "Preventivo" / "Correctivo" o null = todos. */
    public List<Mantenimiento> listar(String texto, String tipo) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append("AND (eq.codigo_inventario LIKE ? OR eq.modelo LIKE ? OR m.nombre LIKE ? "
                     + "OR mt.descripcion LIKE ? OR CONCAT(emp.nombres, ' ', emp.apellidos) LIKE ?) ");
            String like = "%" + texto.trim() + "%";
            for (int i = 0; i < 5; i++) params.add(like);
        }
        if (tipo != null) {
            sql.append("AND mt.tipo = ? ");
            params.add(tipo);
        }
        sql.append("ORDER BY mt.fecha DESC, mt.id DESC");

        List<Mantenimiento> lista = new ArrayList<>();
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Equipos que no están dados de baja. */
    public List<Catalogo> equiposMantenibles() throws SQLException {
        return catalogo("SELECT eq.id, CONCAT(eq.codigo_inventario, ' · ', m.nombre, ' ', eq.modelo) AS nombre "
                      + "FROM equipos eq "
                      + "JOIN marcas m ON m.id = eq.marca_id "
                      + "JOIN estados_funcionales efu ON efu.id = eq.estado_funcional_id "
                      + "WHERE efu.nombre <> 'Fuera de servicio' "
                      + "ORDER BY eq.codigo_inventario");
    }

    public List<Catalogo> empleadosActivos() throws SQLException {
        return catalogo("SELECT id, CONCAT(nombres, ' ', apellidos) AS nombre "
                      + "FROM empleados WHERE activo = TRUE ORDER BY nombres, apellidos");
    }

    /** Estados que se pueden aplicar desde un mantenimiento (la baja se hace desde Equipos). */
    public List<Catalogo> estadosFuncionales() throws SQLException {
        return catalogo("SELECT id, nombre FROM estados_funcionales WHERE nombre <> 'Fuera de servicio' ORDER BY id");
    }

    /** empleado_id del usuario logueado, o null si no se encuentra. */
    public Integer empleadoIdDe(String username) throws SQLException {
        if (username == null) return null;
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT empleado_id FROM usuarios WHERE username = ? AND empleado_id IS NOT NULL")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    // ---------- Escritura (mantenimiento + cambio de estado opcional, todo o nada) ----------

    /** estadoPostId: 0 = no cambiar el estado del equipo. */
    public void insertar(Mantenimiento m, int estadoPostId) throws SQLException {
        try (Connection cn = DataBaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO mantenimientos (equipo_id, fecha, tipo, descripcion, realizado_por_id) "
                      + "VALUES (?,?,?,?,?)")) {
                    ps.setInt(1, m.equipoId());
                    ps.setDate(2, Date.valueOf(m.fecha()));
                    ps.setString(3, m.tipo());
                    ps.setString(4, m.descripcion());
                    ps.setInt(5, m.realizadoPorId());
                    ps.executeUpdate();
                }
                cambiarEstado(cn, m.equipoId(), estadoPostId);
                cn.commit();
            } catch (SQLException ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    /** El equipo no se puede cambiar al editar: solo fecha, tipo, descripción y responsable. */
    public void actualizar(Mantenimiento m, int estadoPostId) throws SQLException {
        try (Connection cn = DataBaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE mantenimientos SET fecha=?, tipo=?, descripcion=?, realizado_por_id=? WHERE id=?")) {
                    ps.setDate(1, Date.valueOf(m.fecha()));
                    ps.setString(2, m.tipo());
                    ps.setString(3, m.descripcion());
                    ps.setInt(4, m.realizadoPorId());
                    ps.setInt(5, m.id());
                    ps.executeUpdate();
                }
                cambiarEstado(cn, m.equipoId(), estadoPostId);
                cn.commit();
            } catch (SQLException ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    // ---------- Auxiliares ----------

    /** Nunca "revive" un equipo que ya está dado de baja. */
    private void cambiarEstado(Connection cn, int equipoId, int estadoPostId) throws SQLException {
        if (estadoPostId <= 0) return;
        try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE equipos SET estado_funcional_id = ? WHERE id = ? "
              + "AND estado_funcional_id <> (SELECT id FROM estados_funcionales WHERE nombre = 'Fuera de servicio')")) {
            ps.setInt(1, estadoPostId);
            ps.setInt(2, equipoId);
            ps.executeUpdate();
        }
    }

    private List<Catalogo> catalogo(String sql) throws SQLException {
        List<Catalogo> lista = new ArrayList<>();
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new Catalogo(rs.getInt("id"), rs.getString("nombre")));
        }
        return lista;
    }

    private Mantenimiento mapear(ResultSet rs) throws SQLException {
        return new Mantenimiento(
                rs.getInt("id"),
                rs.getInt("equipo_id"), rs.getString("codigo_inventario"), rs.getString("equipo"),
                rs.getDate("fecha").toLocalDate(),
                rs.getString("tipo"),
                rs.getString("descripcion"),
                rs.getInt("realizado_por_id"), rs.getString("realizado_por"));
    }
}