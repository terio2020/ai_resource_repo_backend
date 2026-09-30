package com.ai.repo.playground.rules;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/** Immutable signature bookkeeping. Principal identity and lease checks belong to the service. */
public record ProposalAgreement(String proposalId, String contentHash, Set<Long> partnerIds,
                                Set<Long> approvedIds, boolean npcAccepted) {
    public ProposalAgreement {
        partnerIds = Set.copyOf(partnerIds);
        approvedIds = Set.copyOf(approvedIds);
        if (proposalId == null || proposalId.isBlank() || contentHash == null || partnerIds.size() != 2
                || partnerIds.stream().anyMatch(id -> id == null || id <= 0)
                || !partnerIds.containsAll(approvedIds)) throw new IllegalArgumentException("INVALID_AGREEMENT");
        if (npcAccepted && !approvedIds.equals(partnerIds)) throw new IllegalArgumentException("MISSING_PARTNER_APPROVAL");
    }

    public static ProposalAgreement propose(String id, String canonicalTerms, Set<Long> partners, long proposer) {
        if (!partners.contains(proposer)) throw new IllegalArgumentException("NOT_A_PARTNER");
        return new ProposalAgreement(id, hash(canonicalTerms), partners, Set.of(proposer), false);
    }

    public ProposalAgreement approve(long authenticatedPartner, String expectedId, String expectedHash) {
        version(expectedId, expectedHash);
        if (!partnerIds.contains(authenticatedPartner)) throw new IllegalArgumentException("NOT_A_PARTNER");
        java.util.HashSet<Long> approved = new java.util.HashSet<>(approvedIds);
        approved.add(authenticatedPartner);
        return new ProposalAgreement(proposalId, contentHash, partnerIds, approved, npcAccepted);
    }

    public ProposalAgreement acceptByNpc(String expectedId, String expectedHash) {
        version(expectedId, expectedHash);
        if (!approvedIds.equals(partnerIds)) throw new IllegalArgumentException("MISSING_PARTNER_APPROVAL");
        return new ProposalAgreement(proposalId, contentHash, partnerIds, approvedIds, true);
    }

    public boolean partnersApproved() { return approvedIds.equals(partnerIds); }
    public boolean contractAccepted() { return partnersApproved() && npcAccepted; }

    private void version(String id, String hash) {
        if (!proposalId.equals(id) || !contentHash.equals(hash)) throw new IllegalArgumentException("STALE_PROPOSAL");
    }
    private static String hash(String terms) {
        if (terms == null || terms.isBlank()) throw new IllegalArgumentException("EMPTY_TERMS");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(terms.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
