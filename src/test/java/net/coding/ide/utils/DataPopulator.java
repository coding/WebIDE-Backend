/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

import net.coding.ide.entity.ConfigEntity;
import net.coding.ide.entity.ProjectEntity;
import net.coding.ide.entity.WorkspaceEntity;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.GitLog;
import net.coding.ide.model.PersonIdent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Date;

public class DataPopulator {
    private DateTimeZone timeZone = DateTimeZone.forID("UTC");

    public ProjectEntity populateProjectEntity(){
        ProjectEntity project = new ProjectEntity();
        project.setName("test-project");
        project.setUrl("git@git.coding.net:kevenyoung03/Test02.git");
        project.setOwnerName("phying");
        return project;
    }

    public ConfigEntity populateConfigEntity() {
        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setId(0L);
        configEntity.setKey("key");
        configEntity.setName("name");
        configEntity.setValue("value");

        return configEntity;
    }

    public WorkspaceEntity populateWorkspaceEntity(){
        WorkspaceEntity wsEntity = new WorkspaceEntity();
        wsEntity.setId(1L);
        wsEntity.setProject(populateProjectEntity());
        wsEntity.setSpaceKey("qwerty");
        wsEntity.setDescription("qwerty workspace");

        wsEntity.setCreatedDate(new DateTime("2015-01-21T11:11", timeZone));
        wsEntity.setLastModifiedDate(new DateTime("2015-01-21T11:11", timeZone));
        wsEntity.setWorkingStatus(WorkspaceEntity.WsWorkingStatus.Online);

        return wsEntity;
    }

    public FileInfo populateFileInfo(String name, String path, boolean isDir, int size, DateTime date){
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(name);
        fileInfo.setDir(isDir);
        fileInfo.setPath(path);
        fileInfo.setSize(size);
        fileInfo.setLastModified(date);
        fileInfo.setLastAccessed(date);
        return fileInfo;
    }

    public GitLog populateGitLog() {
        GitLog gitLog = new GitLog();
        gitLog.setShortName("xxx");
        gitLog.setName("xxxxxx");
        gitLog.setShortName("short message");
        gitLog.setCommitTime((int)new Date().getTime() / 1000);
        gitLog.setAuthorIdent(populatePersonIdent());
        gitLog.setCommiterIdent(populatePersonIdent());
        return gitLog;
    }

    public PersonIdent populatePersonIdent() {
        PersonIdent personIdent = new PersonIdent();
        personIdent.setName("user");
        personIdent.setEmailAddress("user@coding.net");
        return personIdent;
    }
}
