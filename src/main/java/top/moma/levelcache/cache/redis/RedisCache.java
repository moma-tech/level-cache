package top.moma.levelcache.cache.redis;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;
import top.moma.levelcache.setting.RedisCacheSetting;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * RedisCache
 *
 * @author Created by ivan on 2020/7/1 .
 * @version 1.0
 */
public class RedisCache extends AbstractValueAdaptingCache {

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
      RedisTemplate<String, Object> redisTemplate) {
    super(allowNullValues);
    this.name = name;
    this.expiration = expiration;
    this.timeUnit = timeUnit;
    this.usePrefix = usePrefix;
    this.hardRefresh = hardRefresh;
    this.autoRenew = autoRenew;
    this.renewThreshold = renewThreshold;
    this.redisTemplate = redisTemplate;
  }

  @Override
  protected Object lookup(Object key) {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Object getNativeCache() {
    return this.redisTemplate;
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    return null;
  }

  @Override
  public void put(Object key, Object value) {}

  @Override
  public void evict(Object key) {}

  @Override
  public void clear() {}
}
