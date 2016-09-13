/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Created by vangie on 15/4/7.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "T_CONFIG", uniqueConstraints = @UniqueConstraint(name = "UK_CONFIG_KEY", columnNames = {"F_KEY"}))
public class ConfigEntity extends BaseEntity {

    @Column(name = "F_KEY", nullable = false)
    private String key;

    @Column(name = "F_NAME")
    private String name;

    @Column(columnDefinition = "text", name = "F_VALUE")
    private String value;

    public ConfigEntity() {

    }

    public ConfigEntity(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
