package org.lizhao.validator;

import org.lizhao.validator.javafx.service.CommonJavaFxService;
import org.lizhao.validator.javafx.service.ServiceBuilder;
import org.lizhao.validator.javafx.controller.MainController;
import org.lizhao.validator.spring.MainApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;
import org.lizhao.validator.javafx.event.JavaFxExitEvent;
import org.lizhao.validator.spring.utils.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.InputStream;;

public class JavaFxApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(JavaFxApplication.class.getName());

    @Override
    public void init() throws Exception {
        super.init();
        // 全局异常捕获
        Thread.setDefaultUncaughtExceptionHandler(this::handlerUncaughtException);
    }

    private void handlerUncaughtException(Thread t, Throwable e) {
        logger.error("JavaFx异常关闭，Uncaught exception", e);
        SpringUtils.getApplicationContext().publishEvent(new JavaFxApplication());
        Platform.exit();
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
//            stage.setOnCloseRequest(windowEvent -> {
//                SpringUtils.getApplicationContext().close();
//                Platform.exit();
////                System.exit(0);
//            });
            // fxml加载fxml初始化
            FXMLLoader fxmlLoader = new FXMLLoader(JavaFxApplication.class.getResource("/view/main.fxml"));
            // 加载fxml文件，显示软件加载页面，以免后台启动时间长，导致用户体验不佳
            Parent load = fxmlLoader.load();
            // 获取fxml文件对应的controller
            MainController controller = fxmlLoader.getController();
            // service 异步启动 SpringBoot
            CommonJavaFxService service = ServiceBuilder
                    .builder(CommonJavaFxService.class)
                    .task(() -> {
                        String[] args = this.getParameters().getRaw().toArray(new String[0]);
                        ConfigurableApplicationContext applicationContext = SpringApplication.run(MainApplication.class, args);
                        SpringUtils.setApplicationContext(applicationContext);
                        return null;
                    })
                    .afterTaskSucceed(ser -> controller.modal.setVisible(false))
                    .shutdownOnException(true)
                    .printStackOnException(true)
                    .onFailed(event -> logger.warn("springboot context 启动失败"))
                    .build();
            service.start();

            Scene scene = new Scene(load, 1300, 700);
            // 应用bootstrap样式
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            stage.setTitle("遥信规范检测工具");
            InputStream inputStream = getClass().getResourceAsStream("/lizi.png");
            if (inputStream != null) {
                stage.getIcons().add(new Image(inputStream));
            }
            stage.setScene(scene);
            stage.show();
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
//        Platform.runLater(() -> SpringUtils.getApplicationContext().publishEvent(new JavaFxExitEvent(this)));
        SpringUtils.getApplicationContext().publishEvent(new JavaFxExitEvent(this));
    }

    public static void shutdown() {
        // FIXME Shutdown phase 1073741823 ends with 1 bean still running after timeout of 30000ms: [applicationTaskExecutor]
        SpringUtils.shutdown();
        Platform.exit();
    }

    public static void main(String[] args) {
        // 启动JavaFx
        JavaFxApplication.launch(args);
    }

}