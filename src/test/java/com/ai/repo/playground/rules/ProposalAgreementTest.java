package com.ai.repo.playground.rules;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProposalAgreementTest {
    @Test void proposerApprovalAloneCannotFormContract() {
        var proposal = ProposalAgreement.propose("v1", "quantity=6;price=14;day=2", Set.of(1L, 2L), 1L);
        assertFalse(proposal.partnersApproved());
        assertFalse(proposal.contractAccepted());
        assertThrows(IllegalArgumentException.class, () -> proposal.acceptByNpc("v1", proposal.contentHash()));
    }
    @Test void bothPartnersAndNpcMustAcceptExactlyTheSameTerms() {
        var proposal = ProposalAgreement.propose("v1", "quantity=6;price=14;day=2", Set.of(1L, 2L), 1L);
        var signed = proposal.approve(2L, "v1", proposal.contentHash());
        assertTrue(signed.partnersApproved());
        assertFalse(signed.contractAccepted());
        assertTrue(signed.acceptByNpc("v1", signed.contentHash()).contractAccepted());
    }
    @Test void counterProposalDoesNotCarryOldSignatures() {
        var old = ProposalAgreement.propose("v1", "quantity=6;price=14;day=2", Set.of(1L, 2L), 1L);
        var changed = ProposalAgreement.propose("v2", "quantity=2;price=14;day=2", Set.of(1L, 2L), 2L);
        assertThrows(IllegalArgumentException.class, () -> changed.approve(1L, "v1", old.contentHash()));
        assertThrows(IllegalArgumentException.class, () -> changed.acceptByNpc("v2", old.contentHash()));
        assertEquals(Set.of(2L), changed.approvedIds());
    }
    @Test void outsidersCannotApproveAndDuplicateApprovalIsIdempotent() {
        var proposal = ProposalAgreement.propose("v1", "terms", Set.of(1L, 2L), 1L);
        assertThrows(IllegalArgumentException.class, () -> proposal.approve(3L, "v1", proposal.contentHash()));
        assertEquals(proposal, proposal.approve(1L, "v1", proposal.contentHash()));
    }
}
