package com.menkiestes.cdrreputation.integration.betonquest;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Argument;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.quest.condition.PlayerCondition;
import org.betonquest.betonquest.api.quest.condition.PlayerConditionFactory;

public final class ReputationValueConditionFactory implements PlayerConditionFactory {
    @Override
    public PlayerCondition parsePlayer(Instruction instruction) throws QuestException {
        Argument<String> operator = instruction.string().get();
        Argument<Number> target = instruction.number().get();
        return new ReputationValueCondition(operator, target);
    }
}
