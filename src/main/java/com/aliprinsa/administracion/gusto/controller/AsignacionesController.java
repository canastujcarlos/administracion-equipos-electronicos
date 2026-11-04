package main.java.com.aliprinsa.administracion.gusto.controller;

import java.util.List;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.java.com.aliprinsa.administracion.gusto.model.Asignacion;
import main.java.com.aliprinsa.administracion.gusto.repository.AsignacionRepository;
import main.java.com.aliprinsa.administracion.gusto.service.AsignacionService;

public class AsignacionesController {

    private static final String FORM_FXML = "/main/resources/view/asignacion-form-view.fxml";

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private Label lblResultados;
    @FXML private Button btnFinalizar;

    @FXML private TableView<Asignacion> tabla;
    @FXML private TableColumn<Asignacion, String> colCodigo, colEquipo, colEmpleado, colDepartamento,
                                                  colInicio, colFin, colEstado;

    private final AsignacionService service = new AsignacionService(new AsignacionRepository());

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().codigoEquipo()));
        colEquipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().equipo()));
        colEmpleado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().empleado()));
        colDepartamento.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().departamento()));
        colInicio.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().fechaInicio().toString()));
        colFin.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().fechaFin() == null ? "—" : c.getValue().fechaFin().toString()));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().vigente() ? "Vigente" : "Finalizada"));

        // Finalizar solo con una fila vigente seleccionada
        btnFinalizar.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Asignacion s = tabla.getSelectionModel().getSelectedItem();
            return s == null || !s.vigente();
        }, tabla.getSelectionModel().selectedItemProperty()));

        cmbEstado.setItems(FXCollections.observableArrayList("Todas", "Vigentes", "Finalizadas"));
        cmbEstado.getSelectionModel().selectFirst();
        cmbEstado.setOnAction(e -> cargar());
        txtBuscar.setOnAction(e -> cargar());

        cargar();
    }

    @FXML
    private void cargar() {
        String texto = txtBuscar.getText();
        int idx = cmbEstado.getSelectionModel().getSelectedIndex();
        Boolean vigente = idx == 1 ? Boolean.TRUE : idx == 2 ? Boolean.FALSE : null;

        Task<List<Asignacion>> task = new Task<>() {
            @Override protected List<Asignacion> call() throws Exception {
                return service.listar(texto, vigente);
            }
        };
        task.setOnSucceeded(ev -> {
            tabla.setItems(FXCollections.observableArrayList(task.getValue()));
            lblResultados.setText(task.getValue().size() + " asignación(es)");
        });
        task.setOnFailed(ev -> error("No se pudieron cargar las asignaciones", task.getException()));
        new Thread(task, "asignaciones-carga").start();
    }

    @FXML
    private void limpiarFiltros() {
        txtBuscar.clear();
        cmbEstado.getSelectionModel().selectFirst();
        cargar();
    }

    @FXML
    private void nueva() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FORM_FXML));
            Parent root = loader.load();
            AsignacionFormController form = loader.getController();

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.initOwner(tabla.getScene().getWindow());
            dialogo.setTitle("Nueva asignación");
            dialogo.setScene(new Scene(root));
            dialogo.showAndWait();

            if (form.seGuardo()) cargar();
        } catch (Exception ex) {
            error("No se pudo abrir el formulario", ex);
        }
    }

    @FXML
    private void finalizar() {
        Asignacion sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null || !sel.vigente()) return;

        Alert conf = new Alert(AlertType.CONFIRMATION,
                "Se finalizará la asignación de " + sel.codigoEquipo() + " a " + sel.empleado()
              + ".\nEl equipo quedará sin responsable y el historial se conserva.\n\n¿Continuar?",
                ButtonType.YES, ButtonType.NO);
        conf.setHeaderText("Finalizar asignación");
        if (conf.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.finalizar(sel.id());
                return null;
            }
        };
        task.setOnSucceeded(ev -> cargar());
        task.setOnFailed(ev -> error("No se pudo finalizar la asignación", task.getException()));
        new Thread(task, "asignaciones-finalizar").start();
    }

    private void error(String mensaje, Throwable t) {
        if (t != null) t.printStackTrace();
        Platform.runLater(() -> {
            Alert a = new Alert(AlertType.ERROR, t == null ? mensaje : mensaje + ":\n" + t.getMessage());
            a.setHeaderText(null);
            a.showAndWait();
        });
    }
}