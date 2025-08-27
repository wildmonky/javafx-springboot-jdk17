package org.lizhao.validator.spring.config.mybatis;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class DbTransactionManager {

    @Transactional(rollbackFor = Exception.class)
    public <T> T transaction(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
