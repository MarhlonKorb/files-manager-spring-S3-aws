package com.upload_files_app.upload_files_app.services;

import java.io.IOException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.parseMediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.upload_files_app.upload_files_app.entities.file.File;
import com.upload_files_app.upload_files_app.entities.file.FileDto;
import com.upload_files_app.upload_files_app.repositories.FileRepository;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class FileService {

    @Value("${aws.s3.bucket}")
    private String bucket;

    private final S3Client s3Client;

    private final FileRepository fileRepository;

    public FileService(S3Client s3Client, FileRepository fileRepository) {
        this.s3Client = s3Client;
        this.fileRepository = fileRepository;
    }

    /**
     * Baixa um arquivo do S3 e retorna um DTO com os dados do arquivo.
     *
     * @param key A chave do arquivo no bucket S3.
     * @return Um DTO contendo o arquivo como um recurso, o tipo de mídia e o
     * cabeçalho de disposição do conteúdo.
     * @throws IOException Se ocorrer um erro ao baixar o arquivo.
     */
    public FileDto download(String key) throws IOException {
        // Baixa o arquivo do S3 como um array de bytes
        byte[] fileBytes = downloadToByteArray(key);
        // Cria um ByteArrayResource com os bytes do arquivo
        Resource resource = new ByteArrayResource(fileBytes);
        // Define o tipo de conteúdo (Content-Type) do arquivo
        String contentType = getObjectContentType(key);
        // Converte arquivo baixado para um record de FileDto
        return new FileDto(resource, parseMediaType(contentType), HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"");
    }

    /**
     * Faz o upload de um arquivo para o S3.
     *
     * @param file O arquivo a ser carregado.
     * @throws IOException Se ocorrer um erro ao carregar o arquivo.
     */
    public String upload(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo informado");
        }
        // Cria o RequestBody a partir do InputStream do MultipartFile
        var requestBody = RequestBody.fromInputStream(file.getInputStream(), file.getSize());
        // Cria o PutObjectRequest
        final var putObjectRequest = PutObjectRequest.builder()
                .bucket(this.bucket)
                .key(fileName)
                .build();
        // Faz o upload do arquivo para o S3
        s3Client.putObject(putObjectRequest, requestBody);

        // Constrói a URL do objeto
        return s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket(this.bucket)
                .key(fileName)
                .build()).toExternalForm();
    }

    /**
     * Baixa um arquivo do S3 como um array de bytes.
     *
     * @param key A chave do arquivo no bucket S3.
     * @return O conteúdo do arquivo como um array de bytes.
     * @throws IOException Se ocorrer um erro ao baixar o arquivo.
     */
    private byte[] downloadToByteArray(String key) throws IOException {
        final var objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(objectRequest);
        return responseBytes.asByteArray();
    }

    /**
     * Obtém o tipo de conteúdo (Content-Type) do arquivo no S3.
     *
     * @param key A chave do arquivo no bucket S3.
     * @return O tipo de conteúdo do arquivo.
     */
    private String getObjectContentType(String key) {
        final var objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        if (Objects.isNull(objectRequest.responseContentType())) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return objectRequest.responseContentType();
    }

    public void save(File file){
        fileRepository.save(file);
    }

}
