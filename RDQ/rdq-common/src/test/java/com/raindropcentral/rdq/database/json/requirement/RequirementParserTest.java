package com.raindropcentral.rdq.database.json.requirement;

import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rdq.requirement.ItemRequirement;
import com.raindropcentral.rdq.requirement.PermissionRequirement;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequirementParserTest {

    @Test
    void serializeAndParseRoundTripsConcreteRequirement() throws IOException {
        final PermissionRequirement requirement = new PermissionRequirement(
                List.of("rdq.permission.alpha", "rdq.permission.beta"),
                PermissionRequirement.PermissionMode.MINIMUM,
                1,
                "requirement.permission.custom",
                true
        );

        final String json = RequirementParser.serialize(requirement);
        final AbstractRequirement parsedRequirement = RequirementParser.parse(json);

        final PermissionRequirement parsed = assertInstanceOf(PermissionRequirement.class, parsedRequirement);
        assertEquals(requirement.getType(), parsed.getType());
        assertEquals(requirement.getRequiredPermissions(), parsed.getRequiredPermissions());
        assertEquals(requirement.getPermissionMode(), parsed.getPermissionMode());
        assertEquals(requirement.getMinimumRequired(), parsed.getMinimumRequired());
        assertEquals(requirement.getDescription(), parsed.getDescription());
        assertEquals(requirement.isCheckNegated(), parsed.isCheckNegated());
    }

    @Test
    void serializeItemRequirementUsesCustomItemStackSerializer() throws IOException {
        final ItemStack stack = new ItemStack(Material.DIAMOND, 3);
        final ItemRequirement requirement = new ItemRequirement(
                List.of(stack),
                List.of(),
                false,
                "requirement.item.custom",
                false
        );

        final String json = RequirementParser.serialize(requirement);

        assertTrue(json.contains("\"requiredItems\""));
        assertTrue(json.contains("\"type\":\"ITEM\""));
        assertTrue(json.contains("\"type\":\"DIAMOND\""));

        final AbstractRequirement parsedRequirement = RequirementParser.parse(json);
        final ItemRequirement parsed = assertInstanceOf(ItemRequirement.class, parsedRequirement);

        final List<ItemStack> parsedStacks = parsed.getRequiredItems();
        assertEquals(1, parsedStacks.size());
        assertEquals(stack.getType(), parsedStacks.get(0).getType());
        assertEquals(stack.getAmount(), parsedStacks.get(0).getAmount());
    }

    @Test
    void parseInvalidJsonPropagatesIOException() {
        assertThrows(IOException.class, () -> RequirementParser.parse("{\"type\""));
    }
}
