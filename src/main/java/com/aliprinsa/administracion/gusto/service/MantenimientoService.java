package main.java.com.aliprinsa.administracion.gusto.service;

import java.time.LocalDate;
import java.util.List;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.Mantenimiento;
import main.java.com.aliprinsa.administracion.gusto.repository.MantenimientoRepository;

public class MantenimientoService {

    public static final String PREVENTIVO = "Preventivo";
    public static final String CORRECTIVO = "Correctivo";

    private final MantenimientoRepository repository;

    public MantenimientoService(MantenimientoRepository repository) {
        this.repository = repository;
    }

    public List<Mantenimiento> listar(String texto, String tipo) throws Exception {
        return repository.listar(texto, tipo);
    }

    public List<Catalogo> equiposMantenibles() throws Exception { return repository.equiposMantenibles(); }
    public List<Catalogo> empleadosActivos() throws Exception { return repository.empleadosActivos(); }
    public List<Catalogo> estadosFuncionales() throws Exception { return repository.estadosFuncionales(); }
    public Integer empleadoIdDe(String username) throws Exception { return repository.empleadoIdDe(username); }

    /**
     * Crea (id == null) o actualiza un mantenimiento. Lanza IllegalArgumentException con un
     * mensaje listo para mostrar si algo no cumple las reglas. estadoPostId 0 = sin cambios.
     */
    public void guardar(Mantenimiento in, int estadoPostId) throws Exception {
        Mantenimiento m = normalizar(in);
        validar(m);

        if (m.id() == null) repository.insertar(m, estadoPostId);
        else repository.actualizar(m, estadoPostId);
    }

    // ---------- reglas ----------

    private void validar(Mantenimiento m) {
        if (m.equipoId() <= 0) throw new IllegalArgumentException("Selecciona el equipo.");
        if (m.fecha() == null) throw new IllegalArgumentException("La fecha es obligatoria.");
        if (m.fecha().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("La fecha no puede ser futura.");
        if (!PREVENTIVO.equals(m.tipo()) && !CORRECTIVO.equals(m.tipo()))
            throw new IllegalArgumentException("Selecciona el tipo de mantenimiento.");
        if (m.descripcion() == null) throw new IllegalArgumentException("La descripción es obligatoria.");
        if (m.realizadoPorId() <= 0) throw new IllegalArgumentException("Selecciona quién realizó el mantenimiento.");
    }

    private Mantenimiento normalizar(Mantenimiento m) {
        String desc = (m.descripcion() == null || m.descripcion().isBlank()) ? null : m.descripcion().trim();
        return new Mantenimiento(m.id(), m.equipoId(), m.codigoEquipo(), m.equipo(),
                m.fecha(), m.tipo(), desc, m.realizadoPorId(), m.realizadoPor());
    }
}