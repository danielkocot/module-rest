package com.reedelk.rest.internal.openapi;

import com.reedelk.rest.component.RESTListenerConfiguration;
import com.reedelk.rest.component.listener.ErrorResponse;
import com.reedelk.rest.component.listener.Response;
import com.reedelk.rest.component.listener.openapi.v3.model.*;
import com.reedelk.rest.internal.commons.HttpHeader;
import com.reedelk.rest.internal.commons.Messages;
import com.reedelk.rest.internal.commons.RestMethod;
import com.reedelk.rest.internal.server.HttpRequestHandler;
import com.reedelk.rest.internal.server.uri.UriTemplateStructure;
import com.reedelk.runtime.api.commons.StringUtils;
import com.reedelk.runtime.api.message.content.MimeType;
import com.reedelk.runtime.api.script.dynamicmap.DynamicStringMap;
import com.reedelk.runtime.api.script.dynamicvalue.DynamicInteger;
import com.reedelk.runtime.openapi.v3.OpenApiSerializableContext;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class OpenApiRequestHandler implements HttpRequestHandler {

    private static final String HTTP_PATH_SEPARATOR = "/";
    private final Formatter formatter;

    protected com.reedelk.runtime.openapi.v3.model.OpenApiObject openAPI;
    protected OpenApiSerializableContext context;

    protected OpenApiRequestHandler(RESTListenerConfiguration configuration, Formatter formatter) {
        this.openAPI = configuration.getOpenApi().map();
        this.openAPI.setBasePath(configuration.getBasePath());
        this.formatter = formatter;
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        String openApiAsJson = serializeOpenAPI();
        response.addHeader(HttpHeader.CONTENT_TYPE, MimeType.APPLICATION_JSON.toString());
        response.addHeader(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        return response.sendByteArray(Mono.just(openApiAsJson.getBytes()));
    }

    public void add(String path, RestMethod httpMethod, Response response, ErrorResponse errorResponse, OperationObject operationObject) {

        Boolean excludeApiPath = Optional.ofNullable(operationObject)
                .flatMap(config -> Optional.ofNullable(config.getExclude()))
                .orElse(false);

        if (operationObject == null) {
            // This is the default behaviour. If the open api configuration is not present,
            // we just add the path to the OpenAPI specification.
            add(path, httpMethod, response, errorResponse);

        } else if (!excludeApiPath) {
            // If the 'exclude' property is false, we don't add the path, otherwise
            // we add the path to the open API specification.
            Map<com.reedelk.runtime.openapi.v3.model.RestMethod,
                    com.reedelk.runtime.openapi.v3.model.OperationObject> operationsByPath = operationsByPathOf(path);

            addDefaultParameters(operationObject, path);
            addDefaultResponse(operationObject, response); // Adding auto generated responses from request
            addDefaultResponse(operationObject, errorResponse); // Adding auto generated error responses from request

            operationsByPath.put(
                    com.reedelk.runtime.openapi.v3.model.RestMethod.valueOf(httpMethod.name()),
                    operationObject.map());
        }
    }

    public void remove(String path, RestMethod restMethod) {
        Map<com.reedelk.runtime.openapi.v3.model.RestMethod, com.reedelk.runtime.openapi.v3.model.OperationObject> operationsByPath = operationsByPathOf(path);
        operationsByPath.remove(com.reedelk.runtime.openapi.v3.model.RestMethod.valueOf(restMethod.name()));
        if (operationsByPath.isEmpty()) {
            String pathToRemove = realPathOf(path);
            com.reedelk.runtime.openapi.v3.model.PathsObject pathsObject = openAPI.getPaths();
            pathsObject.getPaths().remove(pathToRemove);
        }
    }

    private void add(String path, RestMethod httpMethod, Response response, ErrorResponse errorResponse) {
        Map<com.reedelk.runtime.openapi.v3.model.RestMethod, com.reedelk.runtime.openapi.v3.model.OperationObject> operationsByPath = operationsByPathOf(path);
        // Create a default operation object
        OperationObject defaultOperation = new OperationObject();

        addDefaultParameters(defaultOperation, path);   // Adding auto generated parameters from request path
        addDefaultResponse(defaultOperation, response); // Adding auto generated responses from request
        addDefaultResponse(defaultOperation, errorResponse); // Adding auto generated error responses from request

        operationsByPath.put(
                com.reedelk.runtime.openapi.v3.model.RestMethod.valueOf(httpMethod.name()),
                defaultOperation.map());
    }

    private void addDefaultParameters(OperationObject givenOperation, String path) {
        if (StringUtils.isBlank(path)) return;

        // Add default parameters
        UriTemplateStructure templateStructure = UriTemplateStructure.from(path);
        List<String> requestPathParams = templateStructure.getVariableNames();
        List<ParameterObject> parameters = givenOperation.getParameters();

        // We only set default parameters for param names not overridden by the user.
        requestPathParams.forEach(requestParam -> {
            boolean hasBeenUserDefined = parameters.stream().anyMatch(parameterObject ->
                    requestParam.equals(parameterObject.getName()));
            if (!hasBeenUserDefined) {
                ParameterObject parameterObject = new ParameterObject();
                parameterObject.setName(requestParam);
                parameterObject.setRequired(true);
                parameterObject.setIn(ParameterLocation.path);
                parameterObject.setStyle(ParameterStyle.simple);
                parameters.add(parameterObject);
            }
        });
    }

    private void addDefaultResponse(OperationObject givenOperation, ErrorResponse errorResponse) {
        if (errorResponse == null) return;

        DynamicInteger errorResponseStatus = errorResponse.getStatus();
        if (errorResponseStatus == null) return;

        String description = Messages.RestListener.OPEN_API_ERROR_RESPONSE.format();
        DynamicStringMap errorResponseHeaders = errorResponse.getHeaders();
        Map<String, ResponseObject> responses = givenOperation.getResponses();

        addDefaultResponse(errorResponseStatus, responses, errorResponseHeaders, description);
    }

    private void addDefaultResponse(OperationObject givenOperation, Response response) {
        if (response == null) return;

        DynamicInteger responseStatus = response.getStatus();
        if (responseStatus == null) return;

        String description = Messages.RestListener.OPEN_API_SUCCESS_RESPONSE.format();
        DynamicStringMap responseHeaders = response.getHeaders();
        Map<String, ResponseObject> responses = givenOperation.getResponses();

        addDefaultResponse(responseStatus, responses, responseHeaders, description);
    }

    private void addDefaultResponse(DynamicInteger status, Map<String, ResponseObject> responses, DynamicStringMap headers, String description) {
        // If the return status is a script, we cannot infer so we won't add the entry.
        if (status.isScript()) return;

        ofNullable(status.value()).ifPresent(errorResponseStatusValue -> {
            // if response contains Content-Type header we use that one
            String errorResponseStatusAsString = String.valueOf(errorResponseStatusValue);

            if (!responses.containsKey(errorResponseStatusAsString)) {
                // We only put it if the user has not defined his/her
                // own response for the current status code.
                ResponseObject responseObject = new ResponseObject();
                responseObject.setDescription(description);
                responses.put(errorResponseStatusAsString, responseObject);
            }

            ResponseObject responseObject = responses.get(errorResponseStatusAsString);
            applyDefaultHeaders(responseObject, headers);
        });
    }

    private void applyDefaultHeaders(ResponseObject responseObject, DynamicStringMap headers) {
        headers.keySet().forEach(headerName -> {
            Map<String, HeaderObject> headersMap = responseObject.getHeaders();
            if (!headersMap.containsKey(headerName)) {
                HeaderObject headerObject = new HeaderObject();
                headersMap.put(headerName, headerObject);
            }
        });
    }

    String serializeOpenAPI() {
        return this.formatter.format(openAPI, context);
    }

    private Map<com.reedelk.runtime.openapi.v3.model.RestMethod, com.reedelk.runtime.openapi.v3.model.OperationObject> operationsByPathOf(String path) {
        com.reedelk.runtime.openapi.v3.model.PathsObject pathsObject = openAPI.getPaths();
        Map<String, Map<com.reedelk.runtime.openapi.v3.model.RestMethod, com.reedelk.runtime.openapi.v3.model.OperationObject>> paths = pathsObject.getPaths();
        String fixedPath = realPathOf(path);
        if (!paths.containsKey(fixedPath)) {
            paths.put(fixedPath, new HashMap<>());
        }
        return paths.get(fixedPath);
    }

    private String realPathOf(String path) {
        return path == null ? HTTP_PATH_SEPARATOR : path;
    }
}
