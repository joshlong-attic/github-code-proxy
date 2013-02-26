package com.joshlong.ghproxy.jsonp;

/**
 * @author Josh Long
 */
public class SimpleJsonpContext implements JsonpContext {
    @Override
    public void setJsonPadding(String x) {
        JsonpMappingJacksonHttpMessageConverter.registerCallback(x);
    }
}