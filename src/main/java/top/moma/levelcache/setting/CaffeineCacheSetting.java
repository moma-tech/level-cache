package top.moma.levelcache.setting;

import lombok.AllArgsConstructor;
import lombok.Data;
import top.moma.levelcache.support.ExpiredMode;

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
@AllArgsConstructor
public class CaffeineCacheSetting implements java.io.Serializable {
  private static final long serialVersionUID = 265643425858885778L;

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
  private ExpiredMode caffeineExpiredMode = ExpiredMode.expireAfterWrite;
}
