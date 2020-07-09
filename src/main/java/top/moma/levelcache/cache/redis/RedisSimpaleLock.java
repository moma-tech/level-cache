package top.moma.levelcache.cache.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import top.moma.levelcache.support.JacksonHelper;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * RedisLock
 *
 * <p>for complex project, should use red lock
 *
 * @author Created by ivan on 2020/7/9 .
 * @version 1.0
 */
public class RedisSimpaleLock {
  protected static final Logger logger = LoggerFactory.getLogger(RedisSimpaleLock.class);

  private final long LOCK_MAX_TIME = 2000;
  private final int RETERY_MAX_TIME = 4;
  private RedisTemplate redisTemplate;

  public RedisSimpaleLock(RedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public boolean lock(String key, String value) {
    try {

      int counter = 1;
      while (RETERY_MAX_TIME > counter) {
        Boolean locked =
            redisTemplate
                .opsForValue()
                .setIfAbsent(key, value, LOCK_MAX_TIME, TimeUnit.MILLISECONDS);
        if (Objects.nonNull(locked) && locked) {
          logger.debug("redis lock获取 key={} ", JacksonHelper.toJson(key));
          return true;
        }
        Thread.sleep(100);
        counter++;
      }

    } catch (Exception e) {
      logger.error("redis lock获取异常 key={} ", e);
    }
    logger.debug("redis lock获取失败 key={} ", JacksonHelper.toJson(key));
    return false;
  }

  public void unlock(String key, String value) {
    String lockValue = (String) redisTemplate.opsForValue().get(key);
    if (!StringUtils.isEmpty(lockValue) && value.equals(lockValue)) {
      logger.debug("redis lock释放 key={} ", JacksonHelper.toJson(key));
      redisTemplate.opsForValue().getOperations().delete(key);
    }
  }
}
