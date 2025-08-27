### 使用前，修改package.bat将jpackage路径修改成当前机器上的路径

```java 
// jlink 精简jre
// 非模块化依赖打包 --add-modules ALL-MODULE-PATH --module-path target
// mybatis-plus的两个依赖包存在相同的包路径(com.baomidou.mybatisplus.extension.toolkit)，导致精简jre失败
// 报错：Modules com.baomidou.mybatis.plus.extension and com.baomidou.mybatis.plus.spring export package com.baomidou.mybatisplus.extension.toolkit to module spring.jdbc
jlink --add-modules ALL-MODULE-PATH --module-path target --output app --launcher app=com.zhuofan.validator.JavaFxApplication