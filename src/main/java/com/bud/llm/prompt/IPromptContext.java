package com.bud.llm.prompt;

import com.bud.core.components.BudComponent;
import com.bud.core.profiles.IBudProfile;

public interface IPromptContext {

    BudComponent getBudComponent();

    IBudProfile getBudProfile();

}
