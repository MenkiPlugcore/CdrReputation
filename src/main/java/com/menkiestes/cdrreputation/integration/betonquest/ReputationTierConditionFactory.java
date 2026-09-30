package com.menkiestes.cdrreputation.integration.betonquest;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Argument;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.quest.condition.PlayerCondition;
import org.betonquest.betonquest.api.quest.condition.PlayerConditionFactory;

public final class ReputationTierConditionFactory implements PlayerConditionFactory {
    @Override
    public PlayerCondition parsePlayer(Instruction instruction) throws QuestException {
        Argument<String> tier = instruction.string().get();
        return new ReputationTierCondition(tier);
    }
}
