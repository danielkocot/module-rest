package com.reedelk.rest.internal.server.mapper;

import com.reedelk.rest.component.RESTListener;
import com.reedelk.runtime.api.commons.StreamUtils;
import com.reedelk.runtime.api.message.MessageBuilder;
import com.reedelk.runtime.api.message.content.MimeType;
import com.reedelk.runtime.api.message.content.TypedPublisher;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.function.BiConsumer;

class HttpRequestContentMapper {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestContentMapper.class);

    private HttpRequestContentMapper() {
    }

    /**
     * Given an http request, it finds the most suitable TypedContent for the request.
     * For example, it checks the mime type of the request and it converts it a String
     * if it is a text based mime type, otherwise it keeps as bytes.
     */
    static MessageBuilder map(HttpRequestWrapper request) {
        MimeType mimeType = request.mimeType();
        // The content stream is fed into a byte sink
        // We retain and we release it when we read the bytes in the sink below.
        Flux<byte[]> byteArrayStream = request.data().retain().handle(asByteArrayStream());

        // The stream might be converted to a string stream if the mime type is for instance
        // application/json or text/plain and so on. Otherwise it is kept binary.
        TypedPublisher<?> typedPublisher =
                StreamUtils.FromByteArray.fromMimeType(byteArrayStream, mimeType);

        return MessageBuilder.get(RESTListener.class)
                .withTypedPublisher(typedPublisher, mimeType);
    }

    private static BiConsumer<ByteBuf, SynchronousSink<byte[]>> asByteArrayStream() {
        return (byteBuffer, sink) -> {
            try {
                byte[] bytes = new byte[byteBuffer.readableBytes()];
                byteBuffer.readBytes(bytes);
                sink.next(bytes);
            } catch (Exception e) {
                logger.error("Error while feeding input sink", e);
                sink.complete();
            } finally {
                // Each stream byte buffer is reference counted,
                // therefore we must release it after reading its bytes.
                byteBuffer.release();
            }
        };
    }
}
