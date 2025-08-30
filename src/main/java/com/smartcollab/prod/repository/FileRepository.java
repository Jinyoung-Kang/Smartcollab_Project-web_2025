package com.smartcollab.prod.repository;

import com.smartcollab.prod.entity.FileEntity;
import com.smartcollab.prod.entity.Folder;
import com.smartcollab.prod.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByOwnerAndIsDeletedTrue(User owner);

    List<FileEntity> findByFolderAndIsDeletedFalse(Folder folder);

    List<FileEntity> findByOwnerAndFolder_TeamIsNotNull(User owner);

    // IsDeletedFalse 조건을 추가하여 삭제되지 않은 파일만 검색하도록 변경
    List<FileEntity> findByFolderAndOriginalNameContainingIgnoreCaseAndIsDeletedFalse(Folder folder, String query);

    // ID로 파일을 조회할 때, N+1 문제 방지를 위해 activeVersion을 함께 fetch join
    @Query("SELECT f FROM FileEntity f LEFT JOIN FETCH f.activeVersion WHERE f.fileId = :fileId")
    Optional<FileEntity> findByIdWithActiveVersion(@Param("fileId") Long fileId);
}