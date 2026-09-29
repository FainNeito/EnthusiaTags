package org.enthusia.tags;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SupporterPresentationTest {
    private YamlConfiguration config(String name) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
            java.util.Objects.requireNonNull(getClass().getResourceAsStream("/" + name)),
            StandardCharsets.UTF_8));
    }

    @Test void supporterNamesAndFounderIdsRemainCompatible() {
        var tags = config("config.yml");
        var expected = Map.of("avid", "Avidian", "devotee", "Devoted Individual",
            "glorious_fellow", "✪ Glorious Fellow ✪", "founding_avid", "✧ Founding Supporter ✧",
            "founding_devotee", "✧ Founding Supporter ✧", "founding_glorious", "✧ Founding Supporter ✧");
        expected.forEach((id, plain) -> {
            String text = tags.getString("tags." + id + ".tag-text");
            assertEquals(plain, TagTextFormat.plainText(text));
            assertEquals(text, tags.getString("tags." + id + ".display-name"));
        });
        String founder = tags.getString("tags.founding_avid.tag-text");
        assertEquals(founder, tags.getString("tags.founding_devotee.tag-text"));
        assertEquals(founder, tags.getString("tags.founding_glorious.tag-text"));
        var entitlements = config("entitlements.yml");
        for (String tier : new String[]{"avid", "devotee", "glorious"})
            assertEquals(java.util.List.of("founding_" + tier),
                entitlements.getStringList("definitions.donor_" + tier + "_founder.tags"));
    }

    @Test void supporterMessagesMatchProductionRoseChatAndKeepOriginal() {
        var cosmetics = config("cosmetics.yml");
        assertEquals("<green>+</green> {prefix}{player} #00b9e8has spawned in.", cosmetics.getString("cosmetics.join_avid_supporter.message"));
        assertEquals("<red>-</red> {prefix}{player} #00b9e8has despawned.", cosmetics.getString("cosmetics.quit_avid_supporter.message"));
        assertEquals("<green>+</green> {prefix}{player} #00b9e8has spawned in.", cosmetics.getString("cosmetics.join_founding_avid.message"));
        assertEquals("<red>-</red> {prefix}{player} #00b9e8has despawned.", cosmetics.getString("cosmetics.quit_founding_avid.message"));
        assertEquals("<green>+</green> {prefix}{player} #0034FFhas arrived!", cosmetics.getString("cosmetics.join_devotee_supporter.message"));
        assertEquals("<red>-</red> {prefix}{player} #0034FFhas departed.", cosmetics.getString("cosmetics.quit_devotee_supporter.message"));
        assertEquals("<green>+</green> {prefix}{player} #0034FFhas arrived!", cosmetics.getString("cosmetics.join_founding_devotee.message"));
        assertEquals("<red>-</red> {prefix}{player} #0034FFhas departed.", cosmetics.getString("cosmetics.quit_founding_devotee.message"));
        assertEquals("<green>+</green> {prefix}{player} #ef6a1ehas joined the server!", cosmetics.getString("cosmetics.join_glorious_legacy.message"));
        assertEquals("<red>-</red> {prefix}{player} #ef6a1ehas left the server.", cosmetics.getString("cosmetics.quit_glorious_legacy.message"));
        assertEquals("<green>+</green> {prefix}{player} #ef6a1ehas joined the server!", cosmetics.getString("cosmetics.join_founding_glorious.message"));
        assertEquals("<red>-</red> {prefix}{player} #ef6a1ehas left the server.", cosmetics.getString("cosmetics.quit_founding_glorious.message"));
        assertEquals("ORIGINAL", cosmetics.getString("cosmetics.original_join.type"));
        assertEquals("ORIGINAL", cosmetics.getString("cosmetics.original_quit.type"));
    }
}
