package de.jexcellence.oneblock.command.player.island;

import de.jexcellence.evaluable.section.IPermissionNode;

public enum EIslandPermission implements IPermissionNode {
    
    COMMAND("command", "jexoneblock.island.command"),
    
    INFO("commandInfo", "jexoneblock.island.info"),
    STATS("commandStats", "jexoneblock.island.stats"),
    LEVEL("commandLevel", "jexoneblock.island.level"),
    TOP("commandTop", "jexoneblock.island.top"),
    
    EVOLUTION("commandEvolution", "jexoneblock.island.evolution"),
    ONEBLOCK("commandOneblock", "jexoneblock.island.oneblock"),
    PRESTIGE("commandPrestige", "jexoneblock.island.prestige"),
    
    HOME("commandHome", "jexoneblock.island.home"),
    SETHOME("commandSethome", "jexoneblock.island.sethome"),
    
    MEMBERS("commandMembers", "jexoneblock.island.members"),
    INVITE("commandInvite", "jexoneblock.island.invite"),
    ACCEPT("commandAccept", "jexoneblock.island.accept"),
    DENY("commandDeny", "jexoneblock.island.deny"),
    KICK("commandKick", "jexoneblock.island.kick"),
    BAN("commandBan", "jexoneblock.island.ban"),
    UNBAN("commandUnban", "jexoneblock.island.unban"),
    LEAVE("commandLeave", "jexoneblock.island.leave"),
    
    SETTINGS("commandSettings", "jexoneblock.island.settings"),
    VISITORS("commandVisitors", "jexoneblock.island.visitors"),
    BIOME("commandBiome", "jexoneblock.island.biome"),
    UPGRADES("commandUpgrades", "jexoneblock.island.upgrades"),
    STORAGE("commandStorage", "jexoneblock.island.storage"),
    
    CREATE("commandCreate", "jexoneblock.island.create"),
    DELETE("commandDelete", "jexoneblock.island.delete"),
    
    ADMIN("admin", "jexoneblock.island.admin"),
    BYPASS("bypass", "jexoneblock.island.bypass");
    
    private final String internalName;
    private final String fallbackNode;
    
    EIslandPermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getFallbackNode() {
        return fallbackNode;
    }

    public static EIslandPermission fromInternalName(String internalName) {
        for (var permission : values()) {
            if (permission.internalName.equals(internalName)) {
                return permission;
            }
        }
        return null;
    }

    public static EIslandPermission fromFallbackNode(String fallbackNode) {
        for (var permission : values()) {
            if (permission.fallbackNode.equals(fallbackNode)) {
                return permission;
            }
        }
        return null;
    }

    public static String[] getAllNodes() {
        return java.util.Arrays.stream(values())
            .map(EIslandPermission::getFallbackNode)
            .toArray(String[]::new);
    }

    public boolean isAdministrative() {
        return this == ADMIN || this == BYPASS || this == DELETE;
    }

    public boolean requiresHighAccess() {
        return switch (this) {
            case DELETE, PRESTIGE, SETTINGS, VISITORS, BIOME, UPGRADES, BAN, ADMIN, BYPASS -> true;
            default -> false;
        };
    }

    public EIslandAction getCorrespondingAction() {
        return switch (this) {
            case INFO -> EIslandAction.INFO;
            case STATS -> EIslandAction.STATS;
            case LEVEL -> EIslandAction.LEVEL;
            case TOP -> EIslandAction.TOP;
            case EVOLUTION -> EIslandAction.EVOLUTION;
            case ONEBLOCK -> EIslandAction.ONEBLOCK;
            case PRESTIGE -> EIslandAction.PRESTIGE;
            case HOME -> EIslandAction.HOME;
            case SETHOME -> EIslandAction.SETHOME;
            case MEMBERS -> EIslandAction.MEMBERS;
            case INVITE -> EIslandAction.INVITE;
            case ACCEPT -> EIslandAction.ACCEPT;
            case DENY -> EIslandAction.DENY;
            case KICK -> EIslandAction.KICK;
            case BAN -> EIslandAction.BAN;
            case UNBAN -> EIslandAction.UNBAN;
            case LEAVE -> EIslandAction.LEAVE;
            case SETTINGS -> EIslandAction.SETTINGS;
            case VISITORS -> EIslandAction.VISITORS;
            case BIOME -> EIslandAction.BIOME;
            case UPGRADES -> EIslandAction.UPGRADES;
            case CREATE -> EIslandAction.CREATE;
            case DELETE -> EIslandAction.DELETE;
            default -> null;
        };
    }
}