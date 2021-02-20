package top.moma.levelcache.support;

/**
 * CacheConstants
 *
 * @author Created by ivan on 2020/7/9 .
 * @version 1.0
 */
public interface CacheConstants {
  String CACHE_NAME_SPLIT = "_";
  String LOCK_PREFIX = "LOCK-";
  int RETRY_MAX = 5;
  long WAIT_TIME = 20;
  String DEFAULT_PREFIX = "moma-redis";
}
