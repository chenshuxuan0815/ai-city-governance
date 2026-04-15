package com.beijing12345.api.controller;

import com.beijing12345.api.services.DataService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*") 
@RestController
@RequestMapping("/api")
public class DataController {

    private final DataService dataService; // 这里定义的是 dataService

    public DataController(DataService dataService) {
       this.dataService = dataService; // 
    }

    // 今日总量
    @GetMapping("/today-total")
    public Integer getTodayTotal() {
        return dataService.getTodayTotal(); // 这里用 dataService 就对了
    }
    //本月总量
    @GetMapping("/month-total")  
    public Integer getMonthTotal() {  
       return dataService.getMonthTotal(); // 调用你刚写的那个方法  
    }

    // TOP5类别
    @GetMapping("/top5-categories")
    public List<Map<String, Object>> getTop5Categories() {
        return dataService.getTop5Categories();
    }

    // 日趋势
    @GetMapping("/daily-trend")
    public List<Map<String, Object>> getDailyTrend() {
        return dataService.getDailyTrend();
    }
}