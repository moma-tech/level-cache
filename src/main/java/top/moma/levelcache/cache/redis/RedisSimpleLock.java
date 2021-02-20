package top.moma.levelcache.cache.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import top.moma.m64.core.helper.ObjectHelper;
import top.moma.m64.core.helper.StringHelper;
import top.moma.m64.core.helper.json.JsonHelper;

import java.util.concurrent.TimeUnit;

/**
 * RedisLock
 *
 * <p>for complex project, should use red lock
 *
 * @author Created by ivan on 2020/7/9 .
 * @version 1.0
 */
@Slf4j
public class RedisSimpleLock {

  private final long LOCK_MAX_TIME = 2000;
  private final int RETRY_MAX_TIME = 4;
  private RedisTemplate<String, Object> redisTemplate;

  public RedisSimpleLock(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public boolean lock(String key, String value) {
    try {

      int counter = 1;
      while (RETRY_MAX_TIME > counter) {
        Boolean locked =
            redisTemplate
                .opsForValue()
                .setIfAbsent(key, value, LOCK_MAX_TIME, TimeUnit.MILLISECONDS);
        if (ObjectHelper.isNotEmpty(locked) && locked) {
          log.debug("redis lock获取 key={} ", JsonHelper.toJson(key));
          return true;
        }
        Thread.sleep(100);
        counter++;
      }

    } catch (Exception e) {
      log.error("redis lock获取异常 key={}", JsonHelper.toJson(key), e);
    }
    log.debug("redis lock获取失败 key={} ", JsonHelper.toJson(key));
    return false;
  }

  public void unlock(String key, String value) {
    String lockValue = (String) redisTemplate.opsForValue().get(key);
    if (StringHelper.isNotBlank(lockValue) && value.equals(lockValue)) {
      log.debug("redis lock释放 key={} ", JsonHelper.toJson(key));
      redisTemplate.opsForValue().getOperations().delete(key);
    }
  }
}
