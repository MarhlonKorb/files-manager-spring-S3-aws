package com.upload_files_app.upload_files_app.services;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.upload_files_app.upload_files_app.entities.file.File;
import com.upload_files_app.upload_files_app.entities.usuario.Usuario;
import com.upload_files_app.upload_files_app.entities.usuario.UsuarioInput;
import com.upload_files_app.upload_files_app.repositories.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    private final FileService fileService;

    public UsuarioService(UsuarioRepository usuarioRepository, FileService fileService) {
        this.usuarioRepository = usuarioRepository;
        this.fileService = fileService;
    }

    public Set<Usuario> getAll() {
        return usuarioRepository.findAll().stream().collect(Collectors.toSet());
    }

    public void create(UsuarioInput input) {
        var usuario = new Usuario();
        usuario.setNome(input.nome());
        usuario.setEmail(input.email());
        this.usuarioRepository.save(usuario);
    }

    @Transactional(rollbackOn = Exception.class)
    public void insertFile(Long idUsuario, MultipartFile file) throws IOException {
        var usuarioEncontrado = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + idUsuario));

        final var urlImagem = fileService.upload(file);
        final var arquivo = new File();
        arquivo.setDescricao(file.getName());
        arquivo.setUrl(urlImagem);

        // Define o usuário associado ao arquivo
        arquivo.setUsuarioId(usuarioEncontrado);

        // Adiciona o arquivo ao usuário e salvar o usuário
        usuarioEncontrado.getFiles().add(arquivo);
        usuarioRepository.save(usuarioEncontrado);
    }

}
