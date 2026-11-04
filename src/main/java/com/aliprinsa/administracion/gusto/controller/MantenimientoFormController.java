package main.java.com.aliprinsa.administracion.gusto.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.Mantenimiento;
import main.java.com.aliprinsa.administracion.gusto.repository.MantenimientoRepository;
import main.java.com.aliprinsa.administracion.gusto.service.MantenimientoService;
import main.java.com.aliprinsa.administracion.gusto.util.SesionActual;

public class MantenimientoFormController {

    @FXML private Label lblTitulo, lblError;
    @FXML private ComboBox<Catalogo> cmbEquipo, cmbEmpleado, cmbEstadoPost;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private DatePicker dpFecha;
    @FXML private TextArea txtDescripcion;

    private final MantenimientoService service = new MantenimientoService(new MantenimientoRepository());
    private Mantenimiento editado;      // null = alta
    private boolean guardado = false;

    private record Datos(List<Catalogo> equipos, List<Catalogo> empleados,
                         List<Catalogo> estados, Integer empleadoActual) {}

    public boolean seGuardo() { return guardado; }

    @FXML
    private void initialize() {
        cmbTipo.setItems(FXCollections.observableArrayList(
                MantenimientoService.PREVENTIVO, MantenimientoService.CORRECTIVO));
        cmbTipo.getSelectionModel().selectFirst();
        dpFecha.setValue(LocalDate.now());
    }

    /** Llamar justo después de cargar el FXML. null = nuevo mantenimiento. */
    public void setMantenimiento(Mantenimiento m) {
        this.editado = m;
        lblTitulo.setText(m == null ? "Nuevo mantenimiento" : "Editar mantenimiento");
        cargarCatalogos();
    }

    private void cargarCatalogos() {
        String username = SesionActual.getUsername();

        Task<Datos> task = new Task<>() {
            @Override protected Datos call() throws Exception {
                return new Datos(service.equiposMantenibles(), service.empleadosActivos(),
                                 service.estadosFuncionales(), service.empleadoIdDe(username));
            }
        };
        task.setOnSucceeded(ev -> {
            Datos d = task.getValue();

            List<Catalogo> equipos = new ArrayList<>(d.equipos());
            // Al editar, el equipo debe aparecer aunque ya no esté en la lista (p. ej. dado de baja después)
            if (editado != null && equipos.stream().noneMatch(c -> c.id() == editado.equipoId())) {
                equipos.add(0, new Catalogo(editado.equipoId(), editado.codigoEquipo() + " · " + editado.equipo()));
            }
            cmbEquipo.setItems(FXCollections.observableArrayList(equipos));
            cmbEmpleado.setItems(FXCollections.observableArrayList(d.empleados()));

            List<Catalogo> estados = new ArrayList<>();
            estados.add(new Catalogo(0, "Sin cambios"));
            estados.addAll(d.estados());
            cmbEstadoPost.setItems(FXCollections.observableArrayList(estados));
            cmbEstadoPost.getSelectionModel().selectFirst();

            if (editado != null) {
                llenarCampos(editado);
            } else if (d.empleadoActual() != null) {
                seleccionar(cmbEmpleado, d.empleadoActual());
            }
        });
        task.setOnFailed(ev -> mostrarError("No se pudieron cargar los datos: " + task.getException().getMessage()));
        new Thread(task, "mantenimiento-form-catalogos").start();
    }

    private void llenarCampos(Mantenimiento m) {
        seleccionar(cmbEquipo, m.equipoId());
        cmbEquipo.setDisable(true);                 // el equipo no se cambia al editar
        cmbTipo.setValue(m.tipo());
        dpFecha.setValue(m.fecha());
        seleccionar(cmbEmpleado, m.realizadoPorId());
        txtDescripcion.setText(m.descripcion());
    }

    @FXML
    private void guardar() {
        lblError.setText("");

        Mantenimiento m = new Mantenimiento(
                editado == null ? null : editado.id(),
                idDe(cmbEquipo), null, null,
                dpFecha.getValue(),
                cmbTipo.getValue(),
                txtDescripcion.getText(),
                idDe(cmbEmpleado), null);
        int estadoPostId = idDe(cmbEstadoPost);

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.guardar(m, estadoPostId);
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            guardado = true;
            ((Stage) lblError.getScene().getWindow()).close();
        });
        task.setOnFailed(ev -> {
            Throwable t = task.getException();
            if (t instanceof IllegalArgumentException) mostrarError(t.getMessage());
            else { t.printStackTrace(); mostrarError("Error al guardar: " + t.getMessage()); }
        });
        new Thread(task, "mantenimiento-guardar").start();
    }

    @FXML
    private void cancelar() {
        ((Stage) lblError.getScene().getWindow()).close();
    }

    // ---------- utilidades ----------

    private static int idDe(ComboBox<Catalogo> cmb) {
        return cmb.getValue() == null ? 0 : cmb.getValue().id();
    }

    private static void seleccionar(ComboBox<Catalogo> cmb, int id) {
        for (Catalogo c : cmb.getItems()) {
            if (c.id() == id) { cmb.getSelectionModel().select(c); return; }
        }
    }

    private void mostrarError(String msg) {
        Platform.runLater(() -> lblError.setText(msg));
    }
}