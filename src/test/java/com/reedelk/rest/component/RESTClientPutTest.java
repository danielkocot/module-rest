package com.reedelk.rest.component;

import com.reedelk.rest.TestComponent;
import com.reedelk.runtime.api.flow.FlowContext;
import com.reedelk.runtime.api.message.Message;
import com.reedelk.runtime.api.message.MessageBuilder;
import com.reedelk.runtime.api.script.dynamicvalue.DynamicByteArray;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.reedelk.rest.internal.commons.HttpHeader.CONTENT_TYPE;
import static com.reedelk.rest.internal.commons.RestMethod.PUT;
import static com.reedelk.runtime.api.message.content.MimeType.TEXT_PLAIN;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

class RESTClientPutTest extends RESTClientAbstractTest {

    @Test
    void shouldWithBodyExecuteCorrectlyWhenResponse200() {
        // Given
        String requestBody = "{\"Name\":\"John\"}";
        byte[] requestBodyAsBytes = requestBody.getBytes();
        String expectedResponseBody = "PUT was successful";
        RESTClient client = clientWith(PUT, BASE_URL, PATH, EVALUATE_PAYLOAD_BODY);

        doReturn(Optional.of(requestBodyAsBytes))
                .when(scriptEngine)
                .evaluate(eq(EVALUATE_PAYLOAD_BODY), any(FlowContext.class), any(Message.class));

        doReturn(requestBodyAsBytes)
                .when(converterService)
                .convert(requestBodyAsBytes, byte[].class);

        givenThat(put(urlEqualTo(PATH))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, TEXT_PLAIN.toString())
                        .withStatus(200)
                        .withBody(expectedResponseBody)));

        Message payload = MessageBuilder.get(TestComponent.class).withJson(requestBody).build();

        // Expect
        AssertHttpResponse.isSuccessful(client, payload, flowContext, expectedResponseBody, TEXT_PLAIN);
    }

    @Test
    void shouldWithEmptyBodyExecuteCorrectlyWhenResponse200() {
        // Given
        String expectedResponseBody = "It works";
        RESTClient client = clientWith(PUT, BASE_URL, PATH);

        doReturn(Optional.of(new byte[]{}))
                .when(scriptEngine)
                .evaluate(Mockito.isNull(DynamicByteArray.class), any(FlowContext.class), any(Message.class));

        givenThat(put(urlEqualTo(PATH))
                .withRequestBody(binaryEqualTo(new byte[0]))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, TEXT_PLAIN.toString())
                        .withStatus(200)
                        .withBody(expectedResponseBody)));

        Message emptyPayload = MessageBuilder.get(TestComponent.class).empty().build();

        // Expect
        AssertHttpResponse.isSuccessful(client, emptyPayload, flowContext, expectedResponseBody, TEXT_PLAIN);
    }

    @Test
    void shouldThrowExceptionWhenResponseNot2xx() {
        // Given
        String expectedErrorMessage = "Error exception caused by XYZ";
        RESTClient component = clientWith(PUT, BASE_URL, PATH);

        givenThat(put(urlEqualTo(PATH))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(CONTENT_TYPE, TEXT_PLAIN.toString())
                        .withBody(expectedErrorMessage)));

        Message emptyPayload = MessageBuilder.get(TestComponent.class).empty().build();

        // Expect
        AssertHttpResponse.isNotSuccessful(component, emptyPayload, flowContext, expectedErrorMessage);
    }
}
