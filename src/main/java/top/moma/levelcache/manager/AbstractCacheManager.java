package top.moma.levelcache.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import top.moma.levelcache.setting.MomaCacheSetting;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AbstractCacheManager
 *
 * <p>Abstract Cache Manager
 *
 * @author Ivan
 * @version 1.0 Created by Ivan at 2020/11/21.
 */
public class AbstractCacheManager implements CacheManager {
  private Logger logger = LoggerFactory.getLogger(AbstractCacheManager.class);

  /** Cache Containers Two Levels */
  private final ConcurrentMap<String, ConcurrentMap<String, Cache>> cacheContainer =
      new ConcurrentHashMap<>(16);

  @Override
  public Collection<Cache> getCaches(String name) {
    ConcurrentMap<String, Cache> cacheMap = cacheContainer.get(name);
    if (cacheMap.isEmpty()) {
      return null;
    }
    return cacheMap.values();
  }

  @Override
  public Cache getCache(String name, MomaCacheSetting momaCacheSetting) {
    ConcurrentMap<String, Cache> cacheMap = cacheContainer.get(name);
    if (!cacheMap.isEmpty()) {
      if (1 < cacheMap.size()) {
        logger.warn("存在同名不同配置的缓存，name:{}, size：{}", name, cacheMap.size());
      }
      Cache cache = cacheMap.get(momaCacheSetting.getCacheId());
    }

    return null;
  }

  @Override
  public Collection<String> getCacheNames() {
    return null;
  }
}
