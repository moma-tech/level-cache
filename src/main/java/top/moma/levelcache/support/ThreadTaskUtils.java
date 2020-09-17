package top.moma.levelcache.support;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * ThreadTaskUtils
 *
 * <p>线程池
 *
 * @version 1.0
 * @author Created by ivan at 2020/7/16.
 */
public class ThreadTaskUtils {
  private static ThreadPoolTaskExecutor taskExecutor = null;

  static {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // 配置核心线程数
    executor.setCorePoolSize(5);
    // 配置最大线程数
    executor.setMaxPoolSize(5);
    // 配置队列大小
    executor.setQueueCapacity(100);
    // 配置线程池中的线程的名称前缀
    executor.setThreadNamePrefix("moma-cache-");
    // rejection-policy：当pool已经达到max size的时候，如何处理新任务
    // CALLER_RUNS：不在新线程中执行任务，而是有调用者所在的线程来执行
    // DiscardPolicy：丢弃任务，不抛出异常。
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
    // 执行初始化
    executor.initialize();
  }

  public static void run(Runnable runnable) {
    taskExecutor.execute(runnable);
  }
}
