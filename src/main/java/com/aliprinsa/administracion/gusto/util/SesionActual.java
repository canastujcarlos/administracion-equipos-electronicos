package main.java.com.aliprinsa.administracion.gusto.util;

public final class SesionActual {

    private static String nombre;
    private static String apellido;
    private static String rol;
    private static String username;   // correo con el que inició sesión

    private SesionActual() {}

    /** Orden: nombre, apellido, rol, correo. */
    public static void iniciar(String nombre, String apellido, String rol, String username) {
        SesionActual.nombre = nombre;
        SesionActual.apellido = apellido;
        SesionActual.rol = rol;
        SesionActual.username = username;
    }

    public static void cerrar() {
        nombre = null;
        apellido = null;
        rol = null;
        username = null;
    }

    public static boolean haySesion() { return rol != null; }
    public static String getNombreCompleto() { return nombre + " " + apellido; }
    public static String getRol() { return rol; }
    public static String getUsername() { return username; }
    public static String getCorreo() { return username; }   // alias, por si otra clase lo usa
    public static boolean esAdministrador() { return "Administrador".equals(rol); }
}