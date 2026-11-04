package main.java.com.aliprinsa.administracion.gusto.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import main.java.com.aliprinsa.administracion.gusto.config.DataBaseConnection;
import main.java.com.aliprinsa.administracion.gusto.dto.response.DashboardDTOResponse;

public class DashboardRepository {

    public DashboardDTOResponse obtenerResumen() throws SQLException {
        try (Connection cn = DataBaseConnection.getConnection()) {

            int total = contar(cn, "SELECT COUNT(*) FROM equipos");

            int garantias = contar(cn,
                "SELECT COUNT(*) FROM equipos "
              + "WHERE garantia_hasta BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 60 DAY)");

            int sinAsignar = contar(cn,
                "SELECT COUNT(*) FROM equipos e WHERE NOT EXISTS "
              + "(SELECT 1 FROM asignaciones a WHERE a.equipo_id = e.id AND a.fecha_fin IS NULL)");

            Map<String, Integer> porEstado = agrupar(cn,
                "SELECT ef.nombre, COUNT(*) FROM equipos e "
              + "JOIN estados_funcionales ef ON e.estado_funcional_id = ef.id "
              + "GROUP BY ef.nombre ORDER BY ef.nombre");

            Map<String, Integer> porTipo = agrupar(cn,
                "SELECT t.nombre, COUNT(*) FROM equipos e "
              + "JOIN tipos_equipo t ON e.tipo_equipo_id = t.id "
              + "GROUP BY t.nombre ORDER BY t.nombre");

            return new DashboardDTOResponse(total, garantias, sinAsignar, porEstado, porTipo);
        }
    }

    private int contar(Connection cn, String sql) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Map<String, Integer> agrupar(Connection cn, String sql) throws SQLException {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getString(1), rs.getInt(2));
            }
        }
        return mapa;
    }
}