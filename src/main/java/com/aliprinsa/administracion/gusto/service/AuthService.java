package main.java.com.aliprinsa.administracion.gusto.service;

import java.util.List;
import main.java.com.aliprinsa.administracion.gusto.dto.request.LoginDTORequest;
import main.java.com.aliprinsa.administracion.gusto.dto.response.LoginDTOResponse;
import main.java.com.aliprinsa.administracion.gusto.model.Departamento;
import main.java.com.aliprinsa.administracion.gusto.repository.AuthRepository;
import main.java.com.aliprinsa.administracion.gusto.security.jbcrypt.BCrypt;



/**
 
 * @author admin.practicas
 */
public class AuthService {
    //atributos 
    private final AuthRepository authRepository;
    
    
    //constructor 
    public AuthService(AuthRepository authRepository){
        this.authRepository = authRepository;
        
    }
    
 //metodos (acciones que el programa puede hacer)
//este metodo es como funciona el login en nuestro programa 
public LoginDTOResponse login(LoginDTORequest req) throws Exception {
    if (req == null || req.getEmail() == null || req.getPassword() == null
            || req.getEmail().isBlank() || req.getPassword().isEmpty()) {
        throw new IllegalArgumentException("Ingresa tu correo y contraseña.");
    }

    String correo = req.getEmail().trim().toLowerCase();
    LoginDTOResponse response =
            authRepository.findUserbyEmail(new LoginDTORequest(correo, req.getPassword()));

    // Mismo mensaje para "no existe" y "contraseña incorrecta"
    if (response == null
            || response.getContrasena() == null
            || !BCrypt.checkpw(req.getPassword(), response.getContrasena())) {
        throw new IllegalArgumentException("Correo o contraseña incorrectos.");
    }
    return response;
}

    public List<Departamento> listarDepartamentos() throws Exception {
    return authRepository.listarDepartamentos();
}

public void registrar(String nombres, String apellidos, String correo,
                      int departamentoId, String password) throws Exception {
    if (authRepository.existeCorreo(correo)) {
        throw new IllegalArgumentException("Ese correo ya está registrado.");
    }
    String hash = BCrypt.hashpw(password, BCrypt.gensalt());
    authRepository.registrarEmpleadoYUsuario(nombres, apellidos, correo, departamentoId, hash);
}
}