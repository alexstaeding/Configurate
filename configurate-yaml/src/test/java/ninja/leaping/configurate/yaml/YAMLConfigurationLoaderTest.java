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
package ninja.leaping.configurate.yaml;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Basic sanity checks for the loader
 */
public class YAMLConfigurationLoaderTest {

    @Test
    public void testSimpleLoading() throws IOException {
        URL url = getClass().getResource("/example.yml");
        ConfigurationLoader<ConfigurationNode> loader = YAMLConfigurationLoader.builder()
                .setURL(url).build();
        ConfigurationNode node = loader.load();
        assertEquals("unicorn", node.getNode("test", "op-level").getValue());
        assertEquals("dragon", node.getNode("other", "op-level").getValue());
        assertEquals("dog park", node.getNode("other", "location").getValue());


        @SuppressWarnings("unchecked")
        Function<Object, Map<String, List<?>>> f = o -> (HashMap<String, List<?>>)o;
        List<Map<String, List<?>>> fooList = new ArrayList<>(node.getNode("foo").getList(f));
        assertEquals(0, fooList.get(0).get("bar").size());
    }
}
