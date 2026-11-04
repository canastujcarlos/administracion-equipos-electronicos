package main.java.com.aliprinsa.administracion.gusto.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import main.java.com.aliprinsa.administracion.gusto.dto.request.LoginDTORequest;
import main.java.com.aliprinsa.administracion.gusto.dto.response.LoginDTOResponse;
import main.java.com.aliprinsa.administracion.gusto.service.AuthService;
import main.java.com.aliprinsa.administracion.gusto.util.SceneManager;
import main.java.com.aliprinsa.administracion.gusto.util.SesionActual;

public class LoginController {

    private final AuthService authService;
    private final SceneManager sceneManager;

    @FXML private TextField txtFieldEmail;
    @FXML private PasswordField txtFieldPassword;
    @FXML private Label lblError;
    @FXML private Button btnIniciarSesion;

    public LoginController(AuthService authService, SceneManager sceneManager) {
        this.authService = authService;
        this.sceneManager = sceneManager;
    }

    @FXML
    private void handleLogin() {
        ocultarError();

        String correo = txtFieldEmail.getText();
        String password = txtFieldPassword.getText();

        if (correo.isBlank() || password.isEmpty()) {
            mostrarError("Ingresa tu correo y contraseña.");
            return;
        }

        btnIniciarSesion.setDisable(true);
        try {
            LoginDTOResponse response = authService.login(new LoginDTORequest(correo, password));

            // Orden: nombre, apellido, rol, correo (el correo va al final)
            SesionActual.iniciar(response.getNombre(), response.getApellido(),
                    response.getRol(), correo.trim().toLowerCase());
            txtFieldPassword.clear();

            sceneManager.showMainView();

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("No se pudo conectar con el sistema. Intenta de nuevo.");
            e.printStackTrace();
        } finally {
            btnIniciarSesion.setDisable(false);
        }
    }

    @FXML
    private void handleIrARegistro() {
        try {
            sceneManager.showRegistroView();
        } catch (Exception e) {
            mostrarError("No se pudo abrir el registro.");
            e.printStackTrace();
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