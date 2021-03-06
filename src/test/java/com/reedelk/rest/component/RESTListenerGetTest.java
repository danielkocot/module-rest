package com.reedelk.rest.component;

import com.reedelk.rest.TestComponent;
import com.reedelk.rest.component.listener.ErrorResponse;
import com.reedelk.rest.component.listener.Response;
import com.reedelk.runtime.api.commons.ModuleContext;
import com.reedelk.runtime.api.exception.PlatformException;
import com.reedelk.runtime.api.message.Message;
import com.reedelk.runtime.api.message.MessageAttributes;
import com.reedelk.runtime.api.message.MessageBuilder;
import com.reedelk.runtime.api.message.content.MimeType;
import com.reedelk.runtime.api.script.dynamicmap.DynamicStringMap;
import com.reedelk.runtime.api.script.dynamicvalue.DynamicByteArray;
import com.reedelk.runtime.api.script.dynamicvalue.DynamicInteger;
import com.reedelk.runtime.api.script.dynamicvalue.DynamicValue;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

import static com.reedelk.rest.internal.attribute.RESTListenerAttributes.*;
import static com.reedelk.rest.internal.commons.RestMethod.GET;
import static com.reedelk.runtime.api.commons.ImmutableMap.of;
import static com.reedelk.runtime.api.commons.StringUtils.EMPTY;
import static com.reedelk.runtime.api.message.content.MimeType.APPLICATION_JSON;
import static com.reedelk.runtime.api.message.content.MimeType.UNKNOWN;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.http.HttpStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

class RESTListenerGetTest extends RESTListenerAbstractTest {

    private final long moduleId = 10L;
    private final ModuleContext moduleContext = new ModuleContext(moduleId);

    private HttpGet getRequest;

    @BeforeEach
    void setUp() {
        super.setUp();
        getRequest = new HttpGet("http://" + DEFAULT_HOST + ":"+ DEFAULT_PORT);
    }

    @Test
    void shouldReturn200WhenPathIsNull() {
        // Given
        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener((message, callback) -> callback.onResult(context, message));
        listener.onStart();

        // Expect
        assertStatusCodeIs(getRequest, SC_OK);
    }

    @Test
    void shouldReturn200WhenPathIsNullAndRequestEndsWithRoot() {
        // Given
        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener((message, callback) -> callback.onResult(context, message));
        listener.onStart();

        HttpUriRequest request = new HttpGet("http://" + DEFAULT_HOST + ":"+ DEFAULT_PORT + "/");

        // Expect
        assertStatusCodeIs(request, SC_OK);
    }

    @Test
    void shouldReturn200WhenPathIsRoot() {
        // Given
        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener((message, callback) -> callback.onResult(context, message));
        listener.setPath("/");
        listener.onStart();

        // Expect
        assertStatusCodeIs(getRequest, SC_OK);
    }

    @Test
    void shouldReturn200WhenPathIsRootAndRequestEndsWithRoot() {
        // Given
        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener((message, callback) -> callback.onResult(context, message));
        listener.setPath("/");
        listener.onStart();

        HttpUriRequest request = new HttpGet("http://" + DEFAULT_HOST + ":"+ DEFAULT_PORT + "/");

        // Expect
        assertStatusCodeIs(request, SC_OK);
    }

    @Test
    void shouldReturn200WhenPathDefinedWithRegularExpression() {
        // Given
        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener(((message, callback) -> callback.onResult(context, message)));
        listener.setPath("/web/{page:.*}");
        listener.onStart();

        HttpUriRequest request = new HttpGet("http://" + DEFAULT_HOST + ":"+ DEFAULT_PORT + "/web/assets/javascript/home.js");

        // Expect
        assertStatusCodeIs(request, SC_OK);
    }

    @Test
    void shouldReturn200WhenBasePathOnlyIsDefined() {
        // Given
        RESTListenerConfiguration configWithBasePath = new RESTListenerConfiguration();
        configWithBasePath.setHost(DEFAULT_HOST);
        configWithBasePath.setPort(DEFAULT_PORT);
        configWithBasePath.setBasePath("/api/internal");

        RESTListener listener = listenerWith(GET, configWithBasePath);
        listener.addEventListener((message, callback) -> callback.onResult(context, message));
        listener.onStart();

        HttpUriRequest request = new HttpGet("http://" + DEFAULT_HOST + ":"+ DEFAULT_PORT + "/api/internal");

        // Expect
        assertStatusCodeIs(request, SC_OK);
    }

    @Test
    void shouldReturn200WhenBasePathAndPathAreBothDefined() {
        // Given
        RESTListenerConfiguration configWithBasePath = new RESTListenerConfiguration();
        configWithBasePath.setHost(DEFAULT_HOST);
        configWithBasePath.setPort(DEFAULT_PORT);
        configWithBasePath.setBasePath("/api/internal");

        RESTListener listener = listenerWith(GET, configWithBasePath);
        listener.addEventListener((message, callback) -> callback.onResult(context, message));
        listener.setPath("/group/{groupId}");
        listener.onStart();

        HttpUriRequest request = new HttpGet("http://" + DEFAULT_HOST + ":"+ DEFAULT_PORT + "/api/internal/group/managers");

        // Expect
        assertStatusCodeIs(request, SC_OK);
    }

    @Test
    void shouldReturn500WhenSuccessResponseBodyThrowsExceptionWhenEvaluated() {
        // Given
        DynamicByteArray scriptWithSyntaxError = DynamicByteArray.from("#[unknownVariable]", moduleContext);
        Response response = new Response();
        response.setBody(scriptWithSyntaxError);

        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener((message, callback) -> callback.onResult(context, message));
        listener.setResponse(response);
        listener.setPath("/");
        listener.onStart();

        HttpUriRequest request = new HttpGet("http://" + DEFAULT_HOST + ":"+ DEFAULT_PORT + "/");

        DynamicValue nullValue = null;

        doReturn(Optional.empty())
                .when(scriptEngine)
                .evaluate(eq(nullValue), eq(context), any(Message.class));

        doThrow(new PlatformException("Script error"))
                .when(scriptEngine)
                .evaluate(eq(scriptWithSyntaxError), eq(context), any(Message.class));

        // Expect
        assertStatusCodeIs(request, 500);
    }

    @Test
    void shouldReturn404() {
        // Given
        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener((message, callback) -> callback.onResult(context, message));
        listener.onStart();

        HttpUriRequest request = new HttpGet("http://localhost:8881/api");

        // Expect
        assertStatusCodeIs(request, SC_NOT_FOUND);
    }

    @Test
    void shouldReturn500() {
        // Given
        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener((message, callback) -> callback.onError(context, new IllegalStateException("flow error")));
        listener.onStart();

        // Expect
        assertStatusCodeIs(getRequest, SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturn500WithCustomResponseBody() throws IOException {
        // Given
        IllegalStateException thrownException = new IllegalStateException("flow error");

        DynamicStringMap errorResponseHeaders = DynamicStringMap.empty();
        DynamicByteArray errorResponseBody = DynamicByteArray.from("#['custom error']", moduleContext);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setBody(errorResponseBody);
        errorResponse.setHeaders(errorResponseHeaders);

        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.setErrorResponse(errorResponse);
        listener.addEventListener((message, callback) -> callback.onError(context, thrownException));
        listener.onStart();

        doReturn(Optional.of("custom error".getBytes()))
                .when(scriptEngine)
                .evaluate(errorResponseBody, context, thrownException);

        doReturn(new HashMap<>()) // Empty map
                .when(scriptEngine)
                .evaluate(errorResponseHeaders, context, thrownException);

        DynamicValue nullDynamicValue = null;
        doReturn(Optional.empty()) // Empty value
                .when(scriptEngine)
                .evaluate(nullDynamicValue, context, thrownException);

        // Expect
        assertContentIs(getRequest, "custom error");
    }

    @Test
    void shouldReturn504CustomErrorResponseCode() {
        // Given
        IllegalStateException thrownException = new IllegalStateException("flow error");

        DynamicStringMap errorResponseHeaders = DynamicStringMap.empty();
        DynamicInteger errorResponseCode = DynamicInteger.from("#[504]", moduleContext);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setHeaders(errorResponseHeaders);
        errorResponse.setStatus(errorResponseCode);

        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.setErrorResponse(errorResponse);
        listener.addEventListener((message, callback) -> callback.onError(context, thrownException));
        listener.onStart();

        doReturn(Optional.of(504))
                .when(scriptEngine)
                .evaluate(errorResponseCode, context, thrownException);

        doReturn(new HashMap<>()) // Empty map
                .when(scriptEngine)
                .evaluate(errorResponseHeaders, context, thrownException);

        // Expect
        assertStatusCodeIs(getRequest, 504);
    }

    @Test
    void shouldReturnErrorWhenEvaluateErrorResponseBodyThrowsError() throws IOException {
        // Given
        IllegalStateException thrownException = new IllegalStateException("flow error");

        DynamicStringMap errorResponseHeaders = DynamicStringMap.empty();
        DynamicByteArray errorResponseBody = DynamicByteArray.from("#[unknownVariable]", moduleContext);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setHeaders(errorResponseHeaders);
        errorResponse.setBody(errorResponseBody);

        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.setErrorResponse(errorResponse);
        listener.addEventListener((message, callback) -> callback.onError(context, thrownException));
        listener.onStart();

        PlatformException evaluationException = new PlatformException("Could not evaluate script");
        doThrow(evaluationException)
                .when(scriptEngine)
                .evaluate(errorResponseBody, context, thrownException);

        doReturn(new HashMap<>()) // Empty map
                .when(scriptEngine)
                .evaluate(errorResponseHeaders, context, thrownException);

        // Status
        DynamicValue nullDynamicValue = null;
        doReturn(Optional.empty()) // Empty map
                .when(scriptEngine)
                .evaluate(nullDynamicValue, context, thrownException);

        // Expect
        String result = makeCall(getRequest);
        assertThat(result).contains(evaluationException.getMessage());
    }

    @Test
    void shouldReturnErrorWhenEvaluateErrorResponseStatusThrowsError() throws IOException {
        // Given
        IllegalStateException thrownException = new IllegalStateException("flow error");

        DynamicInteger errorResponseStatus = DynamicInteger.from("#[unknownVariable]", moduleContext);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(errorResponseStatus);

        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.setErrorResponse(errorResponse);
        listener.addEventListener((message, callback) -> callback.onError(context, thrownException));
        listener.onStart();

        PlatformException evaluationException = new PlatformException("Could not evaluate script");
        doThrow(evaluationException)
                .when(scriptEngine)
                .evaluate(errorResponseStatus, context, thrownException);

        // Expect
        assertStatusCodeIs(getRequest, 500);
    }

    @Test
    void shouldReturnEmptyResponseContent() {
        // Given
        Message emptyMessage = MessageBuilder.get(TestComponent.class).empty().build();

        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener((message, callback) -> callback.onResult(context, emptyMessage));
        listener.onStart();

        // Expect
        assertContentIs(getRequest, EMPTY);
    }

    @Test
    void shouldReturnErrorResponseContent() {
        // Given
        String errorMessage = "my error";
        DynamicStringMap errorResponseHeaders = DynamicStringMap.empty();
        DynamicByteArray errorResponseBody = DynamicByteArray.from("#[error]", moduleContext);
        IllegalStateException exception = new IllegalStateException(errorMessage);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setHeaders(errorResponseHeaders);
        errorResponse.setBody(errorResponseBody);


        // Evaluation of error message
        doReturn(Optional.of(errorMessage.getBytes()))
                .when(scriptEngine)
                .evaluate(errorResponseBody, context, exception);

        // Status
        DynamicValue nullDynamicValue = null;
        doReturn(Optional.empty()) // Empty map
                .when(scriptEngine)
                .evaluate(nullDynamicValue, context, exception);

        // Headers
        doReturn(new HashMap<>()) // Empty map
                .when(scriptEngine)
                .evaluate(errorResponseHeaders, context, exception);

        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener((message, callback) -> callback.onError(context, exception));
        listener.setErrorResponse(errorResponse);
        listener.onStart();

        // Expect
        assertContentIs(getRequest, errorMessage);
    }

    @Test
    void shouldReturnCorrectContentType() throws IOException {
        // Given
        String json = "{\"name\":\"John\"}";
        Message responseMessage = MessageBuilder.get(TestComponent.class).withJson(json).build();

        DynamicByteArray responseBody = DynamicByteArray.from("#[message.payload()]", moduleContext);
        Response listenerResponse = new Response();
        listenerResponse.setBody(responseBody);

        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.setResponse(listenerResponse);
        listener.addEventListener((message, callback) -> callback.onResult(context, responseMessage));
        listener.onStart();

        // When
        HttpResponse response = HttpClientBuilder.create().build().execute(getRequest);

        // Then
        assertContentTypeIs(response, APPLICATION_JSON.toString());
    }

    @Test
    void shouldSetMimeTypeUnknownForInboundMessage() throws IOException {
        // Given
        RESTListener listener = listenerWith(GET, defaultConfiguration);
        listener.addEventListener((message, callback) -> {
            inboundMessage = message;
            callback.onResult(context, message);
        });
        listener.onStart();

        // When
        HttpClientBuilder.create().build().execute(getRequest);

        // Then
        MimeType inboundMessageMimeType = inboundMessage.content().mimeType();
        assertThat(inboundMessageMimeType).isEqualTo(UNKNOWN);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCorrectlyMapHttpRequestToInboundMessageAttributes() throws IOException {
        // Given
        RESTListenerConfiguration configWithBasePath = new RESTListenerConfiguration();
        configWithBasePath.setHost(DEFAULT_HOST);
        configWithBasePath.setPort(DEFAULT_PORT);
        configWithBasePath.setBasePath("/api/internal");

        RESTListener listener = listenerWith(GET, configWithBasePath);
        listener.setPath("/group/{groupId}");
        listener.addEventListener((message, callback) -> {
            inboundMessage = message;
            callback.onResult(context, message);
        });
        listener.onStart();

        HttpUriRequest request = new HttpGet("http://" + DEFAULT_HOST + ":"+ DEFAULT_PORT + "/api/internal/group/managers?query1=value1&query2=value2&query2=value3");

        // When
        HttpClientBuilder.create().build().execute(request);

        // Then
        MessageAttributes attributes = inboundMessage.attributes();
        assertThat(attributes).isNotNull();

        assertThatAttributeIsEqualTo(attributes, HEADERS, headers -> {
            Map<String, List<String>> headersMap = (Map<String, List<String>>) headers;
            assertThat(headersMap).containsEntry("Accept-Encoding", Arrays.asList("gzip", "deflate"));
            assertThat(headersMap).containsEntry("Connection", singletonList("Keep-Alive"));
            assertThat(headersMap).containsEntry("Host", singletonList("localhost:8881"));
        });
        assertThatAttributeIsEqualTo(attributes, MATCHING_PATH, "/group/{groupId}");
        assertThatAttributeIsEqualTo(attributes, METHOD, "GET");
        assertThatAttributeIsEqualTo(attributes, PATH_PARAMS, new TreeMap<>(of("groupId", "managers")));
        assertThatAttributeIsEqualTo(attributes, QUERY_PARAMS, new TreeMap<>(of("query1", singletonList("value1"), "query2", asList("value2", "value3"))));
        assertThatAttributeIsEqualTo(attributes, QUERY_STRING, "query1=value1&query2=value2&query2=value3");
        assertThatAttributeIsEqualTo(attributes, REQUEST_PATH, "/api/internal/group/managers");
        assertThatAttributeIsEqualTo(attributes, REQUEST_URI, "/api/internal/group/managers?query1=value1&query2=value2&query2=value3");
        assertThatAttributeIsEqualTo(attributes, SCHEME, "http");
        assertThatAttributeIsEqualTo(attributes, VERSION, "HTTP/1.1");

        String remoteAddress = attributes.get(REMOTE_ADDRESS);
        assertThat(remoteAddress).startsWith("/127.0.0.1");
    }

    private void assertThatAttributeIsEqualTo(MessageAttributes attributes, String attributeKey, Serializable expectedValue) {
        assertThatAttributeIsEqualTo(attributes, attributeKey, attributeValue ->
                assertThat(attributeValue).isEqualTo(expectedValue));
    }

    private void assertThatAttributeIsEqualTo(MessageAttributes attributes, String attributeKey, Consumer<Serializable> matcher) {
        Serializable attributeValue = attributes.get(attributeKey);
        matcher.accept(attributeValue);
    }
}
