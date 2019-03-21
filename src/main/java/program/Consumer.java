package program;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by admin on 2018/12/3.
 */
public class Consumer implements Runnable {

    private BlockingQueue<String> queue;
    private static String path;

    static {
        String basePath = Consumer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            basePath = URLDecoder.decode(basePath, "utf-8");
            basePath = basePath.substring(0, basePath.lastIndexOf("/"));
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(basePath + "/settings.properties"));
            Properties p = new Properties();
            p.load(new InputStreamReader(bis, "GBK"));
            path = p.getProperty("path");
            path = URLDecoder.decode(path, "utf-8");
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Consumer(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String src = queue.poll(3, TimeUnit.SECONDS);
                if (src == null && Producer.isFinish) {
                    break;
                }
                if (src != null) {
                    File dir = null;
                    if (src.contains("系列")) {
                        src = src.replace("系列", "");
                        String pathName = src.substring(src.lastIndexOf("/") + 1);
                        pathName = pathName.substring(0, pathName.indexOf("_"));
                        dir = new File(path, pathName);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                    }
                    System.out.println(src);
                    URL url = new URL(src);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
                    conn.setRequestProperty("Referer", "https://www.pixiv.net/");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(60000);
                    String filename = src.substring(src.lastIndexOf("/") + 1);
                    InputStream is = conn.getInputStream();
                    File file;
                    if (dir == null) {
                        file = new File(path, filename);
                    } else {
                        file = new File(dir, filename);
                    }

                    FileUtils.copyInputStreamToFile(is, file);
                    is.close();
                    conn.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
