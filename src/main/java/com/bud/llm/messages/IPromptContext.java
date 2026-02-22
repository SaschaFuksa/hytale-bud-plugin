package com.bud.llm.messages;

import com.bud.components.BudComponent;
import com.bud.profile.IBudProfile;

public interface IPromptContext {

    BudComponent getBudComponent();

    IBudProfile getBudProfile();

}
