package com.joshlong.ghproxy;

import org.apache.commons.lang.reflect.FieldUtils;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializationConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletException;
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

    private static String callbackNameAttribute = "callback";
    private static int scope = RequestAttributes.SCOPE_REQUEST;

    private String callbackFunctionName = "callback";
    private Boolean _cachedPrefixJson = null;
    private Charset characterSet = Charset.defaultCharset();

    private List<MediaType> mediaTypesSupportingJsonp = Arrays.asList(
            new MediaType("application", "x-javascript", characterSet),
            new MediaType("application", "x-json", characterSet),
            new MediaType("application", "jsonp", characterSet));


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

        String tlCallbackName = getCallback();

        String cbf = StringUtils.hasText(tlCallbackName) ? tlCallbackName : this.callbackFunctionName;

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


            boolean jsonpCallbackRequired = StringUtils.hasText(cbf);

            if (jsonpCallbackRequired) {
                jsonGenerator.writeRaw(cbf + "(");
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


    public static String getCallback() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Object o = requestAttributes.getAttribute(callbackNameAttribute, scope);
        if (null != o && o instanceof String)
            return (String) o;
        return null;
    }

    public static void registerCallback(String callback) {


        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        attributes.removeAttribute(callbackNameAttribute, scope);

        if (!StringUtils.hasText(callback))
            return;

        attributes.setAttribute(callbackNameAttribute, callback, scope);
        RequestContextHolder.setRequestAttributes(attributes);
    }




    /**
     * @author Josh Long
     */
    public static class JsonpCallbackHandlerMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

        public static class JsonpCallbackNamedValueInfo extends NamedValueInfo {
            private JsonpCallbackNamedValueInfo() {
                super("", false, ValueConstants.DEFAULT_NONE);
            }

            private JsonpCallbackNamedValueInfo(JsonpCallback annotation) {
                super(annotation.value(), annotation.required(), annotation.defaultValue());
            }
        }

        public JsonpCallbackHandlerMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
            super(beanFactory);
        }

        @Override
        protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
            JsonpCallback jsonpCallback = parameter.getParameterAnnotation(JsonpCallback.class);
            if (null != jsonpCallback) {
                return new JsonpCallbackNamedValueInfo(jsonpCallback);
            }
            return new JsonpCallbackNamedValueInfo();
        }

        @Override
        protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
            Object arg = null;
            String[] paramValues = request.getParameterValues(name);
            if (paramValues != null) {
                if (paramValues.length >= 1) {
                    arg = paramValues[0];
                }
            }
            assert arg != null : "we must have resolved a value by this point";
            JsonpAwareMappingJacksonHttpMessageConverter.registerCallback((String) arg);
            return arg;
        }

        @Override
        protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
            throw new MissingServletRequestParameterException(name, parameter.getParameterType().getSimpleName());
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(JsonpCallback.class);
        }
    }


}
