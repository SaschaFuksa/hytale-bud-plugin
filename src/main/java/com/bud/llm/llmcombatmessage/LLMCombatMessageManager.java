package com.bud.llm.llmcombatmessage;

import com.bud.llm.llmmessage.ILLMBudNPCMessage;

public class LLMCombatMessageManager {

    public static String createPrompt(String combatPrompt, ILLMBudNPCMessage npcMessage) {
        String introduction = """
                You want to say something about the last combat actions of your Buddy.
                First you get an overview about the different creatures in your world.
                In the next text, you get the last combat interactions your Buddy had.
                After the combat information, you get information about yourself.
                In this information, you can find details about different creatures and your personal view on them.
                Finally, select the info referring to the Buddy's enemies and say something short and related.
                """;
        String entityInformations = getEntityInformations();
        String combat_info = """
                The current combat information from your Buddy is as follows:
                Your Buddy had following interaction: %s
                """.formatted(combatPrompt);
        String bud_info = npcMessage.getPersonalCombatView();
        return introduction + "\n" + entityInformations + "\n" + combat_info + "\n" + bud_info;

    }

    private static String getEntityInformations() {
        return """
                Here are some details about different creatures in categories in your world:
                Water creatures:
                - Abyssal: Mysterious and shadowy beings from the depths like sharks, whales or eels. (medium threat)
                - Freshwater: Creatures inhabiting lakes and rivers such as jellyfish, crabs or small fish. (no threat)
                Land creatures civilians:
                - Feran: Fox-Human hybrids living in deserts. (no threat)
                - Kweebec: Small humanoids living in forests. (no threat)
                Land creatures enemies:
                - Goblin: Small, mischievous humanoids that often cause trouble for adventurers. (medium threat)
                - Outlander: Savage and brutal humanoid warriors from harsh environments. (high threat)
                - Mythic: Powerful and legendary beings like dragons, yetis, wraiths, or emberwulfs. (high threat)
                - Trork: Strong and sturdy orc-like creatures from the mountains. (medium threat)
                - Undead: Reanimated corpses such as zombies, skeletons, or ghouls. (high threat)
                - Voidspawn: Dark and twisted creatures born from the void. (high threat)
                Animal creatures:
                - Livestock: Domesticated animals like sheep, pigs, chickens, or llamas. (no threat)
                - Avian: Bird-like creatures that soar the skies, such as hawks, bats, or penguins. (no threat)
                - Critter: Small and nimble animals like rats, scorpions, spiders or snakes. (medium threat)
                - Reptile: Cold-blooded creatures such as dinosaurs, rhino toads, or crocodiles. (medium threat)
                - Scarak: Insectoid beings with hive minds. (high threat)
                - Predators: Fierce hunting animals like wolves, lions, or bears. (high threat)
                - Elemental: Beings made of natural elements like golems. (medium threat)
                """;
    }

}
