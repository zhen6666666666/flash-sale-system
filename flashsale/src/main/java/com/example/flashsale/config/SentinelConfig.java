package com.example.flashsale.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelConfig {

    /**
     * 注册 Sentinel 切面，使 @SentinelResource 注解生效
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    /**
     * 项目启动时初始化限流规则
     */
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 1. 针对获取地址/Token 接口进行限流（QPS 阈值设为 200）
        FlowRule pathRule = new FlowRule();
        pathRule.setResource("getPathRule");
        pathRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        pathRule.setCount(200);
        rules.add(pathRule);

        // 2. 针对核心秒杀下单接口进行限流（QPS 阈值设为 150）
        FlowRule seckillRule = new FlowRule();
        seckillRule.setResource("doSeckillRule");
        seckillRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        seckillRule.setCount(150);
        rules.add(seckillRule);

        FlowRuleManager.loadRules(rules);
    }
}