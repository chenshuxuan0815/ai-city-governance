package com.beijing12345.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("\n========================================");
        System.out.println("🎉 北京12345市民服务热线智能分析平台已启动！");
        System.out.println("🌐 实时数据API访问地址: http://localhost:8080");
        System.out.println("📊 历史数据: http://localhost:8080/api/data/historical");
        System.out.println("📈 模拟数据: http://localhost:8080/api/data/simulated");
        System.out.println("🏆 TOP5类别: http://localhost:8080/api/data/top-categories");
        System.out.println("⏰ 实时数据: http://localhost:8080/api/data/realtime");
        System.out.println("========================================\n");
    }
}