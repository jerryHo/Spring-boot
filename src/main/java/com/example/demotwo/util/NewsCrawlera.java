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
// import org.springframework.stereotype.Component;

// import java.nio.charset.StandardCharsets;
// import java.time.Duration;
// import java.util.ArrayList;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Set;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
// import java.util.stream.Collectors;

// /**
//  * 香港电台新闻爬虫工具类
//  * 功能：提取完整新闻列表（标题+链接+时间+多媒体）、单独提取所有链接、单独提取中文标题
//  */
// @Slf4j
// @Component
// public class NewsCrawlera {

//     // 目标新闻页面URL
//     private static final String TARGET_URL = "https://news.rthk.hk/rthk/ch/latest-news.htm";
//     // 最大重试次数
//     private static final int MAX_RETRIES = 3;
//     // 重试间隔（毫秒）
//     private static final long RETRY_DELAY_MS = 2000;
//     // 匹配HTTP/HTTPS链接的正则
//     private static final Pattern HTTP_HTTPS_PATTERN = Pattern.compile("https?:\\/\\/[^\\s\"']+", Pattern.UNICODE_CASE);

//     /**
//      * 新闻实体类：存储单条新闻的完整信息
//      */
//     @Data
//     public static class NewsItem {
//         // 新闻中文标题
//         private String title;
//         // 新闻完整链接
//         private String link;
//         // 发布时间（格式：2025-12-06 HKT 00:11）
//         private String publishTime;
//         // 是否包含视频
//         private boolean hasVideo;
//         // 是否包含音频
//         private boolean hasAudio;
//     }

//     // ========================= 核心功能：提取完整新闻列表 =========================
//     /**
//      * 提取完整新闻列表（标题+链接+发布时间+视频/音频标识）
//      * @return 结构化新闻列表
//      */
//     public List<NewsItem> extractCompleteNewsList() {
//         List<NewsItem> newsList = new ArrayList<>();
//         int retryCount = 0;

//         while (retryCount < MAX_RETRIES) {
//             WebDriver driver = null;
//             try {
//                 log.info("开始提取完整新闻列表（第{}次尝试），目标URL：{}", retryCount + 1, TARGET_URL);

//                 // 配置Chrome浏览器（无头模式）
//                 ChromeOptions options = new ChromeOptions();
//                 options.addArguments("--headless=new");          // 无头模式（无界面）
//                 options.addArguments("--no-sandbox");            // 解决Linux环境权限问题
//                 options.addArguments("--disable-dev-shm-usage"); // 解决内存不足问题
//                 options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"); // 模拟真实浏览器

//                 // 初始化WebDriver
//                 driver = new ChromeDriver(options);
//                 driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(40)); // 页面加载超时
//                 driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));  // 元素查找超时
//                 driver.get(TARGET_URL);

//                 // ========== 修正：正确处理编码并解析HTML ==========
//                 // 1. 获取页面源码并强制UTF-8编码
//                 String pageSource = driver.getPageSource();
//                 byte[] utf8Bytes = pageSource.getBytes(StandardCharsets.UTF_8);
//                 String utf8PageSource = new String(utf8Bytes, StandardCharsets.UTF_8);
//                 // 2. 使用正确的Jsoup.parse重载（第三个参数为Parser，而非String）
//                 Document doc = Jsoup.parse(utf8PageSource, TARGET_URL, Parser.htmlParser());

//                 // 定位所有新闻行（匹配HTML结构中的.ns2-row）
//                 Elements newsRowElements = doc.select(".ns2-row");
//                 log.info("共匹配到 {} 条新闻行", newsRowElements.size());

//                 // 遍历每条新闻，提取完整信息
//                 for (Element row : newsRowElements) {
//                     NewsItem news = new NewsItem();

//                     // 1. 提取中文标题（h4.ns2-title > a > font[my=my]）
//                     Element titleFont = row.selectFirst(".ns2-title a font[my=my]");
//                     if (titleFont != null) {
//                         news.setTitle(titleFont.text().trim());
//                     }

//                     // 2. 提取新闻链接（h4.ns2-title > a 的href属性，转换为绝对链接）
//                     Element newsLink = row.selectFirst(".ns2-title a");
//                     if (newsLink != null) {
//                         String link = newsLink.attr("href").trim();
//                         news.setLink(link.startsWith("http") ? link : "https://news.rthk.hk" + link);
//                     }

//                     // 3. 提取发布时间（.ns2-created > font[my=my]）
//                     Element timeFont = row.selectFirst(".ns2-created font[my=my]");
//                     if (timeFont != null) {
//                         news.setPublishTime(timeFont.text().trim());
//                     }

//                     // 4. 判断是否包含视频/音频（根据multimediaIndicators中的图标）
//                     Elements multimediaIcons = row.select(".multimediaIndicators img");
//                     for (Element icon : multimediaIcons) {
//                         String altText = icon.attr("alt").toLowerCase();
//                         if (altText.contains("video")) {
//                             news.setHasVideo(true);
//                         } else if (altText.contains("audio")) {
//                             news.setHasAudio(true);
//                         }
//                     }

//                     // 过滤无效新闻（标题和链接非空才保留）
//                     if (news.getTitle() != null && !news.getTitle().isEmpty()
//                             && news.getLink() != null && !news.getLink().isEmpty()) {
//                         newsList.add(news);
//                         log.debug("提取到新闻：标题={}, 时间={}, 视频={}, 音频={}",
//                                 news.getTitle(), news.getPublishTime(), news.isHasVideo(), news.isHasAudio());
//                     }
//                 }

//                 // 日志展示最终提取结果
//                 log.info("=== 完整新闻列表提取完成 | 有效新闻数：{} ===", newsList.size());
//                 for (int i = 0; i < newsList.size(); i++) {
//                     NewsItem item = newsList.get(i);
//                     log.info("新闻 {}: [{}] {} (视频：{}, 音频：{})",
//                             i + 1, item.getPublishTime(), item.getTitle(), item.isHasVideo(), item.isHasAudio());
//                 }

//                 return newsList;

//             } catch (Exception e) {
//                 retryCount++;
//                 log.error("第{}次提取新闻列表失败：{}", retryCount, e.getMessage());
//                 if (retryCount >= MAX_RETRIES) {
//                     log.error("所有重试次数已用尽，返回空列表", e);
//                     return new ArrayList<>();
//                 }
//                 // 重试前等待
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

//     // ========================= 保留功能：提取所有HTTP/HTTPS链接 =========================
//     /**
//      * 提取页面中所有HTTP/HTTPS链接（去重）
//      * @return 去重后的链接列表
//      */
//     public List<String> extractAllLinks() {
//         Set<String> allLinks = new HashSet<>();
//         int retryCount = 0;

//         while (retryCount < MAX_RETRIES) {
//             WebDriver driver = null;
//             try {
//                 log.info("开始提取所有链接（第{}次尝试），目标URL：{}", retryCount + 1, TARGET_URL);

//                 ChromeOptions options = new ChromeOptions();
//                 options.addArguments("--headless=new");
//                 options.addArguments("--no-sandbox");
//                 options.addArguments("--disable-dev-shm-usage");
//                 options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

//                 driver = new ChromeDriver(options);
//                 driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
//                 driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
//                 driver.get(TARGET_URL);

//                 String pageSource = new String(driver.getPageSource().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
//                 Document doc = Jsoup.parse(pageSource, TARGET_URL); // 简化版：默认HTML解析器

//                 // 从所有元素的属性中提取链接
//                 Elements allElements = doc.getAllElements();
//                 for (Element element : allElements) {
//                     element.attributes().forEach(attribute -> {
//                         String attrValue = attribute.getValue().trim();
//                         Matcher matcher = HTTP_HTTPS_PATTERN.matcher(attrValue);
//                         while (matcher.find()) {
//                             String link = matcher.group().trim();
//                             allLinks.add(link);
//                             log.debug("从属性提取链接：{}", link);
//                         }
//                     });
//                 }

//                 // 从文本节点中提取链接
//                 extractLinksFromTextNodes(doc, allLinks);

//                 // 转换为列表并日志展示
//                 List<String> linkList = new ArrayList<>(allLinks);
//                 log.info("=== 所有链接提取完成 | 总数：{} ===", linkList.size());
//                 for (int i = 0; i < linkList.size(); i++) {
//                     log.info("链接 {}: {}", i + 1, linkList.get(i));
//                 }

//                 return linkList;

//             } catch (Exception e) {
//                 retryCount++;
//                 log.error("第{}次提取链接失败：{}", retryCount, e.getMessage());
//                 if (retryCount >= MAX_RETRIES) {
//                     log.error("所有重试次数已用尽，返回空列表", e);
//                     return new ArrayList<>();
//                 }
//                 try {
//                     Thread.sleep(RETRY_DELAY_MS);
//                 } catch (InterruptedException ie) {
//                     Thread.currentThread().interrupt();
//                     log.error("重试等待被中断", ie);
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

//     // ========================= 保留功能：仅提取中文标题 =========================
//     /**
//      * 仅提取页面中的中文标题（去重）
//      * @return 中文标题列表
//      */
//     public List<String> extractChineseTitles() {
//         List<String> chineseTitles = new ArrayList<>();
//         int retryCount = 0;

//         while (retryCount < MAX_RETRIES) {
//             WebDriver driver = null;
//             try {
//                 log.info("开始提取中文标题（第{}次尝试），目标URL：{}", retryCount + 1, TARGET_URL);

//                 ChromeOptions options = new ChromeOptions();
//                 options.addArguments("--headless=new");
//                 options.addArguments("--no-sandbox");
//                 options.addArguments("--disable-dev-shm-usage");
//                 options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

//                 driver = new ChromeDriver(options);
//                 driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(40));
//                 driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
//                 driver.get(TARGET_URL);

//                 // ========== 修正：同样处理编码和解析器 ==========
//                 String pageSource = driver.getPageSource();
//                 byte[] utf8Bytes = pageSource.getBytes(StandardCharsets.UTF_8);
//                 String utf8PageSource = new String(utf8Bytes, StandardCharsets.UTF_8);
//                 Document doc = Jsoup.parse(utf8PageSource, TARGET_URL, Parser.htmlParser());

//                 // 匹配所有可能的标题元素（优先匹配新闻列表的标题）
//                 Elements titleElements = doc.select(
//                         ".ns2-title a font[my=my], h1, h2, h3, h4, " +
//                         ".sptab-title, .detailNewsSlideTitleText, .itemTitle, .title, .headline"
//                 );

//                 log.info("共匹配到 {} 个标题元素", titleElements.size());

//                 // 提取并过滤中文标题
//                 for (Element element : titleElements) {
//                     String rawText = element.text().trim();
//                     log.debug("原始标题文本：'{}'", rawText);

//                     if (!rawText.isEmpty() && containsChinese(rawText)) {
//                         String cleanText = rawText.replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
//                         chineseTitles.add(cleanText);
//                     }
//                 }

//                 // 去重
//                 chineseTitles = chineseTitles.stream().distinct().collect(Collectors.toList());

//                 log.info("=== 中文标题提取完成 | 总数：{} ===", chineseTitles.size());
//                 for (int i = 0; i < chineseTitles.size(); i++) {
//                     log.info("标题 {}: {}", i + 1, chineseTitles.get(i));
//                 }

//                 return chineseTitles;

//             } catch (Exception e) {
//                 retryCount++;
//                 log.error("第{}次提取标题失败：{}", retryCount, e.getMessage());
//                 if (retryCount >= MAX_RETRIES) {
//                     log.error("所有重试次数已用尽，返回空列表", e);
//                     return new ArrayList<>();
//                 }
//                 try {
//                     Thread.sleep(RETRY_DELAY_MS);
//                 } catch (InterruptedException ie) {
//                     Thread.currentThread().interrupt();
//                     log.error("重试等待被中断", ie);
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

//     // ========================= 辅助方法 =========================
//     /**
//      * 从文本节点中提取链接（供extractAllLinks使用）
//      */
//     private void extractLinksFromTextNodes(Node node, Set<String> links) {
//         if (node instanceof TextNode textNode) {
//             String text = new String(textNode.text().trim().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
//             Matcher matcher = HTTP_HTTPS_PATTERN.matcher(text);
//             while (matcher.find()) {
//                 String link = matcher.group().trim().replaceAll("[.,;:\"']$", "");
//                 links.add(link);
//                 log.debug("从文本节点提取链接：{}", link);
//             }
//         }
//         // 递归遍历子节点
//         for (Node child : node.childNodes()) {
//             extractLinksFromTextNodes(child, links);
//         }
//     }

//     /**
//      * 判断文本是否包含中文（供extractChineseTitles使用）
//      */
//     private boolean containsChinese(String text) {
//         Pattern chinesePattern = Pattern.compile("[\\u4E00-\\u9FA5\\u3000-\\u303F\\uFF00-\\uFFEF]");
//         return chinesePattern.matcher(text).find();
//     }
// }

package com.example.demotwo.util;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 香港电台(RTHK)新闻爬虫工具类（独立处理，HK01请见Hk01NewsCrawler.java）
 * 功能：提取完整新闻列表（标题+链接+时间+多媒体）、单独提取所有链接、单独提取中文标题
 * @Component：声明为Spring Bean，解决@Autowired注入问题
 */
@Component // 核心：添加Spring组件注解，让容器管理Bean
public class NewsCrawlera {
    // ===================== 核心配置（RTHK专属） =====================
    private static final Logger log = LoggerFactory.getLogger(NewsCrawlera.class);
    private static final String RTHK_TARGET_URL = "https://news.rthk.hk/rthk/ch/latest-news.htm";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;
    private static final Pattern HTTP_HTTPS_PATTERN = Pattern.compile("https?:\\/\\/[^\\s\"']+", Pattern.UNICODE_CASE);
    
    // ChromeDriver路径配置（适配Render/Linux环境）
    static {
        // Render环境下ChromeDriver默认路径（需先安装Chrome和ChromeDriver）
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        // 本地测试可替换为：System.setProperty("webdriver.chrome.driver", "你的本地ChromeDriver路径");
    }

    /**
     * 新闻实体类：存储单条RTHK新闻的完整信息
     * 注：若项目未引入Lombok，需手动写getter/setter（已替换，无需Lombok）
     */
    public static class NewsItem {
        // 新闻中文标题
        private String title;
        // 新闻完整链接
        private String link;
        // 发布时间（格式：2025-12-06 HKT 00:11）
        private String publishTime;
        // 是否包含视频
        private boolean hasVideo;
        // 是否包含音频
        private boolean hasAudio;

        // Getter & Setter（替换Lombok的@Data）
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public String getPublishTime() { return publishTime; }
        public void setPublishTime(String publishTime) { this.publishTime = publishTime; }
        public boolean isHasVideo() { return hasVideo; }
        public void setHasVideo(boolean hasVideo) { this.hasVideo = hasVideo; }
        public boolean isHasAudio() { return hasAudio; }
        public void setHasAudio(boolean hasAudio) { this.hasAudio = hasAudio; }
    }

    // ========================= 核心功能：提取RTHK完整新闻列表 =========================
    /**
     * 提取RTHK完整新闻列表（标题+链接+发布时间+视频/音频标识）
     * @return 结构化新闻列表
     */
    public List<NewsItem> extractCompleteNewsList() {
        List<NewsItem> newsList = new ArrayList<>();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            WebDriver driver = null;
            try {
                log.info("开始提取RTHK完整新闻列表（第{}次尝试），目标URL：{}", retryCount + 1, RTHK_TARGET_URL);

                // 配置Chrome浏览器（适配Linux/Render无头模式）
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless=new");          // 无头模式（无界面）
                options.addArguments("--no-sandbox");            // 解决Linux权限问题
                options.addArguments("--disable-dev-shm-usage"); // 解决内存不足问题
                options.addArguments("--disable-gpu");           // 禁用GPU（Linux无GPU）
                options.addArguments("--remote-allow-origins=*");// 跨域允许
                options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"); // 模拟真实浏览器
                options.addArguments("--lang=zh-HK");            // 设置繁体中文环境

                // 初始化WebDriver
                driver = new ChromeDriver(options);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(40)); // 页面加载超时
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));  // 元素查找超时
                driver.get(RTHK_TARGET_URL);

                // 修正：强制UTF-8编码解析页面
                String pageSource = driver.getPageSource();
                byte[] utf8Bytes = pageSource.getBytes(StandardCharsets.UTF_8);
                String utf8PageSource = new String(utf8Bytes, StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(utf8PageSource, RTHK_TARGET_URL, Parser.htmlParser());

                // 定位所有新闻行（匹配RTHK的.ns2-row）
                Elements newsRowElements = doc.select(".ns2-row");
                log.info("RTHK页面共匹配到 {} 条新闻行", newsRowElements.size());

                // 遍历每条新闻，提取完整信息
                for (Element row : newsRowElements) {
                    NewsItem news = new NewsItem();

                    // 1. 提取中文标题（h4.ns2-title > a > font[my=my]）
                    Element titleFont = row.selectFirst(".ns2-title a font[my=my]");
                    if (titleFont != null) {
                        news.setTitle(titleFont.text().trim());
                    }

                    // 2. 提取新闻链接（转换为绝对链接）
                    Element newsLink = row.selectFirst(".ns2-title a");
                    if (newsLink != null) {
                        String link = newsLink.attr("href").trim();
                        news.setLink(link.startsWith("http") ? link : "https://news.rthk.hk" + link);
                    }

                    // 3. 提取发布时间（.ns2-created > font[my=my]）
                    Element timeFont = row.selectFirst(".ns2-created font[my=my]");
                    if (timeFont != null) {
                        news.setPublishTime(timeFont.text().trim());
                    }

                    // 4. 判断是否包含视频/音频
                    Elements multimediaIcons = row.select(".multimediaIndicators img");
                    for (Element icon : multimediaIcons) {
                        String altText = icon.attr("alt").toLowerCase();
                        if (altText.contains("video")) {
                            news.setHasVideo(true);
                        } else if (altText.contains("audio")) {
                            news.setHasAudio(true);
                        }
                    }

                    // 过滤无效新闻（标题和链接非空才保留）
                    if (news.getTitle() != null && !news.getTitle().isEmpty()
                            && news.getLink() != null && !news.getLink().isEmpty()) {
                        newsList.add(news);
                        log.debug("提取到RTHK新闻：标题={}, 时间={}, 视频={}, 音频={}",
                                news.getTitle(), news.getPublishTime(), news.isHasVideo(), news.isHasAudio());
                    }
                }

                log.info("=== RTHK完整新闻列表提取完成 | 有效新闻数：{} ===", newsList.size());
                return newsList;

            } catch (Exception e) {
                retryCount++;
                log.error("第{}次提取RTHK新闻列表失败：{}", retryCount, e.getMessage());
                if (retryCount >= MAX_RETRIES) {
                    log.error("所有重试次数已用尽，返回空列表", e);
                    return new ArrayList<>();
                }
                // 重试前等待
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("重试等待被中断", ie);
                    return new ArrayList<>();
                }
            } finally {
                // 关闭浏览器（关键：避免内存泄漏）
                if (driver != null) {
                    driver.quit();
                }
            }
        }
        return new ArrayList<>();
    }

    // ========================= 保留功能：提取RTHK所有HTTP/HTTPS链接 =========================
    public List<String> extractAllLinks() {
        Set<String> allLinks = new HashSet<>();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            WebDriver driver = null;
            try {
                log.info("开始提取RTHK所有链接（第{}次尝试），目标URL：{}", retryCount + 1, RTHK_TARGET_URL);

                ChromeOptions options = buildChromeOptions();
                driver = new ChromeDriver(options);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
                driver.get(RTHK_TARGET_URL);

                String pageSource = new String(driver.getPageSource().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(pageSource, RTHK_TARGET_URL);

                // 从所有元素属性提取链接
                Elements allElements = doc.getAllElements();
                for (Element element : allElements) {
                    element.attributes().forEach(attribute -> {
                        String attrValue = attribute.getValue().trim();
                        Matcher matcher = HTTP_HTTPS_PATTERN.matcher(attrValue);
                        while (matcher.find()) {
                            allLinks.add(matcher.group().trim());
                        }
                    });
                }

                // 从文本节点提取链接
                extractLinksFromTextNodes(doc, allLinks);

                List<String> linkList = new ArrayList<>(allLinks);
                log.info("=== RTHK链接提取完成 | 总数：{} ===", linkList.size());
                return linkList;

            } catch (Exception e) {
                retryCount++;
                log.error("第{}次提取RTHK链接失败：{}", retryCount, e.getMessage());
                if (retryCount >= MAX_RETRIES) {
                    log.error("所有重试次数已用尽，返回空列表", e);
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

    // ========================= 保留功能：仅提取RTHK中文标题 =========================
    public List<String> extractChineseTitles() {
        List<String> chineseTitles = new ArrayList<>();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            WebDriver driver = null;
            try {
                log.info("开始提取RTHK中文标题（第{}次尝试），目标URL：{}", retryCount + 1, RTHK_TARGET_URL);

                ChromeOptions options = buildChromeOptions();
                driver = new ChromeDriver(options);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(40));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
                driver.get(RTHK_TARGET_URL);

                // 强制UTF-8编码解析
                String pageSource = driver.getPageSource();
                byte[] utf8Bytes = pageSource.getBytes(StandardCharsets.UTF_8);
                String utf8PageSource = new String(utf8Bytes, StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(utf8PageSource, RTHK_TARGET_URL, Parser.htmlParser());

                // 匹配RTHK所有标题元素
                Elements titleElements = doc.select(
                        ".ns2-title a font[my=my], h1, h2, h3, h4, " +
                        ".sptab-title, .detailNewsSlideTitleText, .itemTitle, .title, .headline"
                );

                log.info("RTHK共匹配到 {} 个标题元素", titleElements.size());

                // 提取并过滤中文标题
                for (Element element : titleElements) {
                    String rawText = element.text().trim();
                    if (!rawText.isEmpty() && containsChinese(rawText)) {
                        String cleanText = rawText.replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
                        chineseTitles.add(cleanText);
                    }
                }

                // 去重
                chineseTitles = chineseTitles.stream().distinct().collect(Collectors.toList());
                log.info("=== RTHK中文标题提取完成 | 总数：{} ===", chineseTitles.size());
                return chineseTitles;

            } catch (Exception e) {
                retryCount++;
                log.error("第{}次提取RTHK标题失败：{}", retryCount, e.getMessage());
                if (retryCount >= MAX_RETRIES) {
                    log.error("所有重试次数已用尽，返回空列表", e);
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

    // ========================= 辅助方法 =========================
    /**
     * 构建Chrome配置（复用，减少冗余）
     */
    private ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        options.addArguments("--lang=zh-HK");
        return options;
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
                links.add(link);
            }
        }
        // 递归遍历子节点
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
}