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
 * 香港01新闻爬虫（修复无结果+内存优化+反爬适配+补充缺失方法）
 * 新增：extractAllLinks()、extractChineseTitles() 兼容原有调用
 */
public class NewsCrawlera {
    // ===================== 核心配置（可根据实际情况调整） =====================
    private static final String TARGET_URL = "https://www.hk01.com/";
    private static final int HTTP_TIMEOUT_SECONDS = 15;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;
    private static final int MAX_NEWS_COUNT = 20;

    // 多选择器兼容
    private static final String[] NEWS_ITEM_SELECTORS = {
        "div[data-testid='content-card']",
        "div.card-item",
        "article[data-type='news']",
        "div[class*='content-card']",
        "div[class^='news-item']"
    };

    // 动态User-Agent池
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"
    };

    // 真实Cookie（需替换为自己的）
    private static final String COOKIE = "HK01_LANG=zh-HK; _ga=GA1.1.123456789.1735000000; _gid=GA1.1.987654321.1735000000; accept_cookie=1; HK01_SESSION=xxx";

    // ===================== 单例与日志 =====================
    private static final Logger log = LoggerFactory.getLogger(NewsCrawlera.class);
    
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // ===================== 核心方法（原有） =====================
    public List<NewsItem> extractCompleteNewsList() {
        List<NewsItem> newsList = new ArrayList<>(MAX_NEWS_COUNT);
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                HttpRequest request = buildAntiCrawlRequest();
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                
                if (!validateResponse(response)) {
                    retryCount++;
                    log.warn("第{}次重试：响应状态异常", retryCount);
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    continue;
                }

                String responseBody = response.body();
                String newsContainerHtml = extractNewsContainerHtml(responseBody);
                Document doc = parseMinimalDom(newsContainerHtml);
                responseBody = null; // 释放内存

                Elements newsElements = matchNewsElements(doc);
                if (newsElements.isEmpty()) {
                    retryCount++;
                    log.warn("第{}次重试：未匹配到新闻元素", retryCount);
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    continue;
                }

                parseNewsItems(newsElements, newsList);
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

        log.info("最终爬取到 {} 条有效新闻", newsList.size());
        return newsList;
    }

    // ===================== 补充缺失方法（解决编译错误） =====================
    /**
     * 提取所有新闻链接（兼容原有调用）
     * @return 新闻链接列表
     */
    public List<String> extractAllLinks() {
        List<NewsItem> newsList = extractCompleteNewsList();
        List<String> linkList = new ArrayList<>();
        for (NewsItem news : newsList) {
            if (news.getNewsUrl() != null && !news.getNewsUrl().isEmpty()) {
                linkList.add(news.getNewsUrl());
            }
        }
        log.info("提取到 {} 条有效新闻链接", linkList.size());
        return linkList;
    }

    /**
     * 提取所有中文标题（兼容原有调用）
     * @return 中文标题列表
     */
    public List<String> extractChineseTitles() {
        List<NewsItem> newsList = extractCompleteNewsList();
        List<String> titleList = new ArrayList<>();
        for (NewsItem news : newsList) {
            if (news.getTitle() != null && !news.getTitle().isEmpty()) {
                titleList.add(news.getTitle());
            }
        }
        log.info("提取到 {} 条有效中文标题", titleList.size());
        return titleList;
    }

    // ===================== 辅助方法（原有） =====================
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
                .header("DNT", "1")
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .GET()
                .build();
    }

    private String getRandomUserAgent() {
        Random random = new Random();
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }

    private boolean validateResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        log.info("HTTP响应状态码：{}", statusCode);
        
        if (statusCode != 200) {
            String shortBody = response.body().length() > 500 
                    ? response.body().substring(0, 500) 
                    : response.body();
            log.error("请求被拦截，状态码：{}，响应内容：{}", statusCode, shortBody);
            return false;
        }
        
        if (response.body().isEmpty()) {
            log.error("响应体为空");
            return false;
        }
        return true;
    }

    private String extractNewsContainerHtml(String fullHtml) {
        String startTag = "<div class='news-list-container'>";
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

    private Document parseMinimalDom(String html) {
        return Jsoup.parse(html, TARGET_URL, Parser.htmlParser())
                .outputSettings(new Document.OutputSettings()
                        .charset(StandardCharsets.UTF_8)
                        .prettyPrint(false)
                        .outline(false)
                        .syntax(Document.OutputSettings.Syntax.html));
    }

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

    private void parseNewsItems(Elements newsElements, List<NewsItem> newsList) {
        int count = 0;
        for (Element card : newsElements) {
            if (count >= MAX_NEWS_COUNT) {
                break;
            }

            try {
                NewsItem news = parseSingleNews(card);
                if (isValidNews(news)) {
                    newsList.add(news);
                    count++;
                }
            } catch (Exception e) {
                log.error("解析单条新闻失败", e);
            } finally {
                card = null; // 释放引用
            }
        }
    }

    private NewsItem parseSingleNews(Element card) {
        NewsItem news = new NewsItem();
        
        Element titleEl = card.selectFirst("h3,a[class*='title'],div[class*='title']");
        if (titleEl != null) {
            news.setTitle(titleEl.text().trim());
            if (titleEl.tagName().equals("a")) {
                news.setNewsUrl(absolutizeUrl(titleEl.attr("href")));
            } else {
                Element linkEl = card.selectFirst("a");
                if (linkEl != null) {
                    news.setNewsUrl(absolutizeUrl(linkEl.attr("href")));
                }
            }
        }

        Element categoryEl = card.selectFirst("span[class*='category'],div[class*='category']");
        if (categoryEl != null) {
            news.setCategory(categoryEl.text().trim());
        }

        Element timeEl = card.selectFirst("time,span[class*='time'],div[class*='time']");
        if (timeEl != null) {
            String timeText = timeEl.text().trim();
            news.setPublishTimeFormatted(timeText);
        }

        Element imgEl = card.selectFirst("img");
        if (imgEl != null) {
            news.setImageUrl(absolutizeUrl(imgEl.attr("src")));
        }

        return news;
    }

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

    private boolean isValidNews(NewsItem news) {
        return news.getTitle() != null && !news.getTitle().isEmpty()
                && news.getNewsUrl() != null && !news.getNewsUrl().isEmpty();
    }

    // ===================== 内部类：新闻实体 =====================
    public static class NewsItem {
        private String title;
        private String category;
        private String publishTimeFormatted;
        private String newsUrl;
        private String imageUrl;

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

    // ===================== 测试方法 =====================
    public static void main(String[] args) {
        NewsCrawlera crawler = new NewsCrawlera();
        
        // 测试补充的方法
        List<String> links = crawler.extractAllLinks();
        List<String> titles = crawler.extractChineseTitles();
        
        System.out.println("===== 新闻链接 =====");
        links.forEach(System.out::println);
        
        System.out.println("\n===== 新闻标题 =====");
        titles.forEach(System.out::println);
    }
}