package com.ai.repo.playground.rules;

import java.util.Map;
import java.util.random.RandomGenerator;

/** Server-drawn terms for a fictional high-pressure franchise pitch.
 * The support outcome is locked before either Agent investigates or signs.
 */
public record V5FranchiseOffer(String offerId, int version, int appearsMonth,
        long entryFeeMinor, long monthlyRoyaltyMinor, long unitPremiumMinor,
        long exitFeeMinor, Support support) {
    public enum Support { DELIVERED, WEAK, ABSENT }
    public enum Investigation { TERMS, STORES, SUPPLY }

    public V5FranchiseOffer {
        require(offerId != null && offerId.matches("[A-Za-z0-9:_-]{1,100}"), "INVALID_FRANCHISE_ID");
        require(version == 1 && appearsMonth >= 1 && appearsMonth <= 4, "INVALID_FRANCHISE_VERSION");
        require(entryFeeMinor >= 4_000 && entryFeeMinor <= 7_000
                && monthlyRoyaltyMinor >= 600 && monthlyRoyaltyMinor <= 1_200
                && unitPremiumMinor >= 100 && unitPremiumMinor <= 300
                && exitFeeMinor >= 2_000 && exitFeeMinor <= 4_000 && support != null,
                "INVALID_FRANCHISE_TERMS");
    }

    /** Called once and serialized with the room; never redraw after an Agent choice. */
    public static V5FranchiseOffer draw(String offerId, int month, RandomGenerator random) {
        require(random != null, "INVALID_FRANCHISE_RANDOM");
        Support support = switch (random.nextInt(5)) {
            case 0 -> Support.DELIVERED;
            case 1 -> Support.WEAK;
            default -> Support.ABSENT;
        };
        return new V5FranchiseOffer(offerId, 1, month, 6_000, 900, 200, 3_000, support);
    }

    /** No hidden quality in the initial pitch; the Agent sees verified terms. */
    public Map<String,Object> publicTerms() {
        return Map.of("offerId",offerId,"version",version,"appearsMonth",appearsMonth,
                "entryFeeMinor",entryFeeMinor,"monthlyRoyaltyMinor",monthlyRoyaltyMinor,
                "unitPremiumMinor",unitPremiumMinor,"exitFeeMinor",exitFeeMinor,
                "npcRole","FRANCHISE_SALESPERSON");
    }

    /** One bounded investigation yields a deterministic clue, not a new random draw. */
    public String clue(Investigation kind) {
        require(kind != null, "INVALID_FRANCHISE_INVESTIGATION");
        return switch (kind) {
            case TERMS -> "VERIFIED_RECURRING_ROYALTY_AND_EXIT_FEE";
            case STORES -> support == Support.DELIVERED ? "MODEL_STORE_SUPPORT_VERIFIED"
                    : support == Support.WEAK ? "MODEL_STORE_RESULTS_INCONSISTENT"
                    : "MODEL_STORE_CANNOT_VERIFY_SUPPORT";
            case SUPPLY -> support == Support.DELIVERED ? "SUPPLY_DELIVERY_TRACKABLE"
                    : support == Support.WEAK ? "SUPPLY_DELIVERY_DELAYED"
                    : "SUPPLY_COMMITMENT_UNVERIFIED";
        };
    }

    /** These game costs must enter the ordinary month ledger after both Agents sign. */
    public long entryCost(int month) { return month == appearsMonth ? entryFeeMinor : 0; }
    public long monthlyCost(int month) { return month >= appearsMonth ? monthlyRoyaltyMinor : 0; }
    public long unitPremium(int month) { return month >= appearsMonth ? unitPremiumMinor : 0; }
    public int supportBuyers(int month) {
        if (month <= appearsMonth) return 0;
        return support == Support.DELIVERED ? 1 : support == Support.WEAK && month % 3 == 0 ? 1 : 0;
    }
    /** Rebranding without promised support loses existing walk-in demand. */
    public int lostBuyers(int month) {
        if (month <= appearsMonth) return 0;
        return switch (support) {
            case DELIVERED -> 0;
            case WEAK -> 1;
            case ABSENT -> 2;
        };
    }
    public String resultEvent(int month) {
        require(month >= appearsMonth,"FRANCHISE_NOT_STARTED");
        if (month == appearsMonth) return "FRANCHISE_SIGNED";
        return switch (support) {
            case DELIVERED -> "FRANCHISE_SUPPORT_DELIVERED";
            case WEAK -> "FRANCHISE_SUPPORT_WEAK";
            case ABSENT -> "FRANCHISE_SUPPORT_ABSENT";
        };
    }

    private static void require(boolean condition,String code) {
        if (!condition) throw new IllegalArgumentException(code);
    }
}
