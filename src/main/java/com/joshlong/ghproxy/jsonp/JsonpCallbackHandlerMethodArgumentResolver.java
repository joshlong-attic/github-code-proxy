package com.joshlong.ghproxy.jsonp;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;

import javax.servlet.ServletException;


/**
 * @author Josh Long
 */
public   class JsonpCallbackHandlerMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

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
        JsonpMappingJacksonHttpMessageConverter.registerCallback((String) arg);
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