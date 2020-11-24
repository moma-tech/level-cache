package top.moma.levelcache.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import top.moma.levelcache.setting.MomaCacheSetting;

import java.util.*;
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
public abstract class AbstractCacheManager implements CacheManager {
  private Logger logger = LoggerFactory.getLogger(AbstractCacheManager.class);

  /** Cache Containers Two Levels */
  private final ConcurrentMap<String, ConcurrentMap<String, Cache>> cacheContainer =
      new ConcurrentHashMap<>(16);

  private volatile Set<String> cacheNames = new HashSet<String>();

  static Set<AbstractCacheManager> cacheManagers = new LinkedHashSet<>();

  @Override
  public Collection<Cache> getCaches(String name) {
    ConcurrentMap<String, Cache> cacheMap = cacheContainer.get(name);
    if (cacheMap.isEmpty()) {
      return Collections.emptyList();
    }
    return cacheMap.values();
  }

  @Override
  public Cache getCache(String name, MomaCacheSetting momaCacheSetting) {
    ConcurrentMap<String, Cache> cacheMap = cacheContainer.get(name);
    if (!cacheMap.isEmpty()) {
      Cache cache = cacheMap.get(momaCacheSetting.getCacheId());
      if (null != cache) {
        return cache;
      }
    }
    synchronized (this.cacheContainer) {
      cacheMap = cacheContainer.get(name);
      if (!cacheMap.isEmpty()) {
        Cache cache = cacheMap.get(momaCacheSetting.getCacheId());
        if (null != cache) {
          return cache;
        }
      } else {
        cacheMap = new ConcurrentHashMap<>(16);
        cacheContainer.put(name, cacheMap);
        updateCacheNames(name);
      }
      Cache cache = buildMomaCache(name, momaCacheSetting);
      if (null != cache) {
        cacheMap.put(momaCacheSetting.getCacheId(), cache);
      }
      return cache;
    }
  }

  @Override
  public Collection<String> getCacheNames() {
    return cacheNames;
  }

  private void updateCacheNames(String name) {
    cacheNames.add(name);
  }

  protected abstract Cache buildMomaCache(String name, MomaCacheSetting momaCacheSetting);
}
