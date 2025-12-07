// package com.example.demotwo.util;

// import lombok.Data;
// import lombok.extern.slf4j.Slf4j;
// import org.jsoup.Jsoup;
// import org.jsoup.nodes.Document;
// import org.jsoup.nodes.Element;
// import org.jsoup.nodes.Node;
// import org.jsoup.nodes.TextNode;
// import org.jsoup.parser.Parser;
// import org.jsoup.select.Elements;
// import org.openqa.selenium.WebDriver;
// import org.openqa.selenium.chrome.ChromeDriver;
// import org.openqa.selenium.chrome.ChromeOptions;
// import org.openqa.selenium.interactions.Actions;
// import org.springframework.stereotype.Component;

// import java.nio.charset.StandardCharsets;
// import java.time.Duration;
// import java.time.LocalDate;
// import java.time.format.DateTimeFormatter;
// import java.util.ArrayList;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Set;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
// import java.util.stream.Collectors;

// /**
//  * 香港01新闻爬虫工具类（适配 data-testid="content-card" 结构）
//  * 功能：提取指定结构的新闻（标题+类别+发布时间+URL+图片URL）
//  */
// @Slf4j
// @Component
// public class NewsCrawler01 {

//     // 香港01目标页面（替换为你要爬取的列表页）
//     private static final String TARGET_URL = "https://www.hk01.com/latest";
//     // 最大重试次数
//     private static final int MAX_RETRIES = 3;
//     // 重试间隔（毫秒）
//     private static final long RETRY_DELAY_MS = 2000;
//     // 匹配HTTP/HTTPS链接的正则
//     private static final Pattern HTTP_HTTPS_PATTERN = Pattern.compile("https?:\\/\\/[^\\s\"']+", Pattern.UNICODE_CASE);
//     // 核心选择器：匹配 data-testid="content-card" 的新闻容器
//     private static final String NEWS_ITEM_SELECTOR = "div[data-testid='content-card']";
//     // 滚动加载次数
//     private static final int SCROLL_TIMES = 3;
//     // 每次滚动后的等待时间（毫秒）
//     private static final long SCROLL_WAIT_MS = 1500;

//     /**
//      * 新闻实体类（精准匹配需求：标题/类别/发布时间/URL/图片URL）
//      */
//     @Data
//     public static class NewsItem {
//         // 新闻标题
//         private String title;
//         // 新闻类别（如：大國小事）
//         private String category;
//         // 发布时间（原始datetime：2025-12-06T09:47:52.000Z）
//         private String publishTimeDatetime;
//         // 发布时间（格式化：2025-12-06 09:47）
//         private String publishTimeFormatted;
//         // 新闻完整URL
//         private String newsUrl;
//         // 新闻图片URL
//         private String imageUrl;
//         // 是否为精选
//         private boolean isFeatured;
//     }

//     // ========================= 核心功能：提取content-card结构的新闻 =========================
//     /**
//      * 提取完整新闻列表（适配 data-testid="content-card" 结构）
//      * @return 结构化新闻列表
//      */
//     public List<NewsItem> extractCompleteNewsList() {
//         List<NewsItem> newsList = new ArrayList<>();
//         int retryCount = 0;

//         while (retryCount < MAX_RETRIES) {
//             WebDriver driver = null;
//             try {
//                 log.info("开始提取香港01新闻（第{}次尝试），目标URL：{}", retryCount + 1, TARGET_URL);

//                 // 配置Chrome浏览器（反爬优化）
//                 ChromeOptions options = new ChromeOptions();
//                 options.addArguments("--headless=new");          // 无头模式
//                 options.addArguments("--no-sandbox");            // 解决Linux权限问题
//                 options.addArguments("--disable-dev-shm-usage"); // 解决内存不足
//                 options.addArguments("--disable-blink-features=AutomationControlled"); // 禁用自动化标识
//                 options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");
//                 options.addArguments("--disable-gpu");           // 禁用GPU加速
//                 options.addArguments("--lang=zh-HK");            // 设置繁体中文环境

//                 // 初始化WebDriver
//                 driver = new ChromeDriver(options);
//                 driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
//                 driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));
//                 driver.get(TARGET_URL);

//                 // 滚动加载更多新闻（触发动态加载）
//                 Actions actions = new Actions(driver);
//                 for (int i = 0; i < SCROLL_TIMES; i++) {
//                     actions.scrollByAmount(0, driver.manage().window().getSize().getHeight() * 2).perform();
//                     log.debug("第{}次滚动加载，等待{}ms", i + 1, SCROLL_WAIT_MS);
//                     Thread.sleep(SCROLL_WAIT_MS);
//                 }

//                 // 解析页面（UTF-8编码处理）
//                 String pageSource = driver.getPageSource();
//                 byte[] utf8Bytes = pageSource.getBytes(StandardCharsets.UTF_8);
//                 String utf8PageSource = new String(utf8Bytes, StandardCharsets.UTF_8);
//                 Document doc = Jsoup.parse(utf8PageSource, TARGET_URL, Parser.htmlParser());

//                 // 定位所有 content-card 新闻容器
//                 Elements newsElements = doc.select(NEWS_ITEM_SELECTOR);
//                 log.info("共匹配到 {} 个 content-card 新闻容器", newsElements.size());

//                 // 遍历提取每条新闻的核心字段
//                 for (Element card : newsElements) {
//                     NewsItem news = new NewsItem();

//                     // 1. 提取新闻标题（data-testid="content-card-title" 的a标签）
//                     Element titleA = card.selectFirst("a[data-testid='content-card-title'], .card-title");
//                     if (titleA != null) {
//                         String title = titleA.text().trim();
//                         news.setTitle(title);
//                         // 提取新闻URL（拼接绝对路径）
//                         String relativeUrl = titleA.attr("href").trim();
//                         if (!relativeUrl.startsWith("http")) {
//                             news.setNewsUrl("https://www.hk01.com" + relativeUrl);
//                         } else {
//                             news.setNewsUrl(relativeUrl);
//                         }
//                     }

//                     // 2. 提取新闻类别（card-category 内的文本）
//                     Element categoryDiv = card.selectFirst(".card-category div");
//                     if (categoryDiv != null) {
//                         String category = categoryDiv.text().trim();
//                         news.setCategory(category);
//                     }

//                     // 3. 提取发布时间（datetime属性 + 格式化）
//                     Element timeElem = card.selectFirst("time[data-testid='content-card-time']");
//                     if (timeElem != null) {
//                         // 原始datetime属性
//                         String datetime = timeElem.attr("datetime").trim();
//                         news.setPublishTimeDatetime(datetime);
//                         // 格式化时间（2025-12-06T09:47:52.000Z → 2025-12-06 09:47）
//                         if (!datetime.isEmpty()) {
//                             String formattedTime = formatDatetime(datetime);
//                             news.setPublishTimeFormatted(formattedTime);
//                         }
//                     }

//                     // 4. 提取图片URL（content-card-thumbnail 内的img标签src）
//                     Element imgElem = card.selectFirst("div[data-testid='content-card-thumbnail'] img");
//                     if (imgElem != null) {
//                         String imageUrl = imgElem.attr("src").trim();
//                         news.setImageUrl(imageUrl);
//                     }

//                     // 5. 判断是否为精选（card-info__item 包含"精選"）
//                     Element featuredElem = card.selectFirst(".card-info__item:contains(精選)");
//                     news.setFeatured(featuredElem != null);

//                     // 过滤有效新闻（标题+URL非空）
//                     if (news.getTitle() != null && !news.getTitle().isEmpty()
//                             && news.getNewsUrl() != null && !news.getNewsUrl().isEmpty()) {
//                         newsList.add(news);
//                         log.debug("提取到新闻：\n" +
//                                         "  标题：{}\n" +
//                                         "  类别：{}\n" +
//                                         "  时间：{}\n" +
//                                         "  URL：{}\n" +
//                                         "  图片：{}\n" +
//                                         "  精选：{}",
//                                 news.getTitle(), news.getCategory(), news.getPublishTimeFormatted(),
//                                 news.getNewsUrl(), news.getImageUrl(), news.isFeatured());
//                     }
//                 }

//                 // 去重（按URL去重）
//                 newsList = newsList.stream()
//                         .collect(Collectors.collectingAndThen(
//                                 Collectors.toMap(NewsItem::getNewsUrl, item -> item, (o1, o2) -> o1),
//                                 map -> new ArrayList<>(map.values())
//                         ));

//                 // 日志输出结果
//                 log.info("=== 香港01新闻提取完成 | 有效新闻数：{} ===", newsList.size());
//                 for (int i = 0; i < Math.min(newsList.size(), 10); i++) {
//                     NewsItem item = newsList.get(i);
//                     log.info("新闻 {}: [{}] {} (类别：{}, 精选：{})",
//                             i + 1, item.getPublishTimeFormatted(), item.getTitle(),
//                             item.getCategory(), item.isFeatured());
//                 }

//                 return newsList;

//             } catch (Exception e) {
//                 retryCount++;
//                 log.error("第{}次提取新闻失败：{}", retryCount, e.getMessage(), e);
//                 if (retryCount >= MAX_RETRIES) {
//                     log.error("所有重试次数已用尽，返回空列表", e);
//                     return new ArrayList<>();
//                 }
//                 // 重试等待
//                 try {
//                     Thread.sleep(RETRY_DELAY_MS);
//                 } catch (InterruptedException ie) {
//                     Thread.currentThread().interrupt();
//                     log.error("重试等待被中断", ie);
//                     return new ArrayList<>();
//                 }
//             } finally {
//                 // 关闭浏览器
//                 if (driver != null) {
//                     driver.quit();
//                 }
//             }
//         }
//         return new ArrayList<>();
//     }

//     // ========================= 辅助方法 =========================
//     /**
//      * 格式化ISO时间（2025-12-06T09:47:52.000Z → 2025-12-06 09:47）
//      */
//     private String formatDatetime(String isoDatetime) {
//         try {
//             // 解析ISO时间
//             DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
//             LocalDate date = LocalDate.parse(isoDatetime, isoFormatter);
//             // 提取时分
//             String timePart = isoDatetime.substring(11, 16);
//             // 拼接格式化时间
//             return date.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + timePart;
//         } catch (Exception e) {
//             // 解析失败返回原始值
//             return isoDatetime;
//         }
//     }

//     // ========================= 保留原有功能（可选） =========================
//     /**
//      * 提取页面中所有HTTP/HTTPS链接（去重）
//      */
//     public List<String> extractAllLinks() {
//         Set<String> allLinks = new HashSet<>();
//         int retryCount = 0;

//         while (retryCount < MAX_RETRIES) {
//             WebDriver driver = null;
//             try {
//                 log.info("开始提取香港01所有链接（第{}次尝试）", retryCount + 1);

//                 ChromeOptions options = new ChromeOptions();
//                 options.addArguments("--headless=new");
//                 options.addArguments("--no-sandbox");
//                 options.addArguments("--disable-dev-shm-usage");
//                 options.addArguments("--disable-blink-features=AutomationControlled");
//                 options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");

//                 driver = new ChromeDriver(options);
//                 driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
//                 driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
//                 driver.get(TARGET_URL);

//                 // 滚动加载
//                 Actions actions = new Actions(driver);
//                 for (int i = 0; i < 2; i++) {
//                     actions.scrollByAmount(0, driver.manage().window().getSize().getHeight() * 2).perform();
//                     Thread.sleep(SCROLL_WAIT_MS);
//                 }

//                 // 解析页面
//                 String pageSource = new String(driver.getPageSource().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
//                 Document doc = Jsoup.parse(pageSource, TARGET_URL);

//                 // 提取所有链接
//                 Elements allElements = doc.getAllElements();
//                 for (Element element : allElements) {
//                     element.attributes().forEach(attribute -> {
//                         String attrValue = attribute.getValue().trim();
//                         Matcher matcher = HTTP_HTTPS_PATTERN.matcher(attrValue);
//                         while (matcher.find()) {
//                             String link = matcher.group().trim();
//                             if (!link.contains("ads.") && !link.endsWith(".jpg") && !link.endsWith(".png")) {
//                                 allLinks.add(link);
//                             }
//                         }
//                     });
//                 }

//                 // 从文本节点提取链接
//                 extractLinksFromTextNodes(doc, allLinks);

//                 List<String> linkList = new ArrayList<>(allLinks);
//                 log.info("=== 链接提取完成 | 总数：{} ===", linkList.size());
//                 return linkList;

//             } catch (Exception e) {
//                 retryCount++;
//                 log.error("第{}次提取链接失败：{}", retryCount, e.getMessage());
//                 if (retryCount >= MAX_RETRIES) {
//                     return new ArrayList<>();
//                 }
//                 try {
//                     Thread.sleep(RETRY_DELAY_MS);
//                 } catch (InterruptedException ie) {
//                     Thread.currentThread().interrupt();
//                     return new ArrayList<>();
//                 }
//             } finally {
//                 if (driver != null) {
//                     driver.quit();
//                 }
//             }
//         }
//         return new ArrayList<>();
//     }

//     /**
//      * 仅提取中文标题
//      */
//     public List<String> extractChineseTitles() {
//         List<String> chineseTitles = new ArrayList<>();
//         int retryCount = 0;

//         while (retryCount < MAX_RETRIES) {
//             WebDriver driver = null;
//             try {
//                 log.info("开始提取香港01中文标题（第{}次尝试）", retryCount + 1);

//                 ChromeOptions options = new ChromeOptions();
//                 options.addArguments("--headless=new");
//                 options.addArguments("--no-sandbox");
//                 options.addArguments("--disable-dev-shm-usage");
//                 options.addArguments("--disable-blink-features=AutomationControlled");
//                 options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");

//                 driver = new ChromeDriver(options);
//                 driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
//                 driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
//                 driver.get(TARGET_URL);

//                 // 滚动加载
//                 Actions actions = new Actions(driver);
//                 for (int i = 0; i < 2; i++) {
//                     actions.scrollByAmount(0, driver.manage().window().getSize().getHeight() * 2).perform();
//                     Thread.sleep(SCROLL_WAIT_MS);
//                 }

//                 // 解析页面
//                 String pageSource = driver.getPageSource();
//                 byte[] utf8Bytes = pageSource.getBytes(StandardCharsets.UTF_8);
//                 String utf8PageSource = new String(utf8Bytes, StandardCharsets.UTF_8);
//                 Document doc = Jsoup.parse(utf8PageSource, TARGET_URL, Parser.htmlParser());

//                 // 提取所有content-card的标题
//                 Elements titleElements = doc.select(NEWS_ITEM_SELECTOR + " .card-title");
//                 log.info("共匹配到 {} 个标题元素", titleElements.size());

//                 // 过滤中文标题
//                 for (Element element : titleElements) {
//                     String rawText = element.text().trim();
//                     if (!rawText.isEmpty() && containsChinese(rawText)) {
//                         String cleanText = rawText.replaceAll("[\\x00-\\x1F\\x7F\\s]+", " ").trim();
//                         chineseTitles.add(cleanText);
//                     }
//                 }

//                 // 去重
//                 chineseTitles = chineseTitles.stream().distinct().collect(Collectors.toList());
//                 log.info("=== 标题提取完成 | 总数：{} ===", chineseTitles.size());
//                 return chineseTitles;

//             } catch (Exception e) {
//                 retryCount++;
//                 log.error("第{}次提取标题失败：{}", retryCount, e.getMessage());
//                 if (retryCount >= MAX_RETRIES) {
//                     return new ArrayList<>();
//                 }
//                 try {
//                     Thread.sleep(RETRY_DELAY_MS);
//                 } catch (InterruptedException ie) {
//                     Thread.currentThread().interrupt();
//                     return new ArrayList<>();
//                 }
//             } finally {
//                 if (driver != null) {
//                     driver.quit();
//                 }
//             }
//         }
//         return new ArrayList<>();
//     }

//     /**
//      * 从文本节点提取链接
//      */
//     private void extractLinksFromTextNodes(Node node, Set<String> links) {
//         if (node instanceof TextNode textNode) {
//             String text = new String(textNode.text().trim().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
//             Matcher matcher = HTTP_HTTPS_PATTERN.matcher(text);
//             while (matcher.find()) {
//                 String link = matcher.group().trim().replaceAll("[.,;:\"']$", "");
//                 if (!link.contains("ads.") && !link.endsWith(".jpg") && !link.endsWith(".png")) {
//                     links.add(link);
//                 }
//             }
//         }
//         for (Node child : node.childNodes()) {
//             extractLinksFromTextNodes(child, links);
//         }
//     }

//     /**
//      * 判断文本是否包含中文
//      */
//     private boolean containsChinese(String text) {
//         Pattern chinesePattern = Pattern.compile("[\\u4E00-\\u9FA5\\u3000-\\u303F\\uFF00-\\uFFEF]");
//         return chinesePattern.matcher(text).find();
//     }

//     // ========================= 测试入口 =========================
//     public static void main(String[] args) {
//         NewsCrawler01 crawler = new NewsCrawler01();
//         // 测试提取完整新闻列表
//         List<NewsItem> newsList = crawler.extractCompleteNewsList();
//         // 打印第一条新闻详情
//         if (!newsList.isEmpty()) {
//             NewsItem firstNews = newsList.get(0);
//             log.info("=== 第一条新闻详情 ===");
//             log.info("标题：{}", firstNews.getTitle());
//             log.info("类别：{}", firstNews.getCategory());
//             log.info("发布时间（原始）：{}", firstNews.getPublishTimeDatetime());
//             log.info("发布时间（格式化）：{}", firstNews.getPublishTimeFormatted());
//             log.info("新闻URL：{}", firstNews.getNewsUrl());
//             log.info("图片URL：{}", firstNews.getImageUrl());
//             log.info("是否精选：{}", firstNews.isFeatured());
//         }
//     }
// }

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
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 香港01新闻爬虫工具类（优化版）
 * 适配 Render 512MB 内存限制 | 纯 Jsoup + HttpClient | 内存/性能优化
 */
@Slf4j
@Component
public class NewsCrawler01 {

    // ========== 核心配置（适配低内存） ==========
    // 目标新闻页面URL
    private static final String TARGET_URL = "https://www.hk01.com/latest";
    // 最大重试次数（减少重试叠加内存）
    private static final int MAX_RETRIES = 2;
    // 重试间隔（毫秒）
    private static final long RETRY_DELAY_MS = 3000;
    // 单次最大爬取新闻数（核心：限制内存占用）
    private static final int MAX_NEWS_COUNT = 15;
    // 解析延迟（防反爬 + 平滑内存）
    private static final long PARSE_DELAY_MS = 300;
    // HTTP 超时时间（避免阻塞）
    private static final int HTTP_TIMEOUT_SECONDS = 30;
    // 核心选择器
    private static final String NEWS_ITEM_SELECTOR = "div[data-testid='content-card']";
    // 正则表达式
    private static final Pattern HTTP_HTTPS_PATTERN = Pattern.compile("https?:\\/\\/[^\\s\"']+", Pattern.UNICODE_CASE);
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4E00-\\u9FA5\\u3000-\\u303F\\uFF00-\\uFFEF]");

    // 复用 HttpClient（减少连接创建开销）
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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

    // ========================= 核心功能：提取完整新闻列表（优化版） =========================
    /**
     * 提取完整新闻列表（适配 data-testid="content-card" 结构）
     * @return 结构化新闻列表（最多 MAX_NEWS_COUNT 条）
     */
    public List<NewsItem> extractCompleteNewsList() {
        // 预分配容量，减少内存扩容开销
        List<NewsItem> newsList = new ArrayList<>(MAX_NEWS_COUNT);
        int retryCount = 0;

        // 内存监控：打印初始状态
        logMemoryUsage("开始提取香港01新闻列表");

        while (retryCount < MAX_RETRIES) {
            try {
                log.info("开始提取香港01新闻（第{}次尝试），目标URL：{}", retryCount + 1, TARGET_URL);

                // ========== 核心优化：移除 Selenium，改用 HttpClient + Jsoup ==========
                // 1. 构建HTTP请求（模拟浏览器头，防反爬）
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(TARGET_URL))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Encoding", "gzip, deflate")
                        .header("Accept-Language", "zh-HK,zh;q=0.9,en;q=0.8")
                        .header("Referer", "https://www.hk01.com/")
                        .header("Cookie", "HK01_LANG=zh-HK; _ga=GA1.1.123456789.1733577600") // 模拟Cookie，减少反爬
                        .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                        .GET()
                        .build();

                // 2. 发送请求并获取响应
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != 200) {
                    log.error("HTTP请求失败，状态码：{}", response.statusCode());
                    retryCount++;
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                // 3. Jsoup解析（优化编码 + 禁用不必要特性）
                Document doc = Jsoup.parse(response.body(), TARGET_URL, Parser.htmlParser())
                        .outputSettings(new Document.OutputSettings()
                                .charset(StandardCharsets.UTF_8)
                                .prettyPrint(false) // 禁用格式化，节省内存
                                .outline(false));   // 禁用大纲，节省内存

                // 4. 定位新闻容器（兼容动态加载后的静态内容）
                Elements newsElements = doc.select(NEWS_ITEM_SELECTOR);
                log.info("共匹配到 {} 个 content-card 新闻容器，本次最多提取 {} 条",
                        newsElements.size(), MAX_NEWS_COUNT);

                // 5. 遍历提取（限制数量 + 逐行释放）
                int parsedCount = 0;
                for (Element card : newsElements) {
                    if (parsedCount >= MAX_NEWS_COUNT) {
                        log.info("已达到单次最大爬取数量（{}条），停止解析", MAX_NEWS_COUNT);
                        break;
                    }

                    NewsItem news = new NewsItem();

                    // 提取标题 + URL
                    Element titleA = card.selectFirst("a[data-testid='content-card-title'], .card-title");
                    if (titleA != null) {
                        news.setTitle(titleA.text().trim());
                        // 拼接绝对URL
                        String relativeUrl = titleA.attr("href").trim();
                        news.setNewsUrl(relativeUrl.startsWith("http") ? relativeUrl : "https://www.hk01.com" + relativeUrl);
                    }

                    // 提取类别
                    Element categoryDiv = card.selectFirst(".card-category div");
                    if (categoryDiv != null) {
                        news.setCategory(categoryDiv.text().trim());
                    }

                    // 提取发布时间（原始 + 格式化）
                    Element timeElem = card.selectFirst("time[data-testid='content-card-time']");
                    if (timeElem != null) {
                        String datetime = timeElem.attr("datetime").trim();
                        news.setPublishTimeDatetime(datetime);
                        news.setPublishTimeFormatted(formatDatetime(datetime));
                    }

                    // 提取图片URL
                    Element imgElem = card.selectFirst("div[data-testid='content-card-thumbnail'] img");
                    if (imgElem != null) {
                        news.setImageUrl(imgElem.attr("src").trim());
                    }

                    // 判断是否精选
                    news.setFeatured(card.selectFirst(".card-info__item:contains(精選)") != null);

                    // 过滤有效新闻
                    if (isValidNews(news)) {
                        newsList.add(news);
                        parsedCount++;
                        // 解析延迟（防反爬 + 平滑内存）
                        Thread.sleep(PARSE_DELAY_MS);
                    }

                    // 每解析5条打印内存状态
                    if (parsedCount % 5 == 0) {
                        logMemoryUsage("已解析 " + parsedCount + " 条新闻");
                    }
                }

                // 6. 去重（按URL去重，减少重复数据）
                newsList = newsList.stream()
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(NewsItem::getNewsUrl, item -> item, (o1, o2) -> o1),
                                map -> new ArrayList<>(map.values())
                        ));

                // 7. 日志输出（精简版）
                log.info("=== 香港01新闻提取完成 | 有效新闻数：{} ===", newsList.size());
                newsList.forEach(item -> log.debug("新闻：[{}] {} (类别：{}, 精选：{})",
                        item.getPublishTimeFormatted(), item.getTitle(), item.getCategory(), item.isFeatured()));

                // 内存监控：提取完成
                logMemoryUsage("香港01新闻列表提取完成");

                return newsList;

            } catch (Exception e) {
                retryCount++;
                log.error("第{}次提取新闻失败：{}", retryCount, e.getMessage());
                if (retryCount >= MAX_RETRIES) {
                    log.error("所有重试次数已用尽，返回空列表", e);
                    return Collections.emptyList();
                }
                // 重试等待
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("重试等待被中断", ie);
                    return Collections.emptyList();
                }
            }
        }
        return Collections.emptyList();
    }

    // ========================= 保留功能：提取所有HTTP/HTTPS链接（优化版） =========================
    public List<String> extractAllLinks() {
        Set<String> allLinks = new HashSet<>(50); // 预分配容量
        int retryCount = 0;

        logMemoryUsage("开始提取香港01所有链接");

        while (retryCount < MAX_RETRIES) {
            try {
                log.info("开始提取香港01所有链接（第{}次尝试）", retryCount + 1);

                // 构建HTTP请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(TARGET_URL))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                        .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                        .GET()
                        .build();

                // 发送请求
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != 200) {
                    log.error("HTTP请求失败，状态码：{}", response.statusCode());
                    retryCount++;
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                // 解析页面
                Document doc = Jsoup.parse(response.body(), TARGET_URL, Parser.htmlParser());

                // 提取链接（过滤广告/图片链接）
                Elements allElements = doc.getAllElements();
                for (Element element : allElements) {
                    element.attributes().forEach(attribute -> {
                        String attrValue = attribute.getValue().trim();
                        Matcher matcher = HTTP_HTTPS_PATTERN.matcher(attrValue);
                        while (matcher.find()) {
                            String link = matcher.group().trim().replaceAll("[.,;:\"']$", "");
                            // 过滤广告、图片链接
                            if (!link.contains("ads.") && !link.endsWith(".jpg") && !link.endsWith(".png") && !link.endsWith(".gif")) {
                                allLinks.add(link);
                            }
                        }
                    });
                }

                // 从文本节点提取链接
                extractLinksFromTextNodes(doc, allLinks);

                // 限制链接数量，减少内存
                List<String> linkList = new ArrayList<>(allLinks).stream()
                        .limit(MAX_NEWS_COUNT * 2)
                        .collect(Collectors.toList());

                log.info("=== 香港01链接提取完成 | 总数：{} ===", linkList.size());
                logMemoryUsage("香港01链接提取完成");

                return linkList;

            } catch (Exception e) {
                retryCount++;
                log.error("第{}次提取链接失败：{}", retryCount, e.getMessage());
                if (retryCount >= MAX_RETRIES) {
                    return Collections.emptyList();
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Collections.emptyList();
                }
            }
        }
        return Collections.emptyList();
    }

    // ========================= 保留功能：仅提取中文标题（优化版） =========================
    public List<String> extractChineseTitles() {
        List<String> chineseTitles = new ArrayList<>(MAX_NEWS_COUNT);
        int retryCount = 0;

        logMemoryUsage("开始提取香港01中文标题");

        while (retryCount < MAX_RETRIES) {
            try {
                log.info("开始提取香港01中文标题（第{}次尝试）", retryCount + 1);

                // 构建HTTP请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(TARGET_URL))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                        .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                        .GET()
                        .build();

                // 发送请求
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != 200) {
                    log.error("HTTP请求失败，状态码：{}", response.statusCode());
                    retryCount++;
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                // 解析页面
                Document doc = Jsoup.parse(response.body(), TARGET_URL, Parser.htmlParser())
                        .outputSettings(new Document.OutputSettings().charset(StandardCharsets.UTF_8));

                // 提取标题元素
                Elements titleElements = doc.select(NEWS_ITEM_SELECTOR + " .card-title");
                log.info("共匹配到 {} 个标题元素", titleElements.size());

                // 过滤中文标题（限制数量）
                int parsedCount = 0;
                for (Element element : titleElements) {
                    if (parsedCount >= MAX_NEWS_COUNT) break;

                    String rawText = element.text().trim();
                    if (!rawText.isEmpty() && containsChinese(rawText)) {
                        // 清理特殊字符
                        String cleanText = rawText.replaceAll("[\\x00-\\x1F\\x7F\\s]+", " ").trim();
                        chineseTitles.add(cleanText);
                        parsedCount++;
                        Thread.sleep(PARSE_DELAY_MS);
                    }
                }

                // 去重
                chineseTitles = chineseTitles.stream().distinct().collect(Collectors.toList());

                log.info("=== 香港01标题提取完成 | 总数：{} ===", chineseTitles.size());
                logMemoryUsage("香港01标题提取完成");

                return chineseTitles;

            } catch (Exception e) {
                retryCount++;
                log.error("第{}次提取标题失败：{}", retryCount, e.getMessage());
                if (retryCount >= MAX_RETRIES) {
                    return Collections.emptyList();
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Collections.emptyList();
                }
            }
        }
        return Collections.emptyList();
    }

    // ========================= 辅助方法（优化版） =========================
    /**
     * 格式化ISO时间（2025-12-06T09:47:52.000Z → 2025-12-06 09:47）
     */
    private String formatDatetime(String isoDatetime) {
        if (isoDatetime == null || isoDatetime.isEmpty()) {
            return "";
        }
        try {
            // 兼容不同的ISO时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            LocalDate date = LocalDate.parse(isoDatetime, formatter);
            String timePart = isoDatetime.length() >= 16 ? isoDatetime.substring(11, 16) : "";
            return date.format(DateTimeFormatter.ISO_LOCAL_DATE) + (timePart.isEmpty() ? "" : " " + timePart);
        } catch (DateTimeParseException e) {
            // 解析失败返回原始值
            return isoDatetime;
        }
    }

    /**
     * 从文本节点提取链接
     */
    private void extractLinksFromTextNodes(Node node, Set<String> links) {
        if (node instanceof TextNode textNode) {
            String text = textNode.text().trim();
            Matcher matcher = HTTP_HTTPS_PATTERN.matcher(text);
            while (matcher.find()) {
                String link = matcher.group().trim().replaceAll("[.,;:\"']$", "");
                if (!link.contains("ads.") && !link.endsWith(".jpg") && !link.endsWith(".png")) {
                    links.add(link);
                }
            }
        }
        // 递归遍历子节点（优化：用新列表避免并发修改）
        for (Node child : new ArrayList<>(node.childNodes())) {
            extractLinksFromTextNodes(child, links);
        }
    }

    /**
     * 判断文本是否包含中文
     */
    private boolean containsChinese(String text) {
        return CHINESE_PATTERN.matcher(text).find();
    }

    /**
     * 校验新闻是否有效（非空）
     */
    private boolean isValidNews(NewsItem news) {
        return news.getTitle() != null && !news.getTitle().isEmpty()
                && news.getNewsUrl() != null && !news.getNewsUrl().isEmpty();
    }

    /**
     * 内存使用监控（关键：定位OOM）
     */
    private void logMemoryUsage(String stage) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        log.info("【内存监控】{} | 已使用：{}MB | 最大可用：{}MB", stage, usedMemory, maxMemory);
    }

    // ========================= 测试入口（本地调试用） =========================
    public static void main(String[] args) {
        NewsCrawler01 crawler = new NewsCrawler01();
        List<NewsItem> newsList = crawler.extractCompleteNewsList();
        if (!newsList.isEmpty()) {
            NewsItem firstNews = newsList.get(0);
            log.info("=== 第一条新闻详情 ===");
            log.info("标题：{}", firstNews.getTitle());
            log.info("类别：{}", firstNews.getCategory());
            log.info("发布时间：{}", firstNews.getPublishTimeFormatted());
            log.info("新闻URL：{}", firstNews.getNewsUrl());
            log.info("是否精选：{}", firstNews.isFeatured());
        }
    }
}