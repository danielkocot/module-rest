package com.reedelk.rest.component.listener.openapi.v3;

import com.reedelk.openapi.v3.Schema;
import com.reedelk.runtime.api.commons.FileUtils;
import com.reedelk.runtime.api.commons.StreamUtils;
import com.reedelk.runtime.api.resource.ResourceText;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OpenApiSerializableContext {

    private final Map<String, Schema> SCHEMAS_MAP = new HashMap<>();

    public Schema register(ResourceText schema) {
        return null;
    }

    public void setSchema(String schemaId, Map<String,Object> schemaData) {
        // If it is a reference we need to register, otherwise we
        // just serialize the schema as is.
        if (!SCHEMAS_MAP.containsKey(schemaId)) {
            SCHEMAS_MAP.put(schemaId, new Schema(schemaData));
        }
    }

    public Map<String, Schema> getSchemas() {
        return Collections.unmodifiableMap(SCHEMAS_MAP);
    }

    public Schema toSchemaReference(PredefinedSchema predefinedSchema) {
        return new Schema(new JSONObject(predefinedSchema.schema()).toMap());
    }

    public Schema toSchemaReference(ResourceText resourceText, OpenApiSerializableContext context) {
        // Schema Data could be JSON or YAML.
        Map<String,Object> schemaData = new JSONObject(StreamUtils.FromString.consume(resourceText.data())).toMap();

        // Extract schema id from JSON could be YAML ?
        String schemaId;
        JSONObject schemaAsJsonObject = new JSONObject(schemaData);
        if (schemaAsJsonObject.has("title")) {
            String titleProperty = schemaAsJsonObject.getString("title");
            schemaId = normalizeNameFrom(titleProperty);
        } else if (schemaAsJsonObject.has("name")) {
            String nameProperty = schemaAsJsonObject.getString("name");
            schemaId = normalizeNameFrom(nameProperty);
        } else {
            String path = resourceText.path();
            schemaId = fromFilePath(path);
        }

        Schema schema = new Schema(schemaId);
        context.setSchema(schemaId, schemaData);
        return schema;
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
     * A function which removes white spaces, dots, hyphens
     * and other not desired character when the schema name
     * is taken from file name.
     *
     */
    private String normalizeNameFrom(String value) {
        //  checkArgument(StringUtils.isNotBlank(value), "value");
        return value.replaceAll("[^a-zA-Z0-9]", "");
    }

    public Schema register(String schemaId, ResourceText schemaResource) {
        // This is a user defined schema with the given ID.

        return null;
    }

    public Schema getSchema(ResourceText schema) {
        // If exists a user defined, then use that ID, otherwise generate one.
        return null;
    }

    public Schema getSchema(PredefinedSchema predefinedSchema) {
        // Immediately build the schema inline.
        return null;
    }
}
