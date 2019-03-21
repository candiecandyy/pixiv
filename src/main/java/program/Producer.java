package program;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.net.URLDecoder;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by admin on 2018/12/3.
 */
public class Producer implements Runnable {

    private BlockingQueue<String> queue;
    private CountDownLatch latch;
    private static String username;
    private static String password;
    private static WebDriver driver;
    private static int pages;
    private static String headless;
    private static String keyword;
    public static volatile boolean isFinish = false;

    static {
        String basePath = Producer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            basePath = URLDecoder.decode(basePath, "utf-8");
            basePath = basePath.substring(0, basePath.lastIndexOf("/"));
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(basePath + "/settings.properties"));
            Properties p = new Properties();
            p.load(new InputStreamReader(bis, "GBK"));
            username = p.getProperty("username");
            password = p.getProperty("password");
            pages = Integer.valueOf(p.getProperty("pages"));
            keyword = p.getProperty("keyword");
            headless = p.getProperty("headless");
            System.setProperty("webdriver.chrome.driver", basePath.substring(1) + "/chromedriver.exe");
            if ("false".equals(headless)) {
                driver = new ChromeDriver();
            } else {
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--headless");
                driver = new ChromeDriver(chromeOptions);
            }
            driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            driver.quit();
        }
    }

    public Producer(BlockingQueue<String> queue, CountDownLatch latch) {
        this.queue = queue;
        this.latch = latch;
    }

    @Override
    public void run() {
        driver.get("https://accounts.pixiv.net/login?lang=zh&source=pc&view_type=page&ref=wwwtop_accounts_index");
        driver.findElements(By.xpath("//input[@autocomplete='username']")).get(0).sendKeys(username);
        driver.findElements(By.xpath("//input[@autocomplete='current-password']")).get(0).sendKeys(password);
        driver.findElements(By.xpath("//button[@class='signup-form__submit']")).get(1).click();
        driver.findElement(By.id("suggest-input")).sendKeys(keyword);
        driver.findElement(By.xpath("//form[@id='suggest-container']/input[@class='submit sprites-search-old']")).click();
        for (int j = 0; j < pages; j++) {
            List<WebElement> elements = driver.findElements(By.xpath("//section[@id='js-react-search-mid']/div/div/figure/div/a"));
            for (int i = 0; i < elements.size(); i++) {
                try {
                    elements.get(i).click();
                    WebElement a = driver.findElement(By.xpath("//div[@role='presentation']/a"));
                    String href = a.getAttribute("href");
                    if (href.matches(".*id=\\d+")) {
                        a.click();
                        String currentHandle = driver.getWindowHandle();
                        for (String handle : driver.getWindowHandles()) {
                            if (!currentHandle.equals(handle)) {
                                driver.switchTo().window(handle);
                                break;
                            }
                        }
                        try {
                            List<WebElement> webElements = driver.findElements(By.xpath("//div[@class='item-container']/img"));
                            for (WebElement img : webElements) {
                                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(false);", img);
                                TimeUnit.SECONDS.sleep(2);
                                String src = img.getAttribute("src");
                                try {
                                    queue.put(src + "系列");
                                    if (queue.size() >= 1 && latch.getCount() > 0) {
                                        latch.countDown();
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }catch (Exception ignored){}
                        driver.close();
                        driver.switchTo().window(currentHandle);
                    } else {
                        WebElement img = driver.findElement(By.xpath("//div[@role='presentation']/a/img"));
                        String src = img.getAttribute("src");
                        try {
                            queue.put(src);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception ignored) {
                }
                try {
                    driver.navigate().back();
                } catch (Exception e) {
                    driver.get("https://accounts.pixiv.net/login?lang=zh&source=pc&view_type=page&ref=wwwtop_accounts_index");
                    driver.findElements(By.xpath("//input[@autocomplete='username']")).get(0).sendKeys(username);
                    driver.findElements(By.xpath("//input[@autocomplete='current-password']")).get(0).sendKeys(password);
                    driver.findElements(By.xpath("//button[@class='signup-form__submit']")).get(1).click();
                    driver.findElement(By.id("suggest-input")).sendKeys(keyword);
                    driver.findElement(By.xpath("//form[@id='suggest-container']/input[@class='submit sprites-search-old']")).click();
                    for (int n = 0; n <= j; n++) {
                        driver.findElement(By.xpath("//span[@class='next']")).click();
                    }
                }

                elements = driver.findElements(By.xpath("//section[@id='js-react-search-mid']/div/div/figure/div/a"));
                if (queue.size() >= 1 && latch.getCount() > 0) {
                    latch.countDown();
                }
            }
            driver.findElement(By.xpath("//span[@class='next']")).click();
        }
        if (latch.getCount() > 0) {
            latch.countDown();
        }
        driver.quit();
        isFinish = true;
    }
}
