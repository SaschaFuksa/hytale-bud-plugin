package com.bud.llm.message.prompt;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.bud.BudPlugin;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class LLMPromptManager {

    private static LLMPromptManager instance;

    private final Map<String, BudMessage> budMessages = new HashMap<>();
    private WorldMessage worldInfoTemplate;
    private TimeMessage timeMessage;
    private final Map<String, ZoneMessage> zoneMessages = new HashMap<>();
    private CombatMessage combatInfoTemplate;
    private EntityCategoriesMessage entityCategories;
    private Map<String, String> systemPrompts = new HashMap<>();

    private LLMPromptManager() {
    }

    public static LLMPromptManager getInstance() {
        if (instance == null) {
            instance = new LLMPromptManager();
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
        this.worldInfoTemplate = WorldMessage.load(dataDir.resolve("world/world_system_info.yml"));
        this.timeMessage = TimeMessage.load(dataDir.resolve("world/time.yml"));
        loadZones(dataDir.resolve("world/zones"));
        this.combatInfoTemplate = CombatMessage.load(dataDir.resolve("interaction/combat.yml"));
        this.entityCategories = EntityCategoriesMessage.load(dataDir.resolve("interaction/entities.yml"));
        this.systemPrompts = SystemPromptMessage.load(dataDir.resolve("system_prompt.yml")).getPrompts();

        debugLog();
    }

    private void copyDefaults(Path dataDir, boolean overwrite) {
        String[] resources = {
                "buds/gronkh.yml", "buds/keyleth.yml", "buds/veri.yml",
                "world/world_system_info.yml", "world/time.yml",
                "world/zones/devasted_lands.yml", "world/zones/emerald_grove.yml",
                "world/zones/howling_sands.yml", "world/zones/ocean.yml", "world/zones/whisperfrost_frontiers.yml",
                "world/zones/fallback.yml",
                "interaction/entities.yml", "interaction/combat.yml", "system_prompt.yml"
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
            budMessages.put(bud, BudMessage.load(budsDir.resolve(bud + ".yml")));
        }
    }

    private void loadZones(Path zonesDir) {
        zoneMessages.clear();
        String[] zones = { "devasted_lands", "emerald_grove", "howling_sands", "ocean",
                "whisperfrost_frontiers", "fallback" };
        for (String zone : zones) {
            zoneMessages.put(zone, ZoneMessage.load(zonesDir.resolve(zone + ".yml")));
        }
    }

    public BudMessage getBudMessage(String budName) {
        BudMessage message = budMessages.get(budName.toLowerCase());
        if (message == null) {
            LoggerUtil.getLogger().warning(
                    () -> "[BUD] No message found for bud: " + budName + " (Keys: " + budMessages.keySet() + ")");
        }
        return message;
    }

    public WorldMessage getWorldInfoTemplate() {
        return worldInfoTemplate;
    }

    public TimeMessage getTimeMessage() {
        return timeMessage;
    }

    public ZoneMessage getZoneMessage(String zoneName) {
        return zoneMessages.get(zoneName.toLowerCase());
    }

    public CombatMessage getCombatInfoTemplate() {
        return combatInfoTemplate;
    }

    public EntityCategoriesMessage getEntityCategories() {
        return entityCategories;
    }

    public String getSystemPrompt(String key) {
        return systemPrompts.get(key);
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
