# Configurar Bucket no S3 da Amazon AWS

## Acessar Menu IAM

![Acessar Menu IAM](C:\Users\marhl\AppData\Roaming\Typora\typora-user-images\image-20240903234655720.png)

## Criar Usuário

![Criar Usuário](C:\Users\marhl\AppData\Roaming\Typora\typora-user-images\image-20240903234706107.png)

## Adicionar Permissão ao Usuário

![Adicionar Permissão](C:\Users\marhl\AppData\Roaming\Typora\typora-user-images\image-20240903235322228.png)

## Criar Bucket Público

![Criar Bucket Público](C:\Users\marhl\AppData\Roaming\Typora\typora-user-images\image-20240903234745441.png)

## Criar Chave de Acesso (access-key-id) e Secret Access Key

![Criar Chave de Acesso](C:\Users\marhl\AppData\Roaming\Typora\typora-user-images\image-20240903234751423.png)

![Criar Secret Access Key](C:\Users\marhl\AppData\Roaming\Typora\typora-user-images\image-20240903234756850.png)

## Configurar Aplicação Java

### Criar `application.yml` e Adicionar Suas Keys Geradas

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/upload-files-app
    username: pguser
    password: pgpw
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
aws:
  s3:
    access-key-id: sua-key
    secret-access-key: sua-secret-key
    region: us-east-2
    bucket: nome-bucket
server:
  port: 8080

```

### Adicionar Dependência do S3 no `pom.xml`

```
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.24.9</version>
</dependency>
```

### Criar Classe de Configuração

```
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@Configuration
public class AwsS3Configuration {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key-id}")
    private String accessKeyId;

    @Value("${aws.s3.secret-access-key}")
    private String secretAccessKey;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Bean
    public S3Client s3Client() {
        AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }

    public void createBucket() {
        CreateBucketRequest createBucketRequest = CreateBucketRequest
            .builder()
            .bucket(bucket)
            .createBucketConfiguration(CreateBucketConfiguration.builder()
                .locationConstraint(region)
                .build())
            .build();
        s3Client().createBucket(createBucketRequest);
    }
}

```

### Criar Lógica na Camada de Serviço

```
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
     * @return Um DTO contendo o arquivo como um recurso, o tipo de mídia e o cabeçalho de disposição do conteúdo.
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

    public void save(File file) {
        fileRepository.save(file);
    }
}

```

### Implementar Seus Endpoints na Controller

```
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
```
