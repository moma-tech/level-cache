package top.moma.levelcache.support;

/**
 * ExpiredMode
 *
 * <p>Cache Expired Mode
 *
 * @author ivan
 * @version 1.0 Created by ivan at 2/22/21.
 */
public enum ExpiredMode {
  /** 最后一次写入或访问后经过固定时间过期 */
  expireAfterAccess,
  /** 最后一次写入后经过固定时间过期 */
  expireAfterWrite,
  /** 最后一次写入或访问后经过固定时间过期 */
  refreshAfterAccess,
  ;
}
