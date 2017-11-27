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
import net.coding.ide.utils.Debouncer;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by phy on 2015/1/30.
 */
@Slf4j
public class WorkspaceWatcher extends Thread {
    private volatile boolean stopWatching = false;
    private Workspace ws;
    private WatchService watcher;
    private Path workingDir;
    private Map<WatchKey, Path> keys = new HashMap<>();

    private Debouncer<FileChangeEvent> debouncer;

    private static String[] ignoreDirs = new String[]{"/.git/objects/"};

    private WatchedPathStore watchedPathStore;

    private WorkspaceManager wsMgr;

    private List<Path> ignorePaths = Lists.newArrayList();

    {
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WorkspaceWatcher(WorkspaceManager wsMgr, Workspace ws, WatchedPathStore watchedPathStore, final ApplicationEventPublisher publisher) {
        this.ws = ws;
        this.wsMgr = wsMgr;
        this.workingDir = ws.getWorkingDir().toPath();
        this.watchedPathStore = watchedPathStore;

        this.debouncer = new Debouncer<>(event -> {
            log.info("publish file change event: {}", event);
            publisher.publishEvent(event);
        }, 50);

        for (String dir : ignoreDirs) {
            try {
                ignorePaths.add(ws.getPath(dir));
                watchedPathStore.add(ws.getSpaceKey(), ws.getPath(dir).toString());
            } catch (AccessDeniedException e) {
                e.printStackTrace();
            }
        }

        watchedPathStore.add(ws.getSpaceKey(), "/");
        watchedPathStore.add(ws.getSpaceKey(), "/.git/"); // for .git/HEAD
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

    public void run() {

        registerAll(workingDir);

        while ( ! stopWatching && ! isInterrupted() ) {

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

                    Path relativePath = workingDir.relativize(filePath);
                    String path = ws.getNormalizePath(relativePath.toString()).toString();

                    if ( ! path.startsWith("/.git/refs/heads/")
                            && ! watchedPathStore.hasWatched(ws.getSpaceKey(), path) ) {
                        log.debug("not watched {} on workspace {}", path, ws.getSpaceKey());
                        continue;
                    }

                    FileChangeEvent fileChangeEvent = null;

                    if (event.kind() == ENTRY_CREATE) {


                        if (isDirectory(filePath, NOFOLLOW_LINKS)) {
                            registerAll(filePath);
                        }

                        try {
                            FileInfo fileInfo = wsMgr.getFileInfo(ws, relativePath.toString());
                            fileChangeEvent = new FileCreateEvent(ws.getSpaceKey(), fileInfo);

                        } catch (NoSuchFileException e) {
                            log.debug("file " + fileName + " not found.", e);
                        }


                    } else if (event.kind() == ENTRY_MODIFY) {

                        try {
                            FileInfo fileInfo = wsMgr.getFileInfo(ws, relativePath.toString());
                            fileChangeEvent = new FileModifyEvent(ws.getSpaceKey(), fileInfo);
                        } catch (NoSuchFileException e) {
                            log.debug("file " + fileName + " not found.", e);
                        }


                    } else if (event.kind() == ENTRY_DELETE) {

                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setName(fileName.toString());
                        fileInfo.setPath(path);
                        fileChangeEvent = new FileDeleteEvent(ws.getSpaceKey(), fileInfo);

                        watchedPathStore.remove(ws.getSpaceKey(), path);

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
