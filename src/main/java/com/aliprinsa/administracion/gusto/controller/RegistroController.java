package main.java.com.aliprinsa.administracion.gusto.controller;

import java.util.List;
import java.util.regex.Pattern;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import main.java.com.aliprinsa.administracion.gusto.model.Departamento;
import main.java.com.aliprinsa.administracion.gusto.service.AuthService;
import main.java.com.aliprinsa.administracion.gusto.util.SceneManager;

public class RegistroController {

    private static final Pattern EMAIL =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final int MIN_PASSWORD = 8;

    private final AuthService authService;
    private final SceneManager sceneManager;

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtCorreo;
    @FXML private ComboBox<Departamento> cmbDepartamento;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblError;
    @FXML private Button btnRegistrar;

    // La factory del SceneManager usa este constructor
    public RegistroController(AuthService authService, SceneManager sceneManager) {
        this.authService = authService;
        this.sceneManager = sceneManager;
    }

    @FXML
    private void initialize() {
        // Muestra el nombre del departamento en el combo
        cmbDepartamento.setConverter(new StringConverter<>() {
            @Override public String toString(Departamento d) {
                return d == null ? "" : d.getNombre();
            }
            @Override public Departamento fromString(String s) {
                return null;
            }
        });

        try {
            List<Departamento> departamentos = authService.listarDepartamentos();
            cmbDepartamento.getItems().setAll(departamentos);
        } catch (Exception e) {
            mostrarError("No se pudieron cargar los departamentos.");
        }
    }

    @FXML
    private void handleRegistro() {
        ocultarError();

        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String correo = txtCorreo.getText().trim().toLowerCase();
        Departamento departamento = cmbDepartamento.getValue();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        // ---- Validaciones ----
        if (nombres.isEmpty() || apellidos.isEmpty() || correo.isEmpty()
                || departamento == null || password.isEmpty() || confirm.isEmpty()) {
            mostrarError("Completa todos los campos.");
            return;
        }
        if (!EMAIL.matcher(correo).matches()) {
            mostrarError("El correo electrónico no es válido.");
            return;
        }
        if (password.length() < MIN_PASSWORD) {
            mostrarError("La contraseña debe tener al menos " + MIN_PASSWORD + " caracteres.");
            return;
        }
        if (!password.equals(confirm)) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        // ---- Guardar ----
        btnRegistrar.setDisable(true);
        try {
            authService.registrar(nombres, apellidos, correo, departamento.getId(), password);

            sceneManager.showAlertInfo("Registro exitoso", "Cuenta creada",
                    "Tu cuenta fue creada. Ya puedes iniciar sesión.",
                    Alert.AlertType.INFORMATION);
            handleVolver();

        } catch (IllegalArgumentException e) {
            // Errores esperados (ej. correo duplicado)
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error inesperado al registrar. Intenta de nuevo.");
            e.printStackTrace();
        } finally {
            btnRegistrar.setDisable(false);
        }
    }

    @FXML
    private void handleVolver() {
        try {
            sceneManager.showLoginView();
        } catch (Exception e) {
            mostrarError("No se pudo volver al inicio de sesión.");
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void ocultarError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}