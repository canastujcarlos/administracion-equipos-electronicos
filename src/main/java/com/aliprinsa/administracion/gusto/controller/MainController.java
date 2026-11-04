package main.java.com.aliprinsa.administracion.gusto.controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import main.java.com.aliprinsa.administracion.gusto.util.SceneManager;
import main.java.com.aliprinsa.administracion.gusto.util.SesionActual;

public class MainController {

    private final SceneManager sceneManager;

    @FXML private StackPane contenido;
    @FXML private Label lblUsuario;
    @FXML private Button btnDashboard;
    @FXML private Button btnEquipos;
    @FXML private Button btnAsignaciones;
    @FXML private Button btnMantenimientos;
    @FXML private Button btnUsuarios;

    // Debe ser public: la factory del SceneManager (otro paquete) lo invoca
    public MainController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @FXML
    private void initialize() {
        lblUsuario.setText(SesionActual.getNombreCompleto() + " · " + SesionActual.getRol());

        // Permisos: solo el administrador ve Usuarios
        boolean admin = SesionActual.esAdministrador();
        btnUsuarios.setVisible(admin);
        btnUsuarios.setManaged(admin);

        irDashboard();
    }

    // ================= Navegación =================

    @FXML
    private void irDashboard() {
        marcarActivo(btnDashboard);
        cargar("dashboard-view.fxml");
    }

    @FXML
    private void irEquipos() {
    marcarActivo(btnEquipos);
    cargar("equipos-view.fxml");   
   }

    @FXML
    private void irAsignaciones() {
        marcarActivo(btnAsignaciones);
        cargar("asignacion-view.fxml");
    }

    @FXML
    private void irMantenimientos() {
        marcarActivo(btnMantenimientos);
        cargar("mantenimiento-view.fxml");
    }

    @FXML
    private void irUsuarios() {
        marcarActivo(btnUsuarios);
        cargar("usuario-view.fxml");
    }

    @FXML
    private void cerrarSesion() {
        SesionActual.cerrar();
        try {
            sceneManager.showLoginView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /** Carga la vista en el área central sin cambiar la escena. */
    private void cargar(String fxml) {
        try {
            Parent vista = sceneManager.cargarVista(fxml);
            contenido.getChildren().setAll(vista);
        } catch (Exception e) {
            e.printStackTrace();
            sceneManager.showAlertInfo("Error", "Navegación",
                    "No se pudo abrir la pantalla: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    /** Resalta en blanco el botón de la pantalla actual. */
    private void marcarActivo(Button activo) {
        Button[] botones = {btnDashboard, btnEquipos, btnAsignaciones,
                            btnMantenimientos, btnUsuarios};
        for (Button b : botones) {
            b.getStyleClass().remove("menu-btn-activo");
        }
        activo.getStyleClass().add("menu-btn-activo");
    }
}