package info.yangguo.waf.request;

import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;
import info.yangguo.waf.script.ScriptEntry;
import info.yangguo.waf.Constant;
import info.yangguo.waf.model.ItermConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScriptHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ScannerHttpRequestFilter.class);
    private ScriptEntry scriptEntry;
    private Map<String, String> scripts;

    public ScriptHttpRequestFilter() {
        this.scriptEntry = new ScriptEntry();
        this.scripts = Maps.newHashMap();

        try {
            FileAlterationMonitor monitor = new FileAlterationMonitor(1000L);
            IOFileFilter rule = FileFilterUtils.and(
                    FileFilterUtils.fileFileFilter(),
                    FileFilterUtils.suffixFileFilter(".groovy"));
            IOFileFilter filter = FileFilterUtils.or(rule);
            File listenFile = new File(ClassLoader.getSystemResource("").getPath() + "/script");
            FileAlterationObserver observer = new FileAlterationObserver(listenFile, filter);
            FileListerAdapter listener = new FileListerAdapter(scripts);
            observer.addListener(listener);
            monitor.addObserver(observer);
            monitor.start();

            FileUtils.listFiles(listenFile, rule, null)
                    .stream()
                    .forEach(file -> {
                        String scriptName = file.getName();
                        try {
                            String scriptValue = FileUtils.readFileToString(file);
                            scripts.put(scriptName, scriptValue);
                        } catch (Exception e) {
                            logger.warn("script file {} initialization failure", scriptName, e);
                        }
                    });
        } catch (Exception e) {
            logger.warn("script filter initialization failure", e);
        }
    }

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, List<ItermConfig> iterms) {
        AtomicBoolean result = new AtomicBoolean(false);
        scripts.entrySet().parallelStream().anyMatch(entry -> {
            Timer itermTimer = Constant.metrics.timer("ScriptHttpRequestFilter[" + entry.getKey() + "]");
            Timer.Context itermContext = itermTimer.time();
            try {
                scriptEntry.execute(originalRequest, httpObject, result, entry.getValue());
                return result.get();
            } finally {
                itermContext.stop();
            }
        });
        return result.get();
    }

    class FileListerAdapter extends FileAlterationListenerAdaptor {
        private Map<String, String> scripts;

        public FileListerAdapter(Map<String, String> scripts) {
            this.scripts = scripts;
        }

        @Override
        public void onFileChange(File file) {
            if (!file.exists() || !file.canRead()) {
                logger.warn("File[{}] is not exists or can not readable!", file);
            } else {
                try {
                    scripts.put(file.getName(), FileUtils.readFileToString(file));
                } catch (IOException e) {
                    logger.warn("An exception occurs when read file[{}]", file);
                }
                logger.info("File[{}] has been changed.", file);
            }
        }

        @Override
        public void onFileCreate(File file) {
            if (!file.exists() || !file.canRead()) {
                logger.warn("File[{}] is not exists or can not readable!", file);
            } else {
                try {
                    scripts.put(file.getName(), FileUtils.readFileToString(file));
                } catch (IOException e) {
                    logger.warn("An exception occurs when read file[{}]", file);
                }
                logger.info("File[{}] has been created.", file);
            }
        }

        @Override
        public void onFileDelete(File file) {
            scripts.remove(file.getName());
        }
    }
}
