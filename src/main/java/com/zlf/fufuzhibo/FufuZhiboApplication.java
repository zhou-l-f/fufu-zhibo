package com.zlf.fufuzhibo;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement //开启注解方式的事务管理
@EnableScheduling //开启定时任务
@MapperScan("com.zlf.fufuzhibo.server.mapper")
@Slf4j
public class FufuZhiboApplication {

    public static void main(String[] args) {
        SpringApplication.run(FufuZhiboApplication.class, args);
    }

    /**
     * 定时任务线程池：默认单线程会让对账、聊天落库等任务串行执行，
     * 某个任务阻塞时会挤占每 2 秒一次的聊天落库
     */
    @Bean
    public TaskScheduler taskScheduler(
            @Value("${fufu.scheduler.pool-size:8}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("fufu-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        return scheduler;
    }

}
