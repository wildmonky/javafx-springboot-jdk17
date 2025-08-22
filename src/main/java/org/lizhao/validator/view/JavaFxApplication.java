package org.lizhao.validator.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;
import org.lizhao.validator.event.JavaFxExitEvent;
import org.lizhao.validator.utils.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        Platform.exit();
        SpringUtils.getApplicationContext().publishEvent(new JavaFxApplication());
//        System.exit(0);
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
//            stage.setOnCloseRequest(windowEvent -> {
//                SpringUtils.getApplicationContext().close();
//                Platform.exit();
////                System.exit(0);
//            });
            // 记载fxml文件
            FXMLLoader fxmlLoader = new FXMLLoader(JavaFxApplication.class.getResource("/view/main.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1400, 800);
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
}