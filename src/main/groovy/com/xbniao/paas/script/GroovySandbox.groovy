package com.xbniao.paas.script

import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import org.kohsuke.groovy.sandbox.GroovyValueFilter
import org.slf4j.Logger

import java.util.concurrent.atomic.AtomicBoolean

/**
 * This {@link org.kohsuke.groovy.sandbox.GroovyInterceptor} implements a security check.
 *
 * @author Kohsuke Kawaguchi
 */
class GroovySandbox extends GroovyValueFilter {
    @Override
    Object filter(Object o) {
        if (o == null || ALLOWED_TYPES.contains(o.class))
            return o;
        if (o instanceof Script || o instanceof Closure)
            return o; // access to properties of compiled groovy script
        throw new SecurityException("Oops, unexpected type: " + o.class);
    }

    private static final Set<Class> ALLOWED_TYPES = [
            Logger,
            String,
            Integer,
            Boolean,
            Long,
            Integer,
            Map,
            Set,
            HttpRequest,
            HttpObject,
            AtomicBoolean
            // all the primitive types should be OK, but I'm too lazy

            // I'm not adding Class, which rules out all the static method calls
    ] as Set
}
