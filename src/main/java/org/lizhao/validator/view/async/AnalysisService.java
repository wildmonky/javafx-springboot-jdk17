package org.lizhao.validator.view.async;

import lombok.extern.slf4j.Slf4j;
import org.lizhao.validator.model.AlarmInfo;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.List;


@Slf4j
public class AnalysisService extends BaseProgressBarService<List<AlarmInfo>> {

    public AnalysisService() {
        super(BaseProgressBarService.springBean("threadPoolTaskExecutor", ThreadPoolTaskExecutor.class));
    }

}
