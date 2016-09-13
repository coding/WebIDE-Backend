/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import static javax.persistence.EnumType.STRING;

/**
 * Created by vangie on 14/12/10.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "T_PROJECT")
public class ProjectEntity extends BaseEntity {

    @Column(name = "F_NAME")
    private String name;

    @Column(name = "F_FULL_NAME")
    private String fullName;

    @Column(name = "F_ICON_URL")
    private String iconUrl;

    @Column(name = "F_GIT_URL")
    private String gitUrl;

    @Column(name = "F_SSH_URL")
    private String sshUrl;

    @Column(name = "F_HTTPS_URL")
    private String httpsUrl;

    @Column(name = "F_HTML_URL")
    private String htmlUrl;

    @Column(name = "F_OWNER_NAME")
    private String ownerName;
}
