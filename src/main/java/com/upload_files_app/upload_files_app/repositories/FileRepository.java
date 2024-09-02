package com.upload_files_app.upload_files_app.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.upload_files_app.upload_files_app.entities.file.File;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

}
