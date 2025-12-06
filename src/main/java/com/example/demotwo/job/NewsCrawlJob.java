package com.example.demotwo.job;

import com.example.demotwo.util.NewsCrawlera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Quartz Job for RTHK News Crawl (implements Quartz's Job interface - fixes type mismatch)
 */
@Slf4j
@Component
// NewsCrawlJob.java
public class NewsCrawlJob implements Job {
    @Autowired
    private NewsCrawlera newsCrawlera;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 改为实际存在的方法名
        newsCrawlera.extractAllLinks();
    }
}