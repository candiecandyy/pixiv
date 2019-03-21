package program;

import java.util.concurrent.*;

/**
 * Created by admin on 2018/12/3.
 */
public class MainClass {

    public static void main(String[] args) {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(20);
        ExecutorService ex = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(1);
        ex.execute(new Producer(queue, latch));

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 5; i++) {
            ex.execute(new Consumer(queue));
        }

        ex.shutdown();
    }
}
