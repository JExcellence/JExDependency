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

package com.raindropcentral.rdt.commands;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.service.TownFobService;
import com.raindropcentral.rdt.service.TownRuntimeService;
import com.raindropcentral.rdt.service.TownService;
import com.raindropcentral.rdt.utils.TownPermissions;
import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.evaluable.section.PermissionsSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the shared {@code /rt} command root.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PRTTest {

    @Mock
    private ACommandSection commandSection;

    @Mock
    private JavaPlugin javaPlugin;

    @Mock
    private TownRuntimeService townRuntimeService;

    @Mock
    private TownFobService townFobService;

    @Mock
    private TownService townService;

    @Mock
    private PermissionsSection permissionsSection;

    @Mock
    private Server server;

    @Mock
    private ConsoleCommandSender console;

    @Mock
    private Player player;

    @Mock
    private Player onlineMember;

    private RDT rdt;

    @BeforeEach
    void setUp() {
        when(this.commandSection.getName()).thenReturn("rt");
        when(this.commandSection.getDescription()).thenReturn("RDT town commands");
        when(this.commandSection.getUsage()).thenReturn("rt <main | spawn | fob | bank>");
        when(this.commandSection.getAliases()).thenReturn(List.of("rt"));
        this.rdt = new RDT(this.javaPlugin, "test", this.townService) {
            @Override
            public TownRuntimeService getTownRuntimeService() {
                return PRTTest.this.townRuntimeService;
            }

            @Override
            public TownFobService getTownFobService() {
                return PRTTest.this.townFobService;
            }
        };
    }

    @Test
    void consoleBroadcastSendsMiniMessageOnlyToOnlineTownMembers() {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown town = new RTown(townUuid, mayorUuid, "Founders", null);
        town.addMember(new RDTPlayer(mayorUuid, townUuid, RTown.MAYOR_ROLE_ID));

        when(this.javaPlugin.getServer()).thenReturn(this.server);
        when(this.townRuntimeService.getTown(townUuid)).thenReturn(town);
        when(this.server.getPlayer(mayorUuid)).thenReturn(this.onlineMember);
        when(this.onlineMember.isOnline()).thenReturn(true);

        final PRT command = new PRT(this.commandSection, this.rdt);

        assertTrue(command.execute(
            this.console,
            "rt",
            new String[]{"broadcast", townUuid.toString(), "<yellow>Hello", "Town</yellow>"}
        ));

        final ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(this.onlineMember).sendMessage(messageCaptor.capture());
        assertEquals(
            "Hello Town",
            PlainTextComponentSerializer.plainText().serialize(messageCaptor.getValue())
        );
        verify(this.console).sendMessage("Sent town broadcast to 1 online member(s) of Founders.");
    }

    @Test
    void playerBroadcastRouteRemainsConsoleOnly() {
        when(this.commandSection.getNotAConsoleMessage(any())).thenReturn(Component.text("console only"));

        final PRT command = new PRT(this.commandSection, this.rdt);

        assertFalse(command.execute(
            this.player,
            "rt",
            new String[]{"broadcast", UUID.randomUUID().toString(), "<yellow>Hi</yellow>"}
        ));

        verify(this.player).sendMessage("console only");
        verifyNoInteractions(this.townRuntimeService);
    }

    @Test
    void playerTabCompletionIncludesFobWhenItMatchesTheTypedPrefix() {
        final PRT command = new PRT(this.commandSection, this.rdt);

        assertEquals(List.of("fob"), command.tabComplete(this.player, "rt", new String[]{"f"}));
    }

    @Test
    void fobCommandStopsAtTheBukkitPermissionGate() {
        when(this.commandSection.getPermissions()).thenReturn(this.permissionsSection);
        when(this.permissionsSection.hasPermission(this.player, EPRTPermission.FOB)).thenReturn(false);

        final PRT command = new PRT(this.commandSection, this.rdt);

        assertTrue(command.execute(this.player, "rt", new String[]{"fob"}));

        verify(this.permissionsSection).sendMissingMessage(this.player, EPRTPermission.FOB);
        verifyNoInteractions(this.townRuntimeService);
        verifyNoInteractions(this.townFobService);
    }

    @Test
    void fobCommandRequiresThePlayerToBelongToATown() {
        final UUID playerUuid = UUID.randomUUID();
        when(this.player.getUniqueId()).thenReturn(playerUuid);
        when(this.townRuntimeService.getTownFor(playerUuid)).thenReturn(null);

        final PRT command = new PRT(this.commandSection, this.rdt);

        assertTrue(command.execute(this.player, "rt", new String[]{"fob"}));

        verify(this.townRuntimeService).getTownFor(playerUuid);
        verifyNoMoreInteractions(this.townRuntimeService);
        verifyNoInteractions(this.townFobService);
    }

    @Test
    void fobCommandRequiresTheTownRolePermission() {
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(UUID.randomUUID(), playerUuid, "Founders", null);
        when(this.player.getUniqueId()).thenReturn(playerUuid);
        when(this.townRuntimeService.getTownFor(playerUuid)).thenReturn(town);
        when(this.townRuntimeService.hasTownPermission(this.player, town, TownPermissions.USE_FOB)).thenReturn(false);

        final PRT command = new PRT(this.commandSection, this.rdt);

        assertTrue(command.execute(this.player, "rt", new String[]{"fob"}));

        verify(this.townRuntimeService).getTownFor(playerUuid);
        verify(this.townRuntimeService).hasTownPermission(this.player, town, TownPermissions.USE_FOB);
        verifyNoMoreInteractions(this.townRuntimeService);
        verifyNoInteractions(this.townFobService);
    }

    @Test
    void fobCommandTreatsTeleportStartupFailuresAsUnavailable() {
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(UUID.randomUUID(), playerUuid, "Founders", null);
        when(this.player.getUniqueId()).thenReturn(playerUuid);
        when(this.townRuntimeService.getTownFor(playerUuid)).thenReturn(town);
        when(this.townRuntimeService.hasTownPermission(this.player, town, TownPermissions.USE_FOB)).thenReturn(true);
        when(this.townFobService.teleportToTownFob(this.player)).thenReturn(false);

        final PRT command = new PRT(this.commandSection, this.rdt);

        assertTrue(command.execute(this.player, "rt", new String[]{"fob"}));

        verify(this.townFobService).teleportToTownFob(this.player);
    }

    @Test
    void fobCommandDelegatesToTownFobServiceWhenThePlayerCanUseIt() {
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(UUID.randomUUID(), playerUuid, "Founders", null);
        when(this.player.getUniqueId()).thenReturn(playerUuid);
        when(this.townRuntimeService.getTownFor(playerUuid)).thenReturn(town);
        when(this.townRuntimeService.hasTownPermission(this.player, town, TownPermissions.USE_FOB)).thenReturn(true);
        when(this.townFobService.teleportToTownFob(this.player)).thenReturn(true);

        final PRT command = new PRT(this.commandSection, this.rdt);

        assertTrue(command.execute(this.player, "rt", new String[]{"fob"}));

        verify(this.townFobService).teleportToTownFob(this.player);
    }
}
