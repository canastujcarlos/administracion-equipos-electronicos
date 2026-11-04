package main.java.com.aliprinsa.administracion.gusto.dto.response;

/**
 *
 * @author admin.practicas
 */
public class LoginDTOResponse {
    private String nombre;
    private String apellido;
    private String contrasena;
    private String rol;

    //sobrecarga de metodos para instanciar objetos
    public LoginDTOResponse(){
        
    }
    
    public LoginDTOResponse(String nombre, String apellido, String contrasena, String rol) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.contrasena = contrasena;
        this.rol = rol;
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

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    } 
}
