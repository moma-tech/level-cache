package top.moma.levelcache.cache.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.CollectionUtils;
import top.moma.levelcache.setting.RedisCacheSetting;
import top.moma.levelcache.support.CacheConstants;
import top.moma.levelcache.support.JacksonHelper;
import top.moma.levelcache.support.LocalThreadPool;
import top.moma.levelcache.support.ThreadTaskUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * RedisCache
 *
 * <p>Value可以为null，null值存活时间减半
 *
 * @author Created by ivan on 2020/7/1 .
 * @version 1.0
 */
@Slf4j
public class RedisCache extends AbstractValueAdaptingCache {

  /** Native Redis Cache */
  private RedisTemplate<String, Object> redisTemplate;

  /** 2nd Cache name */
  private final String redisName;

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
  /** 空值过期时间 */
  private long nullExpiration;
  /** 缓存过期模式 */
  private RedisCacheSetting.RedisExpireMode redisExpireMode;

  private LocalThreadPool localThreadPool = new LocalThreadPool();

  public RedisCache(
      String redisName,
      boolean allowNullValues,
      RedisCacheSetting setting,
      RedisTemplate<String, Object> redisTemplate) {
    this(
        allowNullValues,
        redisName,
        setting.getExpireTime(),
        setting.getTimeUnit(),
        setting.isUsePrefix(),
        setting.isHardRefresh(),
        setting.isAutoRenew(),
        setting.getRenewThreshold(),
        setting.getNullExpiration(),
        setting.getRedisExpireMode(),
        redisTemplate);
  }

  public RedisCache(
      boolean allowNullValues,
      String redisName,
      long expiration,
      TimeUnit timeUnit,
      boolean usePrefix,
      boolean hardRefresh,
      boolean autoRenew,
      long renewThreshold,
      long nullExpiration,
      RedisCacheSetting.RedisExpireMode redisExpireMode,
      RedisTemplate<String, Object> redisTemplate) {
    super(allowNullValues);
    this.redisName = redisName;
    this.expiration = expiration;
    this.timeUnit = timeUnit;
    this.usePrefix = usePrefix;
    this.hardRefresh = hardRefresh;
    this.autoRenew = autoRenew;
    this.renewThreshold = renewThreshold;
    this.nullExpiration = nullExpiration;
    this.redisExpireMode = redisExpireMode;
    this.redisTemplate = redisTemplate;
  }

  @Override
  public String getName() {
    return this.redisName;
  }

  @Override
  public Object getNativeCache() {
    return this.redisTemplate;
  }

  @Override
  protected Object lookup(Object key) {
    RedisKey redisKey = getRedisKey(key);
    log.debug("redis缓存 key={} look up缓存", redisKey.getRedisKey());
    return redisTemplate.opsForValue().get(redisKey.getRedisKey());
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    RedisKey redisKey = getRedisKey(key);
    log.debug("redis缓存 key={} get缓存", redisKey.getRedisKey());
    Object result = redisTemplate.opsForValue().get(redisKey.getRedisKey());
    // KEY 存在, 未正确序列化可能存在空值
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
    log.debug(
        "redis缓存 key={} put缓存，缓存值：{}",
        JacksonHelper.toJson(redisKey.getRedisKey()),
        JacksonHelper.toJson(value));
    this.putValue(redisKey, value);
  }

  @Override
  public void evict(Object key) {
    RedisKey redisKey = getRedisKey(key);
    log.debug("redis清除缓存 key={}", JacksonHelper.toJson(redisKey.getRedisKey()));
    redisTemplate.delete(redisKey.getRedisKey());
  }

  @Override
  public void clear() {
    if (usePrefix) {
      log.info("清空redis缓存 ，缓存前缀为{}", getName());
      String pattern = getName() + "*";
      Set<String> keys =
          redisTemplate.execute(
              (RedisCallback<Set<String>>)
                  connection -> {
                    if (connection instanceof RedisClusterConnection) {
                      // 集群模式
                      return redisTemplate.keys(pattern);
                    }
                    // 单机模式
                    Set<String> keysTmp = new HashSet<>();
                    try (Cursor<byte[]> cursor =
                        connection.scan(
                            new ScanOptions.ScanOptionsBuilder()
                                .match(pattern)
                                .count(10000)
                                .build())) {

                      while (cursor.hasNext()) {
                        keysTmp.add(new String(cursor.next(), "Utf-8"));
                      }
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                    return keysTmp;
                  });
      ;
      if (!CollectionUtils.isEmpty(keys)) {
        redisTemplate.delete(keys);
      }
    }
  }

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
      log.debug(
          "redis缓存 key={} put缓存，缓存值为value={}，独立超时时间，防穿透",
          JacksonHelper.toJson(redisKey.getRedisKey()),
          JacksonHelper.toJson(value));
      this.redisTemplate
          .opsForValue()
          .set(redisKey.getRedisKey(), result, nullExpiration, timeUnit);
      return result;
    } else {
      this.redisTemplate.opsForValue().getOperations().delete(redisKey.getRedisKey());
      log.error("Redis 缓存不允许存NULL值，不缓存数据");
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
    return new RedisKey(key, redisTemplate.getKeySerializer()).prefix(usePrefix, redisName);
  }

  /**
   * lockGetValueMethod
   *
   * <p>get K/V, to avoid multi get, use local thread pool to wait, do retry, and lock;
   *
   * <p>use local thread to avoid　breakdown
   *
   * @author Created by ivan at 下午2:46 2020/7/13.
   * @return T
   */
  private <T> T lockGetValueMethod(RedisKey redisKey, Callable<T> valueLoader) {
    RedisSimpleLock redisSimpleLock = new RedisSimpleLock(redisTemplate);
    String lockValue = UUID.randomUUID().toString();
    for (int i = 0; i < CacheConstants.RETRY_MAX; i++) {
      Object result = redisTemplate.opsForValue().get(redisKey.getRedisKey());
      if (Objects.nonNull(result)) {
        log.debug("redis 缓存 key={}，循环缓存中获得", JacksonHelper.toJson(redisKey.getRedisKey()));
        return (T) fromStoreValue(result);
      }
      try {
        if (redisSimpleLock.lock(redisKey.getRedisKey(), lockValue)) {
          log.debug("redis 缓存, key={},加锁成功，执行方法获取", JacksonHelper.toJson(redisKey.getRedisKey()));
          T value = callValueMethod(redisKey, valueLoader);
          localThreadPool.signalAll(redisKey.getRedisKey());
          return value;
        }
        localThreadPool.await(redisKey.getRedisKey(), CacheConstants.WAIT_TIME);
      } catch (Exception e) {
        throw new ValueRetrievalException(redisKey.getRedisKey(), valueLoader, e);
      } finally {
        redisSimpleLock.unlock(redisKey.getRedisKey(), lockValue);
      }
    }
    log.debug("redis 缓存, key={}, 多次取锁失败, 尝试直接执行方法获取", JacksonHelper.toJson(redisKey.getRedisKey()));
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
      log.debug("redis缓存 key={} get缓存, 执行后续方法", JacksonHelper.toJson(redisKey.getRedisKey()));
      return (T) fromStoreValue(result);
    } catch (Exception e) {
      log.error("redis缓存，取值方法获取失败，key={}", JacksonHelper.toJson(redisKey.getRedisKey()), e);
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
        log.debug("redis刷新缓存 hard key={}", JacksonHelper.toJson(redisKey.getRedisKey()));
        hardRefresh(redisKey, valueLoader);
      } else {
        log.debug("redis刷新缓存 soft key={}", JacksonHelper.toJson(redisKey.getRedisKey()));
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
    RedisSimpleLock redisSimpleLock = new RedisSimpleLock(redisTemplate);
    String lockValue = UUID.randomUUID().toString();
    try {
      if (redisSimpleLock.lock(CacheConstants.LOCK_PREFIX + redisKey.getRedisKey(), lockValue)) {
        redisTemplate.expire(redisKey.getRedisKey(), this.expiration, TimeUnit.MILLISECONDS);
      }
    } catch (Exception e) {
      log.error("Redis 软刷新错误：", e);
    } finally {
      redisSimpleLock.unlock(CacheConstants.LOCK_PREFIX + redisKey.getRedisKey(), lockValue);
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
          RedisSimpleLock redisSimpleLock = new RedisSimpleLock(redisTemplate);
          String lockValue = UUID.randomUUID().toString();
          if (redisSimpleLock.lock(redisKey.getRedisKey(), lockValue)) {
            callValueMethod(redisKey, valueLoader);
          }
        });
  }
}
