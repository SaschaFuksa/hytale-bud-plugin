package com.bud.llm.llmmessage;

import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.introspector.BeanAccess;

/**
 * Abstract base class for messages loaded from YAML resources.
 */
public abstract class AbstractYamlMessage {

    /**
     * Loads a YAML resource and maps it to the specified class.
     * 
     * @param <T>   The type to load
     * @param clazz The class of the type to load
     * @param path  The resource path
     * @return The loaded object
     */
    protected static <T> T loadFromResource(Class<T> clazz, String path) {
        try (InputStream is = clazz.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Could not find resource: " + path);
            }
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(clazz, options));
            yaml.setBeanAccess(BeanAccess.FIELD);
            return yaml.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML from " + path, e);
        }
    }
}
