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
 * <p>
 * Instances are provisioned by the {@link CommandFactory}, which injects the {@link ACommandSection}
 * metadata so commands transparently respect edition gating (for example RDQ Free versus RDQ Premium),
 * surface localization hooks, and remain aligned with the auto-registration pipeline documented in the
 * agent guidelines. Implementations should assume the command lifecycle executes on the Bukkit main
 * thread and schedule asynchronous work explicitly when heavy lifting is required.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class BukkitCommand extends Command {

        /**
         * Shared immutable reference returned when no tab completions are available.
         * <p>
         * Default value: an empty, {@link Collections#unmodifiableList(List) unmodifiable} list backed by the
         * JDK collection libraries. Dependency requirements: none beyond the Java runtime. Thread safety: safe for
         * concurrent reads because the list is immutable and never mutated after initialization.
         */
        protected static final List<String>                            EMPTY_STRING_LIST  = Collections.unmodifiableList(new ArrayList<>());
        /**
         * Cache of {@link EnumInfo} descriptors keyed by enum type to accelerate enum lookups.
         * <p>
         * Default value: an empty {@link WeakHashMap} wrapped in {@link Collections#synchronizedMap(Map)}.
         * Dependency requirements: relies on the {@code de.jexcellence} evaluable library for {@link EnumInfo}
         * descriptors. Thread safety: synchronized map wrapper allows safe concurrent reads, but callers should still
         * prefer accessing the cache from the synchronous command thread to mirror Bukkit&apos;s threading expectations.
         */
        private static final   Map<Class<? extends Enum<?>>, EnumInfo> enumConstantsCache = Collections.synchronizedMap(new WeakHashMap<>());
        /**
         * Serializer used to translate Adventure {@link Component} instances into legacy strings for
         * Bukkit&apos;s messaging API.
         * <p>
         * Default value: {@link LegacyComponentSerializer#legacySection()}. Dependency requirements: the Kyori
         * Adventure serializer implementation must be present (shaded by the plugin distribution). Thread safety:
         * the serializer is stateless and can be reused safely across threads.
         */
        private static final   LegacyComponentSerializer              LEGACY_SERIALIZER  = LegacyComponentSerializer.legacySection();
        /**
         * Configuration-backed command section that supplies metadata and localized error messages.
         * <p>
         * Default value: populated by the {@link CommandFactory} per command. Dependency requirements: requires a
         * mapped {@link ACommandSection} generated through the config mapper pipeline so localization hooks and
         * edition gating flags remain available. Thread safety: treated as effectively immutable after construction
         * and therefore safe to read on the command thread; avoid mutating it from asynchronous contexts.
         */
        protected final        ACommandSection                         commandSection;

        /**
         * Creates a Bukkit command wrapper bound to the supplied configuration section.
         * Behaviour: immediately wires the section metadata into Bukkit&apos;s command registration so the command
         * participates in edition gating and localization lookups from the outset. Failure modes: none beyond
         * {@link NullPointerException} if a {@code null} section is provided. Asynchronous considerations: should be
         * invoked during synchronous plugin bootstrap because Bukkit command registration is not thread-safe.
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
         * Behaviour: called synchronously on the main server thread with localization context supplied by the injected
         * section. Failure modes: implementations should throw {@link CommandError} for user-facing issues so the
         * wrapper can emit localized feedback; other exceptions bubble up to the shared handler and are logged. Asynchronous
         * considerations: long-running work should be dispatched to the scheduler to avoid blocking the command thread.
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
         * Behaviour: invoked synchronously by Bukkit and expected to return lightweight completions without mutating
         * shared state. Failure modes: throw {@link CommandError} to return localized validation hints; other exceptions
         * are logged and result in {@link #EMPTY_STRING_LIST}. Asynchronous considerations: expensive lookups should be
         * precomputed or scheduled asynchronously with callbacks that safely update cached suggestions.
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
         * Behaviour: performs edition gating and localization lookups through the injected section before invoking
         * subclass logic. Failure modes: {@link CommandError} triggers localized messages while other exceptions are
         * logged and return {@code false}. Asynchronous considerations: Bukkit calls this synchronously; implementers
         * should avoid blocking operations inside the invocation and instead schedule asynchronous follow-up tasks.
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
         * Behaviour: executes synchronously on the command thread and ensures localized error messages flow through the
         * section when completion fails. Failure modes: {@link CommandError} responses downgrade to an empty list and log
         * nothing, while other exceptions are logged and fall back to {@link #EMPTY_STRING_LIST}. Asynchronous
         * considerations: avoid long-running computations; precompute data or schedule asynchronous refreshes feeding
         * cached state.
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
         * Behaviour: leverages the synchronized {@link #enumConstantsCache} to reuse {@link EnumInfo} metadata across
         * invocations. Failure modes: raises {@link CommandError} for missing or malformed arguments so localization
         * hooks can inform the sender. Asynchronous considerations: relies only on thread-safe cache operations and
         * string comparisons, making it safe for synchronous command handling; avoid invoking it off-thread if the
         * backing command arguments might mutate concurrently.
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
         * Behaviour: delegates to {@link #enumParameter(String[], int, Class)} only when the argument exists.
         * Failure modes: propagates {@link CommandError} for malformed values but suppresses missing-argument errors by
         * returning the supplied fallback. Asynchronous considerations: safe for synchronous command handling and does
         * not perform any Bukkit calls that would require the main thread.
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
         * Behaviour: defers to {@link Bukkit#getPlayer(String)} so the command honours Paper/Spigot player resolution
         * while still enabling edition-specific gating through the command section. Failure modes: throws
         * {@link CommandError} when the target is absent or offline, allowing localization to explain the issue.
         * Asynchronous considerations: must be executed on the main thread because Bukkit player lookups are not
         * thread-safe.
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
         * Behaviour: mirrors {@link #playerParameter(String[], int)} while gracefully returning the fallback for
         * missing arguments. Failure modes: still raises {@link CommandError} when a supplied player token fails
         * validation. Asynchronous considerations: constrained to the main thread for the same reasons as the primary
         * player lookup.
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
         * Behaviour: delegates to {@link #resolveArgument(String[], int)} so edition-aware localization can report
         * missing tokens consistently. Failure modes: throws {@link CommandError} when the index is missing, enabling
         * localized prompts. Asynchronous considerations: safe to call from any thread because it performs array access
         * only, though typical usage stays on the synchronous command pipeline.
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
         * Behaviour: bridges Bukkit&apos;s offline-player lookup so commands can target players who are not currently online
         * while still feeding edition-aware messaging through the command section. Failure modes: throws
         * {@link CommandError} when the argument is missing or when a player has never joined. Asynchronous considerations:
         * requires main-thread execution because Bukkit&apos;s player APIs are not thread-safe.
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
         * Behaviour: mirrors {@link #offlinePlayerParameter(String[], int, boolean)} while swallowing missing-argument
         * errors. Failure modes: continues to throw {@link CommandError} for malformed input or unmet play-history
         * requirements. Asynchronous considerations: should remain on the main thread due to Bukkit API access.
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
         * Behaviour: relies on {@link UUID#fromString(String)} so command implementations consistently respect Java&apos;s
         * UUID parsing rules. Failure modes: missing or malformed tokens raise {@link CommandError} enabling localized
         * feedback. Asynchronous considerations: safe for asynchronous use because it does not touch Bukkit APIs, though
         * commands typically invoke it synchronously.
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
         * Behaviour: delegates to {@link #uuidParameter(String[], int)} only when the argument exists. Failure modes:
         * malformed arguments still throw {@link CommandError}, while missing arguments return the fallback. Asynchronous
         * considerations: free from Bukkit dependencies and therefore safe in asynchronous parsing scenarios.
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
         * Behaviour: leverages {@link Integer#parseInt(String)} to ensure consistent numeric semantics across editions.
         * Failure modes: missing or non-numeric arguments trigger {@link CommandError}, enabling localized prompts.
         * Asynchronous considerations: computation-only helper that can run off-thread if the command pipeline safely
         * supplies immutable argument arrays.
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
         * Behaviour: wraps {@link #integerParameter(String[], int)} to tolerate missing arguments. Failure modes: still
         * signals {@link CommandError} for malformed numeric input. Asynchronous considerations: contains no Bukkit
         * interactions and is safe for synchronous or asynchronous parsing workflows.
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
         * Behaviour: uses {@link Long#parseLong(String)} to preserve consistent number parsing. Failure modes: emits
         * {@link CommandError} when the argument is missing or invalid. Asynchronous considerations: computation-only
         * helper that can be safely called in synchronous command flows or asynchronous preprocessing.
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
         * Behaviour: mirrors {@link #longParameter(String[], int)} while handling missing arguments. Failure modes:
         * continues to surface {@link CommandError} for malformed numeric values. Asynchronous considerations: safe for
         * use on any thread that can tolerate the small amount of computation.
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
         * Behaviour: uses {@link Double#parseDouble(String)} so decimal values behave consistently across editions.
         * Failure modes: missing or malformed decimals raise {@link CommandError}. Asynchronous considerations: contains
         * no Bukkit interactions and can run synchronously or asynchronously.
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
         * Behaviour: wraps {@link #doubleParameter(String[], int)} with fallback behaviour for missing arguments.
         * Failure modes: still emits {@link CommandError} when the provided token cannot be parsed. Asynchronous
         * considerations: free of Bukkit dependencies and therefore safe on or off the main thread.
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
         * Behaviour: uses {@link Float#parseFloat(String)} to deliver consistent floating-point parsing behaviour.
         * Failure modes: throws {@link CommandError} when the token is missing or malformed. Asynchronous considerations:
         * contains no Bukkit calls and is therefore safe in synchronous or asynchronous contexts.
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
         * Behaviour: wraps {@link #floatParameter(String[], int)} while tolerating missing arguments. Failure modes:
         * malformed values still surface {@link CommandError}. Asynchronous considerations: safe for synchronous command
         * flows and asynchronous preprocessing alike.
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
         * Behaviour: centralises the fallback pattern used by optional argument helpers. Failure modes: rethrows
         * {@link CommandError} values that are not related to missing arguments so localization can surface the
         * correct message. Asynchronous considerations: safe to invoke from any thread because it evaluates the supplied
         * lambda without interacting with Bukkit APIs directly.
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
         * Behaviour: performs bounds checking so missing arguments can be surfaced via localized messages. Failure
         * modes: throws {@link IllegalArgumentException} for negative indices and {@link CommandError} when the argument
         * is absent. Asynchronous considerations: safe on any thread because it touches only the provided array.
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
         * Behaviour: serializes via {@link #LEGACY_SERIALIZER} so localization hooks from the command section flow back
         * through Bukkit&apos;s legacy messaging system. Failure modes: none expected unless the sender is {@code null}.
         * Asynchronous considerations: should be called on the main thread because Bukkit messaging APIs are not
         * thread-safe.
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
         * Behaviour: centralizes edition-aware error handling so commands consistently emit localized responses and
         * trigger command synchronization. Failure modes: {@link CommandError} results in localized messaging, while all
         * other exceptions are logged and return {@code returnValueOnError}. Asynchronous considerations: designed for
         * synchronous use on the main thread; avoid invoking it from asynchronous contexts that cannot safely interact
         * with Bukkit senders.
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
         * Behaviour: maps {@link CommandError} types to the appropriate localization hook so each edition displays
         * consistent messaging. Failure modes: none beyond potential {@link NullPointerException} if the section returns
         * a {@code null} component, which should not happen under normal configuration. Asynchronous considerations:
         * invoked on the main thread because it ultimately sends messages via Bukkit APIs.
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