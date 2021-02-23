package top.moma.levelcache.setting;

import lombok.AllArgsConstructor;
import lombok.Data;
import top.moma.levelcache.support.ExpiredMode;

import java.util.concurrent.TimeUnit;

/**
 * RedisCacheSetting
 *
 * @author Created by ivan on 2020/6/30 .
 * @version 1.0
 */
@Data
@AllArgsConstructor
public class RedisCacheSetting implements java.io.Serializable {
  private static final long serialVersionUID = -3923012495086794506L;

  /** 缓存过期时间 */
  private long expireTime = 0;
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
  /** 空值过期时间 */
  private long nullExpiration = 5000;
  /** 缓存过期模式 */
  private ExpiredMode redisExpireMode = ExpiredMode.expireAfterWrite;

  public boolean isUsePrefix() {
    return usePrefix;
  }

  public boolean isHardRefresh() {
    return hardRefresh;
  }

  public boolean isAutoRenew() {
    return autoRenew;
  }
}
