package com.menkiestes.cdrreputation.integration.betonquest;

import com.menkiestes.cdrreputation.api.CdrReputationApi;
import com.menkiestes.cdrreputation.api.CdrReputationProvider;
import com.menkiestes.cdrreputation.model.ReputationTier;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Argument;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.condition.PlayerCondition;

public final class ReputationTierCondition implements PlayerCondition {
    private final Argument<String> tier;

    public ReputationTierCondition(Argument<String> tier) {
        this.tier = tier;
    }

    @Override
    public boolean check(Profile profile) throws QuestException {
        String expected = tier.getValue(profile);
        if (expected == null || expected.isBlank()) throw new QuestException("cdrrep_tier requires a tier key");
        ReputationTier current = api().getTier(profile.getPlayerUUID());
        return current.key().equalsIgnoreCase(expected.trim());
    }

    @Override
    public boolean isPrimaryThreadEnforced() {
        return true;
    }

    private static CdrReputationApi api() throws QuestException {
        try {
            return CdrReputationProvider.get();
        } catch (IllegalStateException exception) {
            throw new QuestException("CdrReputation API is not ready");
        }
    }
}
