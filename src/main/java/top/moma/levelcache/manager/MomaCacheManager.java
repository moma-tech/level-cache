package top.moma.levelcache.manager;

import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;
import top.moma.levelcache.cache.MomaCache;
import top.moma.levelcache.setting.MomaCacheSetting;

/**
 * MomaCacheManager
 *
 * @author ivan
 * @version 1.0 Created by ivan at 11/24/20.
 */
public class MomaCacheManager extends AbstractCacheManager {

  private RedisTemplate<String, Object> redisTemplate;

  public MomaCacheManager(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
    cacheManagers.add(this);
  }

  @Override
  protected Cache buildMomaCache(String name, MomaCacheSetting momaCacheSetting) {
    return new MomaCache(name, false, momaCacheSetting, redisTemplate);
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
