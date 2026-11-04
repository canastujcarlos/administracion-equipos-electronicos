package main.java.com.aliprinsa.administracion.gusto.model;

/**
 *
 * @author admin.practicas
 */
public class Empleado {
    public String nombre;
    public String apellido;
    public int edad;
    public String departamento;
    
    public Empleado(){
        
    }

    public Empleado(String nombre, String apellido, int edad, String departamento) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = edad;
        this.departamento = departamento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }
    
}
