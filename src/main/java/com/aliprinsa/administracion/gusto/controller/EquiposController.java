package main.java.com.aliprinsa.administracion.gusto.controller;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.java.com.aliprinsa.administracion.gusto.util.EstadosFuncionales;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.Equipo;
import main.java.com.aliprinsa.administracion.gusto.repository.EquipoRepository;
import main.java.com.aliprinsa.administracion.gusto.service.EquipoService;
import main.java.com.aliprinsa.administracion.gusto.service.ExcelExportService;

public class EquiposController {

    private static final String FORM_FXML = "/main/resources/view/equipo-form-view.fxml";

    // Nombres de estado funcional (deben coincidir con la tabla estados_funcionales)
    private static final String ESTADO_OPERATIVO = "Operativo";
    private static final String ESTADO_CON_FALLAS = "Con fallas";
    private static final String ESTADO_FUERA_SERVICIO = "Fuera de servicio";
    private static final String ESTADO_PENDIENTE = "Pendiente de revisión";

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Catalogo> cmbTipo;
    @FXML private ComboBox<Catalogo> cmbEstado;
    @FXML private Label lblResultados;
    @FXML private Button btnEditar;
    @FXML private Button btnBaja;

    @FXML private TableView<Equipo> tabla;
    @FXML private TableColumn<Equipo, String> colCodigo, colTipo, colMarca, colModelo, colSerie,
                                              colUbicacion, colEstado, colResponsable, colGarantia;

    private final EquipoService service = new EquipoService(new EquipoRepository());

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().codigoInventario()));
        colTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().tipo()));
        colMarca.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().marca()));
        colModelo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().modelo()));
        colSerie.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().numeroSerie()));
        colUbicacion.setCellValueFactory(c -> new ReadOnlyStringWrapper(nvl(c.getValue().ubicacion(), "—")));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().estadoFuncional()));
        colResponsable.setCellValueFactory(c -> new ReadOnlyStringWrapper(nvl(c.getValue().responsable(), "Sin responsable")));
        colGarantia.setCellValueFactory(c -> new ReadOnlyStringWrapper(textoGarantia(c.getValue().garantiaHasta())));

        // Estado como insignia pastel
        colEstado.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                badge.setText(estado);
                badge.getStyleClass().setAll("badge", claseEstado(estado));
                setGraphic(badge);
                setText(null);
            }
        });

        // Editar / Dar de baja solo se habilitan con una fila seleccionada
        btnEditar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        btnBaja.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());

        // Doble clic = editar
        tabla.setOnMouseClicked(ev -> { if (ev.getClickCount() == 2) editar(); });

        // Buscar al presionar Enter o al cambiar un filtro
        txtBuscar.setOnAction(e -> cargar());
        cmbTipo.setOnAction(e -> cargar());
        cmbEstado.setOnAction(e -> cargar());

        cargarFiltros();
    }

    // ---------- Carga de datos (en hilo aparte para no congelar la ventana) ----------

    private void cargarFiltros() {
        Task<List<List<Catalogo>>> task = new Task<>() {
            @Override protected List<List<Catalogo>> call() throws Exception {
                return List.of(service.tipos(), service.estadosFuncionales());
            }
        };
        task.setOnSucceeded(ev -> {
            cmbTipo.setItems(conTodos(task.getValue().get(0)));
            cmbEstado.setItems(conTodos(task.getValue().get(1)));
            cmbTipo.getSelectionModel().selectFirst();
            cmbEstado.getSelectionModel().selectFirst();
        });
        task.setOnFailed(ev -> error("No se pudieron cargar los filtros", task.getException()));
        new Thread(task, "equipos-filtros").start();
    }

    @FXML
    private void cargar() {
        String texto = txtBuscar.getText();
        Integer tipo = idDe(cmbTipo);
        Integer estado = idDe(cmbEstado);

        Task<List<Equipo>> task = new Task<>() {
            @Override protected List<Equipo> call() throws Exception {
                return service.listar(texto, tipo, estado);
            }
        };
        task.setOnSucceeded(ev -> {
            tabla.setItems(FXCollections.observableArrayList(task.getValue()));
            lblResultados.setText(task.getValue().size() + " equipo(s)");
        });
        task.setOnFailed(ev -> error("No se pudo cargar el inventario", task.getException()));
        new Thread(task, "equipos-carga").start();
    }

    @FXML
    private void limpiarFiltros() {
        txtBuscar.clear();
        cmbTipo.getSelectionModel().selectFirst();
        cmbEstado.getSelectionModel().selectFirst();
        cargar();
    }

    // ---------- Acciones ----------

    @FXML
    private void nuevo() {
        abrirFormulario(null);
    }

    @FXML
    private void editar() {
        Equipo sel = tabla.getSelectionModel().getSelectedItem();
        if (sel != null) abrirFormulario(sel);
    }

    @FXML
    private void darDeBaja() {
        Equipo sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert conf = new Alert(AlertType.CONFIRMATION,
                "El equipo " + sel.codigoInventario() + " pasará a \"Fuera de servicio\" y, si tiene un responsable, se cerrará su asignación. "
              + "El historial se conserva.\n\n¿Continuar?",
                ButtonType.YES, ButtonType.NO);
        conf.setHeaderText("Dar de baja equipo");
        if (conf.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.darDeBaja(sel.id());
                return null;
            }
        };
        task.setOnSucceeded(ev -> cargar());
        task.setOnFailed(ev -> error("No se pudo dar de baja el equipo", task.getException()));
        new Thread(task, "equipos-baja").start();
    }

    private void abrirFormulario(Equipo equipo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FORM_FXML));
            Parent root = loader.load();
            EquipoFormController form = loader.getController();
            form.setEquipo(equipo);

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.initOwner(tabla.getScene().getWindow());
            dialogo.setTitle(equipo == null ? "Nuevo equipo" : "Editar equipo");
            dialogo.setScene(new Scene(root));
            dialogo.showAndWait();

            if (form.seGuardo()) cargar();
        } catch (Exception ex) {
            error("No se pudo abrir el formulario", ex);
        }
    }

    @FXML
    private void exportarExcel() {
        // Exporta lo que se ve en la tabla (respeta búsqueda y filtros)
        List<Equipo> datos = new ArrayList<>(tabla.getItems());
        if (datos.isEmpty()) {
            Alert a = new Alert(AlertType.INFORMATION, "No hay equipos para exportar.");
            a.setHeaderText(null);
            a.showAndWait();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte de inventario");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("inventario-equipos-"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx");

        File destino = fc.showSaveDialog(tabla.getScene().getWindow());
        if (destino == null) return;   // canceló

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                new ExcelExportService().exportarEquipos(datos, destino);
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            Alert a = new Alert(AlertType.INFORMATION, "Reporte guardado en:\n" + destino.getAbsolutePath());
            a.setHeaderText("Exportación exitosa");
            a.showAndWait();
        });
        task.setOnFailed(ev -> error("No se pudo generar el Excel", task.getException()));
        new Thread(task, "equipos-excel").start();
    }

    // ---------- Utilidades ----------

    private static String claseEstado(String estado) {
    return switch (EstadosFuncionales.categoria(estado)) {
        case OK       -> "badge-ok";
        case FALLA    -> "badge-rep";
        case BAJA     -> "badge-baja";
        case REVISION -> "badge-rev";
        default       -> "badge-otro";
    };
}

    private static javafx.collections.ObservableList<Catalogo> conTodos(List<Catalogo> items) {
        List<Catalogo> l = new ArrayList<>();
        l.add(new Catalogo(0, "Todos"));
        l.addAll(items);
        return FXCollections.observableArrayList(l);
    }

    private static Integer idDe(ComboBox<Catalogo> cmb) {
        Catalogo c = cmb.getValue();
        return c == null ? null : c.id();
    }

    private static String textoGarantia(LocalDate g) {
        if (g == null) return "—";
        long dias = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), g);
        if (dias < 0) return g + " (vencida)";
        if (dias <= 60) return g + " (" + dias + " d)";
        return g.toString();
    }

    private static String nvl(String s, String def) {
        return s == null ? def : s;
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