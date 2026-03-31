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

package com.raindropcentral.core.proxy;

import com.raindropcentral.rplatform.proxy.ProxyActionEnvelope;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Binary codec for {@link ProxyActionEnvelope} payloads.
 */
public final class ProxyActionEnvelopeCodec {

    /**
     * Encodes one proxy action envelope into a binary payload.
     *
     * @param envelope action envelope
     * @return binary payload
     */
    public @NotNull byte[] encode(final @NotNull ProxyActionEnvelope envelope) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DataOutputStream outputStream = new DataOutputStream(byteStream)) {
            outputStream.writeInt(envelope.protocolVersion());
            writeUuid(outputStream, envelope.requestId());
            outputStream.writeUTF(envelope.moduleId());
            outputStream.writeUTF(envelope.actionId());
            writeUuid(outputStream, envelope.playerUuid());
            outputStream.writeUTF(envelope.sourceServerId());
            outputStream.writeUTF(envelope.targetServerId());
            outputStream.writeUTF(envelope.actionToken());
            outputStream.writeLong(envelope.createdAtEpochMilli());
            outputStream.writeInt(envelope.payload().size());
            for (final Map.Entry<String, String> entry : envelope.payload().entrySet()) {
                outputStream.writeUTF(entry.getKey());
                outputStream.writeUTF(entry.getValue());
            }
            outputStream.flush();
            return byteStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode proxy action envelope.", exception);
        }
    }

    /**
     * Decodes one proxy action envelope from a binary payload.
     *
     * @param payload binary payload
     * @return decoded action envelope
     */
    public @NotNull ProxyActionEnvelope decode(final @NotNull byte[] payload) {
        try (DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(payload))) {
            final int protocolVersion = inputStream.readInt();
            final UUID requestId = readUuid(inputStream);
            final String moduleId = inputStream.readUTF();
            final String actionId = inputStream.readUTF();
            final UUID playerUuid = readUuid(inputStream);
            final String sourceServerId = inputStream.readUTF();
            final String targetServerId = inputStream.readUTF();
            final String actionToken = inputStream.readUTF();
            final long createdAtEpochMilli = inputStream.readLong();
            final int payloadSize = inputStream.readInt();
            if (payloadSize < 0) {
                throw new IllegalArgumentException("Invalid proxy payload size: " + payloadSize);
            }

            final Map<String, String> envelopePayload = new LinkedHashMap<>();
            for (int index = 0; index < payloadSize; index++) {
                envelopePayload.put(inputStream.readUTF(), inputStream.readUTF());
            }
            return new ProxyActionEnvelope(
                requestId,
                protocolVersion,
                moduleId,
                actionId,
                playerUuid,
                sourceServerId,
                targetServerId,
                actionToken,
                envelopePayload,
                createdAtEpochMilli
            );
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to decode proxy action envelope payload.", exception);
        }
    }

    private static void writeUuid(final @NotNull DataOutputStream outputStream, final @NotNull UUID uuid) throws IOException {
        outputStream.writeLong(uuid.getMostSignificantBits());
        outputStream.writeLong(uuid.getLeastSignificantBits());
    }

    private static @NotNull UUID readUuid(final @NotNull DataInputStream inputStream) throws IOException {
        return new UUID(inputStream.readLong(), inputStream.readLong());
    }
}
