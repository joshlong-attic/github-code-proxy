package com.joshlong.ghproxy;

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * this annotation marks method arguments as being the parameter
 * from which the current JSONP callback function name should be provided.
 * <p/>
 * So, for the usage:
 * <Code>
 * handleRequest(
 *
 * @author Josh Long
 * @{@link JsonpCallback} String callback
 * )
 * </Code>
 * will have the attribute 'callback' made available as the callback name (and injected into the method invocation).
 * <p/>
 * <code>
 * handleRequest( @{@link JsonpCallback}("cb") String callback )
 * </code>
 * <p/>
 * will tell Spring MVC to lookup the JSONP callback annotation from the current request attribute (<code>"cb"</code>) and then
 * make it available as the callback.
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonpCallback {
    String value() default "callback";
    boolean required() default true;
    String defaultValue() default ValueConstants.DEFAULT_NONE;
}