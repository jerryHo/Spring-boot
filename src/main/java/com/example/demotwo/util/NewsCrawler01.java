package com.example.demotwo.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 香港01新闻爬虫工具类（适配 data-testid="content-card" 结构）
 * 功能：提取指定结构的新闻（标题+类别+发布时间+URL+图片URL）
 */
@Slf4j
@Component
public class NewsCrawler01 {

    // 香港01目标页面（替换为你要爬取的列表页）
    private static final String TARGET_URL = "https://www.hk01.com/latest";
    // 最大重试次数
    private static final int MAX_RETRIES = 3;
    // 重试间隔（毫秒）
    private static final long RETRY_DELAY_MS = 2000;
    // 匹配HTTP/HTTPS链接的正则
    private static final Pattern HTTP_HTTPS_PATTERN = Pattern.compile("https?:\\/\\/[^\\s\"']+", Pattern.UNICODE_CASE);
    // 核心选择器：匹配 data-testid="content-card" 的新闻容器
    private static final String NEWS_ITEM_SELECTOR = "div[data-testid='content-card']";
    // 滚动加载次数
    private static final int SCROLL_TIMES = 3;
    // 每次滚动后的等待时间（毫秒）
    private static final long SCROLL_WAIT_MS = 1500;

    /**
     * 新闻实体类（精准匹配需求：标题/类别/发布时间/URL/图片URL）
     */
    @Data
    public static class NewsItem {
        // 新闻标题
        private String title;
        // 新闻类别（如：大國小事）
        private String category;
        // 发布时间（原始datetime：2025-12-06T09:47:52.000Z）
        private String publishTimeDatetime;
        // 发布时间（格式化：2025-12-06 09:47）
        private String publishTimeFormatted;
        // 新闻完整URL
        private String newsUrl;
        // 新闻图片URL
        private String imageUrl;
        // 是否为精选
        private boolean isFeatured;
    }

    // ========================= 核心功能：提取content-card结构的新闻 =========================
    /**
     * 提取完整新闻列表（适配 data-testid="content-card" 结构）
     * @return 结构化新闻列表
     */
    public List<NewsItem> extractCompleteNewsList() {
        List<NewsItem> newsList = new ArrayList<>();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            WebDriver driver = null;
            try {
                log.info("开始提取香港01新闻（第{}次尝试），目标URL：{}", retryCount + 1, TARGET_URL);

                // 配置Chrome浏览器（反爬优化）
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless=new");          // 无头模式
                options.addArguments("--no-sandbox");            // 解决Linux权限问题
                options.addArguments("--disable-dev-shm-usage"); // 解决内存不足
                options.addArguments("--disable-blink-features=AutomationControlled"); // 禁用自动化标识
                options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");
                options.addArguments("--disable-gpu");           // 禁用GPU加速
                options.addArguments("--lang=zh-HK");            // 设置繁体中文环境

                // 初始化WebDriver
                driver = new ChromeDriver(options);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));
                driver.get(TARGET_URL);

                // 滚动加载更多新闻（触发动态加载）
                Actions actions = new Actions(driver);
                for (int i = 0; i < SCROLL_TIMES; i++) {
                    actions.scrollByAmount(0, driver.manage().window().getSize().getHeight() * 2).perform();
                    log.debug("第{}次滚动加载，等待{}ms", i + 1, SCROLL_WAIT_MS);
                    Thread.sleep(SCROLL_WAIT_MS);
                }

                // 解析页面（UTF-8编码处理）
                String pageSource = driver.getPageSource();
                byte[] utf8Bytes = pageSource.getBytes(StandardCharsets.UTF_8);
                String utf8PageSource = new String(utf8Bytes, StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(utf8PageSource, TARGET_URL, Parser.htmlParser());

                // 定位所有 content-card 新闻容器
                Elements newsElements = doc.select(NEWS_ITEM_SELECTOR);
                log.info("共匹配到 {} 个 content-card 新闻容器", newsElements.size());

                // 遍历提取每条新闻的核心字段
                for (Element card : newsElements) {
                    NewsItem news = new NewsItem();

                    // 1. 提取新闻标题（data-testid="content-card-title" 的a标签）
                    Element titleA = card.selectFirst("a[data-testid='content-card-title'], .card-title");
                    if (titleA != null) {
                        String title = titleA.text().trim();
                        news.setTitle(title);
                        // 提取新闻URL（拼接绝对路径）
                        String relativeUrl = titleA.attr("href").trim();
                        if (!relativeUrl.startsWith("http")) {
                            news.setNewsUrl("https://www.hk01.com" + relativeUrl);
                        } else {
                            news.setNewsUrl(relativeUrl);
                        }
                    }

                    // 2. 提取新闻类别（card-category 内的文本）
                    Element categoryDiv = card.selectFirst(".card-category div");
                    if (categoryDiv != null) {
                        String category = categoryDiv.text().trim();
                        news.setCategory(category);
                    }

                    // 3. 提取发布时间（datetime属性 + 格式化）
                    Element timeElem = card.selectFirst("time[data-testid='content-card-time']");
                    if (timeElem != null) {
                        // 原始datetime属性
                        String datetime = timeElem.attr("datetime").trim();
                        news.setPublishTimeDatetime(datetime);
                        // 格式化时间（2025-12-06T09:47:52.000Z → 2025-12-06 09:47）
                        if (!datetime.isEmpty()) {
                            String formattedTime = formatDatetime(datetime);
                            news.setPublishTimeFormatted(formattedTime);
                        }
                    }

                    // 4. 提取图片URL（content-card-thumbnail 内的img标签src）
                    Element imgElem = card.selectFirst("div[data-testid='content-card-thumbnail'] img");
                    if (imgElem != null) {
                        String imageUrl = imgElem.attr("src").trim();
                        news.setImageUrl(imageUrl);
                    }

                    // 5. 判断是否为精选（card-info__item 包含"精選"）
                    Element featuredElem = card.selectFirst(".card-info__item:contains(精選)");
                    news.setFeatured(featuredElem != null);

                    // 过滤有效新闻（标题+URL非空）
                    if (news.getTitle() != null && !news.getTitle().isEmpty()
                            && news.getNewsUrl() != null && !news.getNewsUrl().isEmpty()) {
                        newsList.add(news);
                        log.debug("提取到新闻：\n" +
                                        "  标题：{}\n" +
                                        "  类别：{}\n" +
                                        "  时间：{}\n" +
                                        "  URL：{}\n" +
                                        "  图片：{}\n" +
                                        "  精选：{}",
                                news.getTitle(), news.getCategory(), news.getPublishTimeFormatted(),
                                news.getNewsUrl(), news.getImageUrl(), news.isFeatured());
                    }
                }

                // 去重（按URL去重）
                newsList = newsList.stream()
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(NewsItem::getNewsUrl, item -> item, (o1, o2) -> o1),
                                map -> new ArrayList<>(map.values())
                        ));

                // 日志输出结果
                log.info("=== 香港01新闻提取完成 | 有效新闻数：{} ===", newsList.size());
                for (int i = 0; i < Math.min(newsList.size(), 10); i++) {
                    NewsItem item = newsList.get(i);
                    log.info("新闻 {}: [{}] {} (类别：{}, 精选：{})",
                            i + 1, item.getPublishTimeFormatted(), item.getTitle(),
                            item.getCategory(), item.isFeatured());
                }

                return newsList;

            } catch (Exception e) {
                retryCount++;
                log.error("第{}次提取新闻失败：{}", retryCount, e.getMessage(), e);
                if (retryCount >= MAX_RETRIES) {
                    log.error("所有重试次数已用尽，返回空列表", e);
                    return new ArrayList<>();
                }
                // 重试等待
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("重试等待被中断", ie);
                    return new ArrayList<>();
                }
            } finally {
                // 关闭浏览器
                if (driver != null) {
                    driver.quit();
                }
            }
        }
        return new ArrayList<>();
    }

    // ========================= 辅助方法 =========================
    /**
     * 格式化ISO时间（2025-12-06T09:47:52.000Z → 2025-12-06 09:47）
     */
    private String formatDatetime(String isoDatetime) {
        try {
            // 解析ISO时间
            DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            LocalDate date = LocalDate.parse(isoDatetime, isoFormatter);
            // 提取时分
            String timePart = isoDatetime.substring(11, 16);
            // 拼接格式化时间
            return date.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + timePart;
        } catch (Exception e) {
            // 解析失败返回原始值
            return isoDatetime;
        }
    }

    // ========================= 保留原有功能（可选） =========================
    /**
     * 提取页面中所有HTTP/HTTPS链接（去重）
     */
    public List<String> extractAllLinks() {
        Set<String> allLinks = new HashSet<>();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            WebDriver driver = null;
            try {
                log.info("开始提取香港01所有链接（第{}次尝试）", retryCount + 1);

                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless=new");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-blink-features=AutomationControlled");
                options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");

                driver = new ChromeDriver(options);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
                driver.get(TARGET_URL);

                // 滚动加载
                Actions actions = new Actions(driver);
                for (int i = 0; i < 2; i++) {
                    actions.scrollByAmount(0, driver.manage().window().getSize().getHeight() * 2).perform();
                    Thread.sleep(SCROLL_WAIT_MS);
                }

                // 解析页面
                String pageSource = new String(driver.getPageSource().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(pageSource, TARGET_URL);

                // 提取所有链接
                Elements allElements = doc.getAllElements();
                for (Element element : allElements) {
                    element.attributes().forEach(attribute -> {
                        String attrValue = attribute.getValue().trim();
                        Matcher matcher = HTTP_HTTPS_PATTERN.matcher(attrValue);
                        while (matcher.find()) {
                            String link = matcher.group().trim();
                            if (!link.contains("ads.") && !link.endsWith(".jpg") && !link.endsWith(".png")) {
                                allLinks.add(link);
                            }
                        }
                    });
                }

                // 从文本节点提取链接
                extractLinksFromTextNodes(doc, allLinks);

                List<String> linkList = new ArrayList<>(allLinks);
                log.info("=== 链接提取完成 | 总数：{} ===", linkList.size());
                return linkList;

            } catch (Exception e) {
                retryCount++;
                log.error("第{}次提取链接失败：{}", retryCount, e.getMessage());
                if (retryCount >= MAX_RETRIES) {
                    return new ArrayList<>();
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new ArrayList<>();
                }
            } finally {
                if (driver != null) {
                    driver.quit();
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * 仅提取中文标题
     */
    public List<String> extractChineseTitles() {
        List<String> chineseTitles = new ArrayList<>();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            WebDriver driver = null;
            try {
                log.info("开始提取香港01中文标题（第{}次尝试）", retryCount + 1);

                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless=new");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-blink-features=AutomationControlled");
                options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");

                driver = new ChromeDriver(options);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
                driver.get(TARGET_URL);

                // 滚动加载
                Actions actions = new Actions(driver);
                for (int i = 0; i < 2; i++) {
                    actions.scrollByAmount(0, driver.manage().window().getSize().getHeight() * 2).perform();
                    Thread.sleep(SCROLL_WAIT_MS);
                }

                // 解析页面
                String pageSource = driver.getPageSource();
                byte[] utf8Bytes = pageSource.getBytes(StandardCharsets.UTF_8);
                String utf8PageSource = new String(utf8Bytes, StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(utf8PageSource, TARGET_URL, Parser.htmlParser());

                // 提取所有content-card的标题
                Elements titleElements = doc.select(NEWS_ITEM_SELECTOR + " .card-title");
                log.info("共匹配到 {} 个标题元素", titleElements.size());

                // 过滤中文标题
                for (Element element : titleElements) {
                    String rawText = element.text().trim();
                    if (!rawText.isEmpty() && containsChinese(rawText)) {
                        String cleanText = rawText.replaceAll("[\\x00-\\x1F\\x7F\\s]+", " ").trim();
                        chineseTitles.add(cleanText);
                    }
                }

                // 去重
                chineseTitles = chineseTitles.stream().distinct().collect(Collectors.toList());
                log.info("=== 标题提取完成 | 总数：{} ===", chineseTitles.size());
                return chineseTitles;

            } catch (Exception e) {
                retryCount++;
                log.error("第{}次提取标题失败：{}", retryCount, e.getMessage());
                if (retryCount >= MAX_RETRIES) {
                    return new ArrayList<>();
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new ArrayList<>();
                }
            } finally {
                if (driver != null) {
                    driver.quit();
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * 从文本节点提取链接
     */
    private void extractLinksFromTextNodes(Node node, Set<String> links) {
        if (node instanceof TextNode textNode) {
            String text = new String(textNode.text().trim().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            Matcher matcher = HTTP_HTTPS_PATTERN.matcher(text);
            while (matcher.find()) {
                String link = matcher.group().trim().replaceAll("[.,;:\"']$", "");
                if (!link.contains("ads.") && !link.endsWith(".jpg") && !link.endsWith(".png")) {
                    links.add(link);
                }
            }
        }
        for (Node child : node.childNodes()) {
            extractLinksFromTextNodes(child, links);
        }
    }

    /**
     * 判断文本是否包含中文
     */
    private boolean containsChinese(String text) {
        Pattern chinesePattern = Pattern.compile("[\\u4E00-\\u9FA5\\u3000-\\u303F\\uFF00-\\uFFEF]");
        return chinesePattern.matcher(text).find();
    }

    // ========================= 测试入口 =========================
    public static void main(String[] args) {
        NewsCrawler01 crawler = new NewsCrawler01();
        // 测试提取完整新闻列表
        List<NewsItem> newsList = crawler.extractCompleteNewsList();
        // 打印第一条新闻详情
        if (!newsList.isEmpty()) {
            NewsItem firstNews = newsList.get(0);
            log.info("=== 第一条新闻详情 ===");
            log.info("标题：{}", firstNews.getTitle());
            log.info("类别：{}", firstNews.getCategory());
            log.info("发布时间（原始）：{}", firstNews.getPublishTimeDatetime());
            log.info("发布时间（格式化）：{}", firstNews.getPublishTimeFormatted());
            log.info("新闻URL：{}", firstNews.getNewsUrl());
            log.info("图片URL：{}", firstNews.getImageUrl());
            log.info("是否精选：{}", firstNews.isFeatured());
        }
    }
}