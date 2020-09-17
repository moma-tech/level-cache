package top.moma.levelcache.setting;

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
