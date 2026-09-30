package com.ai.repo.playground.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class V5StrategyContractTest {
    private final ObjectMapper json = new ObjectMapper();

    private JsonNode parse(String source) {
        try { return json.readTree(source); }
        catch (Exception error) { throw new AssertionError(error); }
    }

    @Test void signedFiniteFieldsRoundTripWithoutInterpretingNarrative() {
        JsonNode block = parse("""
                {"audienceSegment":"NIGHT_READERS","marketingChannel":"LOCAL_EVENT",
                 "servicePromise":"QUIET","monthlyMarketingBudgetMinor":600}
                """);
        V5ShopRules.Strategy strategy = V5StrategyContract.parse(block, 4, 16, 2_000);
        assertEquals(V5ShopRules.Audience.NIGHT_READERS, strategy.audience());
        assertEquals(V5ShopRules.Channel.LOCAL_EVENT, strategy.channel());
        assertEquals(600, strategy.monthlyMarketingBudgetMinor());
        assertEquals(2_000, strategy.minimumReserveMinor());
        assertEquals(strategy, V5StrategyContract.parse(parse(block.toString()), 4, 16, 2_000));
    }

    @Test void unknownFieldsAndUnboundedOrConflictingValuesFailClosed() {
        for (String block : new String[]{
                "{}",
                "{\"audienceSegment\":\"NIGHT_READERS\",\"marketingChannel\":\"NONE\",\"servicePromise\":\"QUIET\",\"monthlyMarketingBudgetMinor\":0,\"freeMoney\":10000}",
                "{\"audienceSegment\":\"EVERYONE\",\"marketingChannel\":\"NONE\",\"servicePromise\":\"QUIET\",\"monthlyMarketingBudgetMinor\":0}",
                "{\"audienceSegment\":\"STUDENTS\",\"marketingChannel\":\"NONE\",\"servicePromise\":\"COMMUNITY\",\"monthlyMarketingBudgetMinor\":300}",
                "{\"audienceSegment\":\"STUDENTS\",\"marketingChannel\":\"FLYERS\",\"servicePromise\":\"COMMUNITY\",\"monthlyMarketingBudgetMinor\":\"300\"}"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> V5StrategyContract.parse(parse(block), 4, 16, 0));
        }
        JsonNode valid = parse("""
                {"audienceSegment":"COMMUTERS","marketingChannel":"FLYERS",
                 "servicePromise":"FAST","monthlyMarketingBudgetMinor":300}
                """);
        assertThrows(IllegalArgumentException.class,
                () -> V5StrategyContract.parse(valid, 7, 16, 0));
    }
}
