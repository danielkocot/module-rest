package com.reedelk.rest.component.listener.openapi.v3;

import com.reedelk.openapi.v3.Schema;
import com.reedelk.runtime.api.commons.FileUtils;
import com.reedelk.runtime.api.commons.StreamUtils;
import com.reedelk.runtime.api.resource.ResourceText;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

public class OpenApiSerializableContext {

    private final Map<String, SchemaDataHolder> schemasMap = new HashMap<>();

    public Map<String, Schema> getSchemas() {
        Map<String,Schema> result = new HashMap<>();
        schemasMap.forEach((resourcePath, schemaDataHolder) ->
                result.put(schemaDataHolder.schemaId, new Schema(schemaDataHolder.schemaData)));
        return result;
    }

    /**
     * This is a user defined schema with the given ID.
     */
    public Schema register(String userDefinedId, ResourceText schemaResource) {
        Map<String, Object> schemaDataAsMap = schemaDataFrom(schemaResource);
        SchemaDataHolder schemaDataHolder = new SchemaDataHolder(userDefinedId, schemaDataAsMap);
        schemasMap.put(schemaResource.path(), schemaDataHolder);
        return new Schema(schemaDataAsMap);
    }

    public Schema getSchema(ResourceText schemaResource) {
        // If exists a user defined, then use that ID, otherwise generate one.
        if (schemasMap.containsKey(schemaResource.path())) {
            SchemaDataHolder schemaDataHolder = schemasMap.get(schemaResource.path());
            return new Schema("#/components/schemas/" + schemaDataHolder.schemaId);
        } else {
            Map<String,Object> schemaDataAsMap = schemaDataFrom(schemaResource);
            String schemaGeneratedId = generateSchemaId(schemaDataAsMap, schemaResource);
            SchemaDataHolder schemaDataHolder = new SchemaDataHolder(schemaGeneratedId, schemaDataAsMap);
            schemasMap.put(schemaResource.path(), schemaDataHolder);
            return new Schema("#/components/schemas/" + schemaGeneratedId);
        }
    }

    /**
     * Immediately build the schema inline.
     */
    public Schema getSchema(PredefinedSchema predefinedSchema) {
        Map<String, Object> schemaAsMap = new JSONObject(predefinedSchema.schema()).toMap();
        return new Schema(schemaAsMap);
    }

    private String fromFilePath(String path) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        String fileNameWithoutExtension = FileUtils.removeExtension(fileName);
        // If file named Person.schema.json -> Person
        if (fileNameWithoutExtension.endsWith(".schema")) {
            fileNameWithoutExtension = FileUtils.removeExtension(fileNameWithoutExtension);
        }
        return normalizeNameFrom(fileNameWithoutExtension);
    }

    /**
     * Extract schema id from JSON could be YAML
     */
    private String generateSchemaId(Map<String, Object> schemaDataAsMap, ResourceText schemaResource) {
        if (schemaDataAsMap.containsKey("title")) {
            String titleProperty = (String) schemaDataAsMap.get("title");
            return normalizeNameFrom(titleProperty);
        } else if (schemaDataAsMap.containsKey("name")) {
            String nameProperty = (String) schemaDataAsMap.get("name");
            return normalizeNameFrom(nameProperty);
        } else {
            String path = schemaResource.path();
            return fromFilePath(path);
        }
    }

    private Map<String, Object> schemaDataFrom(ResourceText schemaResource) {
        String schemaDataAsString = StreamUtils.FromString.consume(schemaResource.data());
        Yaml yaml = new Yaml();
        return yaml.load(schemaDataAsString);
    }

    /**
     * A function which removes white spaces, dots, hyphens
     * and other not desired character when the schema name
     * is taken from file name.
     *
     */
    private String normalizeNameFrom(String value) {
        //  checkArgument(StringUtils.isNotBlank(value), "value");
        return value.replaceAll("[^a-zA-Z0-9]", "");
    }

    static class SchemaDataHolder {

        final String schemaId;
        final Map<String,Object> schemaData;

        SchemaDataHolder(String schemaId, Map<String,Object> schemaData) {
            this.schemaId = schemaId;
            this.schemaData = schemaData;
        }
    }
}