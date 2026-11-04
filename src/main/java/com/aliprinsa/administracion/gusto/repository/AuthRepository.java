package main.java.com.aliprinsa.administracion.gusto.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import main.java.com.aliprinsa.administracion.gusto.config.DataBaseConnection;
import main.java.com.aliprinsa.administracion.gusto.dto.request.LoginDTORequest;
import main.java.com.aliprinsa.administracion.gusto.dto.response.LoginDTOResponse;
import main.java.com.aliprinsa.administracion.gusto.model.Departamento;


/**
 *
 * @author admin.practicas
 */
public class AuthRepository {
    
    //esto busca a los usuarios por el correo que ingresen a la base de datos
    public LoginDTOResponse findUserbyEmail(LoginDTORequest req) throws SQLException {
    String sql = "SELECT e.nombres AS nombre, e.apellidos AS apellido, "
               + "       u.contrasena_hash AS contrasena, r.nombre AS rol "
               + "FROM usuarios u "
               + "INNER JOIN empleados e ON u.empleado_id = e.id "
               + "INNER JOIN roles r ON u.rol_id = r.id "
               + "WHERE u.username = ? AND u.activo = TRUE";

    try (Connection cn = DataBaseConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setString(1, req.getEmail());
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new LoginDTOResponse(
                    rs.getString("nombre"),
                    rs.getString("apellido"),
                    rs.getString("contrasena"),
                    rs.getString("rol"));
            }
        }
    }
    return null;
}
    
    public List<Departamento> listarDepartamentos() throws SQLException {
    List<Departamento> lista = new ArrayList<>();
    String sql = "SELECT id, nombre FROM departamentos ORDER BY nombre";
    try (Connection connection = DataBaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            lista.add(new Departamento(rs.getInt("id"), rs.getString("nombre")));
        }
    }
    return lista;
}

public boolean existeCorreo(String correo) throws SQLException {
    String sql = "SELECT 1 FROM empleados WHERE correo = ?";
    try (Connection cn = DataBaseConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setString(1, correo);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }
}

public void registrarEmpleadoYUsuario(String nombres, String apellidos, String correo,
                                      int departamentoId, String hash) throws SQLException {
    try (Connection cn = DataBaseConnection.getConnection()) {
        cn.setAutoCommit(false);   // las dos inserciones van juntas o no va ninguna
        try {
            int empleadoId;
            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO empleados (nombres, apellidos, correo, departamento_id) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombres);
                ps.setString(2, apellidos);
                ps.setString(3, correo);
                ps.setInt(4, departamentoId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    empleadoId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO usuarios (username, contrasena_hash, rol_id, empleado_id) "
                  + "VALUES (?, ?, (SELECT id FROM roles WHERE nombre = 'Consultor'), ?)")) {
                ps.setString(1, correo);   // el correo funciona como username
                ps.setString(2, hash);
                ps.setInt(3, empleadoId);
                ps.executeUpdate();
            }
            cn.commit();
        } catch (SQLException e) {
            cn.rollback();
            throw e;
        }
    }
}
    }

