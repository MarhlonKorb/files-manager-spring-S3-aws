package com.upload_files_app.upload_files_app.rest.usuario;

import java.io.IOException;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.upload_files_app.upload_files_app.entities.usuario.Usuario;
import com.upload_files_app.upload_files_app.entities.usuario.UsuarioInput;
import com.upload_files_app.upload_files_app.services.UsuarioService;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService UsuarioService) {
        this.usuarioService = UsuarioService;
    }

    @GetMapping
    public Set<Usuario> getAll(){
        return usuarioService.getAll();
    }

    @PostMapping
    public void create(@RequestBody UsuarioInput input) {
        usuarioService.create(input);
    }
    
    @PostMapping("/upload/{id}")
    public void uploadImagem(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file) throws IOException {
        usuarioService.insertFile(id, file);
    }

}
