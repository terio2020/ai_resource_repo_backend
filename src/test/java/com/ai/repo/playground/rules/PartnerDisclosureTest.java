package com.ai.repo.playground.rules;

import com.ai.repo.playground.dto.PlaygroundRequests.OwnerBrief;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartnerDisclosureTest {
    private final OwnerBrief brief = new OwnerBrief("纸飞机书店", "CHARACTER",
            List.of("不售卖个人信息"), List.of("可做夜间活动"), List.of("theme", "hardConstraints"));

    @Test void legacyDisclosureDoesNotAuthorizePartnerView() {
        assertEquals(Map.of(), PartnerDisclosure.project(brief, List.of()));
        assertEquals(Map.of("theme", "纸飞机书店"), PartnerDisclosure.project(brief, List.of("theme")));
        assertThrows(IllegalArgumentException.class,
                () -> PartnerDisclosure.requireSourceField("hardConstraints", PartnerDisclosure.project(brief, List.of("theme"))));
    }

    @Test void projectedFieldsAreExactAndImmutable() {
        var view = PartnerDisclosure.project(brief, List.of("theme", "negotiable"));
        assertEquals(2, view.size());
        assertFalse(view.containsKey("hardConstraints"));
        assertThrows(UnsupportedOperationException.class, () -> view.put("priority", "PROFIT"));
        assertThrows(UnsupportedOperationException.class,
                () -> ((List<String>) view.get("negotiable")).add("injected"));
        assertDoesNotThrow(() -> PartnerDisclosure.requireSourceField("theme", view));
        assertDoesNotThrow(() -> PartnerDisclosure.requireSourceField(null, view));
        assertDoesNotThrow(() -> PartnerDisclosure.requireNewContribution(1, 1, "theme", view));
        assertThrows(IllegalArgumentException.class,
                () -> PartnerDisclosure.requireNewContribution(1, 2, "theme", view));
        assertThrows(IllegalArgumentException.class,
                () -> PartnerDisclosure.requireNewContribution(1, 1, "hardConstraints", view));
    }

    @Test void invalidOrDuplicateConsentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PartnerDisclosure.project(brief, null));
        assertThrows(IllegalArgumentException.class, () -> PartnerDisclosure.project(brief, List.of("theme", "theme")));
        assertThrows(IllegalArgumentException.class, () -> PartnerDisclosure.project(brief, List.of("privateNote")));
    }
}
