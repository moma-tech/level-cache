package top.moma.levelcache.annotation;

import top.moma.levelcache.support.ExpiredMode;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/** Secondary level cache Created by ivan at 2/22/21. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SecondaryCache {
  /**
   * 缓存有效时间
   *
   * @return long
   */
  long expireTime() default 5;

  /**
   * 时间单位 {@link TimeUnit}
   *
   * @return TimeUnit
   */
  TimeUnit timeUnit() default TimeUnit.HOURS;
  /** 使用缓存名前缀 */
  boolean usePrefix() default true;

  /**
   * 是否强制刷新（直接执行被缓存方法），默认是false
   *
   * @return boolean
   */
  boolean hardRefresh() default false;

  /** 过期前自动刷新 */
  boolean autoRenew() default false;

  /** 自动刷新阈值 */
  long renewThreshold() default 0;

  /**
   * 非空值和null值之间的时间倍率，默认是1。isAllowNullValue=true才有效
   *
   * <p>如配置缓存的有效时间是200秒，倍率这设置成10， 那么当缓存value为null时，缓存的有效时间将是20秒，非空时为200秒
   *
   * @return int
   */
  long nullExpiration() default 1;

  /**
   * 缓存失效模式
   *
   * @return ExpiredMode
   * @see ExpiredMode
   */
  ExpiredMode expireMode() default ExpiredMode.expireAfterWrite;
}
