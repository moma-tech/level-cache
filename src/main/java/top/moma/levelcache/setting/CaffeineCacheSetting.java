package top.moma.levelcache.setting;

import java.util.concurrent.TimeUnit;

/**
 * CaffeineCacheSetting
 *
 * <p>//Caffeine Cache Setting
 *
 * @author Created by ivan on 2020/6/29 .
 * @version 1.0
 */
public class CaffeineCacheSetting {
  public enum CaffeineExpiredMode {
    /** 最后一次写入或访问后经过固定时间过期 */
    expireAfterAccess,
    /** 最后一次写入后经过固定时间过期 */
    expireAfterWrite,
    ;
  }

  /** 初始空间大小 */
  private int initalCapacity = 20;

  /** 缓存最大条数 */
  private long maximumSize = 1000L;

  /** 缓存的最大权重 */
  private long maximumWeight = -1L;

  /** 缓存过期时间 */
  private int expireTime = 0;

  /** 过期时间单位 */
  private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

  /** 缓存过期模式 */
  private CaffeineExpiredMode caffeineExpiredMode = CaffeineExpiredMode.expireAfterWrite;

  public int getInitalCapacity() {
    return initalCapacity;
  }

  public void setInitalCapacity(int initalCapacity) {
    this.initalCapacity = initalCapacity;
  }

  public long getMaximumSize() {
    return maximumSize;
  }

  public void setMaximumSize(long maximumSize) {
    this.maximumSize = maximumSize;
  }

  public long getMaximumWeight() {
    return maximumWeight;
  }

  public void setMaximumWeight(long maximumWeight) {
    this.maximumWeight = maximumWeight;
  }

  public int getExpireTime() {
    return expireTime;
  }

  public void setExpireTime(int expireTime) {
    this.expireTime = expireTime;
  }

  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  public void setTimeUnit(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  public CaffeineExpiredMode getCaffeineExpiredMode() {
    return caffeineExpiredMode;
  }

  public void setCaffeineExpiredMode(CaffeineExpiredMode caffeineExpiredMode) {
    this.caffeineExpiredMode = caffeineExpiredMode;
  }
}
