package com.joshlong.ghproxy;

import org.apache.commons.lang.reflect.FieldUtils;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializationConfig;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

/**
 * {@link MappingJacksonHttpMessageConverter mapping jackson http message converter}
 * subclass that can also handle JSONP requests. Based largely on the work
 *
 * @author Andy Chan
 * @author Josh Long
 */
public class JsonPAwareMappingJacksonHttpMessageConverter extends MappingJacksonHttpMessageConverter {

    private String callbackFunctionName = "callback";

    private Boolean _cachedPrefixJson = null;

    private List<MediaType> mediaTypesSupportingJsonp = Arrays.asList(
            new MediaType("application", "x-javascript"),
            new MediaType("application", "jsonp"));

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        Set<MediaType> mts2 = new LinkedHashSet<MediaType>();
        mts2.addAll(this.mediaTypesSupportingJsonp);
        mts2.addAll(super.getSupportedMediaTypes());
        mts2.add(new MediaType("application", "json"));
        return new ArrayList<MediaType>(mts2);
    }

    @Override
    protected void writeInternal(Object object, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {


        MediaType contentType = outputMessage.getHeaders().getContentType();
        boolean jsonpCallbackRequired = requiresJsonP(contentType);
        JsonEncoding encoding = getJsonEncoding(contentType);
        JsonGenerator jsonGenerator = getObjectMapper().getJsonFactory().createJsonGenerator(outputMessage.getBody(), encoding);

        if (this.getObjectMapper().getSerializationConfig().isEnabled(SerializationConfig.Feature.INDENT_OUTPUT)) {
            jsonGenerator.useDefaultPrettyPrinter();
        }

        try {
            if (this.getPrefixJson()) {
                jsonGenerator.writeRaw("{} && ");
                jsonGenerator.flush();
            }

            if (jsonpCallbackRequired) {
                jsonGenerator.writeRaw(this.callbackFunctionName + "(");
                jsonGenerator.flush();
            }

            this.getObjectMapper().writeValue(jsonGenerator, object);

            if (jsonpCallbackRequired) {
                jsonGenerator.writeRaw(")");
                jsonGenerator.flush();
            }

        } catch (JsonProcessingException ex) {
            throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }

    protected boolean requiresJsonP(MediaType mediaType) {
        for (MediaType m : this.mediaTypesSupportingJsonp)
            if (m.getType().equalsIgnoreCase(mediaType.getType()) &&
                    m.getSubtype().equalsIgnoreCase(mediaType.getSubtype()))
                return true;
        return false;
    }

    public void setCallbackFunctionName(String c) {
        this.callbackFunctionName = c;
    }

    public String getCallbackFunctionName() {
        return this.callbackFunctionName;
    }

    // todo remove this but i needed access to the private write-only field in the parent class
    protected boolean getPrefixJson() {
        if (null == _cachedPrefixJson) {
            try {
                Field prefixJsonField = FieldUtils.getField(JsonPAwareMappingJacksonHttpMessageConverter.class, "prefixJson", true);
                Object val = prefixJsonField.get(this);
                Assert.isTrue(val instanceof Boolean, "value must be a valid boolean");
                this._cachedPrefixJson = (Boolean) val;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return _cachedPrefixJson;
    }

}
