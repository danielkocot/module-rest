package com.reedelk.rest.component;

import com.reedelk.rest.commons.RestMethod;
import com.reedelk.rest.configuration.StreamingMode;
import com.reedelk.rest.configuration.listener.ErrorResponse;
import com.reedelk.rest.configuration.listener.ListenerConfiguration;
import com.reedelk.rest.configuration.listener.Response;
import com.reedelk.rest.server.HttpRequestHandler;
import com.reedelk.rest.server.Server;
import com.reedelk.rest.server.ServerProvider;
import com.reedelk.runtime.api.annotation.*;
import com.reedelk.runtime.api.component.AbstractInbound;
import com.reedelk.runtime.api.exception.ConfigurationException;
import com.reedelk.runtime.api.exception.ESBException;
import com.reedelk.runtime.api.script.ScriptEngineService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import static com.reedelk.rest.commons.Messages.RestListener.LISTENER_CONFIG_MISSING;
import static com.reedelk.runtime.api.commons.ConfigurationPreconditions.requireNotNull;
import static com.reedelk.runtime.api.commons.ConfigurationPreconditions.requireTrue;
import static com.reedelk.runtime.api.commons.StringUtils.isBlank;
import static org.osgi.service.component.annotations.ServiceScope.PROTOTYPE;

@ESBComponent("REST Listener")
@Component(service = RestListener.class, scope = PROTOTYPE)
public class RestListener extends AbstractInbound {

    @Reference
    private ServerProvider provider;

    @Reference
    private ScriptEngineService scriptEngine;

    @Property("Configuration")
    private ListenerConfiguration configuration;

    @Property("Path")
    @PropertyInfo("The rest path this listener will be bound to. If present must start with '/'. " +
            "The path might contain regexp, e.g: /{name:.*} which would match against anything it is compared to, " +
            "or parameters /{group}/{id}. Path parameters are bound to a key/value map in the inbound message attributes.")
    @Hint("/resource/{id}")
    private String path;

    @Property("Method")
    @Default("GET")
    private RestMethod method;

    @Property("Streaming")
    @Default("AUTO")
    @PropertyInfo("Determines the way the response body is sent to the client. " +
            "When set to Auto and if the size of the payload is not clear, e.g. it is a stream of data, then it uses <b>Transfer-Encoding: chunked</b> " +
            "when sending data back to the client. Otherwise <b>Content-Length</b> encoding with the size of the payload is used. " +
            "When set to Always <b>Transfer-Encoding: chunked</b> is always used, and when none <b>Content-Length</b> is always used instead.")
    private StreamingMode streaming = StreamingMode.AUTO;

    @Property("Response")
    private Response response;

    @Property("Error response")
    private ErrorResponse errorResponse;

    @Override
    public void onStart() {
        requireNotNull(configuration, "RestListener configuration must be defined");
        requireNotNull(configuration.getProtocol(), "RestListener configuration protocol must be defined");
        requireNotNull(method, "RestListener method must be defined");
        requireTrue(isBlank(path) || path.startsWith("/") ,"RestListener path must start with '/'");

        HttpRequestHandler httpRequestHandler = HttpRequestHandler.builder()
                        .inboundEventListener(RestListener.this)
                        .errorResponse(errorResponse)
                        .scriptEngine(scriptEngine)
                        .streaming(streaming)
                        .matchingPath(path)
                        .response(response)
                        .build();

        Server server = provider.getOrCreate(configuration)
                .orElseThrow(() -> new ConfigurationException(LISTENER_CONFIG_MISSING.format()));
        server.addRoute(method, path, httpRequestHandler);
    }

    @Override
    public void onShutdown() {
        provider.get(configuration).ifPresent(server -> {
            server.removeRoute(method, path);
            try {
                provider.release(server);
            } catch (Exception e) {
                throw new ESBException(e);
            }
        });
    }

    public void setConfiguration(ListenerConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setMethod(RestMethod method) {
        this.method = method;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public void setErrorResponse(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }

    public void setStreaming(StreamingMode streaming) {
        this.streaming = streaming;
    }
}