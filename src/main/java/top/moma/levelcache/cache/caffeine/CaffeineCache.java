package top.moma.levelcache.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.NullValue;
import top.moma.levelcache.setting.CaffeineCacheSetting;
import top.moma.levelcache.support.JacksonHelper;

import java.util.concurrent.Callable;

/**
 * CaffeineCache
 *
 * <p>禁止key为null
 *
 * @author Created by ivan on 2020/6/29 .
 * @version 1.0
 */
@Slf4j
public class CaffeineCache extends AbstractValueAdaptingCache {

  /** native caffeine Cache */
  private final Cache<Object, Object> caffeineCache;
  /** 1st level Cahce Name */
  private final String caffeineName;

  public CaffeineCache(String caffeineName, CaffeineCacheSetting caffeineCacheSetting) {
    super(false);
    this.caffeineName = caffeineName;
    caffeineCache = builtCache(caffeineCacheSetting);
  }

  @Override
  public String getName() {
    return caffeineName;
  }

  @Override
  public Object getNativeCache() {
    return caffeineCache;
  }

  @Override
  protected Object lookup(Object key) {
    log.debug("caffeine缓存 key={} look up缓存", JacksonHelper.toJson(key));
    if (caffeineCache instanceof LoadingCache) {
      return ((LoadingCache<Object, Object>) caffeineCache).get(key);
    } else {
      return caffeineCache.getIfPresent(key);
    }
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    log.debug("caffeine缓存 key={} get缓存, 不存在，执行后续", JacksonHelper.toJson(key));
    Object value =
        caffeineCache.get(
            key,
            k -> {
              try {
                return toStoreValue(valueLoader.call());
              } catch (Throwable ex) {
                throw new ValueRetrievalException(key, valueLoader, ex);
              }
            });
    if (null == value || value instanceof NullValue) {
      log.error("Caffeine缓存不允许存NULL值，不缓存数据");
      evict(key);
    }
    return (T) fromStoreValue(value);
  }

  @Override
  public void put(Object key, Object value) {
    if (null != value && !(value instanceof NullValue)) {
      log.debug(
          "caffeine缓存 key={} put缓存，缓存值：{}", JacksonHelper.toJson(key), JacksonHelper.toJson(value));
      this.caffeineCache.put(key, toStoreValue(value));
    } else {
      log.error("Caffeine缓存不允许存NULL值，不缓存数据");
    }
  }

  @Override
  public ValueWrapper putIfAbsent(Object key, Object value) {
    if (null != value && !(value instanceof NullValue)) {
      log.debug(
          "caffeine缓存 key={} put if absent 缓存，缓存值：{}",
          JacksonHelper.toJson(key),
          JacksonHelper.toJson(value));
      ValueWrapper existed = this.get(key);
      if (null == existed) {
        this.put(key, value);
      }
      return existed;
    } else {
      log.error("Caffeine缓存不允许存NULL值，不缓存数据");
      return null;
    }
  }

  @Override
  public void evict(Object key) {
    log.debug("caffeine清除缓存 key={}", JacksonHelper.toJson(key));
    caffeineCache.invalidate(key);
  }

  @Override
  public void clear() {
    log.debug("caffeine 清空缓存");
    caffeineCache.invalidateAll();
  }

  /**
   * builtCache
   *
   * <p>Build Caffeine Cache with setting
   *
   * @author Created by ivan at 上午10:50 2020/6/29.
   * @return com.github.benmanes.caffeine.cache.Cache<java.lang.Object,java.lang.Object>
   */
  private static Cache<Object, Object> builtCache(CaffeineCacheSetting caffeineCacheSetting) {
    Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
    cacheBuilder
        .initialCapacity(caffeineCacheSetting.getInitialCapacity())
        .maximumSize(caffeineCacheSetting.getMaximumSize())
        .maximumWeight(caffeineCacheSetting.getMaximumWeight());
    if (CaffeineCacheSetting.CaffeineExpiredMode.expireAfterWrite.equals(
        caffeineCacheSetting.getCaffeineExpiredMode())) {
      cacheBuilder.expireAfterWrite(
          caffeineCacheSetting.getExpireTime(), caffeineCacheSetting.getTimeUnit());
    } else if (CaffeineCacheSetting.CaffeineExpiredMode.expireAfterAccess.equals(
        caffeineCacheSetting.getCaffeineExpiredMode())) {
      cacheBuilder.expireAfterAccess(
          caffeineCacheSetting.getExpireTime(), caffeineCacheSetting.getTimeUnit());
    }
    return cacheBuilder.build();
  }
}
