/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.repository;

import net.coding.ide.entity.ProjectEntity;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by vangie on 14/12/10.
 */
public interface ProjectRepository extends CrudRepository<ProjectEntity, Long> {

    ProjectEntity findBySshUrl(String sshUrl);

}
