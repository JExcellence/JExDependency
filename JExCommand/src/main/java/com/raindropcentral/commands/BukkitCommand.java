package com.raindropcentral.commands;

import de.jexcellence.evaluable.EnumInfo;
import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import de.jexcellence.evaluable.error.ErrorContext;
import de.jexcellence.evaluable.section.ACommandSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract Bukkit command wrapper that bridges command execution to custom handlers while
 * providing utility methods for argument parsing, error handling, and tab completion.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class BukkitCommand extends Command {
	
        /**
         * Shared immutable reference returned when no tab completions are available.
         * Prevents repeated allocations when commands opt out of tab completion.
         */
        protected static final List<String>                            EMPTY_STRING_LIST  = Collections.unmodifiableList(new ArrayList<>());
        /**
         * Cache of {@link EnumInfo} descriptors keyed by enum type to accelerate enum lookups.
         * <p>
         * The cache is backed by a {@link WeakHashMap} to avoid preventing class unloading when
         * plugins are reloaded.
         */
        private static final   Map<Class<? extends Enum<?>>, EnumInfo> enumConstantsCache = Collections.synchronizedMap(new WeakHashMap<>());
        /**
         * Serializer used to translate Adventure {@link Component} instances into legacy strings for
         * Bukkit's messaging API.
         */
        private static final   LegacyComponentSerializer              LEGACY_SERIALIZER  = LegacyComponentSerializer.legacySection();
        /**
         * Configuration-backed command section that supplies metadata and localized error messages.
         */
        protected final        ACommandSection                         commandSection;

        /**
         * Creates a Bukkit command wrapper bound to the supplied configuration section.
         *
         * @param commandSection configuration node providing metadata, permissions, and messages
         */
        protected BukkitCommand(@NotNull ACommandSection commandSection) {
		
		super(
			commandSection.getName(),
			commandSection.getDescription(),
			commandSection.getUsage(),
			commandSection.getAliases()
		);
		this.commandSection = commandSection;
	}
	
        /**
         * Executes the command-specific logic after the Bukkit wrapper has validated the request.
         *
         * @param sender command executor supplied by Bukkit
         * @param alias  alias used to trigger the command
         * @param args   argument array passed to the command
         * @throws CommandError when the invocation fails with a user-facing error message
         */
        protected abstract void onInvocation(
                CommandSender sender,
                String alias,
                String[] args
        );

        /**
         * Resolves tab completion suggestions for the command.
         *
         * @param sender command executor supplied by Bukkit
         * @param alias  alias used to trigger the command
         * @param args   argument array provided by the tab completion request
         * @return list of suggestions that Bukkit should present to the sender
         * @throws CommandError when the completion logic needs to signal a contextual error
         */
        protected abstract List<String> onTabCompletion(
                CommandSender sender,
                String alias,
                String[] args
        );
	
        /**
         * Executes the command, routing to {@link #onInvocation(CommandSender, String, String[])}
         * and converting thrown {@link CommandError}s into localized feedback.
         *
         * @return {@code true} when the wrapped invocation succeeds, otherwise {@code false}
         */
        @Override
        public boolean execute(
                @NotNull CommandSender sender,
                @NotNull String alias,
                @NotNull String[] args
        ) {

                return this.executeAndHandleCommandErrors(
			() -> {
				this.onInvocation(
					sender,
					alias,
					args
				);
				return true;
			},
			false,
			sender,
			alias,
			args
		);
	}
	
        /**
         * Delegates Bukkit tab completion to {@link #onTabCompletion(CommandSender, String, String[])} while
         * mapping any {@link CommandError} to localized feedback.
         */
        @Override
        public @NotNull List<String> tabComplete(
                @NotNull CommandSender sender,
                @NotNull String alias,
                @NotNull String[] args
        ) {

		return this.executeAndHandleCommandErrors(
			() -> this.onTabCompletion(
				sender,
				alias,
				args
			),
			EMPTY_STRING_LIST,
			sender,
			alias,
			args
		);
	}
	
        /**
         * Resolves an enum argument by lower-case name, throwing a {@link CommandError} when the
         * provided token cannot be matched.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the enum argument being parsed
         * @param enumClass     enum type expected for the argument
         * @param <T>           enum type parameter returned to the caller
         * @return enum constant matching the input token
         * @throws CommandError when the argument is missing or malformed
         */
        protected <T extends Enum<?>> T enumParameter(
                String[] args,
                int argumentIndex,
                Class<T> enumClass
        ) {

		EnumInfo enumInfo = enumConstantsCache.computeIfAbsent(
			enumClass,
			EnumInfo::new
		);
		Object constant = enumInfo.enumConstantByLowerCaseName.get(this.resolveArgument(
			args,
			argumentIndex
		).toLowerCase());
		if (constant == null) {
			throw new CommandError(
				argumentIndex,
				EErrorType.MALFORMED_ENUM,
				enumInfo
			);
		} else {
			return (T) constant;
		}
	}
	
        /**
         * Attempts to parse an enum argument and falls back to the provided default when the
         * argument is absent.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the enum argument being parsed
         * @param enumClass     enum type expected for the argument
         * @param fallback      value returned when the argument is not present
         * @param <T>           enum type parameter returned to the caller
         * @return parsed enum constant or the fallback value
         * @throws CommandError when the argument is present but malformed
         */
        protected <T extends Enum<?>> T enumParameterOrElse(
                String[] args,
                int argumentIndex,
                Class<T> enumClass,
                T fallback
        ) {
		
		return this.invokeIfArgPresentOrElse(
			() -> this.enumParameter(
				args,
				argumentIndex,
				enumClass
			),
			fallback
		);
	}
	
        /**
         * Resolves the argument at {@code argumentIndex} as an online {@link Player} instance.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the player argument being parsed
         * @return player currently connected to the server
         * @throws CommandError when the argument is missing or the player is not online
         */
        protected Player playerParameter(
                String[] args,
                int argumentIndex
        ) {
		
		Player player = Bukkit.getPlayer(this.resolveArgument(
			args,
			argumentIndex
		));
		if (player == null) {
			throw new CommandError(
				argumentIndex,
				EErrorType.PLAYER_NOT_ONLINE
			);
		} else {
			return player;
		}
	}
	
        /**
         * Attempts to resolve a {@link Player} and falls back to the supplied default when the
         * argument is missing.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the player argument being parsed
         * @param fallback      player returned when the argument is not provided
         * @return resolved player or the fallback value
         * @throws CommandError when the provided argument references a player who is offline
         */
        protected Player playerParameterOrElse(
                String[] args,
                int argumentIndex,
                Player fallback
        ) {
		
		return this.invokeIfArgPresentOrElse(
			() -> this.playerParameter(
				args,
				argumentIndex
			),
			fallback
		);
	}
	
        /**
         * Returns the argument string at the requested index, validating that it exists.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the string argument being accessed
         * @return argument token provided by the sender
         * @throws CommandError when the argument is missing
         */
        protected String stringParameter(
                String[] args,
                int argumentIndex
        ) {

                return this.resolveArgument(
			args,
			argumentIndex
		);
	}
	
        /**
         * Resolves the argument at {@code argumentIndex} as an {@link OfflinePlayer} and optionally
         * verifies the player has previously joined the server.
         *
         * @param args            raw argument array supplied to the command
         * @param argumentIndex   index of the player argument being parsed
         * @param hasToHavePlayed when {@code true}, requires the player to have joined before
         * @return offline player matching the provided name or UUID
         * @throws CommandError when the argument is missing or the player has not joined previously
         */
        protected OfflinePlayer offlinePlayerParameter(
                String[] args,
                int argumentIndex,
                boolean hasToHavePlayed
        ) {
		
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(this.resolveArgument(
			args,
			argumentIndex
		));
		if (hasToHavePlayed && ! offlinePlayer.hasPlayedBefore()) {
			throw new CommandError(
				argumentIndex,
				EErrorType.PLAYER_UNKNOWN
			);
		} else {
			return offlinePlayer;
		}
	}
	
        /**
         * Attempts to resolve an {@link OfflinePlayer} argument and returns the fallback when the
         * argument is absent.
         *
         * @param args            raw argument array supplied to the command
         * @param argumentIndex   index of the player argument being parsed
         * @param hasToHavePlayed when {@code true}, requires the player to have joined before
         * @param fallback        offline player returned when the argument is missing
         * @return resolved offline player or the fallback value
         * @throws CommandError when a provided argument fails validation
         */
        protected OfflinePlayer offlinePlayerParameterOrElse(
                String[] args,
                int argumentIndex,
                boolean hasToHavePlayed,
                OfflinePlayer fallback
        ) {
		
		return this.invokeIfArgPresentOrElse(
			() -> this.offlinePlayerParameter(
				args,
				argumentIndex,
				hasToHavePlayed
			),
			fallback
		);
	}
	
        /**
         * Parses an argument into a {@link UUID}.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the UUID argument being parsed
         * @return UUID derived from the argument token
         * @throws CommandError when the argument is missing or not a valid UUID string
         */
        protected UUID uuidParameter(
                String[] args,
                int argumentIndex
        ) {
		
		try {
			return UUID.fromString(this.resolveArgument(
				args,
				argumentIndex
			));
		} catch (IllegalArgumentException e) {
			throw new CommandError(
				argumentIndex,
				EErrorType.MALFORMED_UUID
			);
		}
	}
	
        /**
         * Parses a {@link UUID} argument when present or returns the provided fallback otherwise.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the UUID argument being parsed
         * @param fallback      UUID returned when the argument is not provided
         * @return parsed UUID or the fallback value
         * @throws CommandError when a provided argument is malformed
         */
        protected UUID uuidParameterOrElse(
                String[] args,
                int argumentIndex,
                UUID fallback
        ) {
		
		return this.invokeIfArgPresentOrElse(
			() -> this.uuidParameter(
				args,
				argumentIndex
			),
			fallback
		);
	}
	
        /**
         * Parses an argument into an {@link Integer}.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the integer argument being parsed
         * @return integer value parsed from the argument token
         * @throws CommandError when the argument is missing or not a valid integer string
         */
        protected Integer integerParameter(
                String[] args,
                int argumentIndex
        ) {
		
		try {
			return Integer.parseInt(this.resolveArgument(
				args,
				argumentIndex
			));
		} catch (NumberFormatException e) {
			throw new CommandError(
				argumentIndex,
				EErrorType.MALFORMED_INTEGER
			);
		}
	}
	
        /**
         * Parses an {@link Integer} argument when present or returns the provided fallback otherwise.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the integer argument being parsed
         * @param fallback      integer returned when the argument is not provided
         * @return parsed integer or the fallback value
         * @throws CommandError when the provided argument cannot be parsed as an integer
         */
        protected Integer integerParameterOrElse(
                String[] args,
                int argumentIndex,
                Integer fallback
        ) {
		
		return this.invokeIfArgPresentOrElse(
			() -> this.integerParameter(
				args,
				argumentIndex
			),
			fallback
		);
	}
	
        /**
         * Parses an argument into a {@link Long}.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the long argument being parsed
         * @return long value parsed from the argument token
         * @throws CommandError when the argument is missing or not a valid long string
         */
        protected Long longParameter(
                String[] args,
                int argumentIndex
        ) {
		
		try {
			return Long.parseLong(this.resolveArgument(
				args,
				argumentIndex
			));
		} catch (NumberFormatException e) {
			throw new CommandError(
				argumentIndex,
				EErrorType.MALFORMED_LONG
			);
		}
	}
	
        /**
         * Parses a {@link Long} argument when present or returns the provided fallback otherwise.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the long argument being parsed
         * @param fallback      long returned when the argument is not provided
         * @return parsed long or the fallback value
         * @throws CommandError when the provided argument cannot be parsed as a long
         */
        protected Long longParameterOrElse(
                String[] args,
                int argumentIndex,
                Long fallback
        ) {
		
		return this.invokeIfArgPresentOrElse(
			() -> this.longParameter(
				args,
				argumentIndex
			),
			fallback
		);
	}
	
        /**
         * Parses an argument into a {@link Double}.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the double argument being parsed
         * @return double value parsed from the argument token
         * @throws CommandError when the argument is missing or not a valid double string
         */
        protected Double doubleParameter(
                String[] args,
                int argumentIndex
        ) {
		
		try {
			return Double.parseDouble(this.resolveArgument(
				args,
				argumentIndex
			));
		} catch (NumberFormatException e) {
			throw new CommandError(
				argumentIndex,
				EErrorType.MALFORMED_DOUBLE
			);
		}
	}
	
        /**
         * Parses a {@link Double} argument when present or returns the provided fallback otherwise.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the double argument being parsed
         * @param fallback      double returned when the argument is not provided
         * @return parsed double or the fallback value
         * @throws CommandError when the provided argument cannot be parsed as a double
         */
        protected Double doubleParameterOrElse(
                String[] args,
                int argumentIndex,
                Double fallback
        ) {
		
		return this.invokeIfArgPresentOrElse(
			() -> this.doubleParameter(
				args,
				argumentIndex
			),
			fallback
		);
	}
	
        /**
         * Parses an argument into a {@link Float}.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the float argument being parsed
         * @return float value parsed from the argument token
         * @throws CommandError when the argument is missing or not a valid float string
         */
        protected Float floatParameter(
                String[] args,
                int argumentIndex
        ) {
		
		try {
			return Float.parseFloat(this.resolveArgument(
				args,
				argumentIndex
			));
		} catch (NumberFormatException e) {
			throw new CommandError(
				argumentIndex,
				EErrorType.MALFORMED_FLOAT
			);
		}
	}
	
        /**
         * Parses a {@link Float} argument when present or returns the provided fallback otherwise.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the float argument being parsed
         * @param fallback      float returned when the argument is not provided
         * @return parsed float or the fallback value
         * @throws CommandError when the provided argument cannot be parsed as a float
         */
        protected Float floatParameterOrElse(
                String[] args,
                int argumentIndex,
                Float fallback
        ) {
		
		return this.invokeIfArgPresentOrElse(
			() -> this.floatParameter(
				args,
				argumentIndex
			),
			fallback
		);
	}
	
        /**
         * Executes the supplied lookup only when the argument is present, returning a fallback when
         * the lookup fails due to {@link EErrorType#MISSING_ARGUMENT}.
         *
         * @param executable lookup supplier evaluated when the argument exists
         * @param fallback   value returned when the argument is absent
         * @param <T>        type returned by the lookup
         * @return lookup result or the fallback value when the argument is missing
         */
        private <T> T invokeIfArgPresentOrElse(
                Supplier<T> executable,
                T fallback
        ) {
		
		try {
			return executable.get();
		} catch (CommandError error) {
			if (error.errorType == EErrorType.MISSING_ARGUMENT) {
				return fallback;
			} else {
				throw error;
			}
		}
	}
	
        /**
         * Validates that an argument exists at the requested index and returns it.
         *
         * @param args          raw argument array supplied to the command
         * @param argumentIndex index of the argument being accessed
         * @return argument token stored at the requested index
         * @throws CommandError           when the argument is missing
         * @throws IllegalArgumentException when {@code argumentIndex} is negative
         */
        private String resolveArgument(
                String[] args,
                int argumentIndex
        ) {
		
		if (argumentIndex < 0) {
			throw new IllegalArgumentException("Argument indices start at zero");
		}
		if (argumentIndex >= args.length) {
			throw new CommandError(
				argumentIndex,
				EErrorType.MISSING_ARGUMENT
			);
		}
		return args[argumentIndex];
	}
	
        /**
         * Sends an Adventure {@link Component} message to a {@link CommandSender} after converting it
         * to a legacy-formatted string for maximum platform compatibility.
         *
         * @param sender    recipient of the message
         * @param component component to be serialized and delivered
         */
        private void sendComponentMessage(CommandSender sender, Component component) {
		
		// Convert to legacy format for universal Bukkit/Spigot/Paper compatibility
		String legacyMessage = LEGACY_SERIALIZER.serialize(component);
		sender.sendMessage(legacyMessage);
	}
	
        /**
         * Wraps command execution to translate {@link CommandError}s into localized feedback and log
         * unexpected failures.
         *
         * @param executable         command or tab completion logic to run
         * @param returnValueOnError value returned when an error occurs
         * @param sender             command executor supplied by Bukkit
         * @param alias              alias used to invoke the command
         * @param args               argument array supplied to the command
         * @param <T>                type returned by the executable
         * @return result of the executable or {@code returnValueOnError} when an error occurs
         */
        private <T> T executeAndHandleCommandErrors(
                Supplier<T> executable,
                T returnValueOnError,
                CommandSender sender,
                String alias,
                String[] args
	) {
		
		try {
			return executable.get();
		} catch (CommandError commandError) {
			this.handleError(
				commandError,
				sender,
				alias,
				args
			);
			return returnValueOnError;
		} catch (
			  final Exception exception
		) {
			ErrorContext context = new ErrorContext(
				sender,
				alias,
				args,
				null
			);
			this.sendComponentMessage(sender, this.commandSection.getInternalErrorMessage(context));
			Logger.getLogger("RCommands").log(
				Level.WARNING,
				"Error occurred while executing the command",
				exception
			);
			return returnValueOnError;
		}
	}
	
        /**
         * Resolves a localized error message from the {@link ACommandSection} and delivers it to the
         * sender.
         *
         * @param error  domain error thrown by the command logic
         * @param sender command executor supplied by Bukkit
         * @param alias  alias used to invoke the command
         * @param args   argument array supplied to the command
         */
        private void handleError(
                CommandError error,
                CommandSender sender,
                String alias,
		String[] args
	) {
		
		ErrorContext context = new ErrorContext(
			sender,
			alias,
			args,
			error.argumentIndex
		);
		Component message = switch (error.errorType) {
			case MALFORMED_DOUBLE -> this.commandSection.getMalformedDoubleMessage(context);
			case MALFORMED_FLOAT -> this.commandSection.getMalformedFloatMessage(context);
			case MALFORMED_LONG -> this.commandSection.getMalformedLongMessage(context);
			case MALFORMED_INTEGER -> this.commandSection.getMalformedIntegerMessage(context);
			case MALFORMED_UUID -> this.commandSection.getMalformedUuidMessage(context);
			case MALFORMED_ENUM -> this.commandSection.getMalformedEnumMessage(
				context,
				(EnumInfo) error.parameter
			);
			case MISSING_ARGUMENT -> this.commandSection.getMissingArgumentMessage(context);
			case NOT_A_PLAYER -> this.commandSection.getNotAPlayerMessage(context);
			case NOT_A_CONSOLE -> this.commandSection.getNotAConsoleMessage(context);
			case PLAYER_UNKNOWN -> this.commandSection.getPlayerUnknownMessage(context);
			case PLAYER_NOT_ONLINE -> this.commandSection.getPlayerNotOnlineMessage(context);
		};
		this.sendComponentMessage(sender, message);
	}
	
}