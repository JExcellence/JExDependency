package com.raindropcentral.rdq.requirement;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import de.jexcellence.evaluable.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRequirementTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("ItemRequirementTest");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldCreateRequirementFromDirectItemStacks() {
        final ItemStack required = namedStack(Material.PAPER, "Quest Scroll", 3);

        final ItemRequirement requirement = new ItemRequirement(List.of(required), null, true, "requirement.quest", true);

        final List<ItemStack> items = requirement.getRequiredItems();
        assertEquals(1, items.size(), "Direct constructor path should retain the supplied item count");
        assertTrue(items.get(0).isSimilar(required), "Required item should be cloned from the supplied stack");
        assertEquals(3, items.get(0).getAmount(), "Required item amount should match the supplied amount");
        assertTrue(requirement.getItemBuilders().isEmpty(), "No builders should be stored when concrete items are supplied");
        assertTrue(requirement.isConsumeOnComplete(), "consumeOnComplete should default to true when unspecified");
        assertTrue(requirement.isExactMatch(), "exactMatch should default to true when unspecified");
        assertEquals("requirement.quest", requirement.getDescription(), "Description should propagate from the constructor");
    }

    @Test
    void shouldCreateRequirementFromItemBuilders() {
        final ItemStack built = new ItemStack(Material.EMERALD, 2);
        final ItemBuilder builder = Mockito.mock(ItemBuilder.class);
        Mockito.when(builder.build()).thenReturn(built.clone());

        final ItemRequirement requirement = new ItemRequirement(null, List.of(builder), false, null, false);

        final List<ItemStack> items = requirement.getRequiredItems();
        assertEquals(1, items.size(), "Builder constructor path should generate required items");
        assertEquals(Material.EMERALD, items.get(0).getType(), "Generated item type should match builder output");
        assertEquals(2, items.get(0).getAmount(), "Generated item amount should match builder output");
        assertEquals(List.of(builder), requirement.getItemBuilders(), "Builders should be retained for inspection");
        assertFalse(requirement.isConsumeOnComplete(), "consumeOnComplete should respect explicit flag");
        assertFalse(requirement.isExactMatch(), "exactMatch should respect explicit flag");
        Mockito.verify(builder, Mockito.times(1)).build();
    }

    @Test
    void shouldRequireExactMetadataMatches() {
        final ItemStack required = namedStack(Material.PAPER, "Quest Scroll", 3);
        final ItemRequirement requirement = new ItemRequirement(List.of(required), null, true, null, true);

        this.player.getInventory().clear();
        this.player.getInventory().addItem(namedStack(Material.PAPER, "Quest Scroll", 3));

        assertTrue(requirement.isMet(this.player), "Exact requirement should be met when identical stacks are present");
        assertEquals(1.0, requirement.calculateProgress(this.player),
                "Progress should report completion when all items are present");

        this.player.getInventory().clear();
        this.player.getInventory().addItem(namedStack(Material.PAPER, "Quest Scroll", 2));
        this.player.getInventory().addItem(new ItemStack(Material.PAPER, 2));

        assertFalse(requirement.isMet(this.player),
                "Exact requirement should fail when metadata does not match");
        assertEquals(2.0 / 3.0, requirement.calculateProgress(this.player), 1.0E-6,
                "Progress should only count metadata-matching stacks");
    }

    @Test
    void shouldMatchByMaterialWhenExactMatchDisabled() {
        final ItemStack required = new ItemStack(Material.EMERALD, 4);
        final ItemRequirement requirement = new ItemRequirement(List.of(required), null, true, null, false);

        this.player.getInventory().clear();
        final ItemStack normalEmeralds = new ItemStack(Material.EMERALD, 2);
        final ItemStack namedEmeralds = namedStack(Material.EMERALD, "Fancy Emerald", 2);
        this.player.getInventory().addItem(normalEmeralds);
        this.player.getInventory().addItem(namedEmeralds);

        assertTrue(requirement.isMet(this.player),
                "Non-exact requirement should aggregate all matching materials");
        assertEquals(1.0, requirement.calculateProgress(this.player),
                "Progress should treat all matching materials as valid");

        this.player.getInventory().clear();
        this.player.getInventory().addItem(new ItemStack(Material.EMERALD, 3));

        assertFalse(requirement.isMet(this.player), "Requirement should fail when material count is insufficient");
        assertEquals(0.75, requirement.calculateProgress(this.player), 1.0E-6,
                "Progress should scale with the material count");
    }

    @Test
    void shouldConsumeMatchingItemsWhenEnabled() {
        final ItemStack required = namedStack(Material.GOLD_INGOT, "Blessed Ingot", 4);
        final ItemRequirement requirement = new ItemRequirement(List.of(required), null, true, null, true);

        this.player.getInventory().clear();
        this.player.getInventory().setItem(0, namedStack(Material.GOLD_INGOT, "Blessed Ingot", 3));
        this.player.getInventory().setItem(1, namedStack(Material.GOLD_INGOT, "Blessed Ingot", 2));

        requirement.consume(this.player);

        final ItemStack slot0 = this.player.getInventory().getItem(0);
        final ItemStack slot1 = this.player.getInventory().getItem(1);

        assertTrue(slot0 == null || slot0.getAmount() == 0,
                "First stack should be removed entirely after consumption");
        assertEquals(1, slot1.getAmount(),
                "Second stack should retain the remaining quantity after consumption");
    }

    @Test
    void shouldReportDetailedProgressAndMissingItems() {
        final ItemStack scrolls = namedStack(Material.PAPER, "Quest Scroll", 3);
        final ItemStack emeralds = new ItemStack(Material.EMERALD, 5);
        final ItemRequirement requirement = new ItemRequirement(List.of(scrolls, emeralds), null, true, null, true);

        this.player.getInventory().clear();
        this.player.getInventory().addItem(namedStack(Material.PAPER, "Quest Scroll", 2));
        this.player.getInventory().addItem(new ItemStack(Material.EMERALD, 5));

        assertTrue(requirement.isExactMatch(), "Exact matching should be enabled for this requirement");

        final List<ItemRequirement.ItemProgress> progress = requirement.getDetailedProgress(this.player);
        assertEquals(2, progress.size(), "Detailed progress should mirror the number of required items");

        final ItemRequirement.ItemProgress scrollProgress = progress.get(0);
        assertEquals(0, scrollProgress.index(), "Progress index should align with the requirement order");
        assertEquals(3, scrollProgress.requiredAmount(), "Required amount should be preserved");
        assertEquals(2, scrollProgress.currentAmount(), "Current amount should reflect matching stacks");
        assertFalse(scrollProgress.completed(), "Entry should be marked incomplete when shortage exists");
        assertEquals(66, scrollProgress.getProgressPercentage(),
                "Progress percentage should reflect normalized progress");
        assertEquals(1, scrollProgress.getShortage(), "Shortage should reflect the remaining quantity");

        final ItemRequirement.ItemProgress emeraldProgress = progress.get(1);
        assertTrue(emeraldProgress.completed(), "Emerald requirement should be marked complete");
        assertEquals(0, emeraldProgress.getShortage(), "No shortage should remain for completed entries");

        final List<ItemStack> missingItems = requirement.getMissingItems(this.player);
        assertEquals(1, missingItems.size(), "Only unmet items should be reported as missing");
        assertEquals(1, missingItems.get(0).getAmount(), "Missing stack should represent the outstanding quantity");
        assertTrue(missingItems.get(0).isSimilar(namedStack(Material.PAPER, "Quest Scroll", 1)),
                "Missing stack should preserve metadata");

        assertDoesNotThrow(() -> requirement.validate(), "Valid requirement should pass validation");
    }

    @Test
    void validateShouldDetectInvalidState() throws Exception {
        final ItemRequirement requirement = new ItemRequirement();

        assertThrows(IllegalStateException.class, requirement::validate,
                "Empty requirement should fail validation");

        final Field requiredItemsField = ItemRequirement.class.getDeclaredField("requiredItems");
        requiredItemsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final List<ItemStack> requiredItems = (List<ItemStack>) requiredItemsField.get(requirement);

        requiredItems.clear();
        requiredItems.add(null);
        assertThrows(IllegalStateException.class, requirement::validate,
                "Null entries should be rejected during validation");

        requiredItems.clear();
        requiredItems.add(new ItemStack(Material.AIR));
        assertThrows(IllegalStateException.class, requirement::validate,
                "Air entries should be rejected during validation");

        requiredItems.clear();
        final ItemStack zeroAmount = new ItemStack(Material.DIRT);
        zeroAmount.setAmount(0);
        requiredItems.add(zeroAmount);
        assertThrows(IllegalStateException.class, requirement::validate,
                "Zero-amount entries should be rejected during validation");
    }

    @Test
    void constructorShouldRejectInvalidEntries() {
        final List<ItemStack> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> new ItemRequirement(withNull, null, true, null, true),
                "Constructor should reject null required items");

        final List<ItemStack> withAir = List.of(new ItemStack(Material.AIR));
        assertThrows(IllegalArgumentException.class,
                () -> new ItemRequirement(withAir, null, true, null, true),
                "Constructor should reject air required items");

        final ItemStack zeroAmount = new ItemStack(Material.DIRT);
        zeroAmount.setAmount(0);
        final List<ItemStack> withZero = List.of(zeroAmount);
        assertThrows(IllegalArgumentException.class,
                () -> new ItemRequirement(withZero, null, true, null, true),
                "Constructor should reject zero-amount required items");
    }

    private ItemStack namedStack(final Material material, final String name, final int amount) {
        final ItemStack stack = new ItemStack(material, amount);
        final ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }
}
