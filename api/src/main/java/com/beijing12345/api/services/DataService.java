package com.beijing12345.api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class DataService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 模拟前 11 天（4.1-4.11）历史基数（按北京真实占比）
    private final Map<String, Integer> historyBaseMap = Map.ofEntries(
        Map.entry("市场管理", 212000),
        Map.entry("住房", 92500),
        Map.entry("城乡建设", 79500),
        Map.entry("社会秩序", 70500),
        Map.entry("劳动和社会保障", 58500),
        Map.entry("文体市场管理", 48200),
        Map.entry("交通管理", 38500),
        Map.entry("公共服务", 32500),
        Map.entry("教育培训", 29500),
        Map.entry("医疗卫生", 26500),
        Map.entry("其他", 35800)
    );

    // 启动时初始化今天的基础数据（避免前端查空）
    @PostConstruct
    public void initTodayData() {
        try {
            String checkSql = "SELECT COUNT(*) FROM fact_complaints_minute WHERE DATE(event_time) = CURDATE()";
            Long count = jdbcTemplate.queryForObject(checkSql, Long.class);
            if (count != null && count > 0) return;

            String[] cats = historyBaseMap.keySet().toArray(new String[0]);
            int currentHour = LocalDateTime.now().getHour();
            for (int hour = 0; hour <= currentHour; hour++) {
                for (int i = 0; i < 35 + ThreadLocalRandom.current().nextInt(40); i++) {
                    String time = String.format("%s %02d:%02d:00", LocalDate.now(), hour, ThreadLocalRandom.current().nextInt(60));
                    String cat = cats[ThreadLocalRandom.current().nextInt(cats.length)];
                    String sql = "INSERT INTO fact_complaints_minute (event_time, category_id, category_name, request_count) VALUES (?, ?, ?, ?)";
                    jdbcTemplate.update(sql, time, ThreadLocalRandom.current().nextInt(25) + 1, cat, ThreadLocalRandom.current().nextInt(85));
                }
            }
            System.out.println("✅ 今日初始数据生成完毕（已生成 " + (currentHour + 1) * 55 + " 条）");
        } catch (Exception e) {
            System.err.println("初始化数据出错（可能已有数据）: " + e.getMessage());
        }
    }

    // 本月总量 = 历史基数（约 66 万）+ 今日实时
    public Integer getMonthTotal() {
        int historySum = historyBaseMap.values().stream().mapToInt(v -> v).sum();
        Integer todayTotal = getTodayTotal();
        return historySum + (todayTotal != null ? todayTotal : 0);
    }
    
    // 今日总的诉求量
    public Integer getTodayTotal() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        
        String today = LocalDate.now().toString();
        String endTime = String.format("%s %02d:%02d:00", today, hour, minute);
        String startTime = today + " 00:00:00";

        // 使用参数化查询
        String sql = "SELECT COALESCE(SUM(request_count), 0) " +
                     "FROM fact_complaints_minute " +
                     "WHERE event_time >= ? " +
                     "  AND event_time <= ?";
        
        System.out.println("SQL: " + sql);
        System.out.println("参数: startTime=" + startTime + ", endTime=" + endTime);
        
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, new Object[]{startTime, endTime});
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SQL 执行失败: " + e.getMessage());
            return 0;
        }
    }
    
    public List<Map<String, Object>> getTop5Categories() {
        // 1. 查今日分类统计
        String sql = "SELECT category_name, SUM(request_count) as today_count " +
                     "FROM fact_complaints_minute WHERE DATE(event_time) = CURDATE() " +
                     "GROUP BY category_name";
        List<Map<String, Object>> todayList = jdbcTemplate.queryForList(sql);

        // 2. 合并历史 + 今日
        Map<String, Integer> totalMap = new HashMap<>(historyBaseMap);
        for (Map<String, Object> item : todayList) {
            String name = (String) item.get("category_name");
            if (name != null) {
                Object countObj = item.get("today_count");
                int todayCount = 0;
                if (countObj != null) {
                    if (countObj instanceof Number) {
                        todayCount = ((Number) countObj).intValue();
                    } else {
                        try {
                            todayCount = Integer.parseInt(countObj.toString());
                        } catch (NumberFormatException e) {
                            todayCount = 0;
                        }
                    }
                }
                totalMap.put(name, totalMap.getOrDefault(name, 0) + todayCount);
            }
        }

        // 3. 计算本月总量
        int monthTotal = getMonthTotal();

        // 4. 排序取 TOP5
        List<Map<String, Object>> top5 = totalMap.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(5)
            .map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("category_name", e.getKey());
                map.put("total", e.getValue());
                map.put("percentage", monthTotal > 0 ? String.format("%.1f", e.getValue() * 100.0 / monthTotal) : "0.0");
                return map;
            })
            .collect(Collectors.toList());

        // 5. 如果不足 5 条，手动补 0（确保前端永远拿到 5 条数据）
        while (top5.size() < 5) {
            Map<String, Object> map = new HashMap<>();
            map.put("category_name", "其他");
            map.put("total", 0);
            map.put("percentage", "0.0");
            top5.add(map);
        }

        return top5;
    }

    // 今日趋势（按小时，未到时间不显示）
    public List<Map<String, Object>> getDailyTrend() {
        // 查 hourly_summary 表，直接拿预计算的每小时总量
        String sql = "SELECT hour, total FROM hourly_summary ORDER BY hour";
        List<Map<String, Object>> dbData = jdbcTemplate.queryForList(sql);
        return dbData;
    }

    // 自动模拟来电（每 8 秒一次）
    @Scheduled(fixedRate = 8500)
    public void autoSimulateRequest() {
        String[] cats = historyBaseMap.keySet().toArray(new String[0]);
        String cat = cats[ThreadLocalRandom.current().nextInt(cats.length)];
        String sql = "INSERT INTO fact_complaints_minute (event_time, category_id, category_name, request_count) VALUES (NOW(), ?, ?, ?)";
        jdbcTemplate.update(sql, ThreadLocalRandom.current().nextInt(25) + 1, cat, ThreadLocalRandom.current().nextInt(85));
    }
}