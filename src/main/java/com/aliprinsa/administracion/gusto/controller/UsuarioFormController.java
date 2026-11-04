package main.java.com.aliprinsa.administracion.gusto.controller;

import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import main.java.com.aliprinsa.administracion.gusto.model.Catalogo;
import main.java.com.aliprinsa.administracion.gusto.model.Usuario;
import main.java.com.aliprinsa.administracion.gusto.repository.UsuarioRepository;
import main.java.com.aliprinsa.administracion.gusto.service.UsuarioService;
import main.java.com.aliprinsa.administracion.gusto.util.SesionActual;

public class UsuarioFormController {

    @FXML private Label lblTitulo, lblError;
    @FXML private TextField txtNombres, txtApellidos, txtCorreo;
    @FXML private ComboBox<Catalogo> cmbDepartamento, cmbRol;
    @FXML private VBox boxPassword;
    @FXML private PasswordField txtPassword, txtConfirmar;
    @FXML private CheckBox chkActivo;

    private final UsuarioService service = new UsuarioService(new UsuarioRepository());
    private Usuario editado;      // null = alta
    private boolean guardado = false;

    public boolean seGuardo() { return guardado; }

    /** Llamar justo después de cargar el FXML. null = nuevo usuario. */
    public void setUsuario(Usuario u) {
        this.editado = u;
        boolean alta = (u == null);

        lblTitulo.setText(alta ? "Nuevo usuario" : "Editar usuario");
        // La contraseña solo se pide al crear (para cambiarla existe "Restablecer contraseña")
        boxPassword.setVisible(alta);
        boxPassword.setManaged(alta);
        // "Activo" solo tiene sentido al editar; un usuario nuevo siempre nace activo
        chkActivo.setVisible(!alta);
        chkActivo.setManaged(!alta);
        txtCorreo.setDisable(!alta);   // el correo es el username: no se cambia

        cargarCatalogos();
    }

    private void cargarCatalogos() {
        Task<List<List<Catalogo>>> task = new Task<>() {
            @Override protected List<List<Catalogo>> call() throws Exception {
                return List.of(service.departamentos(), service.roles());
            }
        };
        task.setOnSucceeded(ev -> {
            cmbDepartamento.setItems(FXCollections.observableArrayList(task.getValue().get(0)));
            cmbRol.setItems(FXCollections.observableArrayList(task.getValue().get(1)));
            if (editado != null) llenarCampos(editado);
            else seleccionarPorNombre(cmbRol, "Consultor");    // rol por defecto, como en el registro
        });
        task.setOnFailed(ev -> mostrarError("No se pudieron cargar los catálogos: " + task.getException().getMessage()));
        new Thread(task, "usuario-form-catalogos").start();
    }

    private void llenarCampos(Usuario u) {
        txtNombres.setText(u.nombres());
        txtApellidos.setText(u.apellidos());
        txtCorreo.setText(u.username());
        seleccionar(cmbDepartamento, u.departamentoId());
        seleccionar(cmbRol, u.rolId());
        chkActivo.setSelected(u.activo());
    }

    @FXML
    private void guardar() {
        lblError.setText("");

        String password = txtPassword.getText();
        if (editado == null && !password.equals(txtConfirmar.getText())) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        Usuario u = new Usuario(
                editado == null ? null : editado.id(),
                txtCorreo.getText(),
                idDe(cmbRol), null,
                editado == null ? null : editado.empleadoId(),
                txtNombres.getText(), txtApellidos.getText(),
                idDe(cmbDepartamento), null,
                editado == null || chkActivo.isSelected());
        String usernameActual = SesionActual.getUsername();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.guardar(u, password, usernameActual);
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
        new Thread(task, "usuario-guardar").start();
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

    private static void seleccionarPorNombre(ComboBox<Catalogo> cmb, String nombre) {
        for (Catalogo c : cmb.getItems()) {
            if (nombre.equals(c.nombre())) { cmb.getSelectionModel().select(c); return; }
        }
    }

    private void mostrarError(String msg) {
        Platform.runLater(() -> lblError.setText(msg));
    }
}