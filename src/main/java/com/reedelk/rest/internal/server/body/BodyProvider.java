package com.reedelk.rest.internal.server.body;

import com.reedelk.runtime.api.flow.FlowContext;
import com.reedelk.runtime.api.message.Message;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerResponse;

public interface BodyProvider {

    Publisher<byte[]> from(HttpServerResponse response, Message message, FlowContext flowContext);

    Publisher<byte[]> from(HttpServerResponse response, Throwable throwable, FlowContext flowContext);
}
