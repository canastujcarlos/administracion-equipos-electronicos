package main.java.com.aliprinsa.administracion.gusto.controller;

import java.time.LocalDate;
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
import main.java.com.aliprinsa.administracion.gusto.repository.AsignacionRepository;
import main.java.com.aliprinsa.administracion.gusto.service.AsignacionService;

public class AsignacionFormController {

    @FXML private Label lblError;
    @FXML private ComboBox<Catalogo> cmbEquipo, cmbEmpleado;
    @FXML private DatePicker dpInicio;
    @FXML private TextArea txtObservaciones;

    private final AsignacionService service = new AsignacionService(new AsignacionRepository());
    private boolean guardado = false;

    public boolean seGuardo() { return guardado; }

    @FXML
    private void initialize() {
        dpInicio.setValue(LocalDate.now());

        Task<List<List<Catalogo>>> task = new Task<>() {
            @Override protected List<List<Catalogo>> call() throws Exception {
                return List.of(service.equiposDisponibles(), service.empleadosActivos());
            }
        };
        task.setOnSucceeded(ev -> {
            cmbEquipo.setItems(FXCollections.observableArrayList(task.getValue().get(0)));
            cmbEmpleado.setItems(FXCollections.observableArrayList(task.getValue().get(1)));
            if (task.getValue().get(0).isEmpty()) {
                mostrarError("No hay equipos operativos disponibles para asignar.");
            }
        });
        task.setOnFailed(ev -> mostrarError("No se pudieron cargar los datos: " + task.getException().getMessage()));
        new Thread(task, "asignacion-form-catalogos").start();
    }

    @FXML
    private void guardar() {
        lblError.setText("");

        int equipoId = idDe(cmbEquipo);
        int empleadoId = idDe(cmbEmpleado);
        LocalDate inicio = dpInicio.getValue();
        String obs = txtObservaciones.getText();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.asignar(equipoId, empleadoId, inicio, obs);
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
        new Thread(task, "asignacion-guardar").start();
    }

    @FXML
    private void cancelar() {
        ((Stage) lblError.getScene().getWindow()).close();
    }

    private static int idDe(ComboBox<Catalogo> cmb) {
        return cmb.getValue() == null ? 0 : cmb.getValue().id();
    }

    private void mostrarError(String msg) {
        Platform.runLater(() -> lblError.setText(msg));
    }
}