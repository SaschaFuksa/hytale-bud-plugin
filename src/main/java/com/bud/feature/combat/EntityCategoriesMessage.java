package com.bud.feature.combat;

import java.nio.file.Path;
import java.util.Map;

import com.bud.llm.messages.AbstractYamlMessage;

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
