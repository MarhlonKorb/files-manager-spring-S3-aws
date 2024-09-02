package com.upload_files_app.upload_files_app.rest.file;
import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.upload_files_app.upload_files_app.entities.file.FileDto;
import com.upload_files_app.upload_files_app.services.FileService;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/download/{key}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String key) {
        try {
            // Baixa o arquivo
            FileDto fileDto = this.fileService.download(key);

            // Cria uma resposta HTTP com o arquivo
            return ResponseEntity.ok()
                    .contentType(fileDto.contentType())
                    .header(fileDto.contentDispositionKey(), fileDto.contentDispositionValue())
                    .body(fileDto.resource());
        } catch (IOException e) {
            // Retorna um erro 404 se o arquivo não for encontrado ou houver um problema
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            this.fileService.upload(file);
            return ResponseEntity.ok().body("Arquivo salvo com sucesso.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao enviar arquivo: " + e.getMessage());
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getLocalizedMessage());
        }
    }

} 