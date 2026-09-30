package com.menkiestes.cdrreputation.integration.betonquest;

import com.menkiestes.cdrreputation.api.CdrReputationApi;
import com.menkiestes.cdrreputation.api.CdrReputationProvider;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Argument;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.condition.PlayerCondition;

import java.util.Locale;

public final class ReputationValueCondition implements PlayerCondition {
    private final Argument<String> operator;
    private final Argument<Number> target;

    public ReputationValueCondition(Argument<String> operator, Argument<Number> target) {
        this.operator = operator;
        this.target = target;
    }

    @Override
    public boolean check(Profile profile) throws QuestException {
        String comparison = operator.getValue(profile);
        Number targetNumber = target.getValue(profile);
        if (comparison == null || targetNumber == null) throw new QuestException("cdrrep_value requires an operator and number");

        double expected = targetNumber.doubleValue();
        if (!Double.isFinite(expected)) throw new QuestException("cdrrep_value target must be finite");
        int actual = api().getReputation(profile.getPlayerUUID());

        return switch (comparison.trim().toLowerCase(Locale.ROOT)) {
            case ">", "gt" -> actual > expected;
            case ">=", "gte" -> actual >= expected;
            case "<", "lt" -> actual < expected;
            case "<=", "lte" -> actual <= expected;
            case "=", "==", "eq" -> actual == expected;
            case "!=", "ne" -> actual != expected;
            default -> throw new QuestException("Unknown cdrrep_value operator: " + comparison);
        };
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
