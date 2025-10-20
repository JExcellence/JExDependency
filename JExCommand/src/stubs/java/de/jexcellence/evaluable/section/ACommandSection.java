package de.jexcellence.evaluable.section;

import de.jexcellence.evaluable.EnumInfo;
import de.jexcellence.evaluable.error.ErrorContext;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Minimal abstraction of the proprietary command section used for configuration
 * backed command metadata.
 */
public abstract class ACommandSection {

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getUsage();

    public abstract List<String> getAliases();

    public PermissionsSection getPermissions() {
        return null;
    }

    public abstract Component getInternalErrorMessage(ErrorContext context);

    public abstract Component getMalformedDoubleMessage(ErrorContext context);

    public abstract Component getMalformedFloatMessage(ErrorContext context);

    public abstract Component getMalformedLongMessage(ErrorContext context);

    public abstract Component getMalformedIntegerMessage(ErrorContext context);

    public abstract Component getMalformedUuidMessage(ErrorContext context);

    public abstract Component getMalformedEnumMessage(ErrorContext context, EnumInfo enumInfo);

    public abstract Component getMissingArgumentMessage(ErrorContext context);

    public abstract Component getNotAPlayerMessage(ErrorContext context);

    public abstract Component getNotAConsoleMessage(ErrorContext context);

    public abstract Component getPlayerUnknownMessage(ErrorContext context);

    public abstract Component getPlayerNotOnlineMessage(ErrorContext context);
}
