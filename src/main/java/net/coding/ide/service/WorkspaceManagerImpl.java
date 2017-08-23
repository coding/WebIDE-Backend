/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jcraft.jsch.JSchException;
import lombok.extern.slf4j.Slf4j;
import net.coding.ide.entity.ProjectEntity;
import net.coding.ide.entity.WorkspaceEntity;
import net.coding.ide.event.WorkspaceDeleteEvent;
import net.coding.ide.event.WorkspaceOfflineEvent;
import net.coding.ide.event.WorkspaceOnlineEvent;
import net.coding.ide.event.WorkspaceStatusEvent;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.FileSearchResultEntry;
import net.coding.ide.model.Workspace;
import net.coding.ide.model.exception.*;
import net.coding.ide.repository.ProjectRepository;
import net.coding.ide.repository.WorkspaceRepository;
import net.coding.ide.utils.FileUtil;
import net.coding.ide.utils.ProjectUtil;
import net.coding.ide.utils.RandomGenerator;
import net.coding.ide.utils.TemporaryFileFilter;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static net.coding.ide.entity.WorkspaceEntity.WsWorkingStatus.*;

/**
 * Created by vangie on 14/11/11.
 */
@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class WorkspaceManagerImpl extends BaseService implements WorkspaceManager {

    @Value("${SPACE_HOME}")
    private File spaceHome;

    @Autowired
    private WorkspaceRepository wsRepo;

    @Autowired
    private RandomGenerator randomGene;

    @Autowired
    private WatchedPathStore watchedPathStore;

    @Autowired
    private KeyManager keyMgr;

    @Autowired
    private GitManager gitMgr;

    @Autowired
    private ProjectRepository prjRepo;

    @Value("${USERNAME}")
    private String username;

    @Value("${REPO_CACHE_HOME}")
    private String repoCacheHome;

    @Autowired
    private ApplicationEventPublisher publisher;

    private Map<String, WorkspaceWatcher> watcherMap = Maps.newHashMap();

    private Cache<String, Workspace> wsCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .softValues()
            .build();

    @Override
    public Workspace setup(String spaceKey) {

        WorkspaceEntity wsEntity = wsRepo.findBySpaceKey(spaceKey);

        if (wsEntity == null) {
            throw new WorkspaceMissingException(format("Workspace %s is not found.", spaceKey));
        }

        if (wsEntity.getWorkingStatus() == Deleted) {
            throw new WorkspaceDeletedException(format("Workspace %s is already deleted.", spaceKey));
        }

        File baseDir = getBaseDir(spaceKey);

        Workspace ws = wsCache.getIfPresent(spaceKey);

        if (ws == null) {
            ws = new Workspace(wsEntity, baseDir);
            wsCache.put(spaceKey, ws);
        }

        if (isNeededClone(ws) || !keyMgr.isKeyExist(ws)) {
            initWorkspace(ws);
        }

        return ws;
    }

    private ProjectEntity createProject(final String gitUrl) {
        return transactionTemplate.execute(status -> doCreateProject(gitUrl));

    }

    private ProjectEntity doCreateProjectWithName(String projectName, String gitUrl) {
        ProjectEntity projectEntity = prjRepo.findByName(projectName);

        if (projectEntity != null) {
            return projectEntity;
        }

        projectEntity = new ProjectEntity();
        projectEntity.setName(projectName);
        projectEntity.setUrl(gitUrl);
        projectEntity.setIconUrl(ProjectUtil.randomIcon());

        return prjRepo.save(projectEntity);
    }

    private ProjectEntity doCreateProject(String gitUrl) {

        if (!checkGitUrl(gitUrl)) {
            throw new TransportProtocolUnsupportedException("Unsupported git transfer protocol");
        }

        ProjectEntity projectEntity = prjRepo.findByUrl(gitUrl);

        if (projectEntity != null) {
            return projectEntity;
        }

        projectEntity = new ProjectEntity();

        projectEntity.setUrl(gitUrl);
        setProjectName(projectEntity, gitUrl);

        projectEntity.setOwnerName(username);
        projectEntity.setIconUrl(ProjectUtil.randomIcon());

        projectEntity = prjRepo.save(projectEntity);

        return projectEntity;
    }

    @Override
    public Workspace createFromUrl(String gitUrl) {

        ProjectEntity projectEntity = createProject(gitUrl.trim());

        WorkspaceEntity wsEntity = createWorkspaceEntity(projectEntity);

        String spaceKey = wsEntity.getSpaceKey();

        File baseDir = getBaseDir(spaceKey);

        Workspace ws = new Workspace(wsEntity, baseDir);

        initWorkspace(ws);

        wsCache.put(spaceKey, ws);

        return ws;
    }

    public Workspace createFromTemplate(String projectName, String templateName) {
        // todo: fix always demo
        ProjectEntity projectEntity = transactionTemplate.execute(status ->
                doCreateProjectWithName(projectName, buildTemplateUrl("demo")));

        WorkspaceEntity wsEntity = createWorkspaceEntity(projectEntity);
        String spaceKey = wsEntity.getSpaceKey();

        File baseDir = getBaseDir(spaceKey);

        Workspace ws = new Workspace(wsEntity, baseDir);

        initWorkspace(ws);

        wsCache.put(spaceKey, ws);

        return ws;
    }

    private String buildTemplateUrl(String templateName) {
        File template = new File(repoCacheHome, templateName);

        if ( ! template.isDirectory() || ! template.exists() ) {
            throw new NotFoundException("could not found template");
        } else return template.getAbsolutePath();
    }

    private WorkspaceEntity createWorkspaceEntity(final ProjectEntity project) {
        return transactionTemplate.execute(status -> doCreateWorkspaceEntity(project));
    }

    private WorkspaceEntity doCreateWorkspaceEntity(final ProjectEntity project) {
        WorkspaceEntity ws = wsRepo.findNotDeletedByProject(project);

        if (ws == null) {
            String spaceKey;
            do {
                spaceKey = randomGene.generate(project.getUrl());
            } while (wsRepo.isSpaceKeyExist(spaceKey));

            WorkspaceEntity wsEntity = new WorkspaceEntity();
            wsEntity.setProject(project);
            wsEntity.setSpaceKey(spaceKey);
            wsEntity.setWorkingStatus(Offline);
            wsRepo.save(wsEntity);

            return wsEntity;
        } else {
            return ws;
        }
    }

    private void setProjectName(ProjectEntity projectEntity, String url) {
        String projectName = url.substring(url.lastIndexOf("/") + 1);
        if (projectName.endsWith(".git")) {
            projectName = projectName.substring(0, projectName.length() - 4);
        }
        projectEntity.setName(projectName);
    }

    protected boolean checkGitUrl(String url) {
        if (url.startsWith("ssh://")) {
            return true;
        } else if (url.startsWith("https://")
                || url.startsWith("http://")
                || url.startsWith("git://")
                || url.startsWith("ftp://")
                || url.startsWith("ftps://")
                || url.startsWith("rsync://")
                || url.startsWith("\\")) {
            return false;
        } else if (url.indexOf("@") != -1) {
            return true;
        } else if (url.startsWith("file://")) {
            return true;
        } else {
            File dir = new File(url);

            return dir.exists() && dir.isDirectory();
        }
    }

    @Override
    public void delete(String spaceKey) {

        if (isDeleted(spaceKey)) {
            throw new WorkspaceDeletedException(format("Workspace %s is already deleted!", spaceKey));
        }

        File baseDir = getBaseDir(spaceKey);

        publisher.publishEvent(new WorkspaceDeleteEvent(spaceKey));

        wsCache.invalidate(spaceKey);

        Workspace.purge(baseDir);
    }

    private void initWorkspace(Workspace ws) {

        prepareSshKey(ws);

        try {
            gitClone(ws);
        } catch (GitCloneAuthFailException e) {
            log.error("git clone fail", e);
            // cleanup
            delete(ws.getSpaceKey());
            throw e;
        }
    }


    private void prepareSshKey(Workspace ws) {
        keyMgr.copyToWorkspace(ws);
    }


    private void gitClone(Workspace ws) {
        if (isNeededClone(ws)) {
            try {
                gitMgr.clone(ws);
                gitMgr.config(ws);
            } catch (GitAPIException e) {
                if (e.getCause().getCause() instanceof JSchException) {
                    if (e.getCause().getCause().getMessage().equals("Auth fail")) {
                        throw new GitCloneAuthFailException("please add your public key to source project first.", e);
                    }
                }
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isNeededClone(Workspace ws) {
        File workingDir = ws.getWorkingDir();
        if (!workingDir.exists()) {
            return true;
        } else if (!workingDir.isDirectory() || workingDir.list().length == 0) {
            workingDir.delete();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Workspace getWorkspace(String spaceKey) {

        Workspace ws = wsCache.getIfPresent(spaceKey);
        if (ws == null) {
            WorkspaceEntity wsEntity = wsRepo.findBySpaceKey(spaceKey);
            File baseDir = getBaseDir(spaceKey);
            ws = new Workspace(wsEntity, baseDir);
            wsCache.put(spaceKey, ws);
        }

        return ws;
    }

    @Override
    public List<WorkspaceEntity> list() {
        return wsRepo.findNotDeleted();
    }

    @Override
    public WorkspaceEntity getWorkspaceEntity(String spaceKey) {
        return wsRepo.findBySpaceKey(spaceKey);
    }

    protected File getBaseDir(String spaceKey) {
        return new File(spaceHome, spaceKey);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE + 3)
    @EventListener
    public void handleWorkspaceStatusEvent(WorkspaceStatusEvent event) {
        String spaceKey = event.getSpaceKey();

        if (event instanceof WorkspaceOnlineEvent) {
            updateWorkingStatus(spaceKey, Online);
            watch(spaceKey);
        } else if (event instanceof WorkspaceOfflineEvent) {
            if (!wsRepo.isDeleted(spaceKey)) {
                updateWorkingStatus(spaceKey, Offline);
            }
            unwatch(spaceKey);
        } else if (event instanceof WorkspaceDeleteEvent) {
            updateWorkingStatus(spaceKey, Deleted);
            unwatch(spaceKey);
        }
    }

    private void updateWorkingStatus(String spaceKey, WorkspaceEntity.WsWorkingStatus status) {
        WorkspaceEntity entity = wsRepo.findBySpaceKey(spaceKey);
        entity.setWorkingStatus(status);
        wsRepo.save(entity);
    }



    private void watch(String spaceKey) {
        synchronized (watcherMap) {
            log.info("Starting Workspace:{} Watcher", spaceKey);

            if (!watcherMap.containsKey(spaceKey)) {
                WorkspaceWatcher watcher = createNewWatcher(spaceKey);
                watcher.start();
                watcher.setName("FileWatcherThread_" + spaceKey);
                watcherMap.put(spaceKey, watcher);
            }
        }
    }

    private void unwatch(String spaceKey) {
        synchronized (watcherMap) {
            log.info("Stopping Workspace:{} Watcher", spaceKey);

            if (watcherMap.containsKey(spaceKey)) {
                WorkspaceWatcher watcher = watcherMap.remove(spaceKey);
                watcher.stopWatching();
            }
        }
    }

    private WorkspaceWatcher createNewWatcher(String spaceKey) {
        Workspace ws = getWorkspace(spaceKey);
        return new WorkspaceWatcher(this, ws, watchedPathStore, publisher);
    }

    /**
     * 对连接文件的处理方案:
     *
     * 如果是软链接, 则：
     * 设置 fileInfo.setSymbolicLink(true)
     * 设置 fileInfo.target
     *
     * 链接是文件：
     * 1. 如果 targetExist，返回 target 的信息
     * 2. 如果是 targetNotExist，返回 link 文件信息, readable, writable 均为 false
     *
     * 链接是文件夹：
     * 1. 如果 targetExist，返回 target 的信息。
     * 2. 如果是 targetNotExist，返回 link 文件信息, 设置 isDir 为 false、 readable, writeable 均为 false
     *
     * @param ws
     * @param path
     * @return
     * @throws Exception
     */
    @Override
    public FileInfo getFileInfo(Workspace ws, String path) throws Exception {
        Path p = ws.getPath(path); // 文件相对地址

        FileInfo fileInfo = new FileInfo();

        // set isSymbolicLink
        boolean isSymbolicLink = Files.isSymbolicLink(p); // 是否为软链接
        boolean targetExist = true;

        if (!isSymbolicLink) {
            ws.mustExisted(path);
        } else { // 如果是 symbolicLink 则文件本身肯定存在
            fileInfo.setSymbolicLink(true);
            fileInfo.setTarget(Files.readSymbolicLink(p).toString());

            targetExist = ws.linkTargetExist(p);
        }

        String nPath = ws.getNormalizePath(path).toString();

        // set file name
        if (nPath.equals(".")) {
            fileInfo.setName(ws.getWorkspaceEntity().getProject().getName());
        } else {
            fileInfo.setName(p.getFileName().toString());
        }

        // set isDirectory, directoriesCount and filesCount
        boolean isDirectory = Files.isDirectory(p); // file not exist will be false

        fileInfo.setDir(isDirectory);
        fileInfo.setPath(nPath);
        fileInfo.setContentType(FileUtil.getContentType(p.toFile()));
        fileInfo.setGitStatus(gitMgr.status(ws, ws.getRelativePath(path)));

        // update file time, readable, writable
        if (!isSymbolicLink
                || (isSymbolicLink && targetExist)) {
            updateFileTime(fileInfo, p);
        } else {
            updateFileTime(fileInfo, p, LinkOption.NOFOLLOW_LINKS);
        }

        return fileInfo;
    }

    private void updateFileTime(FileInfo fileInfo, Path p) throws IOException {
        updateFileTime(fileInfo, p, null);
    }

    private void updateFileTime(FileInfo fileInfo, Path p, LinkOption linkOption) throws IOException {
        BasicFileAttributes attr;

        if (linkOption == null) {
            attr = Files.readAttributes(p, BasicFileAttributes.class);
        } else {
            attr = Files.readAttributes(p, BasicFileAttributes.class, linkOption);
        }

        // file size, lastModiled and lastAccessed

        fileInfo.setSize(attr.size());
        DateTimeZone timeZone = DateTimeZone.forID("UTC");
        DateTime lm = new DateTime(attr.lastModifiedTime().toMillis(), timeZone);
        fileInfo.setLastModified(lm.withZone(DateTimeZone.getDefault()));
        DateTime la = new DateTime(attr.lastAccessTime().toMillis(), timeZone);
        fileInfo.setLastAccessed(la.withZone(DateTimeZone.getDefault()));
    }

    @Override
    public List<FileInfo> listFiles(Workspace ws, String path, boolean order, boolean group) throws Exception {
        List<FileInfo> result = Lists.newLinkedList();

        Path p = ws.getPath(path);

        if (!Files.isDirectory(p)) {
            return Lists.newLinkedList();
        }

        if (Files.isSymbolicLink(p) && !ws.linkTargetExist(p)) {
            throw new WorkspaceIOException("It's a illegal link: target file not exist.");
        }

        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(p);

        for (Path subPath : directoryStream) {
            String s = ws.getWorkingDir().toPath().relativize(subPath).toString();

            try {
                FileInfo fileInfo = getFileInfo(ws, s);

                result.add(fileInfo);
            } catch (Exception e) {
                log.error("could not get file info for => {} " +
                        "when listing for workspace => {}, path => {}, order => {}, group => {}, exception is => {}",
                        s, ws.getSpaceKey(), path, order, group, e.getMessage());
            }
        }
        directoryStream.close();

        if (order) {
            Collections.sort(result, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        }

        if (group) {
            Collections.sort(result, (o1, o2) -> {

                if (o1.isDir() == o2.isDir()) {
                    return 0;
                } else if (o1.isDir()) {
                    return -1;
                } else {
                    return 1;
                }
            });
        }

        // filter out the temporary files
        result = TemporaryFileFilter.filter(result);

        watchedPathStore.add(ws.getSpaceKey(), path.endsWith("/") ? path : path + "/");
        return result;
    }

    @Override
    public List<FileSearchResultEntry> search(Workspace ws, String keyword, boolean includeNonProjectItems) throws IOException {
        List<FileSearchResultEntry> result = Lists.newArrayList();

        List<String> paths = ws.search(keyword, includeNonProjectItems);
        for (String path : paths) {
            Path p = ws.getPath(path);
            String contentType = FileUtil.getContentType(p.toFile());

            result.add(new FileSearchResultEntry(path, contentType));
        }

        return result;
    }

    @Override
    public boolean isOnline(String spaceKey) {
        return wsRepo.isOnline(spaceKey);
    }

    @Override
    public boolean isDeleted(String spaceKey) {
        return wsRepo.isDeleted(spaceKey);
    }

    @Override
    @Transactional
    public void setEncoding(Workspace ws, String charSet) {
        WorkspaceEntity wsEntity = wsRepo.findBySpaceKey(ws.getSpaceKey());
        if (wsEntity != null) {
            wsEntity.setEncoding(charSet);
            wsRepo.save(wsEntity);
        } else {
            log.error("can not find workspace {}.", ws.getSpaceKey());
        }
    }

}
