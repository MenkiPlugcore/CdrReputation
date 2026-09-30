package com.menkiestes.cdrreputation.integration.betonquest;

import com.menkiestes.cdrreputation.api.CdrReputationApi;
import com.menkiestes.cdrreputation.api.CdrReputationProvider;
import com.menkiestes.cdrreputation.model.ReputationContext;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Argument;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;

public final class ReputationChangeAction implements PlayerAction {
    public enum Mode { ADD, REMOVE, SET }

    private final Mode mode;
    private final Argument<Number> amount;
    private final Argument<String> reason;

    public ReputationChangeAction(Mode mode, Argument<Number> amount, Argument<String> reason) {
        this.mode = mode;
        this.amount = amount;
        this.reason = reason;
    }

    @Override
    public void execute(Profile profile) throws QuestException {
        int value = resolveInteger(amount.getValue(profile));
        if ((mode == Mode.ADD || mode == Mode.REMOVE) && value < 0) {
            throw new QuestException("cdrrep_" + mode.name().toLowerCase() + " requires a non-negative amount");
        }

        String resolvedReason = reason.getValue(profile);
        if (resolvedReason == null || resolvedReason.isBlank()) resolvedReason = "BETONQUEST_ACTION";
        ReputationContext context = new ReputationContext(resolvedReason, "BETONQUEST", "QUEST");
        CdrReputationApi api = api();

        switch (mode) {
            case ADD -> api.addReputation(profile.getPlayerUUID(), value, context);
            case REMOVE -> api.removeReputation(profile.getPlayerUUID(), value, context);
            case SET -> api.setReputation(profile.getPlayerUUID(), value, context);
        }
    }

    @Override
    public boolean isPrimaryThreadEnforced() {
        return true;
    }

    private static int resolveInteger(Number number) throws QuestException {
        if (number == null) throw new QuestException("Reputation value cannot be null");
        double value = number.doubleValue();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new QuestException("Reputation value must be a valid 32-bit integer");
        }
        return (int) value;
    }

    private static CdrReputationApi api() throws QuestException {
        try {
            return CdrReputationProvider.get();
        } catch (IllegalStateException exception) {
            throw new QuestException("CdrReputation API is not ready");
        }
    }
}
