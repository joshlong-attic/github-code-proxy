package com.joshlong.ghproxy.jsonp;

/**
 * Simple class that wraps whatever payload you'd
 * normally return with information about the JSONP setJsonPadding
 *
 * @author Josh Long
 */
public class JsonWithPadding<T> {

    private T payload;
    private String callback;

    public JsonWithPadding(String c, T t) {
        this.payload = t;
        this.callback = c;
    }

    public T getPayload() {
        return this.payload;
    }

    public String getCallback() {
        return this.callback;
    }
}
