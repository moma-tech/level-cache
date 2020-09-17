package top.moma.levelcache.setting;

/**
 * Cache Modes
 *
 * <p>Created by ivan on 2020/9/16.
 *
 * @author ivan
 */
public enum MomaCacheMode {
  /** Caffeine Only */
  CAFFEINE_CACHE_ONLY,
  REDIS_CACHE_ONLY,
  ALL_CACHE,
  ;
}
