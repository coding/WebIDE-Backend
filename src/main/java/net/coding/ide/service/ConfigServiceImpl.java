/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import net.coding.ide.entity.ConfigEntity;
import net.coding.ide.repository.ConfigRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vangie on 15/4/8.
 */
@Service
public class ConfigServiceImpl extends BaseService implements ConfigService {

    @Autowired
    private ConfigRepository cfgRepo;

    @Override
    public String getValue(String key) {
        String rawCfg = cfgRepo.getValueByKey(key);
        if (rawCfg == null) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        Matcher m = Pattern.compile("\\$\\{(.+?)\\}").matcher(rawCfg);
        while (m.find()) {
            String cfgKey = m.group(1);
            if (exist(cfgKey)) {
                m.appendReplacement(result, getValue(cfgKey));
            } else {
                m.appendReplacement(result, "\\${" + cfgKey + "}");
            }
        }
        m.appendTail(result);
        return result.toString();
    }

    @Override
    public String getValue(String key, String defaultValue) {
        if (exist(key)) {
            return getValue(key);
        } else {
            return defaultValue;
        }
    }

    @Override
    public String getValue(String key, String... subs) {
        String value = getValue(key);
        if (value != null) {
            for (int i = 0; i < subs.length; i++) {
                value = value.replaceAll("\\{" + i + "\\}", subs[i]);
            }
            return value;
        }
        return null;
    }

    @Override
    public String[] getValues(String key, String separatorChar) {
        return StringUtils.split(getValue(key), separatorChar);
    }

    @Override
    @Transactional
    public void setValue(String key, String value) {
        cfgRepo.updateValue(key, value);
    }

    @Override
    public String getName(String key) {
        return cfgRepo.getNameByKey(key);
    }

    @Override
    public ConfigEntity getByKey(String key) {
        return cfgRepo.getByKey(key);
    }

    @Override
    @Transactional
    public void setName(String key, String name) {
        cfgRepo.updateName(key, name);
    }

    @Override
    @Transactional
    public void setCfg(String key, String name, String value) {
        ConfigEntity cfg = cfgRepo.getByKey(key);
        if (cfg == null)
            cfg = new ConfigEntity();
        cfg.setKey(key);
        cfg.setName(name);
        cfg.setValue(value);
        cfgRepo.save(cfg);
    }


    @Override
    public Iterable<ConfigEntity> findAll() {
        return cfgRepo.findAll();
    }

    @Override
    @Transactional
    public void deleteCfg(String key) {
        cfgRepo.delete(cfgRepo.getByKey(key));
    }


    @Override
    public boolean exist(String key) {
        return cfgRepo.getByKey(key) != null;
    }

}
