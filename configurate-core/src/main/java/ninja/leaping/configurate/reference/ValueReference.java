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

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class ValueReference<T, NodeType extends ConfigurationNode> {
    private final ConfigurationReference<NodeType> root;
    private final List<Object> path;
    private final TypeToken<T> type;

    public ValueReference(ConfigurationReference<NodeType> root, List<Object> path, TypeToken<T> type) {
        this.root = root;
        this.path = path;
        this.type = type;
    }

    public ValueReference(ConfigurationReference<NodeType> root, List<Object> path, Class<T> type) {
        this(root, path, TypeToken.of(type));
    }

    @Nullable
    public T get() {
        try {
            return getNode().getValue(type);
        } catch (ObjectMappingException e) {
            root.getErrorCallback().accept(e, ConfigurationReference.ErrorPhase.LOADING);
            return null;
        }
    }

    public boolean set(@Nullable T value) {
        try {
            getNode().setValue(type, value);
            return true;
        } catch (ObjectMappingException e) {
            root.getErrorCallback().accept(e, ConfigurationReference.ErrorPhase.SAVING);
            return false;
        }
    }

    public boolean setAndSave(@Nullable T value) throws IOException {
        if (set(value)) {
            root.save();
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public NodeType getNode() {
        return (NodeType) root.getNode().getNode(path);
    }
}
