package org.lizhao.validator.view.async;

import javafx.scene.control.ProgressIndicator;
import lombok.Getter;
import lombok.Setter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Setter
@Getter
public class DownloadService extends BaseProgressIndicatorService<Void>{

    public DownloadService() {}

    public DownloadService(ProgressIndicator progressIndicator) {
        super(BaseProgressBarService.springBean("threadPoolTaskExecutor", ThreadPoolTaskExecutor.class));
        super.setProgressIndicator(progressIndicator);
    }

    @Override
    public void start() {
        super.start();
    }

}
