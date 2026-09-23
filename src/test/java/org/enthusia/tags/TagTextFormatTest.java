package org.enthusia.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class TagTextFormatTest {
    @Test
    void nullAndEmptyValuesStayEmpty() {
        assertEquals("", TagTextFormat.canonicalMiniMessage(null));
        assertEquals("", TagTextFormat.canonicalMiniMessage(""));
        assertEquals("", TagTextFormat.safeDynamicValue(null));
        assertEquals("", TagTextFormat.safeDynamicValue(""));
        assertEquals("", TagTextFormat.plainText(null));
    }

    @Test
    void existingMiniMessageIsPreservedWhenNoLegacyCodesExist() {
        String input = "<gold>Hello <bold>world</bold></gold>";
        assertEquals(input, TagTextFormat.canonicalMiniMessage(input));
        assertEquals("Hello world", TagTextFormat.plainText(input));
    }

    @Test
    void legacyFormattingConvertsToMiniMessageWithoutChangingVisibleText() {
        String legacy = "&aHello &lworld&r!";
        String canonical = TagTextFormat.canonicalMiniMessage(legacy);

        assertFalse(canonical.contains("&a"));
        assertFalse(canonical.contains("&l"));
        assertEquals("Hello world!", TagTextFormat.plainText(canonical));
        assertTrue(TagTextFormat.legacyText(canonical).contains("Hello"));
    }

    @Test
    void legacyHexColorsConvertWithoutLeakingFormattingSyntaxIntoPlainText() {
        String canonical = TagTextFormat.canonicalMiniMessage("&#12abEFPlayer");
        assertEquals("Player", TagTextFormat.plainText(canonical));
        assertFalse(canonical.contains("&#12abEF"));
    }

    @Test
    void dynamicMiniMessageTagsAreEscapedInsteadOfExecuted() {
        String userControlled = "<red>Alice</red>";
        String safe = TagTextFormat.safeDynamicValue(userControlled);

        assertEquals(userControlled, TagTextFormat.plainText(safe));
        assertFalse(safe.equals(userControlled), "dynamic tags must be escaped before insertion");
    }

    @Test
    void dynamicLegacyFormattingIsCanonicalizedButVisibleTextIsStable() {
        String safe = TagTextFormat.safeDynamicValue("&cAlice");
        assertEquals("Alice", TagTextFormat.plainText(safe));
        assertFalse(safe.contains("&c"));
    }
}
