package com.bud.llm.llmcombatmessage;

import java.util.HashMap;
import java.util.List;

import com.bud.llm.llmbudmessage.ILLMBudNPCMessage;
import com.bud.npc.npcdata.BudFeranData;
import com.bud.npc.npcdata.BudKweebecData;
import com.bud.npc.npcdata.BudTrorkData;

public class LLMCombatMessageManager {

        private static final HashMap<String, String> ENTITY_CATEGORY_INFORMATIONS = setupEntityCategoryInformations();
        private static final HashMap<List<String>, String> ENTITY_CATEGORY_MAP = setupEntityCategoryMap();

        public static String createPrompt(String combatPrompt, ILLMBudNPCMessage npcMessage, String targetName) {
                String budInfo = npcMessage.getSystemPrompt();
                String introduction = """
                                You want to say something about the last combat actions of your Buddy.
                                First you get an overview about the different creatures in your world.
                                In the next text, you get the last combat interactions your Buddy had.
                                After the combat information, you get information about yourself.
                                In this information, you can find details about different creatures and your personal view on them.
                                Finally, select the info referring to the Buddy's enemies and say something short and related.
                                """;
                String entityInformations = getEntityInformations(targetName);
                String combat_info = """
                                The current combat information from your Buddy is as follows:
                                Your Buddy had following interaction: %s
                                """.formatted(combatPrompt);
                String combatView = npcMessage.getPersonalCombatView();
                return budInfo + "\n" + introduction + "\n" + entityInformations + "\n" + combat_info + "\n"
                                + combatView;

        }

        private static String getEntityInformations(String targetName) {
                String lowerTargetName = targetName.toLowerCase();
                for (var entry : ENTITY_CATEGORY_MAP.entrySet()) {
                        for (String keyword : entry.getKey()) {
                                if (lowerTargetName.contains(keyword)) {
                                        String category = entry.getValue();
                                        String info = ENTITY_CATEGORY_INFORMATIONS.get(category);
                                        if (category.equals("Player Allies")) {
                                                String friendData = loadDataFor(info);
                                                return "The friend you faced belongs to the category: " + category
                                                                + ". " + info + ". " + friendData;
                                        }
                                        return "The target you faced belongs to the category: " + category + ". "
                                                        + info;
                                }
                        }
                }
                return "No specific information available for the encountered entity.";
        }

        private static HashMap<String, String> setupEntityCategoryInformations() {
                HashMap<String, String> entityInfoMap = new HashMap<>();
                entityInfoMap.put("Abyssal",
                                "Mysterious and shadowy beings from the depths. They live deep in the ocean and some are considered a medium threat.");
                entityInfoMap.put("Freshwater",
                                "Creatures inhabiting lakes and rivers. Often very harmless. They clear the water and are food for many predators.");
                entityInfoMap.put("Feran",
                                "Fox-Human hybrids living in deserts, no threat as long as not attacked. They live in small groups together in tents. They love shiny objects.");
                entityInfoMap.put("Kweebec",
                                "Small plant humanoids living in forests, very peaceful, smart and no threat. They love plants and animals. They protect the forest and the old temples.");
                entityInfoMap.put("Goblin",
                                "Small, mischievous humanoids that often cause trouble for adventurers. They can be a medium threat and are often annoying. They like to steal items. They live in caves or small camps.");
                entityInfoMap.put("Outlander",
                                "Savage and brutal humanoid warriors from harsh environments. They are considered a high threat. They live in tribes and value strength above all. They are also like a sect and eat victims alive.");
                entityInfoMap.put("Mythic",
                                "Powerful and legendary beings. Often people think they are rumors and don't exist. If you see one, you feel extremely dangerous and fascinated at the same time. They are a high threat.");
                entityInfoMap.put("Trork",
                                "Strong and sturdy orc-like creatures from the mountains. They are a medium threat. They live in tribes and value strength. They like stones and minerals.");
                entityInfoMap.put("Undead",
                                "Reanimated corpses. They are a high threat and often attack in groups. They are relentless and don't feel pain. At night, they are even more dangerous. The most dangerous are the white wanderers in icy regions.");
                entityInfoMap.put("Voidspawn",
                                "Dark and twisted creatures born from the void. They are a high threat and often have strange abilities. They corrupt the environment around them. They only appear at night.");
                entityInfoMap.put("Livestock",
                                "Domesticated animals living all over the world. They are no threat and often kept for farming. They provide food and materials.");
                entityInfoMap.put("Avian",
                                "Bird-like creatures that soar the skies. They are no threat and often admired for their beauty.");
                entityInfoMap.put("Critter",
                                "Small and nimble animals. Some can be a medium threat, especially the poisonous ones. They are often food for bigger predators.");
                entityInfoMap.put("Reptile",
                                "Cold-blooded creatures. Some can be a medium threat, especially the bigger ones.");
                entityInfoMap.put("Scarak",
                                "Insectoid beings with hive minds. They are a high threat and often attack in swarms. They can quickly overwhelm their enemies with numbers and are toxic.");
                entityInfoMap.put("Predators",
                                "Fierce hunting animals. They are a high threat and often at the top of the food chain. They are strong, fast and cunning.");
                entityInfoMap.put("Elemental",
                                "Beings made of natural elements. They are a medium threat and often protect sacred places. They can control their element and use it in combat.");
                entityInfoMap.put("Player Allies",
                                "Other Buds that assist you in your adventures. They are no threat and fight alongside you.");
                return entityInfoMap;
        }

        private static HashMap<List<String>, String> setupEntityCategoryMap() {
                HashMap<List<String>, String> entityCategoryMap = new HashMap<>();
                entityCategoryMap.put(List.of("eel", "shark", "shellfish", "trilobite", "whale"),
                                "Abyssal");
                entityCategoryMap.put(
                                List.of("bluegill", "catfish", "clownfish", "crab", "frostgill", "jellyfish",
                                                "man of war", "lobster", "minnow", "pike", "piranha",
                                                "pufferfish", "salmon", "snapjaw", "tang",
                                                "rainbow trout"),
                                "Freshwater");
                entityCategoryMap.put(List.of("Feran"),
                                "Feran");
                entityCategoryMap.put(List.of("Kweebec"),
                                "Kweebec");
                entityCategoryMap.put(List.of("Goblin"),
                                "Goblin");
                entityCategoryMap.put(List.of("Outlander"),
                                "Outlander");
                entityCategoryMap.put(List.of("dragon", "emberwulf", "wraith", "yeti", "shadow knight"),
                                "Mythic");
                entityCategoryMap.put(List.of("Trork"),
                                "Trork");
                entityCategoryMap.put(List.of("undead", "ghoul", "skeleton", "zombie", "werewolf"),
                                "Undead");
                entityCategoryMap.put(List.of("void", "hedera"),
                                "Voidspawn");
                entityCategoryMap.put(
                                List.of("antelope", "armadillo", "bison", "boar", "bunny", "camel", "chick", "cow",
                                                "calf", "doe",
                                                "stag", "goat", "kid", "horse", "foal", "bull", "mosshorn", "mouflon",
                                                "pig", "piglet",
                                                "rabbit", "ram",
                                                "sheep", "lamb", "skrill", "turkey", "warthog"),
                                "Livestock");
                entityCategoryMap.put(
                                List.of("archaeopteryx", "bat", "bluebird", "crow", "duck", "greenfinch", "flamingo",
                                                "hawk", "owl",
                                                "parrot", "penguin", "pigeon", "pterodactyl", "raven", "sparrow",
                                                "terabird", "vulture",
                                                "woodpecker"),
                                "Avian");
                entityCategoryMap.put(
                                List.of("cactee", "frog", "gecko", "larva", "meerkat", "molerat", "mouse", "rat",
                                                "scorpion", "slug",
                                                "snail", "cobra", "snake", "rattlesnake", "spider", "squirrel"),
                                "Critter");
                entityCategoryMap.put(
                                List.of("crocodile", "fen stalker", "lizard", "raptor", "rex", "snapdragon", "toad",
                                                "tortoise",
                                                "trillodon"),
                                "Reptile");
                entityCategoryMap.put(List.of("Scarak"),
                                "Scarak");
                entityCategoryMap.put(List.of("bear", "fox", "hyena", "leopard", "tiger", "wolf"),
                                "Predators");
                entityCategoryMap.put(List.of("golem", "spark", "spirit"),
                                "Elemental");
                entityCategoryMap.put(
                                List.of(BudTrorkData.NPC_DISPLAY_NAME, BudKweebecData.NPC_DISPLAY_NAME,
                                                BudFeranData.NPC_DISPLAY_NAME),
                                "Player Allies");
                return entityCategoryMap;
        }

        private static String loadDataFor(String info) {
                switch (info) {
                        case BudTrorkData.NPC_DISPLAY_NAME -> {
                                return "Gronkh is Trork Buddy who values strength and minerals. He is proud and strong.";
                        }
                        case BudKweebecData.NPC_DISPLAY_NAME -> {
                                return "Keyleth is a Kweebec Buddy who loves nature and ancient temples. She is peaceful and wise.";
                        }
                        case BudFeranData.NPC_DISPLAY_NAME -> {
                                return "Veri is a desert fox Buddy who loves shiny objects and lives in arid regions.";
                        }
                        default -> {
                                return "No additional data available.";
                        }
                }
        }
}
