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

import com.raindropcentral.rdt.utils.TownOverviewAccessMode;
import me.devnatan.inventoryframework.context.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TownOverviewViewTest {

    @Mock
    private Context context;

    @Test
    void updateScheduleKeepsPeriodicRefreshEnabled() {
        final TownOverviewView view = new TownOverviewView();

        assertEquals(20, view.getUpdateSchedule());
    }

    @Test
    void onUpdateDoesNotReenterInventoryUpdatePipeline() {
        final TownOverviewView view = new TownOverviewView();

        view.onUpdate(this.context);

        verify(this.context, never()).update();
    }

    @Test
    void resolveAccessModeDefaultsToRemoteWhenInitialDataOmitsMode() throws ReflectiveOperationException {
        final TownOverviewView view = new TownOverviewView();
        when(this.context.getInitialData()).thenReturn(Map.of());

        assertEquals(TownOverviewAccessMode.REMOTE, this.resolveAccessMode(view));
    }

    @Test
    void resolveAccessModeAcceptsSerializedModeNames() throws ReflectiveOperationException {
        final TownOverviewView view = new TownOverviewView();
        when(this.context.getInitialData()).thenReturn(Map.of("access_mode", "nexus"));

        assertEquals(TownOverviewAccessMode.NEXUS, this.resolveAccessMode(view));
    }

    private TownOverviewAccessMode resolveAccessMode(final TownOverviewView view) throws ReflectiveOperationException {
        final Method method = TownOverviewView.class.getDeclaredMethod("resolveAccessMode", Context.class);
        method.setAccessible(true);
        return (TownOverviewAccessMode) method.invoke(view, this.context);
    }
}
