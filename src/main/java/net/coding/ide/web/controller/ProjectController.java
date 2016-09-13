/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.controller;

import com.google.gson.JsonObject;
import io.swagger.annotations.ApiOperation;
import net.coding.ide.dto.ProjectDTO;
import net.coding.ide.service.ProjectService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Created by vangie on 15/12/23.
 */

@RestController
public class ProjectController {

    @Autowired
    private ModelMapper mapper;

    @Autowired
    private ProjectService prjSrv;

    @ApiOperation(value = "返回用户的所有项目", httpMethod = "GET", response = JsonObject.class, notes = "返回用户所有的项目")
    @RequestMapping(value = "/projects", method = GET, produces = APPLICATION_JSON_VALUE)
    public List<ProjectDTO> projects() {

        return prjSrv.projects().stream()
                .map(p -> mapper.map(p, ProjectDTO.class))
                .collect(Collectors.toList());
    }
}
