package com.raindropcentral.rplatform.utility.itembuilder.potion;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IPotionBuilderTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void fluentPotionContractAppliesMetadata() {
        FakePotionBuilder builder = new FakePotionBuilder();

        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 200, 1);
        PotionEffect upgradedSpeed = new PotionEffect(PotionEffectType.SPEED, 400, 3);

        FakePotionBuilder chained = builder.setBasePotionType(PotionType.SWIFTNESS);
        assertSame(builder, chained, "setBasePotionType should be fluent");

        chained = chained.addCustomEffect(speed, false);
        assertSame(builder, chained, "addCustomEffect should be fluent");

        chained = chained.addCustomEffect(upgradedSpeed, false);
        PotionMeta intermediateMeta = chained.asMeta();
        assertEquals(speed, intermediateMeta.getCustomEffects().get(0), "overwrite=false should preserve the first effect");

        chained = chained.addCustomEffect(upgradedSpeed, true);
        assertSame(builder, chained);

        ItemStack item = chained
                .setName(Component.text("Swift Brew"))
                .build();

        assertEquals(Material.POTION, item.getType());

        PotionMeta meta = (PotionMeta) item.getItemMeta();
        assertNotNull(meta);
        assertEquals(PotionType.SWIFTNESS, meta.getBasePotionData().getType());
        assertEquals(upgradedSpeed, meta.getCustomEffects().get(0));

        assertNotNull(meta.getDisplayName());
        Component displayName = LegacyComponentSerializer.legacySection().deserialize(meta.getDisplayName());
        assertEquals(Component.text("Swift Brew"), displayName);
    }

    private static final class FakePotionBuilder extends AItemBuilder<PotionMeta, FakePotionBuilder> implements IPotionBuilder<FakePotionBuilder> {

        private FakePotionBuilder() {
            super(Material.POTION);
        }

        @Override
        public FakePotionBuilder setBasePotionType(PotionType type) {
            meta.setBasePotionData(new PotionData(type));
            return this;
        }

        @Override
        public FakePotionBuilder addCustomEffect(PotionEffect effect, boolean overwrite) {
            meta.addCustomEffect(effect, overwrite);
            return this;
        }

        private PotionMeta asMeta() {
            return meta;
        }
    }
}
