package com.bud.app.commands;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.bud.core.registry.BudRegistry;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;

final class BudIdArgumentType extends SingleArgumentType<String> {

    BudIdArgumentType() {
        super("bud", "<bud id>");
    }

    @Nonnull
    @Override
    public String parse(String input, ParseResult result) {
        String budId = BudRegistry.normalize(input);
        if (!BudRegistry.getInstance().exists(budId)) {
            result.fail(Message.raw("Unknown bud id: " + input + ". Valid: "
                    + String.join(", ", BudRegistry.getInstance().getIds())));
        }
        return budId;
    }

    @Override
    public void suggest(@Nonnull CommandSender sender, @Nonnull String currentInput, int argIndex,
            @Nonnull SuggestionResult result) {
        String prefix = currentInput.toLowerCase(Locale.ROOT);
        for (String budId : BudRegistry.getInstance().getIds()) {
            if (budId.startsWith(prefix)) {
                result.suggest(budId);
            }
        }
    }

}
