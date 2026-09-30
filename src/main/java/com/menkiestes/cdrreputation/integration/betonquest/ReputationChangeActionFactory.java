package com.menkiestes.cdrreputation.integration.betonquest;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Argument;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;

public final class ReputationChangeActionFactory implements PlayerActionFactory {
    private final ReputationChangeAction.Mode mode;

    public ReputationChangeActionFactory(ReputationChangeAction.Mode mode) {
        this.mode = mode;
    }

    @Override
    public PlayerAction parsePlayer(Instruction instruction) throws QuestException {
        Argument<Number> amount = instruction.number().get();
        Argument<String> reason = instruction.string().get();
        return new ReputationChangeAction(mode, amount, reason);
    }
}
