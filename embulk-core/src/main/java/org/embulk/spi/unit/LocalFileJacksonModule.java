package org.embulk.spi.unit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class LocalFileJacksonModule extends SimpleModule {
    public LocalFileJacksonModule() {
        this.addSerializer(LocalFile.class, new LocalFileSerializer());
        this.addDeserializer(LocalFile.class, new LocalFileDeserializer());
    }

    private static class LocalFileSerializer extends JsonSerializer<LocalFile> {
        @Override
        public void serialize(LocalFile value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeFieldName("base64");
            jgen.writeBinary(value.getContent());
            jgen.writeEndObject();
        }
    }

    private static class LocalFileDeserializer extends JsonDeserializer<LocalFile> {
        @Override
        public LocalFile deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.START_OBJECT) {
                t = jp.nextToken();
            }

            switch (t) {
                case VALUE_NULL:
                    return null;

                case FIELD_NAME: {
                    LocalFile result;

                    String keyName = jp.getCurrentName();
                    if ("content".equals(keyName)) {
                        jp.nextToken();
                        result = LocalFile.ofContent(jp.getValueAsString());
                    } else if ("base64".equals(keyName)) {
                        jp.nextToken();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        jp.readBinaryValue(ctxt.getBase64Variant(), out);
                        result = LocalFile.ofContent(out.toByteArray());
                    } else {
                        throw ctxt.mappingException("Unknown key '" + keyName + "' to deserialize LocalFile");
                    }

                    t = jp.nextToken();
                    if (t != JsonToken.END_OBJECT) {
                        throw ctxt.mappingException("Unexpected extra map keys to LocalFile");
                    }
                    return result;
                }

                case END_OBJECT:
                case START_ARRAY:
                case END_ARRAY:
                    throw ctxt.mappingException("Attempted unexpected map or array to LocalFile");

                case VALUE_EMBEDDED_OBJECT: {
                    Object obj = jp.getEmbeddedObject();
                    if (obj == null) {
                        return null;
                    }
                    if (LocalFile.class.isAssignableFrom(obj.getClass())) {
                        return (LocalFile) obj;
                    }
                    throw ctxt.mappingException("Don't know how to convert embedded Object of type " + obj.getClass().getName() + " into LocalFile");
                }

                default:
                    return LocalFile.of(jp.getValueAsString());
            }
        }
    }
}
