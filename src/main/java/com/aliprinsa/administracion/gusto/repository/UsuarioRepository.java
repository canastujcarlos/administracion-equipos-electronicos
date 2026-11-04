package main.java.com.aliprinsa.administracion.gusto.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import main.java.com.aliprinsa.administracion.gusto.config.DataBaseConnection;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.Usuario;

public class UsuarioRepository {

    private static final String SELECT_BASE =
          "SELECT u.id, u.username, u.rol_id, r.nombre AS rol, u.empleado_id, "
        + "       e.nombres, e.apellidos, e.departamento_id, d.nombre AS departamento, u.activo "
        + "FROM usuarios u "
        + "JOIN roles r ON r.id = u.rol_id "
        + "LEFT JOIN empleados e ON e.id = u.empleado_id "
        + "LEFT JOIN departamentos d ON d.id = e.departamento_id ";

    // ---------- Consultas ----------

    /** texto y rolId son filtros opcionales (null o 0 = sin filtro). */
    public List<Usuario> listar(String texto, Integer rolId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append("AND (u.username LIKE ? OR CONCAT(e.nombres, ' ', e.apellidos) LIKE ? "
                     + "OR d.nombre LIKE ?) ");
            String like = "%" + texto.trim() + "%";
            for (int i = 0; i < 3; i++) params.add(like);
        }
        if (rolId != null && rolId > 0) {
            sql.append("AND u.rol_id = ? ");
            params.add(rolId);
        }
        sql.append("ORDER BY u.activo DESC, e.nombres, e.apellidos");

        List<Usuario> lista = new ArrayList<>();
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Catalogo> catalogo(String tabla) throws SQLException {
        Set<String> permitidas = Set.of("roles", "departamentos");   // lista blanca
        if (!permitidas.contains(tabla)) throw new IllegalArgumentException("Catalogo no permitido: " + tabla);

        List<Catalogo> lista = new ArrayList<>();
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT id, nombre FROM " + tabla + " ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new Catalogo(rs.getInt("id"), rs.getString("nombre")));
        }
        return lista;
    }

    public int rolIdDe(String nombre) throws SQLException {
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT id FROM roles WHERE nombre = ?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** true si el correo ya existe como empleado o como username. */
    public boolean existeCorreo(String correo) throws SQLException {
        String sql = "SELECT 1 FROM empleados WHERE correo = ? UNION SELECT 1 FROM usuarios WHERE username = ?";
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setString(2, correo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ---------- Escritura ----------

    /** Empleado + usuario en una sola transacción. El correo funciona como username. */
    public void crear(Usuario u, String hash) throws SQLException {
        try (Connection cn = DataBaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                int empleadoId;
                try (PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO empleados (nombres, apellidos, correo, departamento_id) VALUES (?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, u.nombres());
                    ps.setString(2, u.apellidos());
                    ps.setString(3, u.username());
                    ps.setInt(4, u.departamentoId());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        empleadoId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO usuarios (username, contrasena_hash, rol_id, empleado_id, activo) "
                      + "VALUES (?,?,?,?,TRUE)")) {
                    ps.setString(1, u.username());
                    ps.setString(2, hash);
                    ps.setInt(3, u.rolId());
                    ps.setInt(4, empleadoId);
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    /** Actualiza rol y estado del usuario, y nombres/departamento de su empleado. */
    public void actualizar(Usuario u) throws SQLException {
        try (Connection cn = DataBaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE usuarios SET rol_id = ?, activo = ? WHERE id = ?")) {
                    ps.setInt(1, u.rolId());
                    ps.setBoolean(2, u.activo());
                    ps.setInt(3, u.id());
                    ps.executeUpdate();
                }
                if (u.empleadoId() != null) {
                    try (PreparedStatement ps = cn.prepareStatement(
                            "UPDATE empleados SET nombres = ?, apellidos = ?, departamento_id = ? WHERE id = ?")) {
                        ps.setString(1, u.nombres());
                        ps.setString(2, u.apellidos());
                        ps.setInt(3, u.departamentoId());
                        ps.setInt(4, u.empleadoId());
                        ps.executeUpdate();
                    }
                }
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    public void cambiarPassword(int usuarioId, String hash) throws SQLException {
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("UPDATE usuarios SET contrasena_hash = ? WHERE id = ?")) {
            ps.setString(1, hash);
            ps.setInt(2, usuarioId);
            ps.executeUpdate();
        }
    }

    // ---------- Auxiliares ----------

    private Usuario mapear(ResultSet rs) throws SQLException {
        int emp = rs.getInt("empleado_id");
        Integer empleadoId = rs.wasNull() ? null : emp;
        return new Usuario(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getInt("rol_id"), rs.getString("rol"),
                empleadoId,
                rs.getString("nombres"), rs.getString("apellidos"),
                rs.getInt("departamento_id"), rs.getString("departamento"),
                rs.getBoolean("activo"));
    }
}