package com.menkiestes.cdrreputation.integration.betonquest;

import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.integration.Integration;

public final class CdrReputationBetonQuestIntegration implements Integration {
    @Override
    public void enable(BetonQuestApi api) throws QuestException {
        api.actions().registry().register("cdrrep_add", new ReputationChangeActionFactory(ReputationChangeAction.Mode.ADD));
        api.actions().registry().register("cdrrep_remove", new ReputationChangeActionFactory(ReputationChangeAction.Mode.REMOVE));
        api.actions().registry().register("cdrrep_set", new ReputationChangeActionFactory(ReputationChangeAction.Mode.SET));

        api.conditions().registry().register("cdrrep_value", new ReputationValueConditionFactory());
        api.conditions().registry().register("cdrrep_tier", new ReputationTierConditionFactory());
    }

    @Override
    public void postEnable(BetonQuestApi api) throws QuestException {
        // No post-enable work required.
    }

    @Override
    public void disable() throws QuestException {
        // BetonQuest owns the registered factories and removes them during shutdown.
    }
}
