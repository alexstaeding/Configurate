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

import java.nio.file.WatchEvent;

public class CloseWatchEvent implements WatchEvent<WatchServiceListener> {
    private final WatchServiceListener ctx;

    public CloseWatchEvent(WatchServiceListener ctx) {
        this.ctx = ctx;
    }

    @Override
    public Kind kind() {
        return Kind.INSTANCE;
    }

    @Override
    public int count() {
        return 0;
    }

    @Override
    public WatchServiceListener context() {
        return this.ctx;
    }

    public static class Kind implements WatchEvent.Kind<WatchServiceListener> {
        public static final Kind INSTANCE = new Kind();
        private Kind() {

        }

        @Override
        public String name() {
            return "CLOSE";
        }

        @Override
        public Class<WatchServiceListener> type() {
            return WatchServiceListener.class;
        }
    }
}
