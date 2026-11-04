package main.java.com.aliprinsa.administracion.gusto.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.Usuario;
import main.java.com.aliprinsa.administracion.gusto.repository.UsuarioRepository;
import main.java.com.aliprinsa.administracion.gusto.service.UsuarioService;

public class UsuariosController {

    private static final String FORM_FXML = "/main/resources/view/usuario-form-view.fxml";

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Catalogo> cmbRol;
    @FXML private Label lblResultados;
    @FXML private Button btnEditar, btnPassword;

    @FXML private TableView<Usuario> tabla;
    @FXML private TableColumn<Usuario, String> colNombre, colCorreo, colDepartamento, colRol, colEstado;

    private final UsuarioService service = new UsuarioService(new UsuarioRepository());

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombreCompleto()));
        colCorreo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().username()));
        colDepartamento.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().departamento() == null ? "—" : c.getValue().departamento()));
        colRol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().rol()));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().activo() ? "Activo" : "Inactivo"));

        btnEditar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        btnPassword.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        tabla.setOnMouseClicked(ev -> { if (ev.getClickCount() == 2) editar(); });

        txtBuscar.setOnAction(e -> cargar());
        cargarFiltros();
    }

    private void cargarFiltros() {
        Task<List<Catalogo>> task = new Task<>() {
            @Override protected List<Catalogo> call() throws Exception {
                return service.roles();
            }
        };
        task.setOnSucceeded(ev -> {
            List<Catalogo> l = new ArrayList<>();
            l.add(new Catalogo(0, "Todos"));
            l.addAll(task.getValue());
            cmbRol.setItems(FXCollections.observableArrayList(l));
            cmbRol.getSelectionModel().selectFirst();
            cmbRol.setOnAction(e -> cargar());
            cargar();
        });
        task.setOnFailed(ev -> error("No se pudieron cargar los filtros", task.getException()));
        new Thread(task, "usuarios-filtros").start();
    }

    @FXML
    private void cargar() {
        String texto = txtBuscar.getText();
        Catalogo rol = cmbRol.getValue();
        Integer rolId = rol == null ? null : rol.id();

        Task<List<Usuario>> task = new Task<>() {
            @Override protected List<Usuario> call() throws Exception {
                return service.listar(texto, rolId);
            }
        };
        task.setOnSucceeded(ev -> {
            tabla.setItems(FXCollections.observableArrayList(task.getValue()));
            lblResultados.setText(task.getValue().size() + " usuario(s)");
        });
        task.setOnFailed(ev -> error("No se pudieron cargar los usuarios", task.getException()));
        new Thread(task, "usuarios-carga").start();
    }

    @FXML
    private void limpiarFiltros() {
        txtBuscar.clear();
        cmbRol.getSelectionModel().selectFirst();
        cargar();
    }

    @FXML
    private void nuevo() {
        abrirFormulario(null);
    }

    @FXML
    private void editar() {
        Usuario sel = tabla.getSelectionModel().getSelectedItem();
        if (sel != null) abrirFormulario(sel);
    }

    @FXML
    private void restablecerPassword() {
        Usuario sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Optional<String> nueva = pedirPassword(sel.username());
        if (nueva.isEmpty()) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.cambiarPassword(sel.id(), nueva.get());
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            Alert a = new Alert(AlertType.INFORMATION, "La contraseña de " + sel.username() + " fue actualizada.");
            a.setHeaderText(null);
            a.showAndWait();
        });
        task.setOnFailed(ev -> error("No se pudo cambiar la contraseña", task.getException()));
        new Thread(task, "usuarios-password").start();
    }

    private Optional<String> pedirPassword(String usuario) {
        Dialog<String> d = new Dialog<>();
        d.initOwner(tabla.getScene().getWindow());
        d.setTitle("Restablecer contraseña");
        d.setHeaderText("Nueva contraseña para " + usuario);

        PasswordField p1 = new PasswordField();
        p1.setPromptText("Mínimo 8 caracteres");
        PasswordField p2 = new PasswordField();
        p2.setPromptText("Repite la contraseña");
        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #dc2626;");

        VBox box = new VBox(8, p1, p2, lblErr);
        box.setPadding(new Insets(10));
        d.getDialogPane().setContent(box);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, ev -> {
            if (!p1.getText().equals(p2.getText())) {
                lblErr.setText("Las contraseñas no coinciden.");
                ev.consume();
            } else if (p1.getText().length() < 8) {
                lblErr.setText("Debe tener al menos 8 caracteres.");
                ev.consume();
            }
        });
        d.setResultConverter(bt -> bt == ButtonType.OK ? p1.getText() : null);
        return d.showAndWait();
    }

    private void abrirFormulario(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FORM_FXML));
            Parent root = loader.load();
            UsuarioFormController form = loader.getController();
            form.setUsuario(usuario);

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.initOwner(tabla.getScene().getWindow());
            dialogo.setTitle(usuario == null ? "Nuevo usuario" : "Editar usuario");
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