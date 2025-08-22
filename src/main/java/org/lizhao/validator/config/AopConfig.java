package org.lizhao.validator.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AopConfig {

    @PostConstruct
    public void forceByteBuddy() {
//        System.setProperty("spring.aop.proxy-target-class", "true");
//        ProxyProcessorSupport.setByteBuddyClassLoader();
    }

}
