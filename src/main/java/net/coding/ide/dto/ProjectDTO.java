/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.dto;

import lombok.Data;

/**
 * Created by tan on 16/8/23.
 */
@Data
public class ProjectDTO {
    private Long id;

    private String name;

    private String fullName;

    private String iconUrl;

    private String gitUrl;

    private String sshUrl;

    private String httpsUrl;

    private String htmlUrl;

    private String ownerName;
}
