package org.lizhao.validator.spring.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.EventObject;
import java.util.List;

public class SpringUtils {

    public static ConfigurableApplicationContext applicationContext;

    private static final ApplicationHome applicationHome = new ApplicationHome(SpringUtils.class);

    public static void setApplicationContext(ConfigurableApplicationContext applicationContext) {
        SpringUtils.applicationContext = applicationContext;
    }

    public static String getProperty(String key) {
        fillApplicationContext();
        return applicationContext.getEnvironment().getProperty(key);
    }

    public static boolean containsProperty(String key) {
        fillApplicationContext();
        return applicationContext.getEnvironment().containsProperty(key);
    }

    public static ConfigurableApplicationContext getApplicationContext() {
        fillApplicationContext();
        return applicationContext;
    }

    public static void fillApplicationContext() {
        if (applicationContext != null) {
            return;
        }

        LocalApplicationContext localApplicationContext = new LocalApplicationContext();
        localApplicationContext.addDevPropertySource("default", "application.properties");
        applicationContext = localApplicationContext;
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        try {
            return StringUtils.isBlank(name) ? applicationContext.getBean(clazz) : applicationContext.getBean(name, clazz);
        } catch (BeansException e) {
            return applicationContext.getBean(clazz);
        }
    }

    public Resource getResource(String location) throws IOException {
        return applicationContext.getResource(location);
    }

    /**
     * 根据启动类位置判断是否以jar方式启动
     * @return true-以jar包方式启动
     */
    public static boolean isRunAsJar() {
        String protocol = getAppHome();
        return  protocol != null && protocol.contains("jar");
    }

    public static String getAppHome() {
        return applicationHome.getDir().toString();
    }

    public static class LocalApplicationContext extends GenericApplicationContext {

        public void addDevPropertySource(String name, String path) {
            ConfigurableEnvironment environment = getEnvironment();
            PropertiesPropertySourceLoader propertiesPropertySourceLoader = new PropertiesPropertySourceLoader();
            try {
                List<PropertySource<?>> devProperties = propertiesPropertySourceLoader.load(name, new ClassPathResource(path));
                if (devProperties == null || devProperties.isEmpty()) {
                    return;
                }
                environment.getPropertySources().addLast(devProperties.get(0));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void publishEvent(EventObject event) {
        if (applicationContext != null) {
            applicationContext.publishEvent(event);
        }
    }

    public static void shutdown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}
