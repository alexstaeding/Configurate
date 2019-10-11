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

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A reference to a configuration node, that may or may not be updating
 */
public class ConfigurationReference<NodeType extends ConfigurationNode> implements AutoCloseable {
    private volatile NodeType node;
    private final ConfigurationLoader<NodeType> loader;
    private volatile BiConsumer<Exception, ErrorPhase> errorCallback = (e, unused) -> e.printStackTrace();
    private volatile boolean saveSuppressed = false, open = true;

    private ConfigurationReference(ConfigurationLoader<NodeType> loader) {
        this.loader = loader;
    }

    public static <T extends ConfigurationNode> ConfigurationReference<T> create(ConfigurationLoader<T> loader) throws IOException {
        ConfigurationReference<T> ret = new ConfigurationReference<>(loader);
        ret.load();
        return ret;
    }

    public static <T extends ConfigurationNode> ConfigurationReference<T> createWatching(Function<Path, ConfigurationLoader<T>> loaderCreator, Path file, WatchServiceListener listener) throws IOException {
        final ConfigurationReference<T> ret = new ConfigurationReference<>(loaderCreator.apply(file));
        listener.listenToFile(file, ret::onWatchEvent);
        return ret;
    }

    public void load() throws IOException {
        synchronized (this.loader) {
            node = loader.load();
        }
    }

    public void save() throws IOException {
        save(this.node);
    }

    public void save(NodeType newNode) throws IOException {
        synchronized (this.loader) {
            try {
                saveSuppressed = true;
                if (newNode != null) {
                    this.node = newNode;
                }
                loader.save(this.node);
            } finally {
                saveSuppressed = false;
            }
        }
    }

    private boolean onWatchEvent(WatchEvent<?> event) {
        if (open && !saveSuppressed) {
            try {
                load();
            } catch (Exception e) {
                errorCallback.accept(e, ErrorPhase.LOADING);
            }
        }
        return open;
    }

    public NodeType getNode() {
        return this.node;
    }

    public ConfigurationLoader<NodeType> getLoader() {
        return this.loader;
    }

    public ConfigurationNode get(Object... path) {
        return this.node.getNode(path);
    }

    public void set(Object[] path, Object value) {
        this.node.getNode(path).setValue(value);
    }

    public <T> void set(Object[] path, TypeToken<T> type, T value) throws ObjectMappingException {
        this.node.getNode(path).setValue(type, value);
    }

    public <T> ValueReference<T, NodeType> referenceTo(TypeToken<T> type, Object... path) {
        return new ValueReference<>(this, ImmutableList.copyOf(path), type);
    }

    public <T> ValueReference<T, NodeType> referenceTo(Class<T> type, Object... path) {
        return referenceTo(TypeToken.of(type), path);
    }

    public void setErrorCallback(BiConsumer<Exception, ErrorPhase> callback) {
        this.errorCallback = Objects.requireNonNull(callback, "callback");
    }

    BiConsumer<Exception, ErrorPhase> getErrorCallback() {
        return this.errorCallback;
    }

    @Override
    public void close() {
        open = false;
    }

    public enum ErrorPhase {
        LOADING, SAVING
    }
}
