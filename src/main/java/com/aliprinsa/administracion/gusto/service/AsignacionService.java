package main.java.com.aliprinsa.administracion.gusto.service;

import java.time.LocalDate;
import java.util.List;
import main.java.com.aliprinsa.administracion.gusto.model.Asignacion;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.repository.AsignacionRepository;

public class AsignacionService {

    private final AsignacionRepository repository;

    public AsignacionService(AsignacionRepository repository) {
        this.repository = repository;
    }

    public List<Asignacion> listar(String texto, Boolean vigente) throws Exception {
        return repository.listar(texto, vigente);
    }

    public List<Catalogo> equiposDisponibles() throws Exception { return repository.equiposDisponibles(); }
    public List<Catalogo> empleadosActivos() throws Exception { return repository.empleadosActivos(); }

    public void asignar(int equipoId, int empleadoId, LocalDate inicio, String observaciones) throws Exception {
        if (equipoId <= 0) throw new IllegalArgumentException("Selecciona el equipo.");
        if (empleadoId <= 0) throw new IllegalArgumentException("Selecciona el empleado.");
        if (inicio == null) throw new IllegalArgumentException("La fecha de inicio es obligatoria.");

        String obs = (observaciones == null || observaciones.isBlank()) ? null : observaciones.trim();
        repository.insertar(equipoId, empleadoId, inicio, obs);
    }

    public void finalizar(int asignacionId) throws Exception {
        repository.finalizar(asignacionId);
    }
}