package com.bud.feature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.bud.BudPlugin;
import com.bud.feature.bud.MoodMessage;
import com.bud.feature.combat.CombatMessage;
import com.bud.feature.combat.EntityCategoriesMessage;
import com.bud.feature.item.ItemMessage;
import com.bud.feature.world.env.WorldMessage;
import com.bud.feature.world.env.ZoneMessage;
import com.bud.feature.world.time.TimeMessage;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.messages.SystemPromptMessage;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class LLMPromptManager {

    private static LLMPromptManager instance;

    private final Map<String, BudMessage> budMessages = new ConcurrentHashMap<>();
    private WorldMessage worldInfoTemplate;
    private TimeMessage timeMessage;
    private final Map<String, ZoneMessage> zoneMessages = new ConcurrentHashMap<>();
    private CombatMessage combatInfoTemplate;
    private EntityCategoriesMessage entityCategories;
    private Map<String, String> systemPrompts = new ConcurrentHashMap<>();
    private Map<String, String> moodMessage = new ConcurrentHashMap<>();
    private ItemMessage itemPromptMessage;

    private LLMPromptManager() {
    }

    public static LLMPromptManager getInstance() {
        if (instance == null) {
            instance = new LLMPromptManager();
        }
        return instance;
    }

    public void resetPrompts() {
        this.loadPrompts(true);
    }

    public void reloadMissingPrompts() {
        this.loadPrompts(false);
    }

    private void loadPrompts(boolean overwriteDefaults) {
        Path dataDir = BudPlugin.getInstance().getDataDirectory().resolve("prompts");

        copyDefaults(dataDir, overwriteDefaults);

        loadBuds(dataDir.resolve("buds"));
        this.worldInfoTemplate = WorldMessage.load(dataDir.resolve("world/world_system_info.yml"));
        this.timeMessage = TimeMessage.load(dataDir.resolve("world/time.yml"));
        loadZones(dataDir.resolve("world/zones"));
        this.combatInfoTemplate = CombatMessage.load(dataDir.resolve("interaction/combat.yml"));
        this.entityCategories = EntityCategoriesMessage.load(dataDir.resolve("interaction/entities.yml"));
        this.systemPrompts = SystemPromptMessage.load(dataDir.resolve("system_prompt.yml")).getPrompts();
        this.moodMessage = MoodMessage.load(dataDir.resolve("buds/mood.yml")).getMood();
        this.itemPromptMessage = ItemMessage.load(dataDir.resolve("interaction/items.yml"));

        debugLog();
    }

    private void copyDefaults(Path dataDir, boolean overwrite) {
        String[] resources = {
                "buds/gronkh.yml", "buds/keyleth.yml", "buds/veri.yml",
                "world/world_system_info.yml", "world/time.yml",
                "world/zones/devastated_lands.yml", "world/zones/emerald_grove.yml",
                "world/zones/howling_sands.yml", "world/zones/ocean.yml", "world/zones/whisperfrost_frontiers.yml",
                "world/zones/fallback.yml",
                "interaction/entities.yml", "interaction/combat.yml", "system_prompt.yml", "buds/mood.yml",
                "interaction/items.yml"
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
                } catch (IOException e) {
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
        String[] zones = { "devastated_lands", "emerald_grove", "howling_sands", "ocean",
                "whisperfrost_frontiers", "fallback" };
        for (String zone : zones) {
            zoneMessages.put(zone, ZoneMessage.load(zonesDir.resolve(zone + ".yml")));
        }
    }

    @Nonnull
    public BudMessage getBudMessage(@Nonnull String budName) {
        BudMessage message = budMessages.get(budName.toLowerCase());
        if (message == null) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Bud message not found for: " + budName);
            throw new IllegalArgumentException("Bud message not found for: " + budName);
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

    public String getMoodPrompt(String moodKey) {
        return moodMessage.get(moodKey);
    }

    public ItemMessage getItemPromptMessage() {
        return itemPromptMessage;
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
