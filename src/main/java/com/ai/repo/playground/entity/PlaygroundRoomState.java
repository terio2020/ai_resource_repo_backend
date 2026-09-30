package com.ai.repo.playground.entity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.ai.repo.playground.dto.PlaygroundRequests.OwnerBrief;
import com.ai.repo.playground.rules.MonthlyShopRules;
import com.ai.repo.playground.rules.ProposalAgreement;
import com.ai.repo.playground.rules.V5MonthlyWindow;
import com.ai.repo.playground.rules.V5ShopRules;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/** Internal persistence only. Never serialize this object into an HTTP response. */
@Data
public class PlaygroundRoomState {
    private boolean randomMatched;
    private int contractVersion = 2;
    private String ruleVersion = MonthlyShopRules.RULE_VERSION;
    private Map<Long, OwnerBrief> ownerBriefs = new LinkedHashMap<>();
    private Set<Long> readyAgents = new LinkedHashSet<>();
    private Set<String> usedProposalIds = new LinkedHashSet<>();
    private JsonNode proposal;
    private ProposalAgreement agreement;
    private Map<Long, Integer> windowDecisions = new LinkedHashMap<>();
    private Map<Long, Integer> consecutiveModelFailures = new LinkedHashMap<>();
    private MonthlyShopRules.NpcOrder npcOrder;
    private boolean npcWindowOpen;
    private Set<Long> npcAcceptedBy = new LinkedHashSet<>();
    private boolean closingReplyPending;
    private Long closingInitiator;
    private V5MonthlyWindow v5Window;
    private V5ShopRules.Signal v5Signal;
    private MonthlyShopRules.State game;
}
