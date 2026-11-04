package main.java.com.aliprinsa.administracion.gusto.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.DatosComputo;
import main.java.com.aliprinsa.administracion.gusto.model.Equipo;
import main.java.com.aliprinsa.administracion.gusto.repository.EquipoRepository;
import main.java.com.aliprinsa.administracion.gusto.service.EquipoService;
import main.java.com.aliprinsa.administracion.gusto.util.SesionActual;

public class EquipoFormController {

    /** Tipos de equipo que muestran el bloque "Datos de computo". Ajusta la lista si hace falta. */
    private static final Set<String> TIPOS_CON_COMPUTO = Set.of("Laptop", "Desktop");

    @FXML private Label lblTitulo, lblError, lblLevantadoPor;

    // Identificacion y red
    @FXML private TextField txtCodigo, txtModelo, txtSerie, txtHostname, txtMac, txtIp;
    @FXML private ComboBox<Catalogo> cmbTipo, cmbMarca;

    // Ubicacion y estado
    @FXML private ComboBox<Catalogo> cmbSede, cmbUbicacion, cmbEstadoFisico, cmbEstadoFuncional;
    @FXML private CheckBox chkRequiereAtencion;

    // Fechas
    @FXML private DatePicker dpLevantamiento, dpCompra, dpGarantia;

    // Datos de computo
    @FXML private VBox boxComputo;
    @FXML private TextField txtProcesador, txtRam, txtCapacidadDisco, txtVersionSo, txtDominio, txtAntivirus;
    @FXML private ComboBox<String> cmbTipoDisco;
    @FXML private ComboBox<Catalogo> cmbSo;

    @FXML private TextArea txtAccesorios, txtObservaciones;

    private final EquipoService service = new EquipoService(new EquipoRepository());
    private Equipo equipoEditado;          // null = alta
    private Integer ubicacionPendiente;    // ubicacion a seleccionar cuando termine de cargar la lista de la sede
    private boolean guardado = false;

    public boolean seGuardo() { return guardado; }

    @FXML
    private void initialize() {
        cmbTipoDisco.setItems(FXCollections.observableArrayList("SSD", "HDD", "NVMe"));

        // El bloque de computo solo aparece (y ocupa espacio) para Laptop / Desktop
        boxComputo.managedProperty().bind(boxComputo.visibleProperty());
        boxComputo.setVisible(false);
        cmbTipo.getSelectionModel().selectedItemProperty().addListener((obs, ant, tipo) ->
                boxComputo.setVisible(tipo != null && TIPOS_CON_COMPUTO.contains(tipo.nombre())));

        // Sede -> Ubicacion en cascada
        cmbSede.getSelectionModel().selectedItemProperty().addListener((obs, ant, sede) -> {
            if (sede != null) cargarUbicaciones(sede.id() == 0 ? null : sede.id());
        });
    }

    /** Llamar justo despues de cargar el FXML. null = nuevo equipo. */
    public void setEquipo(Equipo e) {
        this.equipoEditado = e;
        lblTitulo.setText(e == null ? "Nuevo equipo" : "Editar equipo " + e.codigoInventario());

        if (e == null) {
            dpLevantamiento.setValue(LocalDate.now());
            lblLevantadoPor.setText(SesionActual.haySesion()
                    ? "Se registrará a nombre de: " + SesionActual.getNombreCompleto() : "");
        } else {
            lblLevantadoPor.setText(e.levantadoPor() == null ? "" : "Levantado por: " + e.levantadoPor());
        }
        cargarCatalogos();
    }

    // ---------- Carga de catalogos ----------

    private void cargarCatalogos() {
        Task<List<List<Catalogo>>> task = new Task<>() {
            @Override protected List<List<Catalogo>> call() throws Exception {
                return List.of(service.tipos(), service.marcas(), service.sedes(),
                               service.estadosFisicos(), service.estadosFuncionales(),
                               service.sistemasOperativos());
            }
        };
        task.setOnSucceeded(ev -> {
            List<List<Catalogo>> c = task.getValue();
            cmbTipo.setItems(FXCollections.observableArrayList(c.get(0)));
            cmbMarca.setItems(FXCollections.observableArrayList(c.get(1)));
            cmbSede.setItems(conOpcion("— Sin sede —", c.get(2)));
            cmbEstadoFisico.setItems(FXCollections.observableArrayList(c.get(3)));
            cmbEstadoFuncional.setItems(FXCollections.observableArrayList(c.get(4)));
            cmbSo.setItems(conOpcion("— Sin definir —", c.get(5)));

            if (equipoEditado != null) llenarCampos(equipoEditado);
            else valoresPorDefecto();
        });
        task.setOnFailed(ev -> mostrarError("No se pudieron cargar los catálogos: " + task.getException().getMessage()));
        new Thread(task, "equipo-form-catalogos").start();
    }

    private void cargarUbicaciones(Integer sedeId) {
        Task<List<Catalogo>> task = new Task<>() {
            @Override protected List<Catalogo> call() throws Exception {
                return service.ubicacionesPorSede(sedeId);
            }
        };
        task.setOnSucceeded(ev -> {
            cmbUbicacion.setItems(conOpcion("— Sin ubicación —", task.getValue()));
            if (ubicacionPendiente != null) {
                seleccionar(cmbUbicacion, ubicacionPendiente);
                ubicacionPendiente = null;
            }
            if (cmbUbicacion.getSelectionModel().isEmpty()) cmbUbicacion.getSelectionModel().selectFirst();
        });
        task.setOnFailed(ev -> mostrarError("No se pudieron cargar las ubicaciones: " + task.getException().getMessage()));
        new Thread(task, "equipo-form-ubicaciones").start();
    }

    private void valoresPorDefecto() {
        // Se elige por NOMBRE porque los catalogos llegan ordenados alfabeticamente
        seleccionarPorNombre(cmbEstadoFuncional, "Operativo");
        seleccionarPorNombre(cmbEstadoFisico, "Bueno");
        ubicacionPendiente = null;
        cmbSede.getSelectionModel().selectFirst();      // dispara la carga de ubicaciones
        cmbSo.getSelectionModel().selectFirst();
    }

    // ---------- Edicion: llenar el formulario ----------

    private void llenarCampos(Equipo e) {
        txtCodigo.setText(e.codigoInventario());
        txtModelo.setText(e.modelo());
        txtSerie.setText(e.numeroSerie());
        txtHostname.setText(e.hostname());
        txtMac.setText(e.macAddress());
        txtIp.setText(e.ipAddress());
        chkRequiereAtencion.setSelected(e.requiereAtencion());
        dpLevantamiento.setValue(e.fechaLevantamiento());
        dpCompra.setValue(e.fechaCompra());
        dpGarantia.setValue(e.garantiaHasta());
        txtAccesorios.setText(e.accesorios());
        txtObservaciones.setText(e.observaciones());

        seleccionar(cmbTipo, e.tipoId());
        seleccionar(cmbMarca, e.marcaId());
        seleccionar(cmbEstadoFisico, e.estadoFisicoId());
        seleccionar(cmbEstadoFuncional, e.estadoFuncionalId());

        // Sede (por nombre) y, al terminar de cargar sus ubicaciones, la ubicacion del equipo
        ubicacionPendiente = e.ubicacionId();
        Catalogo sede = null;
        if (e.sede() != null) {
            for (Catalogo s : cmbSede.getItems()) {
                if (s.nombre().equals(e.sede())) { sede = s; break; }
            }
        }
        if (sede != null) cmbSede.getSelectionModel().select(sede);
        else cmbSede.getSelectionModel().selectFirst();

        DatosComputo c = e.computo();
        if (c != null) {
            txtProcesador.setText(c.procesador());
            txtRam.setText(c.ramGb() == null ? "" : String.valueOf(c.ramGb()));
            cmbTipoDisco.setValue(c.tipoDisco());
            txtCapacidadDisco.setText(c.capacidadDiscoGb() == null ? "" : String.valueOf(c.capacidadDiscoGb()));
            if (c.soId() != null) seleccionar(cmbSo, c.soId()); else cmbSo.getSelectionModel().selectFirst();
            txtVersionSo.setText(c.versionSo());
            txtDominio.setText(c.dominioGrupo());
            txtAntivirus.setText(c.antivirus());
        } else {
            cmbSo.getSelectionModel().selectFirst();
        }
    }

    // ---------- Guardar ----------

    @FXML
    private void guardar() {
        lblError.setText("");

        final DatosComputo computo;
        try {
            computo = boxComputo.isVisible() ? leerComputo() : null;
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
            return;
        }

        Equipo e = new Equipo(
                equipoEditado == null ? null : equipoEditado.id(),
                txtCodigo.getText(),
                idDe(cmbTipo), null,
                idDe(cmbMarca), null,
                txtModelo.getText(),
                txtSerie.getText(),
                txtHostname.getText(),
                txtMac.getText(),
                txtIp.getText(),
                idOpcional(cmbUbicacion), null,
                null,                                   // sede: se deduce de la ubicacion
                idDe(cmbEstadoFisico), null,
                idDe(cmbEstadoFuncional), null,
                chkRequiereAtencion.isSelected(),
                dpLevantamiento.getValue(),
                dpCompra.getValue(),
                dpGarantia.getValue(),
                txtAccesorios.getText(),
                txtObservaciones.getText(),
                null, null, null,                       // responsable, departamento, levantadoPor: solo lectura
                computo);

        // Username (correo) del usuario logueado; si es null el equipo se asocia al primer empleado
        final String usernameActual = SesionActual.getUsername();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.guardar(e, usernameActual);
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            guardado = true;
            ((Stage) lblError.getScene().getWindow()).close();
        });
        task.setOnFailed(ev -> {
            Throwable t = task.getException();
            if (t instanceof IllegalArgumentException) mostrarError(t.getMessage());   // regla de negocio
            else { t.printStackTrace(); mostrarError("Error al guardar: " + t.getMessage()); }
        });
        new Thread(task, "equipo-guardar").start();
    }

    private DatosComputo leerComputo() {
        Catalogo so = cmbSo.getValue();
        return new DatosComputo(
                txtProcesador.getText(),
                entero(txtRam, "La RAM"),
                cmbTipoDisco.getValue(),
                entero(txtCapacidadDisco, "La capacidad del disco"),
                (so == null || so.id() == 0) ? null : so.id(), null,
                txtVersionSo.getText(),
                txtDominio.getText(),
                txtAntivirus.getText());
    }

    @FXML
    private void cancelar() {
        ((Stage) lblError.getScene().getWindow()).close();
    }

    // ---------- Utilidades ----------

    private static Integer entero(TextField campo, String nombre) {
        String t = campo.getText();
        if (t == null || t.isBlank()) return null;
        try {
            return Integer.valueOf(t.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(nombre + " debe ser un número entero.");
        }
    }

    private static ObservableList<Catalogo> conOpcion(String textoOpcion, List<Catalogo> items) {
        List<Catalogo> l = new ArrayList<>();
        l.add(new Catalogo(0, textoOpcion));
        l.addAll(items);
        return FXCollections.observableArrayList(l);
    }

    /** Id del combo (0 si no hay nada seleccionado): para campos obligatorios. */
    private static int idDe(ComboBox<Catalogo> cmb) {
        return cmb.getValue() == null ? 0 : cmb.getValue().id();
    }

    /** Id del combo, o null si esta vacio o en la opcion "— Sin ... —": para campos opcionales. */
    private static Integer idOpcional(ComboBox<Catalogo> cmb) {
        Catalogo c = cmb.getValue();
        return (c == null || c.id() == 0) ? null : c.id();
    }

    private static void seleccionar(ComboBox<Catalogo> cmb, int id) {
        for (Catalogo c : cmb.getItems()) {
            if (c.id() == id) { cmb.getSelectionModel().select(c); return; }
        }
    }

    private static void seleccionarPorNombre(ComboBox<Catalogo> cmb, String nombre) {
        for (Catalogo c : cmb.getItems()) {
            if (c.nombre().equals(nombre)) { cmb.getSelectionModel().select(c); return; }
        }
        cmb.getSelectionModel().selectFirst();
    }

    private void mostrarError(String msg) {
        Platform.runLater(() -> lblError.setText(msg));
    }
}