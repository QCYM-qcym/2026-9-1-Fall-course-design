package com.shandong.weather;

import com.shandong.weather.config.PortableStopSignal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class PortableStopSignalTests {
    @TempDir Path directory;

    @Test
    void ordinaryApplicationHasNoPortableShutdownWatcher() {
        new ApplicationContextRunner().withUserConfiguration(PortableStopSignal.class)
                .run(context -> assertThat(context).doesNotHaveBean(PortableStopSignal.class));
    }

    @Test
    void onlyTheExactPrivateStopFileClosesTheContextNormally() throws Exception {
        var context = mock(ConfigurableApplicationContext.class);
        var file = directory.resolve("unique-run.stop");
        var signal = new PortableStopSignal(context, file.toString());
        try {
            signal.run(null);
            Files.createFile(directory.resolve("other-run.stop"));
            verify(context, after(350).never()).close();
            Files.createFile(file);
            verify(context, timeout(2000)).close();
        } finally {
            signal.destroy();
        }
    }
}
