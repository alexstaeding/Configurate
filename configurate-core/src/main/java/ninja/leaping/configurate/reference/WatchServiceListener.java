/*
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.configurate.reference;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class WatchServiceListener implements AutoCloseable {
    @SuppressWarnings("rawtypes") // fite me
    private static final WatchEvent.Kind<?>[] DEFAULT_WATCH_EVENTS = new WatchEvent.Kind[] {StandardWatchEventKinds.OVERFLOW, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY};
    private static final int PARALLEL_THRESHOLD = 100;
    private final ExecutorService executor;
    private final WatchService watchService;
    private final ConcurrentHashMap<WatchKey, DirectoryListenerRegistration> activeListeners = new ConcurrentHashMap<>();

    public static Builder builder() {
        return new Builder();
    }

    public static WatchServiceListener create() throws IOException {
        return new WatchServiceListener(Executors.newCachedThreadPool(), FileSystems.getDefault());
    }

    private WatchServiceListener(ExecutorService executor, FileSystem fileSystem) throws IOException {
        this.executor = executor;
        this.watchService = fileSystem.newWatchService();
        executor.submit(() -> {
            while (!executor.isShutdown()) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    break;
                }
                DirectoryListenerRegistration registration = activeListeners.get(key);
                if (registration != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {

                        // Process listeners
                        executor.submit(() -> {
                            try {
                                Set<Function<WatchEvent<?>, Boolean>> items;
                                synchronized (activeListeners) {
                                    items = new HashSet<>(registration.getDirListeners());
                                    Set<Function<WatchEvent<?>, Boolean>> fileListeners = registration.getFileListeners().get(event.context());
                                    if (fileListeners != null) {
                                        items.addAll(fileListeners);
                                    }
                                }
                                Set<Function<WatchEvent<?>, Boolean>> itemsToRemove = new HashSet<>();

                                for (Function<WatchEvent<?>, Boolean> cb : items) {
                                    if (!cb.apply(event)) {
                                        itemsToRemove.add(cb);
                                    }
                                }

                                synchronized (activeListeners) {
                                    registration.getDirListeners().removeAll(itemsToRemove);
                                    Set<Function<WatchEvent<?>, Boolean>> fileListeners = registration.getFileListeners().get(event.context());
                                    if (fileListeners != null) {
                                        fileListeners.removeAll(itemsToRemove);
                                        if (fileListeners.isEmpty()) {
                                            registration.getFileListeners().remove(event.context());
                                        }
                                    }
                                    if (registration.getDirListeners().isEmpty() && registration.getFileListeners().isEmpty()) {
                                        key.cancel();
                                        activeListeners.remove(key);
                                    }
                                }
                            } catch (Throwable thr) {
                                System.err.println("Error while running reload task for file " + key.watchable());
                                thr.printStackTrace();
                            }
                        });

                        // If the watch key is no longer valid, send all listeners a close event
                        if (!key.reset()) {
                            DirectoryListenerRegistration oldListeners = activeListeners.remove(key);
                            if (oldListeners != null) {
                                CloseWatchEvent closeEvent = new CloseWatchEvent(this);
                                oldListeners.getFileListeners().values().forEach(list -> {
                                    list.forEach(cb -> cb.apply(closeEvent));
                                });
                                oldListeners.getDirListeners().forEach(cb -> cb.apply(closeEvent));
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Listen for changes to a specific file or directory.
     *
     * @param file The path of the file or directory to listen for changes on
     * @param callback A callback function that will be called when changes are made. If return value is false, we will stop monitoring for changes.
     * @return the key from the underlying watch service
     * @throws IOException if a filesystem error occurs
     * @throws IllegalArgumentException if the provided path is a directory
     */
    public WatchKey listenToFile(Path file, Function<WatchEvent<?>, Boolean> callback) throws IOException, IllegalArgumentException {
        if (Files.isDirectory(file)) {
            throw new IllegalArgumentException("Path " + file + " must be a file");
        }

        WatchKey key = file.getParent().register(watchService, DEFAULT_WATCH_EVENTS);
        Path fileName = file.getFileName();
        DirectoryListenerRegistration reg = activeListeners.computeIfAbsent(key, unused ->
                new DirectoryListenerRegistration(key, new ConcurrentHashMap<>(), new HashSet<>()));
        reg.getFileListeners().computeIfAbsent(fileName, nah -> new HashSet<>()).add(callback);
        return key;
    }

    public WatchKey listenToDirectory(Path directory, Function<WatchEvent<?>, Boolean> listener) throws IOException, IllegalArgumentException {
        if (!(Files.isDirectory(directory) || !Files.exists(directory))) {
            throw new IllegalArgumentException("Path $directory must be a directory");
        }

        WatchKey key = directory.register(watchService, DEFAULT_WATCH_EVENTS);
        activeListeners.computeIfAbsent(key, extra -> new DirectoryListenerRegistration(key, new ConcurrentHashMap<>(), new HashSet<>())).getDirListeners().add(listener);
        return key;
    }

    @Override
    public void close() throws IOException {
        watchService.close();
        executor.shutdown();
        CloseWatchEvent closeEvent = new CloseWatchEvent(this);
        activeListeners.forEachValue(PARALLEL_THRESHOLD, reg -> {
            reg.getFileListeners().forEachValue(PARALLEL_THRESHOLD, list -> list.forEach(cb -> cb.apply(closeEvent)));
            reg.getDirListeners().forEach(cb -> cb.apply(closeEvent));
        });
        activeListeners.clear();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
        executor.shutdownNow();
    }

    public static class Builder {
        private ExecutorService executor;
        private FileSystem fileSystem;

        private Builder() {

        }

        public Builder withExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Builder withFileSystem(FileSystem system) {
            this.fileSystem = system;
            return this;
        }

        public WatchServiceListener build() throws IOException {
            if (executor == null) {
                executor = Executors.newCachedThreadPool();
            }
            if (fileSystem == null) {
                fileSystem = FileSystems.getDefault();
            }

            return new WatchServiceListener(executor, fileSystem);
        }
    }
}
