/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.coding.ide.event.FileChangeEvent;
import net.coding.ide.event.FileCreateEvent;
import net.coding.ide.event.FileDeleteEvent;
import net.coding.ide.event.FileModifyEvent;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.Workspace;
import net.coding.ide.utils.Callback;
import net.coding.ide.utils.Debouncer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by phy on 2015/1/30.
 */
@Slf4j
@Component
public class WorkspaceWatcher extends Thread {
    private volatile boolean stopWatching = false;
    private Workspace ws;
    private WatchService watcher;
    private Path workingDir;
    private Map<WatchKey, Path> keys = new HashMap<>();

    private Debouncer<FileChangeEvent> debouncer;

    private static String[] ignoreDirs = new String[]{".git"};

    private static String[] ignoreFiles = new String[]{"^\\..+\\.sw(?:px?|o|x)$"};

    private static Pattern[] patterns = new Pattern[ignoreFiles.length];

    @Autowired
    private WorkspaceManager wsMgr;


    static {
        for (int i = 0; i < ignoreFiles.length; i++) {
            patterns[i] = Pattern.compile(ignoreFiles[i]);
        }
    }

    private List<Path> ignorePaths = Lists.newArrayList();

    {
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WorkspaceWatcher() {

    }

    public WorkspaceWatcher(WorkspaceManager wsMgr, Workspace ws, final ApplicationEventPublisher publisher) {
        this.ws = ws;
        this.wsMgr = wsMgr;
        this.workingDir = ws.getWorkingDir().toPath();
        this.debouncer = new Debouncer<>(event -> {
            log.info("publish file change event: {}", event);
            publisher.publishEvent(event);
        }, 50);

        for (String dir : ignoreDirs) {
            try {
                ignorePaths.add(ws.getPath(dir));
            } catch (AccessDeniedException e) {
                e.printStackTrace();
            }
        }

    }

    public void stopWatching() {
        this.stopWatching = true;
        try {
            watcher.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    private void registerAll(final Path start) {
        // register directory and sub-directories
        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    if (ignorePaths.contains(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        register(dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
            });
        } catch (IOException e) {
            // ignore to keep sample readable
        }
    }

    private boolean isIgnoreFile(String fileName) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(fileName);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }


    public void run() {

        registerAll(workingDir);

        while (!stopWatching) {

            try {
                WatchKey watchKey = watcher.take();

                Path dir = keys.get(watchKey);

                if (dir == null) {
                    continue;
                }

                for (WatchEvent<?> event : watchKey.pollEvents()) {


                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    Path filePath = dir.resolve(fileName);

                    if ((ignorePaths.contains(filePath)) ||
                            ((isIgnoreFile(fileName.toString())) && (event.kind() != ENTRY_DELETE))) {
                        continue;
                    }

                    Path relativePath = workingDir.relativize(filePath);


                    FileChangeEvent fileChangeEvent = null;

                    if (event.kind() == ENTRY_CREATE) {


                        if (isDirectory(filePath, NOFOLLOW_LINKS)) {
                            registerAll(filePath);
                        }

                        try {
                            FileInfo fileInfo = wsMgr.getFileInfo(ws, relativePath.toString());
                            fileChangeEvent = new FileCreateEvent(ws, fileInfo);

                        } catch (NoSuchFileException e) {
                            log.debug("file " + fileName + " not found.", e);
                        }


                    } else if (event.kind() == ENTRY_MODIFY) {

                        try {
                            FileInfo fileInfo = wsMgr.getFileInfo(ws, relativePath.toString());
                            fileChangeEvent = new FileModifyEvent(ws, fileInfo);
                        } catch (NoSuchFileException e) {
                            log.debug("file " + fileName + " not found.", e);
                        }


                    } else if (event.kind() == ENTRY_DELETE) {

                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setName(fileName.toString());
                        fileInfo.setPath(ws.getNormalizePath(relativePath.toString()).toString());
                        fileChangeEvent = new FileDeleteEvent(ws, fileInfo);

                    }

                    if (fileChangeEvent != null) {
                        synchronized (debouncer) {
                            debouncer.call(fileChangeEvent);
                        }
                    }


                }
                // reset key and remove from set if directory no longer accessible
                boolean valid = watchKey.reset();
                if (!valid) {
                    keys.remove(watchKey);
                    // all directories are inaccessible
                    if (keys.isEmpty()) {
                        // 此处有问题
                        break;
                    }
                }

            } catch (ClosedWatchServiceException e) {
                // ignore
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.debouncer.terminate();

    }
}
