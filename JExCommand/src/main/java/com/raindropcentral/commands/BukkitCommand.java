package com.raindropcentral.commands;

import de.jexcellence.evaluable.EnumInfo;
import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import de.jexcellence.evaluable.error.ErrorContext;
import de.jexcellence.evaluable.section.ACommandSection;
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
	
	protected static final List<String>                            EMPTY_STRING_LIST  = Collections.unmodifiableList(new ArrayList<>());
	private static final   Map<Class<? extends Enum<?>>, EnumInfo> enumConstantsCache = Collections.synchronizedMap(new WeakHashMap<>());
	private static final   LegacyComponentSerializer              LEGACY_SERIALIZER  = LegacyComponentSerializer.legacySection();
	protected final        ACommandSection                         commandSection;
	
	protected BukkitCommand(@NotNull ACommandSection commandSection) {
		
		super(
			commandSection.getName(),
			commandSection.getDescription(),
			commandSection.getUsage(),
			commandSection.getAliases()
		);
		this.commandSection = commandSection;
	}
	
	protected abstract void onInvocation(
		CommandSender sender,
		String alias,
		String[] args
	);
	
	protected abstract List<String> onTabCompletion(
		CommandSender sender,
		String alias,
		String[] args
	);
	
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
	
	protected String stringParameter(
		String[] args,
		int argumentIndex
	) {
		
		return this.resolveArgument(
			args,
			argumentIndex
		);
	}
	
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
	 * Sends a Component message to a CommandSender, converting to legacy format for universal compatibility.
	 */
	private void sendComponentMessage(CommandSender sender, Component component) {
		
		// Convert to legacy format for universal Bukkit/Spigot/Paper compatibility
		String legacyMessage = LEGACY_SERIALIZER.serialize(component);
		sender.sendMessage(legacyMessage);
	}
	
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