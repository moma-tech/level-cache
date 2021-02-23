package top.moma.levelcache.annotation;

import top.moma.levelcache.support.ExpiredMode;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/** First level cache Created by ivan at 2/22/21. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface FirstCache {
  /**
   * 缓存初始Size
   *
   * @return int
   */
  int initialCapacity() default 10;

  /**
   * 缓存最大Size
   *
   * @return int
   */
  int maximumSize() default 5000;

  /**
   * 缓存有效时间
   *
   * @return int
   */
  int expireTime() default 9;

  /** 缓存的最大权重 */
  long maximumWeight() default -1L;

  /**
   * 缓存时间单位
   *
   * @return TimeUnit
   */
  TimeUnit timeUnit() default TimeUnit.MINUTES;

  /**
   * 缓存失效模式
   *
   * @return ExpiredMode
   * @see ExpiredMode
   */
  ExpiredMode expireMode() default ExpiredMode.expireAfterWrite;
}
