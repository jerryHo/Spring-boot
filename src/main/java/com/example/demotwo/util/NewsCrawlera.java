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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RTHK新闻爬虫（精准匹配指定HTML结构：.ns2-page -> .ns2-page-inner -> .ns2-row）
 * 解析结构：
 * <div class="ns2-page">
 *   <div class="ns2-page-inner">
 *     <div class="ns2-row"> -> 单条新闻容器
 *       <h4 class="ns2-title"><a href="..."><font my="my">标题</font></a></h4>
 *       <div class="ns2-created"><font my="my">发布时间</font></div>
 *     </div>
 *   </div>
 * </div>
 */
@Component // Spring Bean注解，支持@Autowired注入
public class NewsCrawlera {

  // ===================== 核心配置 =====================
  private static final Logger log = LoggerFactory.getLogger(NewsCrawlera.class);
  // RTHK最新新闻页面URL
  private static final String RTHK_TARGET_URL =
    "https://news.rthk.hk/rthk/ch/latest-news.htm";
  // HTTP请求超时时间
  private static final int HTTP_TIMEOUT_SECONDS = 20;
  // 最大重试次数
  private static final int MAX_RETRIES = 3;
  // 重试间隔（毫秒）
  private static final long RETRY_DELAY_MS = 2000;
  // 匹配HTTP/HTTPS链接的正则
  private static final Pattern HTTP_HTTPS_PATTERN = Pattern.compile(
    "https?:\\/\\/[^\\s\"']+",
    Pattern.UNICODE_CASE
  );

  // 全局HttpClient（复用连接，提升性能）
  private static final HttpClient HTTP_CLIENT = HttpClient
    .newBuilder()
    .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
    .followRedirects(HttpClient.Redirect.NORMAL) // 自动跟随重定向
    .build();

  /**
   * 新闻实体类（精准匹配目标HTML的字段）
   */
  public static class NewsItem {

    private String title; // 新闻标题（如：印度果阿邦夜總會火警25死　莫迪稱將金錢賠償死者家屬和傷者）
    private String link; // 新闻链接（如：https://news.rthk.hk/...）
    private String publishTime; // 发布时间（如：2025-12-07 HKT 16:20）
    private boolean hasVideo; // 是否包含视频（示例中无，代码兼容）
    private boolean hasAudio; // 是否包含音频（示例中无，代码兼容）

    // Getter & Setter
    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getLink() {
      return link;
    }

    public void setLink(String link) {
      this.link = link;
    }

    public String getPublishTime() {
      return publishTime;
    }

    public void setPublishTime(String publishTime) {
      this.publishTime = publishTime;
    }

    public boolean isHasVideo() {
      return hasVideo;
    }

    public void setHasVideo(boolean hasVideo) {
      this.hasVideo = hasVideo;
    }

    public boolean isHasAudio() {
      return hasAudio;
    }

    public void setHasAudio(boolean hasAudio) {
      this.hasAudio = hasAudio;
    }

    // 重写toString，方便打印查看
    @Override
    public String toString() {
      return (
        "NewsItem{" +
        "标题='" +
        title +
        '\'' +
        ", 链接='" +
        link +
        '\'' +
        ", 发布时间='" +
        publishTime +
        '\'' +
        ", 含视频=" +
        hasVideo +
        ", 含音频=" +
        hasAudio +
        '}'
      );
    }
  }

  // ===================== 核心方法：提取完整新闻列表（精准解析目标结构） =====================
  /**
   * 提取RTHK页面所有新闻（精准匹配.ns2-page -> .ns2-page-inner -> .ns2-row结构）
   * @return 解析后的新闻列表（空列表=失败）
   */
  public List<NewsItem> extractCompleteNewsList() {
    List<NewsItem> newsList = new ArrayList<>();
    int retryCount = 0;

    while (retryCount < MAX_RETRIES) {
      try {
        log.info(
          "开始提取RTHK新闻（第{}次尝试），目标URL：{}",
          retryCount + 1,
          RTHK_TARGET_URL
        );

        // 1. 构建反爬HTTP请求
        HttpRequest request = buildRthkRequest();

        // 2. 发送请求并获取响应
        HttpResponse<String> response = HTTP_CLIENT.send(
          request,
          HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8) // 强制UTF-8解码
        );

        // 3. 验证响应有效性
        if (!validateResponse(response)) {
          retryCount++;
          log.warn(
            "第{}次请求响应无效，{}ms后重试",
            retryCount,
            RETRY_DELAY_MS
          );
          TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
          continue;
        }

        // 4. 处理HTML编码（双重确保UTF-8，解决中文乱码）
        String rawHtml = response.body();
        byte[] utf8Bytes = rawHtml.getBytes(StandardCharsets.UTF_8);
        String utf8Html = new String(utf8Bytes, StandardCharsets.UTF_8);

        // 5. 解析HTML（精准匹配目标结构）
        Document doc = Jsoup.parse(
          utf8Html,
          RTHK_TARGET_URL,
          Parser.htmlParser()
        );

        // ========== 核心：精准定位单条新闻容器 ==========
        // 匹配层级：.ns2-page -> .ns2-page-inner -> .ns2-row（包含ns2-first/ns2-odd等子类）
        Elements newsRowList = doc.select(".ns2-page .ns2-page-inner .ns2-row");
        log.info("匹配到新闻容器数量：{}条", newsRowList.size());

        // 6. 遍历解析每条新闻
        for (Element newsRow : newsRowList) {
          NewsItem news = parseSingleNews(newsRow);
          // 过滤无效新闻（标题/链接非空）
          if (
            news.getTitle() != null &&
            !news.getTitle().isEmpty() &&
            news.getLink() != null &&
            !news.getLink().isEmpty()
          ) {
            newsList.add(news);
            log.debug("解析到有效新闻：{}", news);
          } else {
            log.warn("跳过无效新闻（标题/链接为空）：{}", news);
          }
        }

        // 7. 打印最终结果（日志）
        log.info("RTHK新闻解析完成 | 有效新闻数：{}", newsList.size());
        for (int i = 0; i < newsList.size(); i++) {
          log.info("新闻 {}: {}", i + 1, newsList.get(i));
        }

        return newsList;
      } catch (InterruptedException e) {
        // 线程中断处理（规范）
        Thread.currentThread().interrupt();
        log.error("爬虫线程被中断", e);
        return new ArrayList<>();
      } catch (Exception e) {
        // 通用异常处理
        retryCount++;
        log.error("第{}次解析失败：{}", retryCount, e.getMessage(), e);
        if (retryCount >= MAX_RETRIES) {
          log.error("所有重试次数用尽，返回空列表");
          return new ArrayList<>();
        }
        // 重试前等待
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

  // ===================== 辅助方法：验证HTTP响应 =====================
  // ========== 关键修改1：放宽响应验证逻辑 ==========
  private boolean validateResponse(HttpResponse<String> response) {
    // 1. 验证状态码（必须为200）
    int statusCode = response.statusCode();
    if (statusCode != 200) {
      log.error("HTTP状态码异常：{}（预期200）", statusCode);
      // 打印响应头（调试）
      log.error("响应头：{}", response.headers().toString());
      return false;
    }

    // 2. 验证响应体非空
    String body = response.body();
    if (body == null || body.isEmpty()) {
      log.error("HTTP响应体为空");
      return false;
    }

    // ========== 关键：放宽关键词校验，仅保留基础校验 + 打印响应体预览 ==========
    log.warn("=== RTHK响应体预览（前1000字符）===");
    String preview = body.length() > 1000 ? body.substring(0, 1000) : body;
    log.warn(preview); // 打印预览，排查实际返回的内容

    // 仅校验是否为HTML页面，移除严格的RTHK关键词校验
    if (!body.contains("<html") || !body.contains("<body")) {
      log.error("响应体非HTML页面");
      return false;
    }

    return true;
  }

  // ========== 关键修改2：增强反爬请求头 ==========
  private HttpRequest buildRthkRequest() {
    return HttpRequest
      .newBuilder()
      .uri(URI.create(RTHK_TARGET_URL))
      // 补充更多浏览器请求头
      .header(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
      )
      .header(
        "Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
      )
      .header("Accept-Language", "zh-HK,zh;q=0.9,en-US;q=0.8,en;q=0.7")
      .header("Accept-Encoding", "gzip, deflate, br")
      .header("Referer", "https://www.rthk.hk/") // 升级为官网首页
      .header("Cache-Control", "max-age=0")
      .header("Upgrade-Insecure-Requests", "1")
      .header("Sec-Fetch-Dest", "document")
      .header("Sec-Fetch-Mode", "navigate")
      .header("Sec-Fetch-Site", "same-origin")
      .header("Sec-Fetch-User", "?1")
      .header("Connection", "keep-alive")
      // 模拟Cookie（关键：绕过基础反爬）
      .header(
        "Cookie",
        "rthk_lang=zh-HK; _ga=GA1.1.1234567890.1733568000; _gid=GA1.1.0987654321.1733568000"
      )
      .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
      .GET()
      .build();
  }

  // ========== 关键修改3：解析时兼容更多结构 ==========
  private NewsItem parseSingleNews(Element newsRow) {
    NewsItem news = new NewsItem();
    try {
      // 兼容无font[my=my]的情况，直接提取a标签文本
      Element titleA = newsRow.selectFirst("h4.ns2-title a");
      if (titleA != null) {
        news.setTitle(titleA.text().trim()); // 直接取a标签文本
        String relativeLink = titleA.attr("href").trim();
        news.setLink(
          relativeLink.startsWith("http")
            ? relativeLink
            : "https://news.rthk.hk" + relativeLink
        );
      }

      // 兼容无font[my=my]的时间提取
      Element timeDiv = newsRow.selectFirst(".ns2-created");
      if (timeDiv != null) {
        news.setPublishTime(timeDiv.text().trim());
      }

      // 多媒体标识
      Elements multimediaIcons = newsRow.select(".multimediaIndicators img");
      for (Element icon : multimediaIcons) {
        String altText = icon.attr("alt").toLowerCase().trim();
        if (altText.contains("video")) news.setHasVideo(true);
        if (altText.contains("audio")) news.setHasAudio(true);
      }
    } catch (Exception e) {
      log.error("解析单条新闻失败", e);
    }
    return news;
  }

  // ===================== 保留功能：提取所有链接（兼容原有调用） =====================
  public List<String> extractAllLinks() {
    List<NewsItem> newsList = extractCompleteNewsList();
    Set<String> linkSet = new HashSet<>(); // 去重

    // 提取新闻链接
    for (NewsItem news : newsList) {
      if (news.getLink() != null && !news.getLink().isEmpty()) {
        linkSet.add(news.getLink());
      }
    }

    // 额外提取页面中所有HTTP/HTTPS链接（可选）
    try {
      HttpRequest request = buildRthkRequest();
      HttpResponse<String> response = HTTP_CLIENT.send(
        request,
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
      );
      if (validateResponse(response)) {
        String html = new String(
          response.body().getBytes(StandardCharsets.UTF_8),
          StandardCharsets.UTF_8
        );
        Document doc = Jsoup.parse(html);
        Elements allElements = doc.getAllElements();
        for (Element element : allElements) {
          element
            .attributes()
            .forEach(attr -> {
              Matcher matcher = HTTP_HTTPS_PATTERN.matcher(attr.getValue());
              while (matcher.find()) {
                linkSet.add(matcher.group().trim());
              }
            });
        }
      }
    } catch (Exception e) {
      log.error("提取所有链接失败", e);
    }

    List<String> linkList = new ArrayList<>(linkSet);
    log.info("提取到所有链接（去重后）：{}条", linkList.size());
    return linkList;
  }

  // ===================== 保留功能：提取所有中文标题（兼容原有调用） =====================
  public List<String> extractChineseTitles() {
    List<NewsItem> newsList = extractCompleteNewsList();
    List<String> titleList = newsList
      .stream()
      .map(NewsItem::getTitle)
      .filter(title ->
        title != null && !title.isEmpty() && containsChinese(title)
      )
      .distinct() // 去重
      .collect(Collectors.toList());

    log.info("提取到中文标题（去重后）：{}条", titleList.size());
    return titleList;
  }

  // ===================== 辅助方法：判断文本是否含中文 =====================
  private boolean containsChinese(String text) {
    if (text == null || text.isEmpty()) return false;
    // 匹配中文汉字、标点
    return Pattern
      .compile("[\\u4E00-\\u9FA5\\u3000-\\u303F\\uFF00-\\uFFEF]")
      .matcher(text)
      .find();
  }

  // ===================== 测试方法：直接运行验证 =====================
  public static void main(String[] args) {
    // 1. 初始化爬虫
    NewsCrawlera crawler = new NewsCrawlera();

    // 2. 测试解析完整新闻列表
    log.info("===== 开始测试RTHK新闻爬虫 =====");
    List<NewsItem> newsList = crawler.extractCompleteNewsList();

    // 3. 打印测试结果
    if (!newsList.isEmpty()) {
      System.out.println("\n===== 解析结果（前5条） =====");
      for (int i = 0; i < Math.min(newsList.size(), 5); i++) {
        NewsItem item = newsList.get(i);
        System.out.println("[" + (i + 1) + "]");
        System.out.println("标题：" + item.getTitle());
        System.out.println("链接：" + item.getLink());
        System.out.println("时间：" + item.getPublishTime());
        System.out.println("含视频：" + item.isHasVideo());
        System.out.println("含音频：" + item.isHasAudio());
        System.out.println("------------------------");
      }
    } else {
      System.out.println("解析失败，未获取到有效新闻");
    }

    // 4. 测试提取中文标题
    List<String> titles = crawler.extractChineseTitles();
    System.out.println("\n===== 提取的中文标题 =====");
    titles.forEach(title -> System.out.println("- " + title));

    // 5. 测试提取所有链接
    List<String> links = crawler.extractAllLinks();
    System.out.println("\n===== 提取的所有链接（前5条） =====");
    for (int i = 0; i < Math.min(links.size(), 5); i++) {
      System.out.println("- " + links.get(i));
    }
  }
}
