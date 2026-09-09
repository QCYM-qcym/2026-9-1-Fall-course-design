package com.shandong.weather.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/** Opt-in local launcher shutdown, without exposing an HTTP management endpoint. */
@Component
@ConditionalOnProperty(name = "weather.portable.stop-file")
public class PortableStopSignal implements ApplicationRunner, DisposableBean {
    private final ConfigurableApplicationContext context;
    private final Path stopFile;
    private final ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor(task -> {
        var thread = new Thread(task, "portable-stop-signal");
        thread.setDaemon(true);
        return thread;
    });

    public PortableStopSignal(ConfigurableApplicationContext context,
            @Value("${weather.portable.stop-file}") String stopFile) {
        this.context = context;
        this.stopFile = Path.of(stopFile);
    }

    @Override
    public void run(ApplicationArguments args) {
        watcher.scheduleWithFixedDelay(() -> {
            if (Files.isRegularFile(stopFile)) {
                watcher.shutdown();
                context.close();
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        watcher.shutdown();
    }
}
