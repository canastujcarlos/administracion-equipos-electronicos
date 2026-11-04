package main.java.com.aliprinsa.administracion.gusto.model;

public record Usuario(
        Integer id,
        String username,
        int rolId, String rol,
        Integer empleadoId,
        String nombres, String apellidos,
        int departamentoId, String departamento,
        boolean activo) {
    
    public String nombreCompleto() {
        String n = (nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos);
        return n.isBlank() ? "—" : n.trim();
    }
}