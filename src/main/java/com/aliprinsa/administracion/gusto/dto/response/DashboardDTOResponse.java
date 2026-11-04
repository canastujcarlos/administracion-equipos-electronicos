package main.java.com.aliprinsa.administracion.gusto.dto.response;

import java.util.Map;

public class DashboardDTOResponse {

    private final int totalEquipos;
    private final int garantiasPorVencer;
    private final int sinAsignar;
    private final Map<String, Integer> porEstado;
    private final Map<String, Integer> porTipo;

    public DashboardDTOResponse(int totalEquipos, int garantiasPorVencer, int sinAsignar,
                        Map<String, Integer> porEstado, Map<String, Integer> porTipo) {
        this.totalEquipos = totalEquipos;
        this.garantiasPorVencer = garantiasPorVencer;
        this.sinAsignar = sinAsignar;
        this.porEstado = porEstado;
        this.porTipo = porTipo;
    }

    public int getTotalEquipos() {
        return totalEquipos;
    }
    public int getGarantiasPorVencer() { 
        return garantiasPorVencer; 
    }
    public int getSinAsignar() {
        return sinAsignar; 
    }
    public Map<String, Integer> getPorEstado() { 
        return porEstado;
    }
    
    public Map<String, Integer> getPorTipo() {
        return porTipo; 
    }
}