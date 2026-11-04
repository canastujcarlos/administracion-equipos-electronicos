package main.java.com.aliprinsa.administracion.gusto.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import main.java.com.aliprinsa.administracion.gusto.config.DataBaseConnection;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.DatosComputo;
import main.java.com.aliprinsa.administracion.gusto.model.Equipo;

public class EquipoRepository {

    /** Nombre del estado funcional al que pasa un equipo dado de baja (debe existir en la BD). */

    private static final String SELECT_BASE =
          "SELECT eq.id, eq.codigo_inventario, eq.tipo_equipo_id, te.nombre AS tipo, "
        + "       eq.marca_id, m.nombre AS marca, eq.modelo, eq.numero_serie, eq.hostname, "
        + "       eq.mac_address, eq.ip_address, "
        + "       eq.ubicacion_id, ub.nombre AS ubicacion, s.nombre AS sede, "
        + "       eq.estado_fisico_id, ef.nombre AS estado_fisico, "
        + "       eq.estado_funcional_id, efu.nombre AS estado_funcional, eq.requiere_atencion, "
        + "       eq.fecha_levantamiento, eq.fecha_compra, eq.garantia_hasta, "
        + "       eq.accesorios, eq.observaciones, "
        + "       CONCAT(emp.nombres, ' ', emp.apellidos) AS responsable, d.nombre AS departamento, "
        + "       CONCAT(lv.nombres, ' ', lv.apellidos) AS levantado_por, "
        + "       ec.equipo_id AS tiene_computo, ec.procesador, ec.ram_gb, ec.tipo_disco, "
        + "       ec.capacidad_disco_gb, ec.so_id, so.nombre AS so, ec.version_so, "
        + "       ec.dominio_grupo, ec.antivirus "
        + "FROM equipos eq "
        + "JOIN tipos_equipo te ON te.id = eq.tipo_equipo_id "
        + "JOIN marcas m ON m.id = eq.marca_id "
        + "LEFT JOIN ubicaciones ub ON ub.id = eq.ubicacion_id "
        + "LEFT JOIN sedes s ON s.id = ub.sede_id "
        + "JOIN estados_fisicos ef ON ef.id = eq.estado_fisico_id "
        + "JOIN estados_funcionales efu ON efu.id = eq.estado_funcional_id "
        + "JOIN empleados lv ON lv.id = eq.levantado_por_id "
        + "LEFT JOIN asignaciones a ON a.equipo_id = eq.id AND a.fecha_fin IS NULL "
        + "LEFT JOIN empleados emp ON emp.id = a.empleado_id "
        + "LEFT JOIN departamentos d ON d.id = emp.departamento_id "
        + "LEFT JOIN equipos_computo ec ON ec.equipo_id = eq.id "
        + "LEFT JOIN sistemas_operativos so ON so.id = ec.so_id ";

    private static final String SQL_INSERT =
          "INSERT INTO equipos (codigo_inventario, tipo_equipo_id, marca_id, modelo, numero_serie, "
        + "hostname, mac_address, ip_address, ubicacion_id, estado_fisico_id, estado_funcional_id, "
        + "requiere_atencion, fecha_levantamiento, fecha_compra, garantia_hasta, accesorios, "
        + "observaciones, levantado_por_id) "
        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,COALESCE(?, CURDATE()),?,?,?,?,?)";

    private static final String SQL_UPDATE =
          "UPDATE equipos SET codigo_inventario=?, tipo_equipo_id=?, marca_id=?, modelo=?, "
        + "numero_serie=?, hostname=?, mac_address=?, ip_address=?, ubicacion_id=?, "
        + "estado_fisico_id=?, estado_funcional_id=?, requiere_atencion=?, "
        + "fecha_levantamiento=COALESCE(?, fecha_levantamiento), fecha_compra=?, "
        + "garantia_hasta=?, accesorios=?, observaciones=? WHERE id=?";

    // ---------- Consultas ----------

    /** texto, tipoId y estadoFuncionalId son filtros opcionales (null o 0 = sin filtro). */
    public List<Equipo> listar(String texto, Integer tipoId, Integer estadoFuncionalId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append("AND (eq.codigo_inventario LIKE ? OR eq.modelo LIKE ? OR eq.numero_serie LIKE ? "
                     + "OR eq.hostname LIKE ? OR m.nombre LIKE ? OR eq.mac_address LIKE ? "
                     + "OR eq.ip_address LIKE ?) ");
            String like = "%" + texto.trim() + "%";
            for (int i = 0; i < 7; i++) params.add(like);
        }
        if (tipoId != null && tipoId > 0) {
            sql.append("AND eq.tipo_equipo_id = ? ");
            params.add(tipoId);
        }
        if (estadoFuncionalId != null && estadoFuncionalId > 0) {
            sql.append("AND eq.estado_funcional_id = ? ");
            params.add(estadoFuncionalId);
        }
        sql.append("ORDER BY eq.codigo_inventario");

        List<Equipo> lista = new ArrayList<>();
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
        // lista blanca: el nombre de tabla no se puede parametrizar, asi evitamos SQL injection
        Set<String> permitidas = Set.of("tipos_equipo", "marcas", "ubicaciones", "sedes",
                                        "estados_fisicos", "estados_funcionales", "sistemas_operativos");
        if (!permitidas.contains(tabla)) throw new IllegalArgumentException("Catalogo no permitido: " + tabla);

        List<Catalogo> lista = new ArrayList<>();
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT id, nombre FROM " + tabla + " ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new Catalogo(rs.getInt("id"), rs.getString("nombre")));
        }
        return lista;
    }

    /** Ubicaciones de una sede (para el combo en cascada Sede -> Ubicacion). sedeId null = todas. */
    public List<Catalogo> ubicacionesPorSede(Integer sedeId) throws SQLException {
        String sql = "SELECT id, nombre FROM ubicaciones "
                   + (sedeId == null ? "" : "WHERE sede_id = ? ") + "ORDER BY nombre";
        List<Catalogo> lista = new ArrayList<>();
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (sedeId != null) ps.setInt(1, sedeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(new Catalogo(rs.getInt("id"), rs.getString("nombre")));
            }
        }
        return lista;
    }

    /** Verifica duplicados de codigo_inventario, numero_serie o mac_address (excluyendo al propio equipo al editar). */
    public boolean existe(String campo, String valor, Integer excluirId) throws SQLException {
        Set<String> permitidos = Set.of("codigo_inventario", "numero_serie", "mac_address");
        if (!permitidos.contains(campo)) throw new IllegalArgumentException("Campo no permitido: " + campo);

        String sql = "SELECT 1 FROM equipos WHERE " + campo + " = ? AND id <> ?";
        try (Connection cn = DataBaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, excluirId == null ? -1 : excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ---------- Escritura ----------

    /** Inserta el equipo y, si trae datos de computo, su fila en equipos_computo. Todo o nada. */
    public void insertar(Equipo e, String usernameActual) throws SQLException {
        try (Connection cn = DataBaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                int levantadoPor = empleadoIdDe(cn, usernameActual);
                int nuevoId;
                try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    int i = enlazarEquipo(ps, e);
                    ps.setInt(i, levantadoPor);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        nuevoId = keys.getInt(1);
                    }
                }
                guardarComputo(cn, nuevoId, e.computo());
                cn.commit();
            } catch (SQLException | RuntimeException ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    /** Actualiza el equipo y sus datos de computo (si vienen). Todo o nada. */
    public void actualizar(Equipo e) throws SQLException {
        try (Connection cn = DataBaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {
                    int i = enlazarEquipo(ps, e);
                    ps.setInt(i, e.id());
                    ps.executeUpdate();
                }
                guardarComputo(cn, e.id(), e.computo());
                cn.commit();
            } catch (SQLException | RuntimeException ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    /** Baja logica: pasa al estado con codigo 'BAJA' y cierra su asignacion vigente. Todo o nada. */
    public void darDeBaja(int equipoId) throws SQLException {
    try (Connection cn = DataBaseConnection.getConnection()) {
        cn.setAutoCommit(false);
        try {
            int filas;
            try (PreparedStatement ps = cn.prepareStatement(
                    "UPDATE equipos SET estado_funcional_id = "
                  + "(SELECT id FROM estados_funcionales WHERE codigo = 'BAJA') WHERE id = ?")) {
                ps.setInt(1, equipoId);
                filas = ps.executeUpdate();
            }
            if (filas == 0) throw new SQLException("No se encontró el equipo a dar de baja.");

            try (PreparedStatement ps = cn.prepareStatement(
                    "UPDATE asignaciones SET fecha_fin = CURDATE() WHERE equipo_id = ? AND fecha_fin IS NULL")) {
                ps.setInt(1, equipoId);
                ps.executeUpdate();
            }
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

    /** Parametros comunes de INSERT y UPDATE (17). Devuelve el siguiente indice libre. */
    private int enlazarEquipo(PreparedStatement ps, Equipo e) throws SQLException {
        int i = 1;
        ps.setString(i++, e.codigoInventario());
        ps.setInt(i++, e.tipoId());
        ps.setInt(i++, e.marcaId());
        ps.setString(i++, e.modelo());
        ps.setString(i++, e.numeroSerie());
        ps.setString(i++, e.hostname());
        ps.setString(i++, e.macAddress());
        ps.setString(i++, e.ipAddress());
        setIntNullable(ps, i++, e.ubicacionId());
        ps.setInt(i++, e.estadoFisicoId());
        ps.setInt(i++, e.estadoFuncionalId());
        ps.setBoolean(i++, e.requiereAtencion());
        setDateNullable(ps, i++, e.fechaLevantamiento());
        setDateNullable(ps, i++, e.fechaCompra());
        setDateNullable(ps, i++, e.garantiaHasta());
        ps.setString(i++, e.accesorios());
        ps.setString(i++, e.observaciones());
        return i;
    }

    /** Crea o actualiza la fila de equipos_computo. Si c es null no toca nada. */
    private void guardarComputo(Connection cn, int equipoId, DatosComputo c) throws SQLException {
        if (c == null) return;
        String sql = "INSERT INTO equipos_computo (equipo_id, procesador, ram_gb, tipo_disco, "
                   + "capacidad_disco_gb, so_id, version_so, dominio_grupo, antivirus) "
                   + "VALUES (?,?,?,?,?,?,?,?,?) "
                   + "ON DUPLICATE KEY UPDATE procesador=VALUES(procesador), ram_gb=VALUES(ram_gb), "
                   + "tipo_disco=VALUES(tipo_disco), capacidad_disco_gb=VALUES(capacidad_disco_gb), "
                   + "so_id=VALUES(so_id), version_so=VALUES(version_so), "
                   + "dominio_grupo=VALUES(dominio_grupo), antivirus=VALUES(antivirus)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, equipoId);
            ps.setString(i++, c.procesador());
            setIntNullable(ps, i++, c.ramGb());
            ps.setString(i++, c.tipoDisco());
            setIntNullable(ps, i++, c.capacidadDiscoGb());
            setIntNullable(ps, i++, c.soId());
            ps.setString(i++, c.versionSo());
            ps.setString(i++, c.dominioGrupo());
            ps.setString(i, c.antivirus());
            ps.executeUpdate();
        }
    }

    /** empleado_id del usuario logueado; si no se puede determinar, usa el primer empleado. */
    private int empleadoIdDe(Connection cn, String username) throws SQLException {
        if (username != null) {
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT empleado_id FROM usuarios WHERE username = ? AND empleado_id IS NOT NULL")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = cn.prepareStatement("SELECT MIN(id) FROM empleados");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) return rs.getInt(1);
        }
        throw new SQLException("No hay empleados registrados para asociar el equipo.");
    }

    private Equipo mapear(ResultSet rs) throws SQLException {
        DatosComputo computo = null;
        rs.getInt("tiene_computo");
        if (!rs.wasNull()) {
            computo = new DatosComputo(
                    rs.getString("procesador"),
                    intONull(rs, "ram_gb"),
                    rs.getString("tipo_disco"),
                    intONull(rs, "capacidad_disco_gb"),
                    intONull(rs, "so_id"), rs.getString("so"),
                    rs.getString("version_so"),
                    rs.getString("dominio_grupo"),
                    rs.getString("antivirus"));
        }
        return new Equipo(
                rs.getInt("id"),
                rs.getString("codigo_inventario"),
                rs.getInt("tipo_equipo_id"), rs.getString("tipo"),
                rs.getInt("marca_id"), rs.getString("marca"),
                rs.getString("modelo"),
                rs.getString("numero_serie"),
                rs.getString("hostname"),
                rs.getString("mac_address"),
                rs.getString("ip_address"),
                intONull(rs, "ubicacion_id"), rs.getString("ubicacion"),
                rs.getString("sede"),
                rs.getInt("estado_fisico_id"), rs.getString("estado_fisico"),
                rs.getInt("estado_funcional_id"), rs.getString("estado_funcional"),
                rs.getBoolean("requiere_atencion"),
                aLocalDate(rs.getDate("fecha_levantamiento")),
                aLocalDate(rs.getDate("fecha_compra")),
                aLocalDate(rs.getDate("garantia_hasta")),
                rs.getString("accesorios"),
                rs.getString("observaciones"),
                rs.getString("responsable"),
                rs.getString("departamento"),
                rs.getString("levantado_por"),
                computo);
    }

    private static Integer intONull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private static LocalDate aLocalDate(Date d) {
        return d == null ? null : d.toLocalDate();
    }   

    private static void setDateNullable(PreparedStatement ps, int idx, LocalDate d) throws SQLException {
        if (d == null) ps.setNull(idx, Types.DATE); else ps.setDate(idx, Date.valueOf(d));
    }

    private static void setIntNullable(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER); else ps.setInt(idx, v);
    }
}