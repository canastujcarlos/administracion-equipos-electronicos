package main.java.com.aliprinsa.administracion.gusto.service;

import java.util.List;
import java.util.regex.Pattern;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.Usuario;
import main.java.com.aliprinsa.administracion.gusto.repository.UsuarioRepository;
import main.java.com.aliprinsa.administracion.gusto.security.jbcrypt.BCrypt;

public class UsuarioService {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD = 8;

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public List<Usuario> listar(String texto, Integer rolId) throws Exception {
        return repository.listar(texto, rolId);
    }

    public List<Catalogo> roles() throws Exception { return repository.catalogo("roles"); }
    public List<Catalogo> departamentos() throws Exception { return repository.catalogo("departamentos"); }

    /**
     * Crea (id == null) o actualiza un usuario. password solo se usa al crear.
     * usernameActual = quien está logueado (para impedir que se quite el rol o se desactive).
     * Lanza IllegalArgumentException con un mensaje listo para mostrar.
     */
    public void guardar(Usuario in, String password, String usernameActual) throws Exception {
        Usuario u = normalizar(in);
        validar(u);

        if (u.id() == null) {
            validarPassword(password);
            if (repository.existeCorreo(u.username()))
                throw new IllegalArgumentException("Ya existe un usuario con el correo " + u.username() + ".");
            repository.crear(u, BCrypt.hashpw(password, BCrypt.gensalt()));
        } else {
            boolean esUnoMismo = u.username().equalsIgnoreCase(usernameActual);
            if (esUnoMismo && (!u.activo() || u.rolId() != repository.rolIdDe("Administrador")))
                throw new IllegalArgumentException("No puedes quitarte el rol de administrador ni desactivarte a ti mismo.");
            repository.actualizar(u);
        }
    }

    public void cambiarPassword(int usuarioId, String nueva) throws Exception {
        validarPassword(nueva);
        repository.cambiarPassword(usuarioId, BCrypt.hashpw(nueva, BCrypt.gensalt()));
    }

    // ---------- reglas ----------

    private void validar(Usuario u) {
        if (u.nombres() == null) throw new IllegalArgumentException("Los nombres son obligatorios.");
        if (u.apellidos() == null) throw new IllegalArgumentException("Los apellidos son obligatorios.");
        if (u.username() == null || !CORREO.matcher(u.username()).matches())
            throw new IllegalArgumentException("Ingresa un correo válido.");
        if (u.departamentoId() <= 0) throw new IllegalArgumentException("Selecciona el departamento.");
        if (u.rolId() <= 0) throw new IllegalArgumentException("Selecciona el rol.");
    }

    private void validarPassword(String p) {
        if (p == null || p.length() < MIN_PASSWORD)
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_PASSWORD + " caracteres.");
    }

    private Usuario normalizar(Usuario u) {
        return new Usuario(u.id(), limpiar(u.username()) == null ? null : limpiar(u.username()).toLowerCase(),
                u.rolId(), u.rol(), u.empleadoId(),
                limpiar(u.nombres()), limpiar(u.apellidos()),
                u.departamentoId(), u.departamento(), u.activo());
    }

    private static String limpiar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}