package de.jexcellence.oneblock.command.player.island;

import org.jetbrains.annotations.NotNull;

public enum EIslandAction {
    
    MAIN("main", "Opens the main island GUI", "island.main"),
    INFO("info", "Shows island information", "island.info"),
    STATS("stats", "Opens the statistics GUI", "island.stats"),
    LEVEL("level", "Shows island level and experience", "island.level"),
    TOP("top", "Shows the island leaderboard", "island.top"),
    
    EVOLUTION("evolution", "Opens the evolution overview", "island.evolution"),
    ONEBLOCK("oneblock", "Opens the OneBlock core view", "island.oneblock"),
    PRESTIGE("prestige", "Prestige your island", "island.prestige"),
    
    HOME("home", "Teleport to your island", "island.home"),
    TP("tp", "Teleport to your island", "island.home"),
    SETHOME("sethome", "Set your island home location", "island.sethome"),
    
    MEMBERS("members", "View island members", "island.members"),
    INVITE("invite", "Invite a player to your island", "island.invite"),
    ACCEPT("accept", "Accept an island invitation", "island.accept"),
    DENY("deny", "Deny an island invitation", "island.deny"),
    KICK("kick", "Kick a visitor from your island", "island.kick"),
    BAN("ban", "Ban a player from your island", "island.ban"),
    UNBAN("unban", "Unban a player from your island", "island.unban"),
    LEAVE("leave", "Leave the current island", "island.leave"),
    
    SETTINGS("settings", "Open island settings", "island.settings"),
    VISITORS("visitors", "Configure visitor permissions", "island.visitors"),
    BIOME("biome", "Change your island biome", "island.biome"),
    UPGRADES("upgrades", "View and purchase upgrades", "island.upgrades"),
    STORAGE("storage", "Manage your island storage", "island.storage"),
    
    CREATE("create", "Create a new island", "island.create"),
    DELETE("delete", "Delete your island", "island.delete"),
    
    HELP("help", "Show command help", "island.command");
    
    private final String command;
    private final String description;
    private final String permission;
    
    EIslandAction(String command, String description, String permission) {
        this.command = command;
        this.description = description;
        this.permission = permission;
    }

    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public String getPermission() {
        return permission;
    }
    
    public static EIslandAction fromCommand(@NotNull String command) {
        for (var action : values()) {
            if (action.command.equalsIgnoreCase(command)) {
                return action;
            }
        }
        return null;
    }
    
    public static String[] getCommands() {
        return java.util.Arrays.stream(values())
            .map(EIslandAction::getCommand)
            .toArray(String[]::new);
    }
    
    public static EIslandAction[] getByPermissionPrefix(@NotNull String permissionPrefix) {
        return java.util.Arrays.stream(values())
            .filter(action -> action.permission.startsWith(permissionPrefix))
            .toArray(EIslandAction[]::new);
    }
    
    public boolean requiresIsland() {
        return switch (this) {
            case CREATE, HELP, ACCEPT, DENY -> false;
            default -> true;
        };
    }
    
    public boolean requiresArguments() {
        return switch (this) {
            case INVITE, KICK, BAN, UNBAN, ACCEPT, DENY -> true;
            default -> false;
        };
    }
    
    public int getMinArguments() {
        return switch (this) {
            case INVITE, KICK, BAN, UNBAN, ACCEPT, DENY -> 1;
            default -> 0;
        };
    }
    
    public String getUsage() {
        return switch (this) {
            case INVITE -> "/island invite <player>";
            case ACCEPT -> "/island accept <island>";
            case DENY -> "/island deny <island>";
            case KICK -> "/island kick <player>";
            case BAN -> "/island ban <player> [reason]";
            case UNBAN -> "/island unban <player>";
            default -> "/island " + command;
        };
    }
}