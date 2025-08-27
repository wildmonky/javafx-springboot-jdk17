package org.lizhao.validator.javafx.service;

import org.lizhao.validator.JavaFxApplication;
import javafx.concurrent.Worker;
import javafx.event.Event;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * JavaFx Service 构建工具
 */
@Slf4j
public class ServiceBuilder<T extends BaseService<S>, S> {

    T service;

    boolean shutdownOnException = false;

    boolean printStackOnException = false;

    private Consumer<Event> onSucceed;

    private Consumer<Event> onFailed;

    public ServiceBuilder() {}

    public static <T extends BaseService<S>, S> ServiceBuilder<T, S> builder(Class<T> clazz) {
//        ParameterizedType parameterizedType = (ParameterizedType) clazz.getGenericSuperclass();
        ServiceBuilder<T, S> builder = new ServiceBuilder<>();
        try {
            builder.service = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return builder;
    }

    public static <T extends BaseService<S>, S> ServiceBuilder<T, S> buildWith(@NonNull T service) {
        ServiceBuilder<T, S> builder = new ServiceBuilder<>();
        builder.service = service;
        return builder;
    }

    public ServiceBuilder<T, S> task(Supplier<S> task) {
        service.setTask(task);
        return this;
    }

    public ServiceBuilder<T, S> beforeTaskStart(Consumer<BaseService<S>> beforeStartConsumer) {
        service.setBeforeStartConsumer(beforeStartConsumer);
        return this;
    }

    public ServiceBuilder<T, S> afterTaskSucceed(Consumer<BaseService<S>> afterSucceedConsumer) {
        service.setAfterSucceedConsumer(afterSucceedConsumer);
        return this;
    }

    public ServiceBuilder<T, S> taskFinally(Consumer<BaseService<S>> finallyConsumer) {
        service.setFinallyConsumer(finallyConsumer);
        return this;
    }

    /**
     * 如果不想事件继续传播，调用{@link Event#consume()}消费事件
     * @param onSucceedFunc service task成功时逻辑
     * @return ServiceBuilder
     */
    public ServiceBuilder<T, S> onSuccess(Consumer<Event> onSucceedFunc) {
        this.onSucceed = onSucceedFunc;
        return this;
    }

    /**
     * 如果不想事件继续传播，调用{@link Event#consume()}消费事件
     * @param onFailedFunc service task失败时逻辑
     * @return ServiceBuilder
     */
    public ServiceBuilder<T, S> onFailed(Consumer<Event> onFailedFunc) {
        this.onFailed = onFailedFunc;
        return this;
    }

    public ServiceBuilder<T, S> shutdownOnException(boolean shutdownOnException) {
        this.shutdownOnException = shutdownOnException;
        return this;
    }

    public ServiceBuilder<T, S> printStackOnException(boolean printStackOnException) {
        this.printStackOnException = printStackOnException;
        return this;
    }

    public T build() {
        if (onSucceed != null) {
            service.setOnSucceeded(event -> onSucceed.accept(event));
        }
        if (onFailed != null) {
            service.setOnFailed(event -> onFailed.accept(event));
        }
        service.exceptionProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (printStackOnException) {
                    newValue.printStackTrace();
                }
                if (shutdownOnException) {
                    JavaFxApplication.shutdown();
                }
            }
        });
        service.stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.FAILED) {
                log.debug("Service Builder onFailed");
            } else if (newValue == Worker.State.SUCCEEDED) {
                log.debug("Service Builder onSucceed");
            }
        });
        return service;
    }

}
