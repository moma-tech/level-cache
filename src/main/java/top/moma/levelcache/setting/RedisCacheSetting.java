package top.moma.levelcache.setting;

import java.util.concurrent.TimeUnit;

/**
 * RedisCacheSetting
 *
 * @author Created by ivan on 2020/6/30 .
 * @version 1.0
 */
public class RedisCacheSetting implements java.io.Serializable {
  private static final long serialVersionUID = -3923012495086794506L;

  /** 缓存过期时间 */
  private long expiration = 0;
  /** 过期时间单位 */
  private TimeUnit timeUnit = TimeUnit.MICROSECONDS;
  /** 使用缓存名前缀 */
  private boolean usePrefix = true;
  /** 方法级硬刷新 */
  private boolean hardRefresh = false;
  /** 过期前自动刷新 */
  private boolean autoRenew = false;
  /** 自动刷新阈值 */
  private long renewThreshold = 0;

  public long getExpiration() {
    return expiration;
  }

  public void setExpiration(long expiration) {
    this.expiration = expiration;
  }

  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  public void setTimeUnit(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  public boolean isUsePrefix() {
    return usePrefix;
  }

  public void setUsePrefix(boolean usePrefix) {
    this.usePrefix = usePrefix;
  }

  public boolean isHardRefresh() {
    return hardRefresh;
  }

  public void setHardRefresh(boolean hardRefresh) {
    this.hardRefresh = hardRefresh;
  }

  public boolean isAutoRenew() {
    return autoRenew;
  }

  public void setAutoRenew(boolean autoRenew) {
    this.autoRenew = autoRenew;
  }

  public long getRenewThreshold() {
    return renewThreshold;
  }

  public void setRenewThreshold(long renewThreshold) {
    this.renewThreshold = renewThreshold;
  }
}
