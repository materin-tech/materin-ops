package com.materin.tech.ops;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 线上工单反馈与运维系统 - 后端启动类
 */
@SpringBootApplication
@MapperScan("com.materin.tech.ops.**.mapper")
public class MaterinOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaterinOpsApplication.class, args);
    }
}
