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
import org.jsoup.select.Elements;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 香港01新闻爬虫（修复无结果+内存优化+反爬适配）
 * 核心优化：移除HttpClient.close()、多选择器兼容、内存占用控制、反爬绕过
 */
public class NewsCrawlera {
    // ===================== 核心配置（可根据实际情况调整） =====================
    // 目标爬取URL（香港01新闻首页，可替换为具体分类页）
    private static final String TARGET_URL = "https://www.hk01.com/";
    // HTTP请求超时时间（秒）
    private static final int HTTP_TIMEOUT_SECONDS = 15;
    // 最大重试次数
    private static final int MAX_RETRIES = 3;
    // 重试间隔（毫秒）
    private static final int RETRY_DELAY_MS = 2000;
    // 最大新闻条数（限制结果数量，减少内存占用）
    private static final int MAX_NEWS_COUNT = 20;

    // 多选择器兼容（避免单一选择器失效，根据页面结构动态调整）
    private static final String[] NEWS_ITEM_SELECTORS = {
        "div[data-testid='content-card']",
        "div.card-item",
        "article[data-type='news']",
        "div[class*='content-card']",
        "div[class^='news-item']"
    };

    // 动态User-Agent池（降低反爬概率）
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"
    };

    // 真实Cookie（从浏览器复制，有效期约1周，需定期更新）
    // 复制方式：浏览器F12 → Network → 刷新hk01 → 找请求头的Cookie值粘贴
    private static final String COOKIE = "HK01_LANG=zh-HK; _ga=GA1.1.123456789.1735000000; _gid=GA1.1.987654321.1735000000; accept_cookie=1; HK01_SESSION=xxx";

    // ===================== 单例与日志 =====================
    private static final Logger log = LoggerFactory.getLogger(NewsCrawlera.class);
    
    // HttpClient单例（线程安全，无需close()，全局复用）
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)  // 跟随重定向
            .build();

    // ===================== 核心方法 =====================
    /**
     * 爬取完整新闻列表（核心方法）
     * @return 有效新闻列表（最多MAX_NEWS_COUNT条）
     */
    public List<NewsItem> extractCompleteNewsList() {
        List<NewsItem> newsList = new ArrayList<>(MAX_NEWS_COUNT);
        int retryCount = 0;

        // 重试机制
        while (retryCount < MAX_RETRIES) {
            try {
                // 1. 构建反爬请求头
                HttpRequest request = buildAntiCrawlRequest();
                
                // 2. 发送请求并获取响应
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                
                // 校验响应状态
                if (!validateResponse(response)) {
                    retryCount++;
                    log.warn("第{}次重试：响应状态异常", retryCount);
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    continue;
                }

                // 3. 内存优化：只解析新闻容器部分DOM（丢弃无关内容）
                String responseBody = response.body();
                String newsContainerHtml = extractNewsContainerHtml(responseBody);
                Document doc = parseMinimalDom(newsContainerHtml);
                
                // 释放大字符串内存
                responseBody = null;

                // 4. 多选择器匹配新闻元素（避免选择器失效）
                Elements newsElements = matchNewsElements(doc);
                if (newsElements.isEmpty()) {
                    retryCount++;
                    log.warn("第{}次重试：未匹配到新闻元素", retryCount);
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    continue;
                }

                // 5. 解析新闻（逐个处理，减少内存占用）
                parseNewsItems(newsElements, newsList);
                
                // 爬取成功，跳出重试
                break;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("爬虫线程被中断", e);
                return Collections.emptyList();
            } catch (Exception e) {
                retryCount++;
                log.error("第{}次爬取异常", retryCount, e);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Collections.emptyList();
                }
            }
        }

        // 日志输出结果
        log.info("最终爬取到 {} 条有效新闻", newsList.size());
        return newsList;
    }

    // ===================== 辅助方法（反爬+内存优化） =====================
    /**
     * 构建反爬请求（动态UA、Cookie、完整请求头）
     */
    private HttpRequest buildAntiCrawlRequest() {
        return HttpRequest.newBuilder()
                .uri(URI.create(TARGET_URL))
                .header("User-Agent", getRandomUserAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Accept-Language", "zh-HK,zh;q=0.9,en;q=0.8")
                .header("Referer", "https://www.hk01.com/")
                .header("Cookie", COOKIE)
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("DNT", "1")  // 拒绝追踪
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .GET()
                .build();
    }

    /**
     * 获取随机User-Agent
     */
    private String getRandomUserAgent() {
        Random random = new Random();
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }

    /**
     * 验证响应有效性
     */
    private boolean validateResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        log.info("HTTP响应状态码：{}", statusCode);
        
        // 200为正常，其他状态码（403/401/503）均为被拦截
        if (statusCode != 200) {
            String shortBody = response.body().length() > 500 
                    ? response.body().substring(0, 500) 
                    : response.body();
            log.error("请求被拦截，状态码：{}，响应内容：{}", statusCode, shortBody);
            return false;
        }
        
        // 校验响应体是否为空
        if (response.body().isEmpty()) {
            log.error("响应体为空");
            return false;
        }
        return true;
    }

    /**
     * 内存优化：只提取新闻容器部分HTML（减少DOM解析范围）
     */
    private String extractNewsContainerHtml(String fullHtml) {
        // 新闻列表父容器（根据hk01页面结构调整，可F12查看）
        String startTag = "<div class='news-list-container'>";  // 替换为实际父容器标签
        String endTag = "</div>";

        int startIndex = fullHtml.indexOf(startTag);
        if (startIndex == -1) {
            log.warn("未找到新闻父容器，解析完整页面");
            return fullHtml;
        }

        int endIndex = fullHtml.indexOf(endTag, startIndex + startTag.length());
        if (endIndex == -1) {
            return fullHtml;
        }

        return fullHtml.substring(startIndex, endIndex + endTag.length());
    }

    /**
     * 内存优化：最小化DOM解析（禁用格式化、大纲，减少内存占用）
     */
    private Document parseMinimalDom(String html) {
        return Jsoup.parse(html, TARGET_URL, Parser.htmlParser())
                .outputSettings(new Document.OutputSettings()
                        .charset(StandardCharsets.UTF_8)
                        .prettyPrint(false)  // 禁用格式化
                        .outline(false)      // 禁用大纲
                        .syntax(Document.OutputSettings.Syntax.html));
    }

    /**
     * 多选择器匹配新闻元素
     */
    private Elements matchNewsElements(Document doc) {
        Elements newsElements = new Elements();
        for (String selector : NEWS_ITEM_SELECTORS) {
            Elements elements = doc.select(selector);
            log.info("选择器 [{}] 匹配到 {} 个元素", selector, elements.size());
            if (!elements.isEmpty()) {
                newsElements = elements;
                break;
            }
        }
        return newsElements;
    }

    /**
     * 解析新闻条目（逐个处理，及时释放引用）
     */
    private void parseNewsItems(Elements newsElements, List<NewsItem> newsList) {
        int count = 0;
        for (Element card : newsElements) {
            // 限制最大条数，避免内存溢出
            if (count >= MAX_NEWS_COUNT) {
                break;
            }

            try {
                NewsItem news = parseSingleNews(card);
                // 验证新闻有效性
                if (isValidNews(news)) {
                    newsList.add(news);
                    count++;
                }
            } catch (Exception e) {
                log.error("解析单条新闻失败", e);
            } finally {
                // 手动置空，释放Element引用（加速GC）
                card = null;
            }
        }
    }

    /**
     * 解析单条新闻
     */
    private NewsItem parseSingleNews(Element card) {
        NewsItem news = new NewsItem();
        
        // 标题（多选择器兼容）
        Element titleEl = card.selectFirst("h3,a[class*='title'],div[class*='title']");
        if (titleEl != null) {
            news.setTitle(titleEl.text().trim());
            // 新闻链接
            if (titleEl.tagName().equals("a")) {
                news.setNewsUrl(absolutizeUrl(titleEl.attr("href")));
            } else {
                Element linkEl = card.selectFirst("a");
                if (linkEl != null) {
                    news.setNewsUrl(absolutizeUrl(linkEl.attr("href")));
                }
            }
        }

        // 分类
        Element categoryEl = card.selectFirst("span[class*='category'],div[class*='category']");
        if (categoryEl != null) {
            news.setCategory(categoryEl.text().trim());
        }

        // 发布时间
        Element timeEl = card.selectFirst("time,span[class*='time'],div[class*='time']");
        if (timeEl != null) {
            String timeText = timeEl.text().trim();
            // 格式化时间（根据hk01的时间格式调整，示例：2025-12-07 10:00）
            news.setPublishTimeFormatted(formatPublishTime(timeText));
        }

        // 图片链接
        Element imgEl = card.selectFirst("img");
        if (imgEl != null) {
            news.setImageUrl(absolutizeUrl(imgEl.attr("src")));
        }

        return news;
    }

    /**
     * 补全相对URL为绝对URL
     */
    private String absolutizeUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty() || relativeUrl.startsWith("http")) {
            return relativeUrl;
        }
        try {
            return URI.create(TARGET_URL).resolve(relativeUrl).toString();
        } catch (Exception e) {
            log.error("补全URL失败：{}", relativeUrl, e);
            return relativeUrl;
        }
    }

    /**
     * 格式化发布时间（根据实际返回格式调整）
     */
    private String formatPublishTime(String rawTime) {
        // 示例：rawTime = "12小時前" → 转换为当前时间偏移；或直接返回原始格式
        // 可根据hk01的时间格式自定义逻辑，这里先返回原始值
        return rawTime;
    }

    /**
     * 验证新闻有效性（过滤空数据）
     */
    private boolean isValidNews(NewsItem news) {
        return news.getTitle() != null && !news.getTitle().isEmpty()
                && news.getNewsUrl() != null && !news.getNewsUrl().isEmpty();
    }

    // ===================== 内部类：新闻实体（精简字段，减少内存） =====================
    public static class NewsItem {
        private String title;              // 新闻标题（必要）
        private String category;           // 新闻分类（必要）
        private String publishTimeFormatted; // 格式化发布时间（必要）
        private String newsUrl;            // 新闻链接（必要）
        private String imageUrl;           // 图片链接（可选）

        // Getter & Setter（精简，只保留必要字段）
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getPublishTimeFormatted() { return publishTimeFormatted; }
        public void setPublishTimeFormatted(String publishTimeFormatted) { this.publishTimeFormatted = publishTimeFormatted; }

        public String getNewsUrl() { return newsUrl; }
        public void setNewsUrl(String newsUrl) { this.newsUrl = newsUrl; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        @Override
        public String toString() {
            return "NewsItem{" +
                    "title='" + title + '\'' +
                    ", category='" + category + '\'' +
                    ", publishTimeFormatted='" + publishTimeFormatted + '\'' +
                    ", newsUrl='" + newsUrl + '\'' +
                    '}';
        }
    }

    // ===================== 测试方法（可选） =====================
    public static void main(String[] args) {
        NewsCrawlera crawler = new NewsCrawlera();
        List<NewsItem> newsList = crawler.extractCompleteNewsList();
        
        // 打印结果
        for (NewsItem news : newsList) {
            System.out.println("===== 新闻 =====");
            System.out.println("标题：" + news.getTitle());
            System.out.println("分类：" + news.getCategory());
            System.out.println("时间：" + news.getPublishTimeFormatted());
            System.out.println("链接：" + news.getNewsUrl());
            System.out.println("图片：" + news.getImageUrl());
            System.out.println("================\n");
        }
    }
}