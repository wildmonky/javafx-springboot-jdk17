package org.lizhao.validator;

import lombok.extern.slf4j.Slf4j;
import org.lizhao.validator.event.JavaFxExitEvent;
import org.lizhao.validator.utils.RMICleanupUtils;
import org.lizhao.validator.utils.SpringUtils;
import org.lizhao.validator.view.JavaFxApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;

import java.rmi.RemoteException;

@Slf4j
@MapperScan("org.lizhao.validator.mapper")
@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
//        // 打印所有JVM参数
//        System.getProperties().forEach((k, v) -> System.out.println(k + ":" + v));
        ConfigurableApplicationContext applicationContext = SpringApplication.run(MainApplication.class, args);
        SpringUtils.setApplicationContext(applicationContext);
        // 向jvm注册Spring Context关闭钩子
//        applicationContext.registerShutdownHook();
        // 启动JavaFx
        JavaFxApplication.launch(JavaFxApplication.class, args);
    }

    /**
     * 关闭Spring
     */
    @EventListener(JavaFxExitEvent.class)
    public void stop(JavaFxExitEvent javaFxExitEvent) throws RemoteException {
        int exitCode = 0;
        ConfigurableApplicationContext applicationContext = SpringUtils.getApplicationContext();
        if (!applicationContext.isClosed()) {
            // 传入的ExitCodeGenerator产生的exitCode不是0，导致SpringBoot内部退出时发布ExitCodeEvent
            exitCode = SpringApplication.exit(applicationContext, () -> 0);
        }
        log.info("Spring Context退出代码：{}", exitCode);
//        Registry registry = LocateRegistry.getRegistry();
        // 清除RMI
        RMICleanupUtils.cleanup();
    }

}