package com.bud.llm.messages;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

public abstract class AbstractYamlMessage {

    protected static <T> T loadFromFile(Class<T> clazz, Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return loadFromStream(clazz, is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML from file " + path, e);
        }
    }

    private static <T> T loadFromStream(Class<T> clazz, InputStream is) {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(clazz, options));
        yaml.setBeanAccess(BeanAccess.FIELD);
        return yaml.load(is);
    }
}
