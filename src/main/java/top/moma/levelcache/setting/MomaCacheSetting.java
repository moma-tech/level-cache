package top.moma.levelcache.setting;

import java.util.Objects;

/**
 * MomaCacheSetting
 *
 * @author Created by ivan on 2020/7/16 .
 * @version 1.0
 */
public class MomaCacheSetting {

  private CaffeineCacheSetting caffeineCacheSetting;

  private RedisCacheSetting redisCacheSetting;

  private MomaCacheMode cacheMode = MomaCacheMode.ALL_CACHE;

  private String cacheId;

  public MomaCacheSetting(
      CaffeineCacheSetting caffeineCacheSetting, RedisCacheSetting redisCacheSetting) {
    this.caffeineCacheSetting = caffeineCacheSetting;
    this.redisCacheSetting = redisCacheSetting;
    setCacheId();
  }

  private void setCacheId() {
    StringBuilder stringBuilder = new StringBuilder("moma");
    if (Objects.nonNull(caffeineCacheSetting)) {
      stringBuilder
          .append("-cfc-")
          .append(
              caffeineCacheSetting.getTimeUnit().toMillis(caffeineCacheSetting.getExpireTime()));
    }
    if (Objects.nonNull(redisCacheSetting)) {
      stringBuilder
          .append("-redis-")
          .append(redisCacheSetting.getTimeUnit().toMillis(redisCacheSetting.getExpireTime()));
    }
    stringBuilder.append("-cache");
    cacheId = stringBuilder.toString();
  }

  public String getCacheId() {
    return cacheId;
  }

  public CaffeineCacheSetting getCaffeineCacheSetting() {
    return caffeineCacheSetting;
  }

  public void setCaffeineCacheSetting(CaffeineCacheSetting caffeineCacheSetting) {
    this.caffeineCacheSetting = caffeineCacheSetting;
  }

  public RedisCacheSetting getRedisCacheSetting() {
    return redisCacheSetting;
  }

  public void setRedisCacheSetting(RedisCacheSetting redisCacheSetting) {
    this.redisCacheSetting = redisCacheSetting;
  }

  public MomaCacheMode getCacheMode() {
    return cacheMode;
  }

  public void setCacheMode(MomaCacheMode cacheMode) {
    this.cacheMode = cacheMode;
  }
}
