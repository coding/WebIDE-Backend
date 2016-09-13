/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.repository;

import net.coding.ide.entity.ConfigEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by vangie on 15/4/8.
 */
public interface ConfigRepository extends CrudRepository<ConfigEntity, Long> {

    @Query("SELECT t.name FROM #{#entityName} t WHERE t.key = ?1")
    String getNameByKey(String key);

    @Query("SELECT t.value FROM #{#entityName} t WHERE t.key = ?1")
    String getValueByKey(String key);

    ConfigEntity getByKey(String key);

    @Modifying
    @Query("UPDATE #{#entityName} r SET r.name = ?2 WHERE r.key = ?1")
    void updateName(String key, String newName);

    @Modifying
    @Query("UPDATE #{#entityName} r SET r.value = ?2 WHERE r.key = ?1")
    void updateValue(String key, String newValue);
}
