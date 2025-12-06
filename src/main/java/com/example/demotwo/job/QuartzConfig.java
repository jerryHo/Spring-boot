package com.example.demotwo.job;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz Configuration (fixes incompatible type error for NewsCrawlJob)
 */
@Configuration
public class QuartzConfig {

    /**
     * Define Quartz JobDetail (associates NewsCrawlJob with Quartz)
     */
    @Bean
    public JobDetail newsCrawlJobDetail() {
        // FIX: Use JobBuilder to create JobDetail for NewsCrawlJob (implements Quartz Job)
        return JobBuilder.newJob(NewsCrawlJob.class)
                .withIdentity("newsCrawlJob", "crawlGroup")
                .storeDurably() // Keep job even if no trigger is attached
                .build();
    }

    /**
     * Define Trigger (schedule the job to run every 30 minutes)
     */
    @Bean
    public Trigger newsCrawlTrigger() {
        // Schedule: run every 30 minutes (adjust as needed)
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(30)
                .repeatForever();

        // Link trigger to job detail
        return TriggerBuilder.newTrigger()
                .forJob(newsCrawlJobDetail())
                .withIdentity("newsCrawlTrigger", "crawlGroup")
                .withSchedule(scheduleBuilder)
                .build();
    }
}