package org.lizhao.validator.javafx.service;

import javafx.scene.control.ProgressBar;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;


/**
 * <span>通用Service，自动展示进度条。</span><br/>
 * <span>使用方法：</span><br/>
 * <p>
 *     在子类Service的start()或方法中，调用{@link #startProcessBar}传入刷新间隔和获取进度值的方法（注意线程安全）
 * </p>
 */
@Slf4j
@Getter
@Setter
public abstract class BaseProgressBarService<T> extends BaseService<T> {

    private double currentProgress = 0d;

    private ProgressBar progressBar;

    private Future<?> progressBarFlushFuture;

    // 刷新进度条
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    public BaseProgressBarService(ThreadPoolTaskExecutor threadPoolTaskExecutor, ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        super.setExecutor(threadPoolTaskExecutor);
    }

    public void checkProgressBar() {
        if (progressBar == null) {
            throw new IllegalStateException("progressBar is null");
        }
    }

    @Override
    public void succeeded() {
        super.succeeded();
        destroyProgressBar();
    }

    @Override
    protected void failed() {
        super.failed();
        destroyProgressBar();
    }

    /**
     * 开启进度条
     *
     * @param progressSupplier 获取进度值
     * @param intervalMs 刷新间隔
     */
    public void startProcessBar(Supplier<Double> progressSupplier, long intervalMs) {
        initProgressBar();
        AtomicReference<Double> lastProgress = new AtomicReference<>(0d);

        PeriodicTrigger periodicTrigger = new PeriodicTrigger(Duration.ofMillis(intervalMs));
        periodicTrigger.setFixedRate(true);
        progressBarFlushFuture = getThreadPoolTaskScheduler().schedule(() -> {
            if (currentProgress <= 1.0) {
                Double progress = progressSupplier.get();
                if (progress == null) {
                    return;
                }
                currentProgress = progress;
                if (lastProgress.get() != currentProgress) {
                    flushProgressBar(currentProgress);
                    lastProgress.set(currentProgress);
                }

                log.info("BaseProgressBarService ProgressBar 每300ms刷新一次，当前进度:{}", currentProgress);
            }
        }, periodicTrigger);
    }

    /**
     * 初始化进度条，初始化顺序：先设置值，在设置是否可见
     */
    public void initProgressBar() {
        checkProgressBar();
        // 初始进度为0
        progressBar.setProgress(0);
        // 显示进度条
        progressBar.setVisible(true);
        log.debug("进度条初始化完成，初始进度：{}%", progressBar.getProgress() * 100);
    }

    /**
     * 进度条更新
     */
    public void flushProgressBar(double progress) {
        checkProgressBar();
        if (!progressBar.isVisible() || progressBarFlushFuture.isCancelled() || progressBarFlushFuture.isDone()) {
            destroyProgressBar();
            return;
        }

        progressBar.setProgress(progress);
        log.debug("刷新进度条，显示进度为{}%", progress * 100);
    }

    /**
     * 销毁进度条
     */
    public void destroyProgressBar() {
        checkProgressBar();
        // 关闭进度值刷新
        if (!progressBarFlushFuture.isDone()) {
            progressBarFlushFuture.cancel(true);
        }
        // 隐藏进度条
        progressBar.setVisible(false);
        progressBar.setProgress(0);
        log.info("销毁进度条");
    }

}
