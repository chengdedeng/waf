package com.xbniao.paas.script

import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import org.codehaus.groovy.control.CompilerConfiguration
import org.kohsuke.groovy.sandbox.SandboxTransformer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author guo.yang
 */
class ScriptEntry {
    static Logger logger = LoggerFactory.getLogger(ScriptEntry.class)

    void execute(HttpRequest originalRequest, HttpObject httpObject, AtomicBoolean result, String script) {
        GroovySandbox sandbox = new GroovySandbox()
        def cc = new CompilerConfiguration()
        cc.addCompilationCustomizers(new SandboxTransformer())
        def binding = new Binding()
        binding.originalRequest = originalRequest
        binding.httpObject = httpObject
        binding.result = result
        GroovyShell sh = new GroovyShell(binding, cc)
        sandbox.register()
        try {
            sh.evaluate(script)
        } catch (SecurityException e) {
            logger.warn("Script execute exception\n-----------------------------------------------\n{}\n-----------------------------------------------", script, e)
        }
    }
}
