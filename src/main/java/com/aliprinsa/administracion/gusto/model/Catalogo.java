package main.java.com.aliprinsa.administracion.gusto.model;

/** Item generico de catalogo (id + nombre) para llenar ComboBox. */
public record Catalogo(int id, String nombre) {
    @Override
    public String toString() {
        return nombre;   // lo que muestra el ComboBox
    }
}