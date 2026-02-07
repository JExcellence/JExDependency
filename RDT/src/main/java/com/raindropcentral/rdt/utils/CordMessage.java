package com.raindropcentral.rdt.utils;

import net.kyori.adventure.bossbar.BossBar;

import java.util.UUID;

public record CordMessage(
        UUID player,
        int x,
        int z,
        BossBar bossBar
){}