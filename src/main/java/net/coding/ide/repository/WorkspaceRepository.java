/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.repository;

import net.coding.ide.entity.ProjectEntity;
import net.coding.ide.entity.WorkspaceEntity;
import net.coding.ide.entity.WorkspaceEntity.WsWorkingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface WorkspaceRepository extends PagingAndSortingRepository<WorkspaceEntity, Long>, JpaSpecificationExecutor {

    WorkspaceEntity findBySpaceKey(String spaceKey);

    @Query("SELECT CASE count(w) WHEN 0 THEN FALSE ELSE TRUE END FROM #{#entityName} w WHERE w.spaceKey = ?1")
    boolean isSpaceKeyExist(String spaceKey);

    @Query("SELECT CASE w.workingStatus WHEN 'Deleted' THEN TRUE ELSE FALSE END FROM #{#entityName} w WHERE w.spaceKey = ?1")
    boolean isDeleted(String spaceKey);

    @Query("SELECT CASE w.workingStatus WHEN 'Online' THEN TRUE ELSE FALSE END FROM #{#entityName} w WHERE w.spaceKey = ?1")
    boolean isOnline(String spaceKey);

    @Query("SELECT w.project FROM #{#entityName} w WHERE w.spaceKey = ?1")
    ProjectEntity findProjectBySpaceKey(String spaceKey);

    WorkspaceEntity findByProject(ProjectEntity project);

    @Query("SELECT w FROM #{#entityName} w WHERE w.workingStatus != 'Deleted' and w.project=?1")
    WorkspaceEntity findNotDeletedByProject(ProjectEntity projectEntity);

    @Query("SELECT CASE count(w) WHEN 0 THEN FALSE ELSE TRUE END FROM #{#entityName} w WHERE w.project = ?1 AND w.workingStatus != 'Deleted'")
    boolean isProjectReferred(ProjectEntity project);

    @Query("SELECT w FROM #{#entityName} w WHERE w.workingStatus != 'Deleted'")
    List<WorkspaceEntity> findNotDeleted();

    @Modifying
    @Transactional
    @Query("UPDATE #{#entityName} w SET w.workingStatus = ?2 WHERE w.spaceKey = ?1")
    void updateWorkingStatus(String spaceKey, WsWorkingStatus wsWorkingStatus);
}
