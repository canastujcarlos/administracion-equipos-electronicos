package main.java.com.aliprinsa.administracion.gusto.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import main.java.com.aliprinsa.administracion.gusto.dto.response.DashboardDTOResponse;
import main.java.com.aliprinsa.administracion.gusto.service.DashboardService;

public class DashboardController {

    private final DashboardService dashboardService;

    @FXML private Label lblTotal;
    @FXML private Label lblOperativos;
    @FXML private Label lblGarantias;
    @FXML private Label lblSinAsignar;
    @FXML private Label lblEstadoCarga;
    @FXML private Button btnActualizar;
    @FXML private PieChart chartEstados;
    @FXML private BarChart<String, Number> chartTipos;

    // public: la factory del SceneManager (otro paquete) lo invoca
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @FXML
    private void initialize() {
        cargarDatos();
    }

    @FXML
    private void cargarDatos() {
        btnActualizar.setDisable(true);
        lblEstadoCarga.setText("Cargando...");

        Task<DashboardDTOResponse> tarea = new Task<>() {
            @Override
            protected DashboardDTOResponse call() throws Exception {
                return dashboardService.obtenerResumen();
            }
        };

        tarea.setOnSucceeded(e -> {
            mostrar(tarea.getValue());
            lblEstadoCarga.setText("");
            btnActualizar.setDisable(false);
        });

        tarea.setOnFailed(e -> {
            tarea.getException().printStackTrace();
            lblEstadoCarga.setText("No se pudieron cargar los datos.");
            btnActualizar.setDisable(false);
        });

        Thread hilo = new Thread(tarea, "dashboard-loader");
        hilo.setDaemon(true);   // no impide cerrar la app
        hilo.start();
    }

    private void mostrar(DashboardDTOResponse d) {
        lblTotal.setText(String.valueOf(d.getTotalEquipos()));
        lblOperativos.setText(String.valueOf(d.getPorEstado().getOrDefault("Operativo", 0)));
        lblGarantias.setText(String.valueOf(d.getGarantiasPorVencer()));
        lblSinAsignar.setText(String.valueOf(d.getSinAsignar()));

        chartEstados.getData().clear();
        d.getPorEstado().forEach((estado, cant) ->
                chartEstados.getData().add(new PieChart.Data(estado + " (" + cant + ")", cant)));

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        d.getPorTipo().forEach((tipo, cant) ->
                serie.getData().add(new XYChart.Data<>(tipo, cant)));
        chartTipos.getData().setAll(serie);
    }
}