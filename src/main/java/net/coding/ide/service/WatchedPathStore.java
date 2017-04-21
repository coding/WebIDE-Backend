package net.coding.ide.service;

import com.google.common.collect.*;
import org.springframework.stereotype.Component;

import static com.google.common.collect.Multimaps.synchronizedSetMultimap;

@Component
public class WatchedPathStore {

    private SetMultimap<String, String> watchedPath = synchronizedSetMultimap(HashMultimap.create());

    public void add(String spaceKey, String path) {
        watchedPath.put(spaceKey, path);
    }

    public void remove(String spaceKey, String path) {
        watchedPath.remove(spaceKey, path);
    }

    public void clear(String spaceKey) {
        watchedPath.removeAll(spaceKey);
    }

    public boolean hasWatched(String spaceKey, String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("path should start with '/'.");
        }

        int lastSlash = path.lastIndexOf('/');
        String p = path.substring(0, lastSlash + 1);

        return watchedPath.containsEntry(spaceKey, p);
    }
}
