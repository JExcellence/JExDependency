package de.jexcellence.economy.database.entity;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyTest {

        @Test
        void fullConstructorInitializesAllFields() {
                final Currency currency = new Currency(
                        "Coins: ",
                        " coins",
                        "coins",
                        "¢",
                        Material.GOLD_INGOT
                );

                assertEquals("Coins: ", currency.getPrefix());
                assertEquals(" coins", currency.getSuffix());
                assertEquals("coins", currency.getIdentifier());
                assertEquals("¢", currency.getSymbol());
                assertEquals(Material.GOLD_INGOT, currency.getIcon());
        }

        @Test
        void minimalConstructorAppliesDefaultValues() {
                final Currency currency = new Currency("credits");

                assertEquals("", currency.getPrefix());
                assertEquals("", currency.getSuffix());
                assertEquals("credits", currency.getIdentifier());
                assertEquals("", currency.getSymbol());
                assertEquals(Material.GOLD_INGOT, currency.getIcon());
        }

        @Test
        void constructorRejectsNullIdentifier() {
                assertThrows(IllegalArgumentException.class, () -> new Currency(
                        "",
                        "",
                        null,
                        "",
                        Material.EMERALD
                ));
        }

        @Test
        void constructorRejectsNullIcon() {
                assertThrows(IllegalArgumentException.class, () -> new Currency(
                        "",
                        "",
                        "tokens",
                        "T",
                        null
                ));
        }

        @Test
        void settersNormalizeNullFormattingValues() {
                final Currency currency = new Currency("", "", "tokens", "T", Material.DIAMOND);

                currency.setPrefix(null);
                currency.setSuffix(null);
                currency.setSymbol(null);

                assertEquals("", currency.getPrefix());
                assertEquals("", currency.getSuffix());
                assertEquals("", currency.getSymbol());
        }

        @Test
        void settersRejectNullIdentifierAndIcon() {
                final Currency currency = new Currency("", "", "tokens", "T", Material.DIAMOND);

                assertThrows(IllegalArgumentException.class, () -> currency.setIdentifier(null));
                assertThrows(IllegalArgumentException.class, () -> currency.setIcon(null));
        }

        @Test
        void settersUpdateValuesWhenValid() {
                final Currency currency = new Currency("", "", "tokens", "T", Material.DIAMOND);

                currency.setPrefix("Token: ");
                currency.setSuffix(" pts");
                currency.setIdentifier("credits");
                currency.setSymbol("C");
                currency.setIcon(Material.NETHERITE_INGOT);

                assertEquals("Token: ", currency.getPrefix());
                assertEquals(" pts", currency.getSuffix());
                assertEquals("credits", currency.getIdentifier());
                assertEquals("C", currency.getSymbol());
                assertEquals(Material.NETHERITE_INGOT, currency.getIcon());
        }

        @Test
        void builderCreatesCurrencyWithProvidedValues() {
                final Currency currency = Currency.builder()
                        .prefix("Gems: ")
                        .suffix(" gems")
                        .identifier("gems")
                        .symbol("G")
                        .icon(Material.EMERALD)
                        .build();

                assertEquals("Gems: ", currency.getPrefix());
                assertEquals(" gems", currency.getSuffix());
                assertEquals("gems", currency.getIdentifier());
                assertEquals("G", currency.getSymbol());
                assertEquals(Material.EMERALD, currency.getIcon());
        }

        @Test
        void builderAppliesDefaultsForOptionalValues() {
                final Currency currency = Currency.builder()
                        .identifier("silver")
                        .build();

                assertEquals("", currency.getPrefix());
                assertEquals("", currency.getSuffix());
                assertEquals("silver", currency.getIdentifier());
                assertEquals("", currency.getSymbol());
                assertEquals(Material.GOLD_INGOT, currency.getIcon());
        }

        @Test
        void builderNormalizesNullFormattingValues() {
                final Currency currency = Currency.builder()
                        .prefix(null)
                        .suffix(null)
                        .symbol(null)
                        .identifier("karma")
                        .build();

                assertEquals("", currency.getPrefix());
                assertEquals("", currency.getSuffix());
                assertEquals("", currency.getSymbol());
        }

        @Test
        void builderRequiresIdentifier() {
                final Currency.Builder builder = Currency.builder();

                assertThrows(IllegalStateException.class, builder::build);
        }

        @Test
        void builderValidatesIdentifierAndIcon() {
                final Currency.Builder builder = Currency.builder();

                assertThrows(IllegalArgumentException.class, () -> builder.identifier(null));
                assertThrows(IllegalArgumentException.class, () -> builder.icon(null));

                builder.identifier("loyalty").icon(Material.DIAMOND);

                final Currency currency = builder.build();

                assertNotNull(currency);
                assertEquals("loyalty", currency.getIdentifier());
                assertEquals(Material.DIAMOND, currency.getIcon());
        }
}
