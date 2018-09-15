package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;
import info.yangguo.waf.Constant;
import info.yangguo.waf.model.SecurityConfigItem;
import info.yangguo.waf.script.ScriptEntry;
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

public class ScriptSecurity extends Security {
    private static final Logger logger = LoggerFactory.getLogger(ScannerSecurity.class);
    private static ScriptEntry scriptEntry = new ScriptEntry();
    private static Map<String, String> scripts = Maps.newHashMap();

    static {
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
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, List<SecurityConfigItem> items) {
        AtomicBoolean result = new AtomicBoolean(false);
        scripts.entrySet().parallelStream().anyMatch(entry -> {
            Timer itemTimer = Constant.metrics.timer("ScriptSecurity[" + entry.getKey() + "]");
            Timer.Context itemContext = itemTimer.time();
            try {
                scriptEntry.execute(originalRequest, httpObject, result, entry.getValue());
                return result.get();
            } finally {
                itemContext.stop();
            }
        });
        return result.get();
    }

    static class FileListerAdapter extends FileAlterationListenerAdaptor {
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
