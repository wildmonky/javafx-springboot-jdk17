package org.lizhao.validator.javafx.service;

import javafx.scene.control.ProgressIndicator;
import lombok.Getter;
import lombok.Setter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Setter
@Getter
public class DownloadService extends BaseProgressIndicatorService<Void>{

    public DownloadService() {}

    public DownloadService(ProgressIndicator progressIndicator) {
        super(springBean("threadPoolTaskExecutor", ThreadPoolTaskExecutor.class));
        super.setProgressIndicator(progressIndicator);
    }

    @Override
    public void start() {
        super.start();
    }

}
