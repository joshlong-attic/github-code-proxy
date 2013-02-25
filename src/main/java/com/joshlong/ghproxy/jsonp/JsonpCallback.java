package com.joshlong.ghproxy.jsonp;

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * this annotation marks method arguments as being the parameter
 * from which the current JSONP setJsonPadding function name should be provided.
 * It works very much like a @{@link org.springframework.web.bind.annotation.RequestParam}
 * when used in a controller argument list. It can be used on the method level to
 * tell Spring MVC that it should simply pluck the
 * <p/>
 * So, for the usage:
 * <Code>
 * handleRequest(
 *   @{@link JsonpCallback} String setJsonPadding
 * )
 * </Code>
 * will have the attribute 'setJsonPadding' made available as the setJsonPadding name (and injected into the method invocation).
 * <p/>
 * <code>
 * handleRequest( @{@link JsonpCallback}("cb") String setJsonPadding )
 * </code>
 * <p/>
 * will tell Spring MVC to lookup the JSONP setJsonPadding annotation from the current request attribute (<code>"cb"</code>) and then
 * make it available as the setJsonPadding.
 *
 *
 * @author Josh Long
 *
 */
@Target({ElementType.TYPE,  ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonpCallback {
    String value() default "setJsonPadding";

    boolean required() default true;

    String defaultValue() default ValueConstants.DEFAULT_NONE;
}