package org.embulk.deps.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.embulk.spi.unit.LocalFile;

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
                        // This LocalFileJacksonModule is used only with ModelManagerDelegateImpl, whose ObjectMapper is default.
                        // The default ObjectMapper has FAIL_ON_UNKNOWN_PROPERTIES enabled.
                        //
                        // Then, handleUnknownProperty should throw UnrecognizedPropertyException,
                        // which inherits JsonMappingException.
                        ctxt.handleUnknownProperty(jp, this, LocalFile.class, keyName);

                        // Should never reach here. Fall-back to follow up the control flow for the Java compiler.
                        throw UnrecognizedPropertyException.from(jp, LocalFile.class, keyName, null);
                    }

                    t = jp.nextToken();
                    if (t != JsonToken.END_OBJECT) {
                        // This LocalFileJacksonModule is used only with ModelManagerDelegateImpl, whose ObjectMapper is default.
                        // The default ObjectMapper has no DeserializationProblemHandler configured.
                        //
                        // Then, handleUnexpectedToken should throw MismatchedInputException,
                        // which inherits JsonMappingException.
                        ctxt.handleUnexpectedToken(LocalFile.class, t, jp, "Unexpected extra map keys to LocalFile");

                        // Should never reach here. Fall-back to follow up the control flow for the Java compiler.
                        throw MismatchedInputException.from(jp, LocalFile.class, "Unexpected extra map keys to LocalFile");
                    }
                    return result;
                }

                case END_OBJECT:
                case START_ARRAY:
                case END_ARRAY:
                    // This LocalFileJacksonModule is used only with ModelManagerDelegateImpl, whose ObjectMapper is default.
                    // The default ObjectMapper has no DeserializationProblemHandler configured.
                    //
                    // Then, handleUnexpectedToken should throw MismatchedInputException,
                    // which inherits JsonMappingException.
                    ctxt.handleUnexpectedToken(LocalFile.class, t, jp, "Attempted unexpected map or array to LocalFile");

                    // Should never reach here. Fall-back to follow up the control flow for the Java compiler.
                    throw MismatchedInputException.from(jp, LocalFile.class, "Attempted unexpected map or array to LocalFile");

                case VALUE_EMBEDDED_OBJECT: {
                    Object obj = jp.getEmbeddedObject();
                    if (obj == null) {
                        return null;
                    }
                    if (LocalFile.class.isAssignableFrom(obj.getClass())) {
                        return (LocalFile) obj;
                    }

                    // This LocalFileJacksonModule is used only with ModelManagerDelegateImpl, whose ObjectMapper is default.
                    // The default ObjectMapper has no DeserializationProblemHandler configured.
                    //
                    // Then, handleUnexpectedToken should throw MismatchedInputException,
                    // which inherits JsonMappingException.
                    ctxt.handleUnexpectedToken(
                            LocalFile.class, t, jp,
                            "Don't know how to convert embedded Object of type %s into LocalFile", obj.getClass().getName());

                    // Should never reach here. Fall-back to follow up the control flow for the Java compiler.
                    throw MismatchedInputException.from(
                            jp, LocalFile.class,
                            "Don't know how to convert embedded Object of type " + obj.getClass().getName() + " into LocalFile");
                }

                default:
                    return LocalFile.of(jp.getValueAsString());
            }
        }
    }
}
