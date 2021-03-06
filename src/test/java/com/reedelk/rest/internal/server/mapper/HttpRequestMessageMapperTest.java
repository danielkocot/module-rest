package com.reedelk.rest.internal.server.mapper;

import com.reedelk.rest.internal.commons.HttpHeader;
import com.reedelk.runtime.api.message.Message;
import com.reedelk.runtime.api.message.MessageAttributes;
import com.reedelk.runtime.api.message.content.MimeType;
import com.reedelk.runtime.api.message.content.TypedContent;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.server.HttpServerRequest;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import static com.reedelk.rest.internal.attribute.RESTListenerAttributes.*;
import static com.reedelk.runtime.api.message.MessageAttributeKey.CORRELATION_ID;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class HttpRequestMessageMapperTest {

    private final String matchingPath = "/resource/{id}/group/{group}";
    private HttpRequestMessageMapper mapper = new HttpRequestMessageMapper(matchingPath);

    @Test
    void shouldCorrectlyMapMessageAttributesAndContentCorrectly() {
        // Given
        String queryString = "queryParam1=queryValue1&queryParam2=queryValue2";
        String requestPath = "/resource/34/group/user";
        String requestUri = requestPath + "?" + queryString;

        HashMap<String,String> params = new HashMap<>();
        params.put("param1", "value1");

        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add(HttpHeader.CONTENT_TYPE, "application/json");
        httpHeaders.add("X-Correlation-ID", "aabbccdd1");

        HttpServerRequest mockRequest = mock(HttpServerRequest.class);
        doReturn(PUT).when(mockRequest).method();
        doReturn(params).when(mockRequest).params();
        doReturn(requestUri).when(mockRequest).uri();
        doReturn(httpHeaders).when(mockRequest).requestHeaders();
        doReturn(ByteBufFlux.fromInbound(Flux.just("body"))).when(mockRequest).receive();
        doReturn(new InetSocketAddress("localhost", 7070)).when(mockRequest).remoteAddress();
        doReturn(HttpVersion.HTTP_1_0).when(mockRequest).version();
        doReturn(HttpScheme.HTTP.toString()).when(mockRequest).scheme();

        // When
        Message message = mapper.map(mockRequest);

        // Then
        HashMap<String, List<String>> expectedQueryParams = new HashMap<>();
        expectedQueryParams.put("queryParam1", singletonList("queryValue1"));
        expectedQueryParams.put("queryParam2", singletonList("queryValue2"));

        TreeMap<String, List<String>> expectedHeaders = new TreeMap<>(CASE_INSENSITIVE_ORDER);
        expectedHeaders.put(HttpHeader.CONTENT_TYPE, singletonList("application/json"));
        expectedHeaders.put(HttpHeader.X_CORRELATION_ID, singletonList("aabbccdd1"));

        assertThatContainsAttribute(message, REMOTE_ADDRESS, "localhost/127.0.0.1:7070");
        assertThatContainsAttribute(message, MATCHING_PATH, matchingPath);
        assertThatContainsAttribute(message, QUERY_PARAMS, expectedQueryParams);
        assertThatContainsAttribute(message, REQUEST_PATH, requestPath);
        assertThatContainsAttribute(message, REQUEST_URI, requestUri);
        assertThatContainsAttribute(message, QUERY_STRING, queryString);
        assertThatContainsAttribute(message, PATH_PARAMS, params);
        assertThatContainsAttribute(message, VERSION, HttpVersion.HTTP_1_0.text());
        assertThatContainsAttribute(message, HEADERS, expectedHeaders);
        assertThatContainsAttribute(message, SCHEME, HttpScheme.HTTP.toString());
        assertThatContainsAttribute(message, METHOD, PUT.name());
        assertThatContainsAttribute(message, CORRELATION_ID, "aabbccdd1");

        // Check that the content's mime type is correct
        TypedContent<?,?> content = message.content();
        assertThat(content.mimeType()).isEqualTo(MimeType.APPLICATION_JSON);
    }

    private void assertThatContainsAttribute(Message message, String attributeName, Serializable attributeValue) {
        MessageAttributes attributes = message.attributes();
        Object actualAttributeValue = attributes.get(attributeName);
        assertThat(actualAttributeValue).isEqualTo(attributeValue);
    }
}
