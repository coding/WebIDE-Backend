/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

import static javax.persistence.EnumType.STRING;

/**
 * Created by vangie on 14/12/4.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "T_WORKSPACE", uniqueConstraints = @UniqueConstraint(name = "UK_SPACE_KEY", columnNames = {"F_SPACE_KEY"})
        , indexes = {@Index(columnList = "F_CREATED_AT", name = "IDX_CREATED_AT")})
public class WorkspaceEntity extends BaseEntity {

    public enum WsWorkingStatus {
        Online,
        Offline,
        Deleted,
        Maintaining
    }

    @ManyToOne
    @JoinColumn(name = "F_PROJECT_ID", foreignKey = @ForeignKey(name = "FK_WS_PRJ"))
    @JsonIgnore
    private ProjectEntity project;

    @Column(name = "F_SPACE_KEY")
    private String spaceKey;

    @Column(name = "F_WORKING_STATUS")
    @Enumerated(STRING)
    private WsWorkingStatus workingStatus;

    @Column(name = "F_ENCODING")
    private String encoding;

    @Column(columnDefinition = "text", name = "F_DESCRIPTION")
    private String description;

}
