package top.moma.levelcache.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import top.moma.levelcache.cache.caffeine.CaffeineCache;
import top.moma.levelcache.cache.redis.RedisCache;
import top.moma.levelcache.setting.MomaCacheMode;
import top.moma.levelcache.setting.MomaCacheSetting;
import top.moma.m64.core.helper.ObjectHelper;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * MomaCache
 *
 * @author Created by ivan on 2020/7/16 .
 * @version 1.0
 */
@Slf4j
public class MomaCache extends AbstractValueAdaptingCache {

  private String momaCacheName;

  private MomaCacheMode cacheMode;

  private AbstractValueAdaptingCache caffeineCache;
  private AbstractValueAdaptingCache redisCache;

  private RedisTemplate<String, Object> redisTemplate;

  public MomaCache(
      String cacheName,
      boolean allowNullValues,
      MomaCacheSetting momaCacheSetting,
      RedisTemplate<String, Object> redisTemplate) {
    super(allowNullValues);
    this.redisTemplate = redisTemplate;
    this.cacheMode = momaCacheSetting.getCacheMode();

    switch (cacheMode) {
      case REDIS_CACHE_ONLY:
        this.caffeineCache = null;
        this.redisCache =
            new RedisCache(
                cacheName, allowNullValues, momaCacheSetting.getRedisCacheSetting(), redisTemplate);
        break;
      case CAFFEINE_CACHE_ONLY:
        this.caffeineCache =
            new CaffeineCache(cacheName, momaCacheSetting.getCaffeineCacheSetting());
        this.redisCache = null;
        break;
      default:
        this.caffeineCache =
            new CaffeineCache(cacheName, momaCacheSetting.getCaffeineCacheSetting());
        this.redisCache =
            new RedisCache(
                cacheName, allowNullValues, momaCacheSetting.getRedisCacheSetting(), redisTemplate);
    }
  }

  @Override
  protected Object lookup(Object key) {
    switch (cacheMode) {
      case REDIS_CACHE_ONLY:
        return this.redisCache.get(key);
      case CAFFEINE_CACHE_ONLY:
        return this.caffeineCache.get(key);
      default:
        Object result = this.caffeineCache.get(key);
        if (Objects.isNull(result)) {
          result = this.redisCache.get(key);
          this.caffeineCache.putIfAbsent(key, result);
        }
        return result;
    }
  }

  @Override
  public String getName() {
    switch (cacheMode) {
      case REDIS_CACHE_ONLY:
        return this.redisCache.getName();
      case CAFFEINE_CACHE_ONLY:
        return this.caffeineCache.getName();
      default:
        String result = this.caffeineCache.getName();
        if (StringUtils.isEmpty(result)) {
          result = this.redisCache.getName();
        }
        return result;
    }
  }

  @Override
  public Object getNativeCache() {
    switch (cacheMode) {
      case REDIS_CACHE_ONLY:
        return this.redisCache.getNativeCache();
      case CAFFEINE_CACHE_ONLY:
        return this.caffeineCache.getNativeCache();
      default:
        Object result = this.caffeineCache.getNativeCache();
        if (ObjectHelper.isEmpty(result)) {
          result = this.redisCache.getNativeCache();
        }
        return result;
    }
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    switch (cacheMode) {
      case REDIS_CACHE_ONLY:
        return this.redisCache.get(key, valueLoader);
      case CAFFEINE_CACHE_ONLY:
        return this.caffeineCache.get(key, valueLoader);
      default:
        T result = this.caffeineCache.get(key, valueLoader);
        if (ObjectHelper.isEmpty(result)) {
          result = this.redisCache.get(key, valueLoader);
          this.caffeineCache.putIfAbsent(key, valueLoader);
        }
        return result;
    }
  }

  @Override
  public void put(Object key, Object value) {
    switch (cacheMode) {
      case REDIS_CACHE_ONLY:
        this.redisCache.put(key, value);
        break;
      case CAFFEINE_CACHE_ONLY:
        this.caffeineCache.put(key, value);
        break;
      default:
        this.redisCache.put(key, value);
        // TODO 集群模式,需要使用订阅发布进行删除
        this.caffeineCache.evict(key);
    }
  }

  @Override
  public ValueWrapper putIfAbsent(Object key, Object value) {
    switch (cacheMode) {
      case REDIS_CACHE_ONLY:
        return this.redisCache.putIfAbsent(key, value);
      case CAFFEINE_CACHE_ONLY:
        return this.caffeineCache.putIfAbsent(key, value);
      default:
        // TODO 集群模式,需要使用订阅发布进行删除
        this.caffeineCache.evict(key);
        return this.redisCache.putIfAbsent(key, value);
    }
  }

  @Override
  public void evict(Object key) {
    switch (cacheMode) {
      case REDIS_CACHE_ONLY:
        this.redisCache.evict(key);
        break;
      case CAFFEINE_CACHE_ONLY:
        this.caffeineCache.evict(key);
        break;
      default:
        // 从后往前删
        this.redisCache.evict(key);
        // TODO 集群模式,需要使用订阅发布进行本地缓存 删除
        this.caffeineCache.evict(key);
    }
  }

  @Override
  public void clear() {
    switch (cacheMode) {
      case REDIS_CACHE_ONLY:
        this.redisCache.clear();
        break;
      case CAFFEINE_CACHE_ONLY:
        this.caffeineCache.clear();
        break;
      default:
        // 从后往前删
        this.redisCache.clear();
        // TODO 集群模式,需要使用订阅发布进行删除
        this.caffeineCache.clear();
    }
  }
}
