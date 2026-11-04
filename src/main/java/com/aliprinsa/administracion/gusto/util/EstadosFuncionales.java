package main.java.com.aliprinsa.administracion.gusto.util;

/** Único lugar que sabe cómo clasificar los estados funcionales por su nombre. */
public final class EstadosFuncionales {

    public enum Categoria { OK, FALLA, BAJA, REVISION, OTRO }

    private EstadosFuncionales() {}

    public static Categoria categoria(String nombre) {
        if (nombre == null) return Categoria.OTRO;
        return switch (nombre.trim().toLowerCase()) {
            case "operativo" -> Categoria.OK;
            case "con fallas" -> Categoria.FALLA;
            case "fuera de servicio" -> Categoria.BAJA;
            // Se agrega con tilde (como está en la BD) y sin tilde por seguridad
            case "pendiente de revisión", "pendiente de revision" -> Categoria.REVISION;
            default -> Categoria.OTRO;
        };
    }
}
