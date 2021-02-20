package top.moma.levelcache.setting;

import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * CaffeineCacheSetting
 *
 * <p>//Caffeine Cache Setting
 *
 * @author Created by ivan on 2020/6/29 .
 * @version 1.0
 */
@Data
public class CaffeineCacheSetting implements java.io.Serializable {
  private static final long serialVersionUID = 265643425858885778L;

  public enum CaffeineExpiredMode {
    /** 最后一次写入或访问后经过固定时间过期 */
    expireAfterAccess,
    /** 最后一次写入后经过固定时间过期 */
    expireAfterWrite,
    ;
  }

  /** 初始空间大小 */
  private int initialCapacity = 20;

  /** 缓存最大条数 */
  private long maximumSize = 1000L;

  /** 缓存的最大权重 */
  private long maximumWeight = -1L;

  /** 缓存过期时间 */
  private long expireTime = 0;

  /** 过期时间单位 */
  private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

  /** 缓存过期模式 */
  private CaffeineExpiredMode caffeineExpiredMode = CaffeineExpiredMode.expireAfterWrite;
}
