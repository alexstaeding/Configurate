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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@NonNull
class DirectoryListenerRegistration {
    private final WatchKey key;
    private final ConcurrentHashMap<Path, Set<Function<WatchEvent<?>, Boolean>>> fileListeners;
    private final Set<Function<WatchEvent<?>, Boolean>> dirListeners;

    public DirectoryListenerRegistration(WatchKey key, ConcurrentHashMap<Path, Set<Function<WatchEvent<?>, Boolean>>> fileListeners, Set<Function<WatchEvent<?>, Boolean>> dirListeners) {
        this.key = key;
        this.fileListeners = fileListeners;
        this.dirListeners = dirListeners;
    }

    public WatchKey getKey() {
        return key;
    }

    public ConcurrentHashMap<Path, Set<Function<WatchEvent<?>, Boolean>>> getFileListeners() {
        return fileListeners;
    }

    public Set<Function<WatchEvent<?>, Boolean>> getDirListeners() {
        return dirListeners;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DirectoryListenerRegistration)) return false;
        DirectoryListenerRegistration that = (DirectoryListenerRegistration) o;
        return getKey().equals(that.getKey()) &&
                getFileListeners().equals(that.getFileListeners()) &&
                getDirListeners().equals(that.getDirListeners());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getFileListeners(), getDirListeners());
    }
}
