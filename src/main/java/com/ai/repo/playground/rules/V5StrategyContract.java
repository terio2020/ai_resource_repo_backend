package com.ai.repo.playground.rules;

import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;

/** Strict parser for the signed v5 strategy block. The existing product and reserve
 * terms are part of the same signed proposal and are passed from those fields.
 */
public final class V5StrategyContract {
    private static final Set<String> FIELDS = Set.of("audienceSegment", "marketingChannel",
            "servicePromise", "monthlyMarketingBudgetMinor");
    private V5StrategyContract() {}

    public static V5ShopRules.Strategy parse(JsonNode node, int quantity,
                                             int priceCoins, long reserveMinor) {
        require(node != null && node.isObject() && node.size() == FIELDS.size(),
                "INVALID_V5_STRATEGY");
        node.fieldNames().forEachRemaining(name -> require(FIELDS.contains(name), "INVALID_V5_STRATEGY"));
        JsonNode budget = node.get("monthlyMarketingBudgetMinor");
        require(budget != null && budget.isIntegralNumber() && budget.canConvertToLong(),
                "INVALID_V5_STRATEGY");
        try {
            return new V5ShopRules.Strategy(quantity, priceCoins, reserveMinor,
                    V5ShopRules.Audience.valueOf(value(node, "audienceSegment")),
                    V5ShopRules.Channel.valueOf(value(node, "marketingChannel")),
                    V5ShopRules.ServicePromise.valueOf(value(node, "servicePromise")),
                    budget.longValue());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("INVALID_V5_STRATEGY", error);
        }
    }

    private static String value(JsonNode node, String key) {
        JsonNode value = node.get(key);
        require(value != null && value.isTextual() && value.textValue().length() <= 30,
                "INVALID_V5_STRATEGY");
        return value.textValue();
    }

    private static void require(boolean condition, String error) {
        if (!condition) throw new IllegalArgumentException(error);
    }
}
