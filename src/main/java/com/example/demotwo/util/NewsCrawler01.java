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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class NewsCrawler01 {
    // ===================== 核心配置（精准匹配香港01） =====================
    private static final String HK01_TARGET_URL = "https://www.hk01.com/"; // 香港01首页（包含最新新闻）
    private static final int HTTP_TIMEOUT_SECONDS = 20;
    private static final int MAX_RETRIES = 3;
    private static final int MAX_NEWS_COUNT = 15; // 最多提取15条
    private static final long RETRY_DELAY_MS = 2000;

    private static final Logger log = LoggerFactory.getLogger(NewsCrawler01.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * 香港01新闻实体类（精准匹配目标HTML字段）
     */
    public static class NewsItem {
        private String title;       // 新闻标题（如：立法會選舉｜譚詠麟、曾志偉投票　甄子丹：望團結一心幫大埔災民）
        private String link;        // 新闻绝对链接
        private String category;    // 分类（如：政情）
        private String publishTime; // 发布时间（如：2025-12-07T08:25:39.000Z 或 4分鐘前）
        private String imageUrl;    // 新闻图片链接

        // Getter & Setter
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getPublishTime() { return publishTime; }
        public void setPublishTime(String publishTime) { this.publishTime = publishTime; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        // 重写toString，方便日志查看
        @Override
        public String toString() {
            return "NewsItem{" +
                    "标题='" + title + '\'' +
                    ", 分类='" + category + '\'' +
                    ", 发布时间='" + publishTime + '\'' +
                    ", 链接='" + link + '\'' +
                    ", 图片链接='" + imageUrl + '\'' +
                    '}';
        }
    }

    // ===================== 核心方法：提取香港01新闻（精准匹配HTML结构） =====================
    public List<NewsItem> extractCompleteNewsList() {
        List<NewsItem> newsList = new ArrayList<>();
        int retryCount = 0;

        // 内存监控
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        log.info("【内存监控】开始提取香港01新闻列表 | 已使用：{}MB | 最大可用：{}MB", usedMemory, maxMemory);

        while (retryCount < MAX_RETRIES) {
            try {
                log.info("开始提取香港01新闻（第{}次尝试），目标URL：{}", retryCount + 1, HK01_TARGET_URL);

                // 1. 构建反爬请求（已移除受限头）
                HttpRequest request = buildHk01Request();

                // 2. 发送请求并获取响应
                HttpResponse<String> response = HTTP_CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

                // 3. 验证响应有效性
                if (!validateResponse(response)) {
                    retryCount++;
                    log.warn("第{}次请求响应无效，{}ms后重试", retryCount, RETRY_DELAY_MS);
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    continue;
                }

                // 4. 处理编码（确保UTF-8，避免中文乱码）
                String rawHtml = response.body();
                String utf8Html = new String(rawHtml.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

                // 打印响应体预览（调试用，确认是否获取到正确页面）
                log.warn("=== 香港01响应体预览（前1500字符）===");
                String preview = utf8Html.length() > 1500 ? utf8Html.substring(0, 1500) : utf8Html;
                log.warn(preview);

                // 5. 解析HTML（核心：精准匹配目标新闻容器）
                Document doc = Jsoup.parse(utf8Html, HK01_TARGET_URL, Parser.htmlParser());

                // ========== 关键：匹配你提供的新闻容器结构 ==========
                // 选择器：data-testid="content-card" + class="content-card__main"（精准定位单条新闻）
                Elements newsContainers = doc.select("div[data-testid='content-card'].content-card__main");
                log.info("共匹配到香港01新闻容器：{}条", newsContainers.size());

                // 6. 遍历解析每条新闻
                int count = 0;
                for (Element container : newsContainers) {
                    if (count >= MAX_NEWS_COUNT) break; // 限制最大提取数量

                    NewsItem news = parseSingleNews(container);
                    // 过滤无效新闻（标题+链接非空）
                    if (news.getTitle() != null && !news.getTitle().isEmpty()
                            && news.getLink() != null && !news.getLink().isEmpty()) {
                        newsList.add(news);
                        count++;
                        log.debug("解析到有效新闻：{}", news);
                    } else {
                        log.warn("跳过无效新闻（标题/链接为空）：{}", news);
                    }
                }

                // 7. 打印结果日志
                log.info("=== 香港01新闻提取完成 | 有效新闻数：{} ===", newsList.size());
                for (int i = 0; i < newsList.size(); i++) {
                    log.info("新闻 {}: {}", i + 1, newsList.get(i));
                }

                // 内存监控
                usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
                log.info("【内存监控】香港01新闻列表提取完成 | 已使用：{}MB | 最大可用：{}MB", usedMemory, maxMemory);

                return newsList;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("香港01爬虫线程被中断", e);
                return new ArrayList<>();
            } catch (Exception e) {
                retryCount++;
                log.error("第{}次提取香港01新闻失败：{}", retryCount, e.getMessage(), e);
                if (retryCount >= MAX_RETRIES) {
                    log.error("所有重试次数用尽，返回空列表");
                    return new ArrayList<>();
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new ArrayList<>();
                }
            }
        }
        return new ArrayList<>();
    }

    // ===================== 核心方法：解析单条新闻（精准匹配目标HTML字段） =====================
    /**
     * 解析单条新闻容器（div[data-testid='content-card'].content-card__main）
     * 提取标题、链接、分类、发布时间、图片链接
     */
    private NewsItem parseSingleNews(Element newsContainer) {
        NewsItem news = new NewsItem();

        try {
            // ========== 1. 提取标题 + 新闻链接 ==========
            // 结构：<a data-testid="content-card-title" class="card-title" href="相对路径">标题</a>
            Element titleLink = newsContainer.selectFirst("a[data-testid='content-card-title']");
            if (titleLink != null) {
                String title = titleLink.text().trim();
                String relativeLink = titleLink.attr("href").trim();
                // 补全绝对链接（相对路径 -> 完整URL）
                String fullLink = relativeLink.startsWith("http") 
                        ? relativeLink 
                        : "https://www.hk01.com" + relativeLink;

                news.setTitle(title);
                news.setLink(fullLink);
                log.debug("提取标题：{} | 链接：{}", title, fullLink);
            }

            // ========== 2. 提取分类（如：政情） ==========
            // 结构：<div data-testid="content-card-channel" class="card-header"> -> <div class="card-category">分类</div>
            Element categoryEl = newsContainer.selectFirst("div[data-testid='content-card-channel'] .card-category div");
            if (categoryEl != null) {
                String category = categoryEl.text().trim();
                news.setCategory(category);
                log.debug("提取分类：{}", category);
            }

            // ========== 3. 提取发布时间（优先取datetime属性，其次文本） ==========
            // 结构：<time data-testid="content-card-time" datetime="2025-12-07T08:25:39.000Z">4 分鐘前</time>
            Element timeEl = newsContainer.selectFirst("time[data-testid='content-card-time']");
            if (timeEl != null) {
                String publishTime = timeEl.attr("datetime").trim(); // 优先取标准时间格式
                if (publishTime.isEmpty()) {
                    publishTime = timeEl.text().trim(); // 若无则取显示文本（如：4 分鐘前）
                }
                news.setPublishTime(publishTime);
                log.debug("提取发布时间：{}", publishTime);
            }

            // ========== 4. 提取图片链接 ==========
            // 结构：<div data-testid="content-card-thumbnail"> -> <img src="图片URL">
            Element imageEl = newsContainer.selectFirst("div[data-testid='content-card-thumbnail'] img");
            if (imageEl != null) {
                String imageUrl = imageEl.attr("src").trim();
                news.setImageUrl(imageUrl);
                log.debug("提取图片链接：{}", imageUrl);
            }

        } catch (Exception e) {
            log.error("解析单条香港01新闻失败", e);
        }

        return news;
    }

    // ===================== 辅助方法：构建香港01反爬请求（已移除受限头） =====================
    private HttpRequest buildHk01Request() {
        return HttpRequest.newBuilder()
                .uri(URI.create(HK01_TARGET_URL))
                // 强反爬请求头（模拟真实浏览器，避免被识别）
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "zh-HK,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Referer", "https://www.google.com/") // 模拟谷歌跳转
                .header("Cache-Control", "max-age=0")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "cross-site")
                .header("Sec-Fetch-User", "?1")
                // 关键：模拟Cookie（绕过基础反爬）
                .header("Cookie", "HK01_LANG=zh-HK; _ga=GA1.1.123456789.1733568000; _gid=GA1.1.987654321.1733568000; accept_cookie=1; _fbp=fb.1.1733568000123.456789;")
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .GET()
                .build();
    }

    // ===================== 辅助方法：验证响应有效性 =====================
    private boolean validateResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        if (statusCode != 200) {
            log.error("香港01响应状态码异常：{}", statusCode);
            return false;
        }

        String body = response.body();
        if (body == null || body.isEmpty()) {
            log.error("香港01响应体为空");
            return false;
        }

        // 验证是否为香港01有效页面（关键词校验）
        if (!body.contains("hk01.com") && !body.contains("香港01") && !body.contains("content-card__main")) {
            log.error("响应体非香港01有效页面（未匹配关键标识）");
            return false;
        }

        return true;
    }

    // ===================== 保留方法：提取所有链接/中文标题（兼容原有调用） =====================
    public List<String> extractAllLinks() {
        List<NewsItem> newsList = extractCompleteNewsList();
        List<String> linkList = new ArrayList<>();
        for (NewsItem news : newsList) {
            if (news.getLink() != null && !news.getLink().isEmpty()) {
                linkList.add(news.getLink());
            }
            // 同时添加图片链接（可选）
            if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                linkList.add(news.getImageUrl());
            }
        }
        log.info("提取香港01链接数（去重前）：{}", linkList.size());
        // 去重
        linkList = linkList.stream().distinct().collect(Collectors.toList());
        log.info("提取香港01链接数（去重后）：{}", linkList.size());
        return linkList;
    }

    public List<String> extractChineseTitles() {
        List<NewsItem> newsList = extractCompleteNewsList();
        List<String> titleList = newsList.stream()
                .map(NewsItem::getTitle)
                .filter(title -> title != null && !title.isEmpty() && containsChinese(title))
                .distinct() // 去重
                .collect(Collectors.toList());

        log.info("提取香港01中文标题数：{}", titleList.size());
        return titleList;
    }

    // 辅助方法：判断文本是否含中文
    private boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) return false;
        return Pattern.compile("[\\u4E00-\\u9FA5\\u3000-\\u303F\\uFF00-\\uFFEF]").matcher(text).find();
    }

    // ===================== 测试方法：本地运行验证 =====================
    public static void main(String[] args) {
        NewsCrawler01 crawler = new NewsCrawler01();
        log.info("===== 开始测试香港01新闻爬虫 =====");
        
        // 测试提取完整新闻列表
        List<NewsItem> newsList = crawler.extractCompleteNewsList();

        // 打印测试结果
        if (!newsList.isEmpty()) {
            System.out.println("\n===== 香港01新闻解析结果（前5条） =====");
            for (int i = 0; i < Math.min(newsList.size(), 5); i++) {
                NewsItem item = newsList.get(i);
                System.out.println("[" + (i + 1) + "]");
                System.out.println("标题：" + item.getTitle());
                System.out.println("分类：" + item.getCategory());
                System.out.println("发布时间：" + item.getPublishTime());
                System.out.println("链接：" + item.getLink());
                System.out.println("图片链接：" + item.getImageUrl());
                System.out.println("------------------------");
            }
        } else {
            System.out.println("解析失败，未获取到有效新闻（可能是IP被拦截）");
        }
    }
}