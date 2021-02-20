package top.moma.levelcache.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import top.moma.levelcache.setting.MomaCacheSetting;
import top.moma.m64.core.helper.CollectionHelper;
import top.moma.m64.core.helper.ObjectHelper;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * AbstractCacheManager
 *
 * <p>Abstract Cache Manager
 *
 * @author Ivan
 * @version 1.0 Created by Ivan at 2020/11/21.
 */
@Slf4j
public abstract class AbstractCacheManager implements CacheManager {

  /** Cache Containers Two Levels */
  private final ConcurrentMap<String, ConcurrentMap<String, Cache>> cacheContainer =
      CollectionHelper.newConcurrentMap();

  private volatile Set<String> cacheNames = new HashSet<>();

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
    if (ObjectHelper.isNotEmpty(cacheMap)) {
      Cache cache = cacheMap.get(momaCacheSetting.getCacheId());
      if (ObjectHelper.isNotEmpty(cache)) {
        return cache;
      }
    }
    synchronized (this.cacheContainer) {
      cacheMap = cacheContainer.get(name);
      if (ObjectHelper.isNotEmpty(cacheMap)) {
        Cache cache = cacheMap.get(momaCacheSetting.getCacheId());
        if (ObjectHelper.isNotEmpty(cache)) {
          return cache;
        }
      } else {
        cacheMap = CollectionHelper.newConcurrentMap();
        cacheContainer.put(name, cacheMap);
        updateCacheNames(name);
      }
      Cache cache = buildMomaCache(name, momaCacheSetting);
      if (ObjectHelper.isNotEmpty(cache)) {
        cache = decorator(cache);
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

  protected Cache decorator(Cache cache) {
    return cache;
  }
}
