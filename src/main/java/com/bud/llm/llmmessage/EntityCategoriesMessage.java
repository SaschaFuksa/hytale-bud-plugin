package com.bud.llm.llmmessage;

import java.util.Map;
import java.nio.file.Path;

public class EntityCategoriesMessage extends AbstractYamlMessage {

    private Map<String, CategoryData> categories;

    public Map<String, CategoryData> getCategories() {
        return categories;
    }

    public static class CategoryData {
        private String info;
        private Map<String, String> entities;

        public String getInfo() {
            return info;
        }

        public Map<String, String> getEntities() {
            return entities;
        }
    }

    public static EntityCategoriesMessage load(Path path) {
        return loadFromFile(EntityCategoriesMessage.class, path);
    }
}
