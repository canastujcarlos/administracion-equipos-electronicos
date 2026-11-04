package main.java.com.aliprinsa.administracion.gusto;

import javafx.application.Application;
import javafx.stage.Stage;
import main.java.com.aliprinsa.administracion.gusto.util.SceneManager;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        SceneManager sceneManager = new SceneManager(stage);
        sceneManager.showLoginView();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}   