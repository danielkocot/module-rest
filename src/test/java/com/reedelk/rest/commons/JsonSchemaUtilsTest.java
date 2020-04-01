package com.reedelk.rest.commons;

import com.reedelk.runtime.api.resource.ResourceText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;


@ExtendWith(MockitoExtension.class)
class JsonSchemaUtilsTest {

    @Mock
    private ResourceText mockResource;

    @BeforeEach
    void setUp() {
        lenient().doReturn(Mono.just("{}")).when(mockResource).data();
    }

    @Test
    void shouldCorrectlyReturnSchemaNameFromFilePath() {
        // Given
        String filePath = "assets/Person.json";
        doReturn(filePath).when(mockResource).path();

        // When
        String actual = JsonSchemaUtils.findIdFrom(mockResource);

        // Then
        assertThat(actual).isEqualTo("Person");
    }

    @Test
    void shouldCorrectlyReturnSchemaNameFromFilePathWithDotSchema() {
        // Given
        String filePath = "assets/Person.schema.json";
        doReturn(filePath).when(mockResource).path();

        // When
        String actual = JsonSchemaUtils.findIdFrom(mockResource);

        // Then
        assertThat(actual).isEqualTo("Person");
    }

    @Test
    void shouldCorrectlyReturnSchemaNameFromFilePathWhenNoExtension() {
        // Given
        String filePath = "assets/Person";
        doReturn(filePath).when(mockResource).path();

        // When
        String actual = JsonSchemaUtils.findIdFrom(mockResource);

        // Then
        assertThat(actual).isEqualTo("Person");
    }

    @Test
    void shouldCorrectlyReturnSchemaNameFromFilePathWhenFileContainsSpecialChars() {
        // Given
        String filePath = "assets/My-$Person$_Schema";
        doReturn(filePath).when(mockResource).path();

        // When
        String actual = JsonSchemaUtils.findIdFrom(mockResource);

        // Then
        assertThat(actual).isEqualTo("MyPersonSchema");
    }

    @Test
    void shouldCorrectlyReturnSchemaNameFromFilePathWithSpaces() {
        // Given
        String filePath = "assets/My Person Schema.schema.json";
        doReturn(filePath).when(mockResource).path();

        // When
        String actual = JsonSchemaUtils.findIdFrom(mockResource);

        // Then
        assertThat(actual).isEqualTo("MyPersonSchema");
    }

    @Test
    void shouldCorrectlyReturnSchemaNameFromJsonTitleProperty() {
        // Given
        doReturn(Mono.just("{ \"title\":\"My Person Schema\"}")).when(mockResource).data();

        // When
        String actual = JsonSchemaUtils.findIdFrom(mockResource);

        // Then
        assertThat(actual).isEqualTo("MyPersonSchema");
    }

    @Test
    void shouldCorrectlyReturnSchemaNameFromJsonNameProperty() {
        // Given
        doReturn(Mono.just("{ \"name\":\"My Person Schema\"}")).when(mockResource).data();

        // When
        String actual = JsonSchemaUtils.findIdFrom(mockResource);

        // Then
        assertThat(actual).isEqualTo("MyPersonSchema");
    }

    @Test
    void shouldThrowExceptionWhenSchemaIsNull() {
        // When
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> JsonSchemaUtils.findIdFrom(null));

        // Expect
        assertThat(thrown).hasMessage("schema");
    }
}