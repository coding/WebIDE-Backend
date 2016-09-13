/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.coding.ide.entity.ProjectEntity;
import net.coding.ide.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by vangie on 16/1/27.
 */
@Service
public class ProjectServiceImpl extends BaseService implements ProjectService {
    @Autowired
    private ProjectRepository prjRepo;

    @Override
    @Transactional
    public List<ProjectEntity> projects() {
        return Lists.newArrayList(prjRepo.findAll());
    }
}
