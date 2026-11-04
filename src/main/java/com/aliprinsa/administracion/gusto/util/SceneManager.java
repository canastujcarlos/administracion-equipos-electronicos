package main.java.com.aliprinsa.administracion.gusto.util;

import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Callback;
import main.java.com.aliprinsa.administracion.gusto.controller.DashboardController;
import main.java.com.aliprinsa.administracion.gusto.controller.LoginController;
import main.java.com.aliprinsa.administracion.gusto.controller.MainController;
import main.java.com.aliprinsa.administracion.gusto.controller.RegistroController;
import main.java.com.aliprinsa.administracion.gusto.repository.AuthRepository;
import main.java.com.aliprinsa.administracion.gusto.repository.DashboardRepository;
import main.java.com.aliprinsa.administracion.gusto.service.AuthService;
import main.java.com.aliprinsa.administracion.gusto.service.DashboardService;

public class SceneManager {

    private static final String FXML_PATH = "/main/resources/view/";
    private final Stage stage;

    public SceneManager(Stage stage) {
        this.stage = stage;
    }


    public void showLoginView() throws Exception {
        stage.setMaximized(false);
        cambiarVista("login-view.fxml", "Gestión de Equipos - Iniciar sesión");
    }

    public void showRegistroView() throws Exception {
        stage.setMaximized(false);
        cambiarVista("registro-view.fxml", "Gestión de Equipos - Registro");
    }

    public void showMainView() throws Exception {
        cambiarVista("main-view.fxml", "Gestión de Equipos");
        stage.setMaximized(true);
    }


    public Parent cargarVista(String fxml) throws Exception {
        return cargar(fxml);
    }


    private Parent cargar(String fxml) throws Exception {
        URL recurso = getClass().getResource(FXML_PATH + fxml);
        if (recurso == null) {
            throw new IllegalStateException("No se encontró el FXML: " + FXML_PATH + fxml);
        }
        FXMLLoader loader = new FXMLLoader(recurso);
        loader.setControllerFactory(crearFactory());
        return loader.load();
    }

    private void cambiarVista(String fxml, String titulo) throws Exception {
        Parent root = cargar(fxml);

        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 900, 620));
            stage.setMinWidth(800);
            stage.setMinHeight(560);
            stage.centerOnScreen();
        } else {
            stage.getScene().setRoot(root);
        }
        stage.setTitle(titulo);
    }

    private Callback<Class<?>, Object> crearFactory() {
        return clazz -> {
            if (clazz == LoginController.class) {
                return new LoginController(crearAuthService(), this);
            }
            if (clazz == RegistroController.class) {
                return new RegistroController(crearAuthService(), this);
            }
            if (clazz == MainController.class) {
                return new MainController(this);
            }
  
            if (clazz == DashboardController.class) {
                return new DashboardController(new DashboardService(new DashboardRepository()));
             }

            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("No se pudo crear el controller: " + clazz.getName(), e);
            }
        };
    }

    private AuthService crearAuthService() {
        return new AuthService(new AuthRepository());
    }

//alerta modal
    public void showAlertInfo(String head, String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(head);
        alert.setContentText(content);
        alert.showAndWait();
    }
}