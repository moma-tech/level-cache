package top.moma.levelcache.cache.redis;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;
import top.moma.levelcache.setting.RedisCacheSetting;

import java.util.concurrent.Callable;

/**
 * RedisCache
 *
 * @author Created by ivan on 2020/7/1 .
 * @version 1.0
 */
public class RedisCache extends AbstractValueAdaptingCache {

  private final String name;

  private RedisTemplate<String, Object> redisTemplate;

  private RedisCacheSetting redisCacheSetting;

  protected RedisCache(
      String name,
      boolean allowNullValues,
      RedisCacheSetting redisCacheSetting,
      RedisTemplate<String, Object> redisTemplate) {
    super(allowNullValues);
    this.name = name;
    this.redisTemplate = redisTemplate;
    this.redisCacheSetting = redisCacheSetting;
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
    return null;
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
