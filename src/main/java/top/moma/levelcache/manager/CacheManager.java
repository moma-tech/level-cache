package top.moma.levelcache.manager;

import org.springframework.cache.Cache;
import top.moma.levelcache.setting.MomaCacheSetting;

import java.util.Collection;

/**
 * CacheManager
 *
 * @author Created by ivan on 2020/6/28 .
 * @version 1.0
 */
public interface CacheManager {
  /**
   * Get Caches by Name
   *
   * @author Created by Ivan at 2020/11/21.
   * @param name : cache name
   * @return java.util.Collection<org.springframework.cache.Cache>
   */
  Collection<Cache> getCaches(String name);

  /**
   * Get Cache By Name and setting if non-ex create one
   *
   * @author Created by Ivan at 2020/11/21.
   * @param name : cache name
   * @param momaCacheSetting : cache setting
   * @return org.springframework.cache.Cache
   */
  Cache getCache(String name, MomaCacheSetting momaCacheSetting);

  /**
   * Get all Cache Names
   *
   * @author Created by Ivan at 2020/11/21.
   * @return java.util.Collection<java.lang.String>
   */
  Collection<String> getCacheNames();
}
