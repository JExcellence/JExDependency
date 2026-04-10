/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.service.LevelScope;
import com.raindropcentral.rdt.service.TownService;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests nation-menu routing behavior for {@link TownNationView}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TownNationViewTest {

    @Mock
    private SlotClickContext clickContext;

    private final RDT plugin = new RDT(
        Mockito.mock(JavaPlugin.class),
        "test",
        Mockito.mock(TownService.class)
    );

    @Test
    void handleCreateNationClickRoutesToRequirementsUntilReady() throws ReflectiveOperationException {
        final TownNationView view = new TownNationView();
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        when(this.clickContext.getInitialData()).thenReturn(Map.of("plugin", this.plugin, "town_uuid", town.getTownUUID()));

        this.invokeHandleCreateNationClick(
            view,
            town,
            new com.raindropcentral.rdt.service.NationCreationProgressSnapshot(true, false, false, false, 0.5D, List.of(), List.of())
        );

        verify(this.clickContext).openForPlayer(eq(TownNationRequirementsView.class), any());
    }

    @Test
    void handleCreateNationClickRoutesToNameInputWhenReady() throws ReflectiveOperationException {
        final TownNationView view = new TownNationView();
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        when(this.clickContext.getInitialData()).thenReturn(Map.of("plugin", this.plugin, "town_uuid", town.getTownUUID()));

        this.invokeHandleCreateNationClick(
            view,
            town,
            new com.raindropcentral.rdt.service.NationCreationProgressSnapshot(true, false, false, true, 1.0D, List.of(), List.of())
        );

        verify(this.clickContext).openForPlayer(eq(CreateNationNameAnvilView.class), any());
    }

    @Test
    void handleProgressionClickRoutesToSharedNationLevelHub() throws ReflectiveOperationException {
        final TownNationView view = new TownNationView();
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        when(this.clickContext.getInitialData()).thenReturn(Map.of("plugin", this.plugin, "town_uuid", town.getTownUUID()));

        this.invokeHandleProgressionClick(view, town);

        verify(this.clickContext).openForPlayer(
            TownLevelProgressView.class,
            Map.of(
                "plugin", this.plugin,
                "town_uuid", town.getTownUUID(),
                TownLevelViewSupport.SCOPE_KEY, LevelScope.NATION
            )
        );
    }

    private void invokeHandleCreateNationClick(
        final TownNationView view,
        final RTown town,
        final com.raindropcentral.rdt.service.NationCreationProgressSnapshot snapshot
    ) throws ReflectiveOperationException {
        final Method method = TownNationView.class.getDeclaredMethod(
            "handleCreateNationClick",
            SlotClickContext.class,
            RTown.class,
            com.raindropcentral.rdt.service.NationCreationProgressSnapshot.class
        );
        method.setAccessible(true);
        method.invoke(view, this.clickContext, town, snapshot);
    }

    private void invokeHandleProgressionClick(
        final TownNationView view,
        final RTown town
    ) throws ReflectiveOperationException {
        final Method method = TownNationView.class.getDeclaredMethod(
            "handleProgressionClick",
            SlotClickContext.class,
            RTown.class
        );
        method.setAccessible(true);
        method.invoke(view, this.clickContext, town);
    }
}
