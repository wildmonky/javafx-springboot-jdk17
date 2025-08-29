package org.lizhao.validator.javafx.service;

import lombok.extern.slf4j.Slf4j;
import org.lizhao.validator.spring.model.AlarmInfo;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.List;


@Slf4j
public class AnalysisService extends BaseProgressBarService<List<AlarmInfo>> {

    public AnalysisService() {
        super(springBean("threadPoolTaskExecutor", ThreadPoolTaskExecutor.class),
                springBean("threadPoolTaskScheduler", ThreadPoolTaskScheduler.class));
    }

}
