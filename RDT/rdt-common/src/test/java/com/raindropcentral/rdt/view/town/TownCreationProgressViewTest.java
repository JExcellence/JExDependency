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
import com.raindropcentral.rdt.service.TownCreationProgressSnapshot;
import com.raindropcentral.rdt.service.TownService;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests creation-menu routing behavior for {@link TownCreationProgressView}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TownCreationProgressViewTest {

    @Mock
    private Context origin;

    @Mock
    private Context target;

    @Mock
    private SlotClickContext clickContext;

    private final RDT plugin = new RDT(
        Mockito.mock(JavaPlugin.class),
        "test",
        Mockito.mock(TownService.class)
    );

    @Test
    void handleCreateClickDoesNothingWhenCreationIsUnavailable() throws ReflectiveOperationException {
        final TownCreationProgressView view = new TownCreationProgressView();
        this.invokeHandleCreateClick(
            view,
            new TownCreationProgressSnapshot(false, false, false, 0.0D, List.of(), List.of())
        );

        verify(this.clickContext, never()).openForPlayer(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    void handleCreateClickRoutesToRequirementsUntilReady() throws ReflectiveOperationException {
        final TownCreationProgressView view = new TownCreationProgressView();
        when(this.clickContext.getInitialData()).thenReturn(Map.of("plugin", this.plugin));

        this.invokeHandleCreateClick(
            view,
            new TownCreationProgressSnapshot(true, false, false, 0.5D, List.of(), List.of())
        );

        verify(this.clickContext).openForPlayer(TownCreationRequirementsView.class, Map.of("plugin", this.plugin));
    }

    @Test
    void handleCreateClickRoutesToNameInputWhenReady() throws ReflectiveOperationException {
        final TownCreationProgressView view = new TownCreationProgressView();
        when(this.clickContext.getInitialData()).thenReturn(Map.of("plugin", this.plugin));

        this.invokeHandleCreateClick(
            view,
            new TownCreationProgressSnapshot(true, false, true, 1.0D, List.of(), List.of())
        );

        verify(this.clickContext).openForPlayer(CreateTownNameAnvilView.class, Map.of("plugin", this.plugin));
    }

    private void invokeHandleCreateClick(
        final TownCreationProgressView view,
        final TownCreationProgressSnapshot snapshot
    ) throws ReflectiveOperationException {
        final Method method = TownCreationProgressView.class.getDeclaredMethod(
            "handleCreateClick",
            SlotClickContext.class,
            TownCreationProgressSnapshot.class
        );
        method.setAccessible(true);
        method.invoke(view, this.clickContext, snapshot);
    }
}
