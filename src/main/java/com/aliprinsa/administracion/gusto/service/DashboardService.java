package main.java.com.aliprinsa.administracion.gusto.service;

import main.java.com.aliprinsa.administracion.gusto.dto.response.DashboardDTOResponse;
import main.java.com.aliprinsa.administracion.gusto.repository.DashboardRepository;

public class DashboardService {

    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    public DashboardDTOResponse obtenerResumen() throws Exception {
        return repository.obtenerResumen();
    }
}