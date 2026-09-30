package com.ai.repo.playground.rules;

import com.ai.repo.playground.dto.PlaygroundRequests.OwnerBrief;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** V3 opt-in projection. The legacy v2 disclosure list is not partner consent. */
public final class PartnerDisclosure {
    private static final Set<String> FIELDS = Set.of("theme", "priority", "hardConstraints", "negotiable");

    private PartnerDisclosure() {}

    public static Map<String, Object> project(OwnerBrief brief, List<String> partnerShareFields) {
        if (brief == null || partnerShareFields == null || partnerShareFields.size() > FIELDS.size()
                || partnerShareFields.stream().anyMatch(field -> !FIELDS.contains(field))
                || partnerShareFields.stream().distinct().count() != partnerShareFields.size()) {
            throw new IllegalArgumentException("INVALID_PARTNER_DISCLOSURE");
        }
        Map<String, Object> projected = new LinkedHashMap<>();
        for (String field : partnerShareFields) {
            Object value = switch (field) {
                case "theme" -> brief.theme();
                case "priority" -> brief.priority();
                case "hardConstraints" -> brief.hardConstraints();
                case "negotiable" -> brief.negotiable();
                default -> throw new IllegalArgumentException("INVALID_PARTNER_DISCLOSURE");
            };
            projected.put(field, value instanceof List<?> list ? List.copyOf(list) : value);
        }
        return Map.copyOf(projected);
    }

    public static void requireSourceField(String sourceFieldId, Map<String, Object> authorizedPartnerView) {
        if (sourceFieldId != null && (authorizedPartnerView == null
                || !authorizedPartnerView.containsKey(sourceFieldId))) {
            throw new IllegalArgumentException("UNAUTHORIZED_CONTRIBUTION_SOURCE");
        }
    }

    public static void requireSourceField(String sourceFieldId, Set<String> authorizedSourceFields) {
        if (sourceFieldId != null && (authorizedSourceFields == null
                || !authorizedSourceFields.contains(sourceFieldId)))
            throw new IllegalArgumentException("UNAUTHORIZED_CONTRIBUTION_SOURCE");
    }

    /** New elements must come from the submitting Agent; carried elements need an earlier event check. */
    public static void requireNewContribution(long authenticatedAgentId, long sourceAgentId,
                                              String sourceFieldId, Map<String, Object> authorizedPartnerView) {
        if (authenticatedAgentId <= 0 || authenticatedAgentId != sourceAgentId) {
            throw new IllegalArgumentException("FORGED_CONTRIBUTION_SOURCE");
        }
        requireSourceField(sourceFieldId, authorizedPartnerView);
    }

    public static void requireNewContribution(long authenticatedAgentId, long sourceAgentId,
                                              String sourceFieldId, Set<String> authorizedSourceFields) {
        if (authenticatedAgentId <= 0 || authenticatedAgentId != sourceAgentId)
            throw new IllegalArgumentException("FORGED_CONTRIBUTION_SOURCE");
        requireSourceField(sourceFieldId,authorizedSourceFields);
    }
}
