package main.java.com.aliprinsa.administracion.gusto.model;

/**
 *
 * @author admin.practicas
 */
public record DatosComputo(
        String procesador,
        Integer ramGb,
        String tipoDisco,
        Integer capacidadDiscoGb,
        Integer soId, String so,
        String versionSo,
        String dominioGrupo,
        String antivirus) {
    
}
