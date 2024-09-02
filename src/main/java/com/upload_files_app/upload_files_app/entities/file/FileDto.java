package com.upload_files_app.upload_files_app.entities.file;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record FileDto(Resource resource, MediaType contentType, String contentDispositionKey, String contentDispositionValue) {

}
