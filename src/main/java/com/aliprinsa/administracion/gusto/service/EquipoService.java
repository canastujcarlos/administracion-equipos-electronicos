package main.java.com.aliprinsa.administracion.gusto.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.DatosComputo;
import main.java.com.aliprinsa.administracion.gusto.model.Equipo;
import main.java.com.aliprinsa.administracion.gusto.repository.EquipoRepository;

public class EquipoService {

    private static final Pattern MAC = Pattern.compile("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$");
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final Set<String> TIPOS_DISCO = Set.of("SSD", "HDD", "NVMe");

    private final EquipoRepository repository;

    public EquipoService(EquipoRepository repository) {
        this.repository = repository;
    }

    public List<Equipo> listar(String texto, Integer tipoId, Integer estadoId) throws Exception {
        return repository.listar(texto, tipoId, estadoId);
    }

    public List<Catalogo> tipos() throws Exception { return repository.catalogo("tipos_equipo"); }
    public List<Catalogo> marcas() throws Exception { return repository.catalogo("marcas"); }
    public List<Catalogo> sedes() throws Exception { return repository.catalogo("sedes"); }
    public List<Catalogo> ubicaciones() throws Exception { return repository.catalogo("ubicaciones"); }
    public List<Catalogo> ubicacionesPorSede(Integer sedeId) throws Exception { return repository.ubicacionesPorSede(sedeId); }
    public List<Catalogo> estadosFisicos() throws Exception { return repository.catalogo("estados_fisicos"); }
    public List<Catalogo> estadosFuncionales() throws Exception { return repository.catalogo("estados_funcionales"); }
    public List<Catalogo> sistemasOperativos() throws Exception { return repository.catalogo("sistemas_operativos"); }

    /**
     * Crea (id == null) o actualiza un equipo. Lanza IllegalArgumentException con un
     * mensaje listo para mostrar al usuario si algo no cumple las reglas.
     */
    public void guardar(Equipo in, String usernameActual) throws Exception {
        Equipo e = normalizar(in);
        validar(e);

        if (repository.existe("codigo_inventario", e.codigoInventario(), e.id()))
            throw new IllegalArgumentException("Ya existe un equipo con el código de inventario " + e.codigoInventario() + ".");
        if (repository.existe("numero_serie", e.numeroSerie(), e.id()))
            throw new IllegalArgumentException("Ya existe un equipo con el número de serie " + e.numeroSerie() + ".");
        if (e.macAddress() != null && repository.existe("mac_address", e.macAddress(), e.id()))
            throw new IllegalArgumentException("Ya existe un equipo con la MAC " + e.macAddress() + ".");

        if (e.id() == null) repository.insertar(e, usernameActual);
        else repository.actualizar(e);
    }

    public void darDeBaja(int equipoId) throws Exception {
        repository.darDeBaja(equipoId);
    }

    // ---------- reglas ----------

    private void validar(Equipo e) {
        if (e.codigoInventario() == null) throw new IllegalArgumentException("El código de inventario es obligatorio.");
        if (e.tipoId() <= 0) throw new IllegalArgumentException("Selecciona el tipo de equipo.");
        if (e.marcaId() <= 0) throw new IllegalArgumentException("Selecciona la marca.");
        if (e.modelo() == null) throw new IllegalArgumentException("El modelo es obligatorio.");
        if (e.numeroSerie() == null) throw new IllegalArgumentException("El número de serie es obligatorio.");
        if (e.estadoFisicoId() <= 0) throw new IllegalArgumentException("Selecciona el estado físico.");
        if (e.estadoFuncionalId() <= 0) throw new IllegalArgumentException("Selecciona el estado funcional.");
        if (e.macAddress() != null && !MAC.matcher(e.macAddress()).matches())
            throw new IllegalArgumentException("La MAC debe tener el formato AA:BB:CC:DD:EE:FF.");
        if (e.ipAddress() != null && !IPV4.matcher(e.ipAddress()).matches())
            throw new IllegalArgumentException("La dirección IP debe tener el formato 192.168.1.10.");
        if (e.fechaCompra() != null && e.garantiaHasta() != null && e.garantiaHasta().isBefore(e.fechaCompra()))
            throw new IllegalArgumentException("La garantía no puede vencer antes de la fecha de compra.");
        if (e.fechaLevantamiento() != null && e.fechaLevantamiento().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("La fecha de levantamiento no puede ser futura.");

        DatosComputo c = e.computo();
        if (c != null) {
            if (c.ramGb() != null && c.ramGb() <= 0)
                throw new IllegalArgumentException("La RAM debe ser mayor que 0.");
            if (c.capacidadDiscoGb() != null && c.capacidadDiscoGb() <= 0)
                throw new IllegalArgumentException("La capacidad del disco debe ser mayor que 0.");
            if (c.tipoDisco() != null && !TIPOS_DISCO.contains(c.tipoDisco()))
                throw new IllegalArgumentException("El tipo de disco debe ser SSD, HDD o NVMe.");
        }
    }

    /** Quita espacios y convierte vacios en null (importante: mac_address es UNIQUE y "" se repetiria). */
    private Equipo normalizar(Equipo e) {
        return new Equipo(e.id(), limpiar(e.codigoInventario()),
                e.tipoId(), e.tipo(), e.marcaId(), e.marca(),
                limpiar(e.modelo()), limpiar(e.numeroSerie()), limpiar(e.hostname()),
                e.macAddress() == null ? null : limpiar(e.macAddress().toUpperCase()),
                limpiar(e.ipAddress()),
                e.ubicacionId(), e.ubicacion(), e.sede(),
                e.estadoFisicoId(), e.estadoFisico(),
                e.estadoFuncionalId(), e.estadoFuncional(),
                e.requiereAtencion(),
                e.fechaLevantamiento(), e.fechaCompra(), e.garantiaHasta(),
                limpiar(e.accesorios()), limpiar(e.observaciones()),
                e.responsable(), e.departamento(), e.levantadoPor(),
                normalizarComputo(e.computo()));
    }

    /** Si el bloque de computo viene completamente vacio, se trata como "sin datos" (null). */
    private DatosComputo normalizarComputo(DatosComputo c) {
        if (c == null) return null;
        DatosComputo n = new DatosComputo(
                limpiar(c.procesador()), c.ramGb(), limpiar(c.tipoDisco()), c.capacidadDiscoGb(),
                c.soId(), c.so(), limpiar(c.versionSo()), limpiar(c.dominioGrupo()), limpiar(c.antivirus()));
        boolean vacio = n.procesador() == null && n.ramGb() == null && n.tipoDisco() == null
                && n.capacidadDiscoGb() == null && n.soId() == null && n.versionSo() == null
                && n.dominioGrupo() == null && n.antivirus() == null;
        return vacio ? null : n;
    }

    private static String limpiar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}