package com.reedelk.rest.component;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.reedelk.rest.internal.client.HttpClientFactory;
import com.reedelk.rest.internal.commons.RestMethod;
import com.reedelk.runtime.api.commons.ModuleContext;
import com.reedelk.runtime.api.converter.ConverterService;
import com.reedelk.runtime.api.flow.FlowContext;
import com.reedelk.runtime.api.message.Message;
import com.reedelk.runtime.api.script.ScriptEngineService;
import com.reedelk.runtime.api.script.dynamicmap.DynamicStringMap;
import com.reedelk.runtime.api.script.dynamicvalue.DynamicObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.reedelk.rest.utils.TestTag.INTEGRATION;
import static com.reedelk.runtime.api.commons.ScriptUtils.EVALUATE_PAYLOAD;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@Tag(INTEGRATION)
abstract class RestClient1AbstractTest {

    @Mock
    protected ConverterService converterService;
    @Mock
    protected ScriptEngineService scriptEngine;
    @Mock
    protected FlowContext flowContext;

    static final int PORT = 8181;
    static final String HOST = "localhost";
    static final String PATH = "/v1/resource";
    static final String BASE_URL = "http://" + HOST + ":" + PORT;

    final long moduleId = 10L;
    final ModuleContext moduleContext = new ModuleContext(moduleId);

    private static WireMockServer mockServer;

    DynamicObject EVALUATE_PAYLOAD_BODY = DynamicObject.from(EVALUATE_PAYLOAD, moduleContext);

    private HttpClientFactory clientFactory;


    @BeforeAll
    static void setUpBeforeAll(){
        mockServer = new WireMockServer(PORT);
        mockServer.start();
        configureFor(HOST, PORT);
    }

    @AfterAll
    static void tearDownAfterAll() {
        mockServer.stop();
    }

    @BeforeEach
    void setUp() {
        clientFactory = new HttpClientFactory();
        mockServer.resetAll();

        lenient().doReturn(new byte[0])
                .when(converterService)
                .convert(eq(new byte[0]), eq(byte[].class));
    }

    @AfterEach
    void tearDown() {
        clientFactory.shutdown();
    }

    RestClient1 clientWith(RestMethod method, RestClient1Configuration configuration, String path) {
        RestClient1 restClient = new RestClient1();
        restClient.setConfiguration(configuration);
        restClient.setMethod(method);
        restClient.setPath(path);
        setScriptEngine(restClient);
        setClientFactory(restClient);
        setConverter(restClient);
        restClient.initialize();
        return restClient;
    }

    RestClient1 clientWith(RestMethod method, RestClient1Configuration configuration, String path, DynamicObject body) {
        RestClient1 restClient = new RestClient1();
        restClient.setConfiguration(configuration);
        restClient.setMethod(method);
        restClient.setPath(path);
        restClient.setBody(body);
        setScriptEngine(restClient);
        setClientFactory(restClient);
        setConverter(restClient);
        restClient.initialize();
        return restClient;
    }

    RestClient1 clientWith(RestMethod method, String baseURL, String path) {
        RestClient1 restClient = new RestClient1();
        restClient.setBaseURL(baseURL);
        restClient.setMethod(method);
        restClient.setPath(path);
        setScriptEngine(restClient);
        setClientFactory(restClient);
        setConverter(restClient);
        restClient.initialize();
        return restClient;
    }

    RestClient1 clientWith(RestMethod method, String baseURL, String path, DynamicObject body) {
        RestClient1 restClient = new RestClient1();
        restClient.setBaseURL(baseURL);
        restClient.setMethod(method);
        restClient.setPath(path);
        restClient.setBody(body);
        setScriptEngine(restClient);
        setClientFactory(restClient);
        setConverter(restClient);
        restClient.initialize();
        return restClient;
    }

    RestClient1 clientWith(RestMethod method, String baseURL, String path, DynamicObject body, DynamicStringMap additionalHeaders) {
        RestClient1 restClient = new RestClient1();
        restClient.setHeaders(additionalHeaders);
        restClient.setBaseURL(baseURL);
        restClient.setMethod(method);
        restClient.setPath(path);
        restClient.setBody(body);
        setScriptEngine(restClient);
        setClientFactory(restClient);
        setConverter(restClient);
        restClient.initialize();
        return restClient;
    }

    RestClient1 clientWith(RestMethod method, String baseURL, String path, DynamicStringMap pathParameters, DynamicStringMap queryParameters) {
        RestClient1 restClient = new RestClient1();
        restClient.setBaseURL(baseURL);
        restClient.setMethod(method);
        restClient.setPath(path);
        setScriptEngine(restClient);
        setClientFactory(restClient);
        setConverter(restClient);
        configureRequestAndQueryParams(restClient, pathParameters, queryParameters);
        restClient.initialize();
        return restClient;
    }

    protected void setScriptEngine(RestClient1 restClient) {
        setField(restClient, "scriptEngine", scriptEngine);
    }

    protected void setClientFactory(RestClient1 restClient) {
        setField(restClient, "clientFactory", clientFactory);
    }

    protected void setConverter(RestClient1 restClient) {
        setField(restClient, "converterService", converterService);
    }

    private void setField(RestClient1 client, String fieldName, Object object) {
        try {
            Field field = client.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(client, object);
        } catch (NoSuchFieldException e) {
            fail(String.format("Could not find  field '%s'", fieldName));
        } catch (IllegalAccessException e) {
            fail(String.format("Could not access field '%s'", fieldName));
        }
    }

    private void configureRequestAndQueryParams(RestClient1 client, DynamicStringMap pathParameters, DynamicStringMap queryParameters) {
        if (pathParameters != null && queryParameters != null) {
            client.setPathParameters(pathParameters);
            client.setQueryParameters(queryParameters);
            doReturn(pathParameters)
                    .when(scriptEngine)
                    .evaluate(eq(pathParameters), any(FlowContext.class), any(Message.class));
            doReturn(queryParameters)
                    .when(scriptEngine)
                    .evaluate(eq(queryParameters), any(FlowContext.class), any(Message.class));
        }
        if (pathParameters != null && queryParameters == null) {
            client.setPathParameters(pathParameters);
            doReturn(pathParameters)
                    .when(scriptEngine)
                    .evaluate(eq(pathParameters), any(FlowContext.class), any(Message.class));
        }
        if (pathParameters == null && queryParameters != null) {
            client.setQueryParameters(queryParameters);
            doReturn(queryParameters)
                    .when(scriptEngine)
                    .evaluate(eq(queryParameters), any(FlowContext.class), any(Message.class));
        }
    }
}