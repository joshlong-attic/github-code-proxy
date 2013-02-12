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
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;

/**
 * {@link MappingJacksonHttpMessageConverter mapping jackson http message converter}
 * subclass that can also handle JSONP requests. Based largely on the work
 *
 * @author Andy Chan
 * @author Josh Long
 */
public class JsonpAwareMappingJacksonHttpMessageConverter extends MappingJacksonHttpMessageConverter {


    public static interface JsonDecorator {
        void prefix(Object object, JsonGenerator jsonGenerator, HttpOutputMessage httpOutputMessage) throws Exception;

        void suffix(Object o, JsonGenerator jsonGenerator, HttpOutputMessage httpOutputMessage) throws Exception;
    }

    public static class JsonpDecorator implements JsonDecorator {
        private String callbackAttributeName = "callback";

        public void setCallbackAttributeName(String x) {
            this.callbackAttributeName = x;
        }

        protected String deriveCallbackName() {
            Object callback = RequestContextHolder.getRequestAttributes().getAttribute(
                    this.callbackAttributeName, RequestAttributes.SCOPE_REQUEST);
            if (callback != null && callback instanceof String) {
                return (String) callback;
            }
            return null;
        }

        protected boolean requiresCallback() {
            String callback = deriveCallbackName();
            return callback != null && !StringUtils.hasText(callback);
        }

        @Override
        public void prefix(Object object, JsonGenerator jsonGenerator, HttpOutputMessage httpOutputMessage) throws Exception {
            if (!requiresCallback())
                return;

            jsonGenerator.writeRaw(deriveCallbackName() + "(");
            jsonGenerator.flush();

        }

        @Override
        public void suffix(Object o, JsonGenerator jsonGenerator, HttpOutputMessage httpOutputMessage) throws Exception {
            if (!requiresCallback())
                return;
            jsonGenerator.writeRaw(")");
            jsonGenerator.flush();
        }
    }


    private JsonDecorator jsonDecorator = null;
    private Boolean _cachedPrefixJson = null;
    private Charset characterSet = Charset.defaultCharset();

    private List<MediaType> mediaTypesSupportingJsonp = Arrays.asList(
            new MediaType("application", "x-javascript", characterSet),
            new MediaType("application", "x-json", characterSet),
            new MediaType("application", "jsonp", characterSet));

    public void setJsonDecorator(JsonDecorator j) {
        this.jsonDecorator = j;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        Set<MediaType> mts2 = new LinkedHashSet<MediaType>();
        mts2.addAll(this.mediaTypesSupportingJsonp);
        mts2.addAll(super.getSupportedMediaTypes());
        mts2.add(new MediaType("application", "json", this.characterSet));
        return new ArrayList<MediaType>(mts2);
    }


    @Override
    protected void writeInternal(Object object, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

        MediaType contentType = outputMessage.getHeaders().getContentType();


        ///  String cbf = deriveCallbackFunctionName();

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
            // prefix
            if (null != this.jsonDecorator) try {
                jsonDecorator.prefix(object, jsonGenerator, outputMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            this.getObjectMapper().writeValue(jsonGenerator, object);

            if (null != this.jsonDecorator) try {
                jsonDecorator.suffix(object, jsonGenerator, outputMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
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


    // todo remove this but i needed access to the private write-only field in the parent class
    protected boolean getPrefixJson() {
        if (null == _cachedPrefixJson) {
            try {
                Field prefixJsonField = FieldUtils.getField(JsonpAwareMappingJacksonHttpMessageConverter.class, "prefixJson", true);
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
