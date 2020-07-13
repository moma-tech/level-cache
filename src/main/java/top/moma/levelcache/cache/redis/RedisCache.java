package top.moma.levelcache.cache.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.core.RedisTemplate;
import top.moma.levelcache.setting.RedisCacheSetting;
import top.moma.levelcache.support.CacheConstants;
import top.moma.levelcache.support.JacksonHelper;
import top.moma.levelcache.support.LocalThreadPool;
import top.moma.levelcache.support.ThreadTaskUtils;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * RedisCache
 *
 * @author Created by ivan on 2020/7/1 .
 * @version 1.0
 */
public class RedisCache extends AbstractValueAdaptingCache {
  private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);

  /** Cache name */
  private final String name;

  /** 缓存过期时间 */
  private long expiration;
  /** 过期时间单位 */
  private TimeUnit timeUnit;
  /** 使用缓存名前缀 */
  private boolean usePrefix;
  /** 方法级硬刷新 */
  private boolean hardRefresh;
  /** 过期前自动刷新 */
  private boolean autoRenew;
  /** 自动刷新阈值 */
  private long renewThreshold;
  /** 缓存过期模式 */
  private RedisCacheSetting.RedisExpireMode redisExpireMode;

  private LocalThreadPool localThreadPool = new LocalThreadPool();

  private RedisTemplate<String, Object> redisTemplate;

  protected RedisCache(
      String name,
      boolean allowNullValues,
      RedisCacheSetting setting,
      RedisTemplate<String, Object> redisTemplate) {
    this(
        allowNullValues,
        name,
        setting.getExpiration(),
        setting.getTimeUnit(),
        setting.isUsePrefix(),
        setting.isHardRefresh(),
        setting.isAutoRenew(),
        setting.getRenewThreshold(),
        setting.getRedisExpireMode(),
        redisTemplate);
  }

  public RedisCache(
      boolean allowNullValues,
      String name,
      long expiration,
      TimeUnit timeUnit,
      boolean usePrefix,
      boolean hardRefresh,
      boolean autoRenew,
      long renewThreshold,
      RedisCacheSetting.RedisExpireMode redisExpireMode,
      RedisTemplate<String, Object> redisTemplate) {
    super(allowNullValues);
    this.name = name;
    this.expiration = expiration;
    this.timeUnit = timeUnit;
    this.usePrefix = usePrefix;
    this.hardRefresh = hardRefresh;
    this.autoRenew = autoRenew;
    this.renewThreshold = renewThreshold;
    this.redisExpireMode = redisExpireMode;
    this.redisTemplate = redisTemplate;
  }

  @Override
  protected Object lookup(Object key) {
    RedisKey redisKey = getRedisKey(key);
    logger.debug("redis缓存 key={} get缓存", JacksonHelper.toJson(redisKey.getRedisKey()));
    return redisTemplate.opsForValue().get(redisKey.getRedisKey());
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Object getNativeCache() {
    return this.redisTemplate;
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    RedisKey redisKey = getRedisKey(key);
    logger.debug("redis缓存 key={} get缓存", JacksonHelper.toJson(redisKey.getRedisKey()));
    Object result = redisTemplate.opsForValue().get(redisKey.getRedisKey());
    // KEY 存在
    if (Objects.nonNull(result) || redisTemplate.hasKey(redisKey.getRedisKey())) {
      // auto renew enabled or redis expired mode set to after access
      if (autoRenew
          || RedisCacheSetting.RedisExpireMode.refreshAfterAccess.equals(this.redisExpireMode)) {
        refreshCache(redisKey, valueLoader);
      }
      return (T) fromStoreValue(result);
    }
    // 　方法获取
    return lockGetValueMethod(redisKey, valueLoader);
  }

  @Override
  public void put(Object key, Object value) {
    RedisKey redisKey = getRedisKey(key);
    logger.debug(
        "redis缓存 key={} put缓存，缓存值：{}",
        JacksonHelper.toJson(redisKey.getRedisKey()),
        JacksonHelper.toJson(value));
    this.putValue(redisKey, value);
  }

  @Override
  public void evict(Object key) {
    RedisKey redisKey = getRedisKey(key);
    logger.debug("redis清除缓存 key={}", JacksonHelper.toJson(redisKey.getRedisKey()));
    redisTemplate.delete(redisKey.getRedisKey());
  }

  @Override
  public void clear() {}

  /**
   * putValue
   *
   * <p>put cache K/V, if null allowed && v == null, half the expiration time
   *
   * @author Created by ivan at 下午2:44 2020/7/13.
   * @return java.lang.Object
   */
  private Object putValue(RedisKey redisKey, Object value) {
    Object result = toStoreValue(value);
    if (null != value && !(value instanceof NullValue)) {
      this.redisTemplate.opsForValue().set(redisKey.getRedisKey(), result, expiration, timeUnit);
      return result;
    } else if (isAllowNullValues()) {
      logger.debug(
          "redis缓存 key={} put缓存，缓存值为value={}，超时时间减半",
          JacksonHelper.toJson(redisKey.getRedisKey()),
          JacksonHelper.toJson(value));
      this.redisTemplate
          .opsForValue()
          .set(redisKey.getRedisKey(), result, expiration / 2, timeUnit);
      return result;
    } else {
      this.redisTemplate.opsForValue().getOperations().delete(redisKey.getRedisKey());
      logger.error("Redis 缓存不允许存NULL值，不缓存数据");
      return result;
    }
  }

  /**
   * getRedisKey
   *
   * <p>Format the key
   *
   * @author Created by ivan at 下午2:46 2020/7/13.
   * @return top.moma.levelcache.cache.redis.RedisKey
   */
  private RedisKey getRedisKey(Object key) {
    return new RedisKey(key, redisTemplate.getKeySerializer()).prefix(usePrefix, name);
  }

  /**
   * lockGetValueMethod
   *
   * <p>get K/V, to avoid multi get, use local thread pool to wait, do retry, and lock;
   *
   * @author Created by ivan at 下午2:46 2020/7/13.
   * @return T
   */
  private <T> T lockGetValueMethod(RedisKey redisKey, Callable<T> valueLoader) {
    RedisSimpaleLock redisSimpaleLock = new RedisSimpaleLock(redisTemplate);
    String lockValue = UUID.randomUUID().toString();
    for (int i = 0; i < CacheConstants.RETRY_MAX; i++) {
      Object result = redisTemplate.opsForValue().get(redisKey.getRedisKey());
      if (Objects.nonNull(result)) {
        logger.debug("redis 缓存 key={}，循环缓存中获得", JacksonHelper.toJson(redisKey.getRedisKey()));
        return (T) fromStoreValue(result);
      }
      try {
        if (redisSimpaleLock.lock(redisKey.getRedisKey(), lockValue)) {
          logger.debug(
              "redis 缓存, key={},加锁成功，执行方法获取", JacksonHelper.toJson(redisKey.getRedisKey()));
          T value = callValueMethod(redisKey, valueLoader);
          localThreadPool.signalAll(redisKey.getRedisKey());
          return value;
        }
        localThreadPool.await(redisKey.getRedisKey(), CacheConstants.WAIT_TIME);
      } catch (Exception e) {
        throw new ValueRetrievalException(redisKey.getRedisKey(), valueLoader, e);
      } finally {
        redisSimpaleLock.unlock(redisKey.getRedisKey(), lockValue);
      }
    }
    logger.debug(
        "redis 缓存, key={}, 多次取锁失败, 尝试直接执行方法获取", JacksonHelper.toJson(redisKey.getRedisKey()));
    return callValueMethod(redisKey, valueLoader);
  }

  /**
   * callValueMethod
   *
   * <p>call value loader to get value and put into K/V
   *
   * @author Created by ivan at 下午2:52 2020/7/13.
   * @return T
   */
  private <T> T callValueMethod(RedisKey redisKey, Callable<T> valueLoader) {
    try {
      Object result = putValue(redisKey, valueLoader.call());
      logger.debug("redis缓存 key={} get缓存, 执行后续方法", JacksonHelper.toJson(redisKey.getRedisKey()));
      return (T) fromStoreValue(result);
    } catch (Exception e) {
      logger.error(
          "redis缓存，取值方法获取失败，key={}",
          JacksonHelper.toJson(redisKey.getRedisKey()),
          e.fillInStackTrace());
      throw new ValueRetrievalException(redisKey.getRedisKey(), valueLoader, e);
    }
  }

  /**
   * refreshCache
   *
   * <p>refresh K/V
   *
   * @author Created by ivan at 下午2:53 2020/7/13.
   */
  private <T> void refreshCache(Object key, Callable<T> valueLoader) {
    RedisKey redisKey = this.getRedisKey(key);
    Long ttl = redisTemplate.getExpire(redisKey.getRedisKey());
    if (null != ttl && 0 < ttl && TimeUnit.SECONDS.toMillis(ttl) <= renewThreshold) {
      if (hardRefresh) {
        logger.debug("redis刷新缓存 hard key={}", JacksonHelper.toJson(redisKey.getRedisKey()));
        hardRefresh(redisKey, valueLoader);
      } else {
        logger.debug("redis刷新缓存 soft key={}", JacksonHelper.toJson(redisKey.getRedisKey()));
        softRefresh(redisKey);
      }
    }
  }

  /**
   * softRefresh
   *
   * <p>renew expiration time
   *
   * @author Created by ivan at 下午2:53 2020/7/13.
   */
  private <T> void softRefresh(RedisKey redisKey) {
    RedisSimpaleLock redisSimpaleLock = new RedisSimpaleLock(redisTemplate);
    String lockValue = UUID.randomUUID().toString();
    try {
      if (redisSimpaleLock.lock(CacheConstants.LOCK_PREFIX + redisKey.getRedisKey(), lockValue)) {
        redisTemplate.expire(redisKey.getRedisKey(), this.expiration, TimeUnit.MILLISECONDS);
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    } finally {
      redisSimpaleLock.unlock(CacheConstants.LOCK_PREFIX + redisKey.getRedisKey(), lockValue);
    }
  }

  /**
   * hardRefresh
   *
   * <p>use value loader, to re put value
   *
   * @author Created by ivan at 下午2:56 2020/7/13.
   */
  private <T> void hardRefresh(RedisKey redisKey, Callable<T> valueLoader) {
    ThreadTaskUtils.run(
        () -> {
          RedisSimpaleLock redisSimpaleLock = new RedisSimpaleLock(redisTemplate);
          String lockValue = UUID.randomUUID().toString();
          if (redisSimpaleLock.lock(redisKey.getRedisKey(), lockValue)) {
            callValueMethod(redisKey, valueLoader);
          }
        });
  }
}
