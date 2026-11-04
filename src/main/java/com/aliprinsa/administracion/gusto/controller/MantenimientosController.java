 package main.java.com.aliprinsa.administracion.gusto.controller;

import java.util.List;
import javafx.application.Platform;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.java.com.aliprinsa.administracion.gusto.model.Mantenimiento;
import main.java.com.aliprinsa.administracion.gusto.repository.MantenimientoRepository;
import main.java.com.aliprinsa.administracion.gusto.service.MantenimientoService;

public class MantenimientosController {

    private static final String FORM_FXML = "/main/resources/view/mantenimiento-form-view.fxml";

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private Label lblResultados;
    @FXML private Button btnEditar;

    @FXML private TableView<Mantenimiento> tabla;
    @FXML private TableColumn<Mantenimiento, String> colFecha, colCodigo, colEquipo, colTipo,
                                                     colDescripcion, colRealizadoPor;

    private final MantenimientoService service = new MantenimientoService(new MantenimientoRepository());

    @FXML
    private void initialize() {
        colFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().fecha().toString()));
        colCodigo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().codigoEquipo()));
        colEquipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().equipo()));
        colTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().tipo()));
        colDescripcion.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().descripcion().replaceAll("\\s+", " ")));   // una sola línea en la tabla
        colRealizadoPor.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().realizadoPor()));

        btnEditar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        tabla.setOnMouseClicked(ev -> { if (ev.getClickCount() == 2) editar(); });

        cmbTipo.setItems(FXCollections.observableArrayList(
                "Todos", MantenimientoService.PREVENTIVO, MantenimientoService.CORRECTIVO));
        cmbTipo.getSelectionModel().selectFirst();
        cmbTipo.setOnAction(e -> cargar());
        txtBuscar.setOnAction(e -> cargar());

        cargar();
    }

    @FXML
    private void cargar() {
        String texto = txtBuscar.getText();
        int idx = cmbTipo.getSelectionModel().getSelectedIndex();
        String tipo = idx <= 0 ? null : cmbTipo.getValue();

        Task<List<Mantenimiento>> task = new Task<>() {
            @Override protected List<Mantenimiento> call() throws Exception {
                return service.listar(texto, tipo);
            }
        };
        task.setOnSucceeded(ev -> {
            tabla.setItems(FXCollections.observableArrayList(task.getValue()));
            lblResultados.setText(task.getValue().size() + " mantenimiento(s)");
        });
        task.setOnFailed(ev -> error("No se pudieron cargar los mantenimientos", task.getException()));
        new Thread(task, "mantenimientos-carga").start();
    }

    @FXML
    private void limpiarFiltros() {
        txtBuscar.clear();
        cmbTipo.getSelectionModel().selectFirst();
        cargar();
    }

    @FXML
    private void nuevo() {
        abrirFormulario(null);
    }

    @FXML
    private void editar() {
        Mantenimiento sel = tabla.getSelectionModel().getSelectedItem();
        if (sel != null) abrirFormulario(sel);
    }

    private void abrirFormulario(Mantenimiento mantenimiento) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FORM_FXML));
            Parent root = loader.load();
            MantenimientoFormController form = loader.getController();
            form.setMantenimiento(mantenimiento);

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.initOwner(tabla.getScene().getWindow());
            dialogo.setTitle(mantenimiento == null ? "Nuevo mantenimiento" : "Editar mantenimiento");
            dialogo.setScene(new Scene(root));
            dialogo.showAndWait();

            if (form.seGuardo()) cargar();
        } catch (Exception ex) {
            error("No se pudo abrir el formulario", ex);
        }
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