package de.jexcellence.core.stats.command;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.core.stats.DeliveryMetrics;
import de.jexcellence.core.stats.StatisticsDelivery;
import de.jexcellence.jextranslate.R18nManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * JExCommand 2.0 handlers for {@code /jexstats}. Admin tools for the
 * delivery engine — show queue depth and counters, force an immediate
 * flush.
 */
public final class StatisticsHandler {

    private final StatisticsDelivery delivery;

    public StatisticsHandler(@NotNull StatisticsDelivery delivery) {
        this.delivery = delivery;
    }

    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.ofEntries(
                Map.entry("jexstats", this::onStatus),
                Map.entry("jexstats.status", this::onStatus),
                Map.entry("jexstats.flush", this::onFlush)
        );
    }

    private void onStatus(@NotNull CommandContext ctx) {
        final DeliveryMetrics m = this.delivery.metrics();
        r18n().msg("jexstats.status.line")
                .prefix()
                .with("queue", String.valueOf(this.delivery.queueSize()))
                .with("enqueued", String.valueOf(m.enqueued()))
                .with("batches", String.valueOf(m.batches()))
                .with("delivered", String.valueOf(m.delivered()))
                .with("retries", String.valueOf(m.retries()))
                .with("failures", String.valueOf(m.failures()))
                .with("spooled", String.valueOf(m.spooled()))
                .with("dropped", String.valueOf(m.dropped()))
                .send(ctx.sender());
    }

    private void onFlush(@NotNull CommandContext ctx) {
        final int before = this.delivery.queueSize();
        this.delivery.flushSync();
        r18n().msg("jexstats.flush.done")
                .prefix()
                .with("count", String.valueOf(before))
                .send(ctx.sender());
    }

    private static R18nManager r18n() {
        return R18nManager.getInstance();
    }
}
