package com.bud.llm.llmmessage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import com.bud.BudPlugin;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class BudLLMPromptManager {

    private static BudLLMPromptManager instance;

    private final Map<String, BudLLMMessage> budMessages = new HashMap<>();
    private WorldInfoTemplateMessage worldInfoTemplate;
    private TimeLLMMessage timeMessage;
    private final Map<String, ZoneLLMMessage> zoneMessages = new HashMap<>();
    private CombatInfoTemplateMessage combatInfoTemplate;
    private EntityCategoriesMessage entityCategories;

    private BudLLMPromptManager() {
    }

    public static BudLLMPromptManager getInstance() {
        if (instance == null) {
            instance = new BudLLMPromptManager();
        }
        return instance;
    }

    public void reload(boolean overwriteDefaults) {
        Path dataDir = BudPlugin.getInstance().getDataDirectory().resolve("prompts");

        // Ensure directory structure and copy defaults (always override on explicit
        // init/reload)
        copyDefaults(dataDir, overwriteDefaults);

        // Load all prompts
        loadBuds(dataDir.resolve("buds"));
        this.worldInfoTemplate = WorldInfoTemplateMessage.load(dataDir.resolve("world/world_system_info.yml"));
        this.timeMessage = TimeLLMMessage.load(dataDir.resolve("world/time.yml"));
        loadZones(dataDir.resolve("world/zones"));
        this.combatInfoTemplate = CombatInfoTemplateMessage.load(dataDir.resolve("interaction/combat_system_info.yml"));
        this.entityCategories = EntityCategoriesMessage.load(dataDir.resolve("interaction/entities.yml"));

        debugLog();
    }

    private void copyDefaults(Path dataDir, boolean overwrite) {
        String[] resources = {
                "buds/gronkh.yml", "buds/keyleth.yml", "buds/veri.yml",
                "world/world_system_info.yml", "world/time.yml",
                "world/zones/devasted_lands.yml", "world/zones/dungeons.yml", "world/zones/emerald_grove.yml",
                "world/zones/howling_sands.yml", "world/zones/ocean.yml", "world/zones/whisperfrost_frontiers.yml",
                "interaction/entities.yml", "interaction/combat_system_info.yml"
        };

        for (String res : resources) {
            Path target = dataDir.resolve(res);
            if (overwrite || !Files.exists(target)) {
                try {
                    Files.createDirectories(target.getParent());
                    try (InputStream in = BudPlugin.class.getResourceAsStream("/prompts/" + res)) {
                        if (in != null) {
                            if (overwrite) {
                                Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                LoggerUtil.getLogger().info(() -> "[BUD] Prompt resource updated: " + target);
                            } else {
                                Files.copy(in, target);
                                LoggerUtil.getLogger().info(() -> "[BUD] Prompt resource created: " + target);
                            }
                        } else {
                            LoggerUtil.getLogger()
                                    .severe(() -> "[BUD] Default prompt resource not found in JAR: /prompts/" + res);
                        }
                    }
                } catch (Exception e) {
                    LoggerUtil.getLogger()
                            .severe(() -> "[BUD] Failed to copy default prompt: " + res + " - " + e.getMessage());
                }
            }
        }
    }

    private void loadBuds(Path budsDir) {
        budMessages.clear();
        String[] buds = { "gronkh", "keyleth", "veri" };
        for (String bud : buds) {
            budMessages.put(bud, BudLLMMessage.load(budsDir.resolve(bud + ".yml")));
        }
    }

    private void loadZones(Path zonesDir) {
        zoneMessages.clear();
        String[] zones = { "devasted_lands", "dungeons", "emerald_grove", "howling_sands", "ocean",
                "whisperfrost_frontiers" };
        for (String zone : zones) {
            zoneMessages.put(zone, ZoneLLMMessage.load(zonesDir.resolve(zone + ".yml")));
        }
    }

    public BudLLMMessage getBudMessage(String budName) {
        BudLLMMessage message = budMessages.get(budName.toLowerCase());
        if (message == null) {
            LoggerUtil.getLogger().warning(
                    () -> "[BUD] No message found for bud: " + budName + " (Keys: " + budMessages.keySet() + ")");
        }
        return message;
    }

    public WorldInfoTemplateMessage getWorldInfoTemplate() {
        return worldInfoTemplate;
    }

    public TimeLLMMessage getTimeMessage() {
        return timeMessage;
    }

    public ZoneLLMMessage getZoneMessage(String zoneName) {
        return zoneMessages.get(zoneName.toLowerCase());
    }

    public CombatInfoTemplateMessage getCombatInfoTemplate() {
        return combatInfoTemplate;
    }

    public EntityCategoriesMessage getEntityCategories() {
        return entityCategories;
    }

    public void debugLog() {
        Logger logger = LoggerUtil.getLogger();
        logger.info(() -> "[BUD] --- BudLLMPromptManager Debug ---");
        logger.finer(() -> "[BUD] Buds loaded: " + budMessages.keySet());
        logger.finer(() -> "[BUD] Zones loaded: " + zoneMessages.keySet());
        logger.finer(() -> "[BUD] Time message loaded: " + (timeMessage != null));
        logger.finer(() -> "[BUD] World Template loaded: " + (worldInfoTemplate != null));
        logger.finer(() -> "[BUD] Combat Template loaded: " + (combatInfoTemplate != null));
        logger.finer(
                () -> "[BUD] Entities loaded: " + (entityCategories != null && entityCategories.getCategories() != null
                        ? entityCategories.getCategories().size() + " categories"
                        : "false"));
        logger.finer(() -> "[BUD] ----------------------------------");
    }
}
