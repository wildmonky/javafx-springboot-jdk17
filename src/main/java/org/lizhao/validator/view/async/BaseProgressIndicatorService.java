package org.lizhao.validator.view.async;

import javafx.scene.control.ProgressIndicator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;


/**
 * <span>通用Service，自动展示进度条 圆形的。</span><br/>
 * 分为两种：有进度值(显示进度值)和无进度值(转圈)
 * <span>使用方法：</span><br/>
 * <p>
 *     在子类Service的start()或方法中，调用{@link #startProcessIndicator}传入刷新间隔和获取进度值的方法（注意线程安全）
 *     无进度条---手动调用{@link #destroyProgressIndicator()}关闭进度条
 *     有进度值---达到1.0是，自动关闭进度条 或 手动调用{@link #destroyProgressIndicator()}关闭进度条
 * </p>
 */
@Slf4j
@Getter
@Setter
public abstract class BaseProgressIndicatorService<T> extends BaseService<T> {

    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private ProgressIndicator progressIndicator;

    private Future<?> progressIndicatorFlushFuture;

    public BaseProgressIndicatorService() {}

    public BaseProgressIndicatorService(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        super.setExecutor(this.threadPoolTaskExecutor);
    }

    public void checkProgressIndicator() {
        if (progressIndicator == null) {
            throw new IllegalStateException("progressIndicator is null");
        }
    }

    /**
     * 开启进度条
     *
     * @param progressSupplier
     * @param interval
     */
    public void startProcessIndicator(Supplier<Double> progressSupplier, long interval) {
        initProgressIndicator();
        AtomicReference<Double> lastProgress = new AtomicReference<>(0d);

        progressIndicatorFlushFuture = getThreadPoolTaskExecutor().submit(() -> {
            double currentProgress = 0;
            while (currentProgress < 1.0) {
                Double progress = progressSupplier.get();
                if (progress == null) {
                    break;
                }
                currentProgress = progress;
                if (lastProgress.get() != currentProgress) {
                    flushProgressIndicator(currentProgress);
                    lastProgress.set(currentProgress);
                }
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            destroyProgressIndicator();
        });
    }

    /**
     * 初始化进度条，初始化顺序：先设置值，在设置是否可见
     */
    public void initProgressIndicator() {
        checkProgressIndicator();
        // 初始进度为0
        progressIndicator.setProgress(0);
        // 显示进度条
        progressIndicator.setVisible(true);
        log.debug("进度条初始化完成，初始进度：{}%", progressIndicator.getProgress() * 100);
    }

    /**
     * 进度条更新
     */
    public void flushProgressIndicator(double progress) {
        checkProgressIndicator();
        if (!progressIndicator.isVisible() || progressIndicatorFlushFuture.isDone() || progressIndicatorFlushFuture.isCancelled()) {
            destroyProgressIndicator();
            return;
        }

        progressIndicator.setProgress(progress);
        log.debug("刷新进度条，显示进度为{}%", progress * 100);
    }

    /**
     * 销毁进度条
     */
    public void destroyProgressIndicator() {
        checkProgressIndicator();
        if (!progressIndicatorFlushFuture.isDone()) {
            progressIndicatorFlushFuture.cancel(true);
        }
        // 隐藏进度条
        progressIndicator.setVisible(false);
        progressIndicator.setProgress(0);
        log.debug("销毁进度条");
    }

}
