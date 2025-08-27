module org.lizhao.validator {
    // requires 当前模块需要引入包
    // requires transitive A 依赖本模块的程序会自动依赖A
    // require static lombok 程序编译时，lombok依赖包必须存在，运行时不一定存在。跟maven scope重复？
    // exports 导出当前模块的包，允许所有外部模块引入。注：只能访问公有类、接口、枚举、注解
    // exports A(本模块中的包) to B（外部模块） 只对B模块可引入，其他外部模块无法引入。注：只能访问公有类、接口、枚举、注解
    // 如果是编译时生成代理类，那么open、opens需与exports结合使用。
    // open module org.lizhao.validator 开放整个模块，允许运行时反射访问，与opens不能同时使用
    // opens A 允许包A在运行时，被反射访问
    // opens A to B 允许包A在运行时，被B模块通过反射访问
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires static lombok;

    requires spring.boot.autoconfigure;
    requires spring.boot;
    requires spring.context;
    requires spring.core;
    requires spring.beans;
    requires spring.tx;

    requires jakarta.annotation;

    requires org.apache.poi.ooxml;

    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;

    requires org.slf4j;
    requires org.apache.logging.log4j;

    requires org.mybatis;
    requires com.baomidou.mybatis.plus.core;
    requires com.baomidou.mybatis.plus.annotation;
//    requires com.baomidou.mybatis.plus.spring;
    requires com.baomidou.mybatis.plus.extension;
    requires org.mybatis.spring;
    requires java.sql;
    requires spring.aop;

    requires com.google.gson;
    requires java.desktop;
    requires java.rmi;
    requires java.management.rmi;

    exports org.lizhao.validator;
    exports org.lizhao.validator.javafx.event;
    exports org.lizhao.validator.javafx.controller to javafx.fxml, spring.beans;
    opens org.lizhao.validator.javafx.controller to javafx.fxml, spring.core;

    exports org.lizhao.validator.spring.config;
    opens org.lizhao.validator.spring.config to spring.core;

    exports org.lizhao.validator.spring.config.mybatis;
    opens org.lizhao.validator.spring.config.mybatis to spring.core;

    exports org.lizhao.validator.spring.service;
    opens org.lizhao.validator.spring.service to spring.core;

    exports org.lizhao.validator.spring.mapper;
    opens org.lizhao.validator.spring.mapper;

    exports org.lizhao.validator.spring.entity;
    opens org.lizhao.validator.spring.entity to com.baomidou.mybatis.plus.core;

    exports org.lizhao.validator.spring.model;
    opens org.lizhao.validator.spring.model to com.google.gson;

    // 否则flyway无法获取文件输入流
    opens db.migration.h2;
    // json文件读取
    opens data;
    exports org.lizhao.validator.spring;
    exports org.lizhao.validator.spring.event;

}