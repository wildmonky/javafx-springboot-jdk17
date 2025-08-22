package org.lizhao.validator.view.async;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lizhao.validator.utils.SpringUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 在failed中打印{@link javafx.concurrent.Task}的异常堆栈
 *
 * @see ServiceBuilder
 */
@Slf4j
@Getter
@Setter
public abstract class BaseService<T> extends Service<T> {

    private Supplier<T> task;

    /**
     * 准备动作，如：禁用按钮
     */
    private Consumer<BaseService<T>> beforeStartConsumer;

    /**
     * 处理成功后操作，如：展示数据
     */
    private Consumer<BaseService<T>> afterSucceedConsumer;

    /**
     * 复原 {@link #beforeStart} 中的操作，如：解禁按钮
     */
    private Consumer<BaseService<T>> finallyConsumer;

    /**
     * 填充属性，从Spring中获取实例注入
     */
    public static <K> K springBean(String name, Class<K> clazz) {
        return SpringUtils.getBean(name, clazz);
    }

    @Override
    protected Task<T> createTask() {
        return new Task<>() {
            @Override
            protected T call() throws Exception {
                return task.get();
            }
        };
    }

    @Override
    public void start() {
        if (beforeStartConsumer != null) {
            beforeStartConsumer.accept(this);
        }
        beforeStart();
        super.start();
    }

    @Override
    public void succeeded() {
        super.succeeded();
        if (afterSucceedConsumer != null) {
            afterSucceedConsumer.accept(this);
        }
        afterSucceed();
        if (finallyConsumer != null) {
            finallyConsumer.accept(this);
        }
    }

    /**
     * 打印异常
     */
    @Override
    protected void failed() {
        super.failed();
        super.getException().printStackTrace();
        if (finallyConsumer != null) {
            finallyConsumer.accept(this);
        }
    }

    /**
     * 准备动作，如：禁用按钮
     */
    protected void beforeStart() {}

    /**
     * 复原 {@link #beforeStart} 中的操作
     */
    protected void afterSucceed() {}
}
