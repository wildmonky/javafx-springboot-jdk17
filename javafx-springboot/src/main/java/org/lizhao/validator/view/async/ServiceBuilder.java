package org.lizhao.validator.view.async;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * JavaFx Service 构建工具
 */
public class ServiceBuilder<T extends BaseService<S>, S> {

    T service;

    private Runnable onSucceed;

    private Runnable onFailed;

    public ServiceBuilder() {}

    public static <T extends BaseService<S>, S> ServiceBuilder<T, S> builder(Class<T> clazz) {
        ServiceBuilder<T, S> builder = new ServiceBuilder<>();
        try {
            builder.service = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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

    public ServiceBuilder<T, S> onSuccess(Runnable onSucceed) {
        this.onSucceed = onSucceed;
        return this;
    }

    public ServiceBuilder<T, S> onFailed(Runnable onFailed) {
        this.onFailed = onFailed;
        return this;
    }

    public T build() {
        service.onSucceededProperty().addListener((observable, oldValue, newValue) -> onSucceed.run());
        service.onFailedProperty().addListener((observable, oldValue, newValue) -> onFailed.run());
        return service;
    }

}
