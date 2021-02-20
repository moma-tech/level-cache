package top.moma.levelcache.support;

import top.moma.m64.core.helper.CollectionHelper;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * LocalThreadPool
 *
 * @author Created by ivan on 2020/7/13 .
 * @version 1.0
 */
public class LocalThreadPool {
  private final Map<String, Set<Thread>> threadConMap = new ConcurrentHashMap<>();

  /**
   * await
   *
   * @author Created by ivan at 下午3:06 2020/7/13.
   */
  public final void await(String key, long milliseconds) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    Set<Thread> threadSet = threadConMap.get(key);
    if (threadSet == null) {
      threadSet = new ConcurrentSkipListSet<>(Comparator.comparing(Thread::toString));
      threadConMap.put(key, threadSet);
    }
    threadSet.add(Thread.currentThread());
    LockSupport.parkNanos(this, TimeUnit.MILLISECONDS.toNanos(milliseconds));
  }

  /**
   * signalAll
   *
   * @author Created by ivan at 下午3:06 2020/7/13.
   */
  public final void signalAll(String key) {
    Set<Thread> threadSet = threadConMap.get(key);
    if (CollectionHelper.isNotEmpty(threadSet)) {
      synchronized (threadSet) {
        if (CollectionHelper.isNotEmpty(threadSet)) {
          for (Thread thread : threadSet) {
            LockSupport.unpark(thread);
          }
          threadSet.clear();
        }
      }
    }
  }
}
