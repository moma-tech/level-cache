package top.moma.levelcache.aspect;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import top.moma.levelcache.annotation.*;
import top.moma.levelcache.manager.CacheManager;
import top.moma.levelcache.setting.CaffeineCacheSetting;
import top.moma.levelcache.setting.MomaCacheSetting;
import top.moma.levelcache.setting.RedisCacheSetting;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;

/**
 * 缓存拦截，用于注册方法信息
 *
 * @author yuhao.wang
 */
@Aspect
@Slf4j
public class LayeringAspect {

  private static final String CACHE_KEY_ERROR_MESSAGE = "缓存Key %s 不能为NULL";
  private static final String CACHE_NAME_ERROR_MESSAGE = "缓存名称不能为NULL";

  /** SpEL表达式计算器 */
  private final CacheOperationExpressionEvaluator evaluator =
      new CacheOperationExpressionEvaluator();

  @Autowired private CacheManager cacheManager;

  @Autowired(required = false)
  private KeyGenerator keyGenerator = new SimpleKeyGenerator();

  @Pointcut("@annotation(com.github.xiaolyuh.annotation.Cacheable)")
  public void cacheablePointcut() {}

  @Pointcut("@annotation(com.github.xiaolyuh.annotation.CacheEvict)")
  public void cacheEvictPointcut() {}

  @Pointcut("@annotation(com.github.xiaolyuh.annotation.CachePut)")
  public void cachePutPointcut() {}

  @Around("cacheablePointcut()")
  public Object cacheablePointcut(ProceedingJoinPoint joinPoint) throws Throwable {
    CacheOperationInvoker aopAllianceInvoker = getCacheOperationInvoker(joinPoint);

    // 获取method
    Method method = this.getSpecificmethod(joinPoint);
    // 获取注解
    Cacheable cacheable = AnnotationUtils.findAnnotation(method, Cacheable.class);

    try {
      // 执行查询缓存方法
      return executeCacheable(
          aopAllianceInvoker, cacheable, method, joinPoint.getArgs(), joinPoint.getTarget());
    } catch (SerializationException e) {
      // 如果是序列化异常需要先删除原有缓存
      String[] cacheNames = cacheable.cacheNames();
      // 删除缓存
      delete(cacheNames, cacheable.key(), method, joinPoint.getArgs(), joinPoint.getTarget());

      // 忽略操作缓存过程中遇到的异常
      if (cacheable.ignoreException()) {
        log.warn(e.getMessage(), e);
        return aopAllianceInvoker.invoke();
      }
      throw e;
    } catch (Exception e) {
      // 忽略操作缓存过程中遇到的异常
      if (cacheable.ignoreException()) {
        log.warn(e.getMessage(), e);
        return aopAllianceInvoker.invoke();
      }
      throw e;
    }
  }

  @Around("cacheEvictPointcut()")
  public Object cacheEvictPointcut(ProceedingJoinPoint joinPoint) throws Throwable {
    CacheOperationInvoker aopAllianceInvoker = getCacheOperationInvoker(joinPoint);

    // 获取method
    Method method = this.getSpecificmethod(joinPoint);
    // 获取注解
    CacheEvict cacheEvict = AnnotationUtils.findAnnotation(method, CacheEvict.class);

    try {
      // 执行查询缓存方法
      return executeEvict(
          aopAllianceInvoker, cacheEvict, method, joinPoint.getArgs(), joinPoint.getTarget());
    } catch (Exception e) {
      // 忽略操作缓存过程中遇到的异常
      if (cacheEvict.ignoreException()) {
        log.warn(e.getMessage(), e);
        return aopAllianceInvoker.invoke();
      }
      throw e;
    }
  }

  @Around("cachePutPointcut()")
  public Object cachePutPointcut(ProceedingJoinPoint joinPoint) throws Throwable {
    CacheOperationInvoker aopAllianceInvoker = getCacheOperationInvoker(joinPoint);

    // 获取method
    Method method = this.getSpecificmethod(joinPoint);
    // 获取注解
    CachePut cacheEvict = AnnotationUtils.findAnnotation(method, CachePut.class);

    try {
      // 执行查询缓存方法
      return executePut(
          aopAllianceInvoker, cacheEvict, method, joinPoint.getArgs(), joinPoint.getTarget());
    } catch (Exception e) {
      // 忽略操作缓存过程中遇到的异常
      if (cacheEvict.ignoreException()) {
        log.warn(e.getMessage(), e);
        return aopAllianceInvoker.invoke();
      }
      throw e;
    }
  }

  /**
   * 执行Cacheable切面
   *
   * @param invoker 缓存注解的回调方法
   * @param cacheable {@link Cacheable}
   * @param method {@link Method}
   * @param args 注解方法参数
   * @param target target
   * @return {@link Object}
   */
  private Object executeCacheable(
      CacheOperationInvoker invoker,
      Cacheable cacheable,
      Method method,
      Object[] args,
      Object target) {

    // 解析SpEL表达式获取cacheName和key
    String[] cacheNames = cacheable.cacheNames();
    Assert.notEmpty(cacheable.cacheNames(), CACHE_NAME_ERROR_MESSAGE);
    String cacheName = cacheNames[0];
    Object key = generateKey(cacheable.key(), method, args, target);
    Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, cacheable.key()));

    // 从注解中获取缓存配置
    FirstCache firstCache = cacheable.firstCache();
    SecondaryCache secondaryCache = cacheable.secondaryCache();
    CaffeineCacheSetting firstCacheSetting =
        new CaffeineCacheSetting(
            firstCache.initialCapacity(),
            firstCache.maximumSize(),
            firstCache.maximumWeight(),
            firstCache.expireTime(),
            firstCache.timeUnit(),
            firstCache.expireMode());

    RedisCacheSetting secondaryCacheSetting =
        new RedisCacheSetting(
            secondaryCache.expireTime(),
            secondaryCache.timeUnit(),
            secondaryCache.usePrefix(),
            secondaryCache.hardRefresh(),
            secondaryCache.autoRenew(),
            secondaryCache.renewThreshold(),
            secondaryCache.nullExpiration(),
            secondaryCache.expireMode());

    MomaCacheSetting momaCacheSetting =
        new MomaCacheSetting(firstCacheSetting, secondaryCacheSetting);

    // 通过cacheName和缓存配置获取Cache
    Cache cache = cacheManager.getCache(cacheName, momaCacheSetting);

    // 通Cache获取值
    return cache.get(key, () -> invoker.invoke());
  }

  /**
   * 执行 CacheEvict 切面
   *
   * @param invoker 缓存注解的回调方法
   * @param cacheEvict {@link CacheEvict}
   * @param method {@link Method}
   * @param args 注解方法参数
   * @param target target
   * @return {@link Object}
   */
  private Object executeEvict(
      CacheOperationInvoker invoker,
      CacheEvict cacheEvict,
      Method method,
      Object[] args,
      Object target) {

    // 解析SpEL表达式获取cacheName和key
    String[] cacheNames = cacheEvict.cacheNames();
    Assert.notEmpty(cacheEvict.cacheNames(), CACHE_NAME_ERROR_MESSAGE);
    // 判断是否删除所有缓存数据
    if (cacheEvict.allEntries()) {
      // 删除所有缓存数据（清空）
      for (String cacheName : cacheNames) {
        Collection<Cache> caches = cacheManager.getCache(cacheName);
        if (CollectionUtils.isEmpty(caches)) {
          // 如果没有找到Cache就新建一个默认的
          Cache cache =
              cacheManager.getCache(
                  cacheName,
                  new LayeringCacheSetting(
                      new FirstCacheSetting(), new SecondaryCacheSetting(), "默认缓存配置（清除时生成）"));
          cache.clear();
        } else {
          for (Cache cache : caches) {
            cache.clear();
          }
        }
      }
    } else {
      // 删除指定key
      delete(cacheNames, cacheEvict.key(), method, args, target);
    }

    // 执行方法
    return invoker.invoke();
  }

  /**
   * 删除执行缓存名称上的指定key
   *
   * @param cacheNames 缓存名称
   * @param keySpEL key的SpEL表达式
   * @param method {@link Method}
   * @param args 参数列表
   * @param target 目标类
   */
  private void delete(
      String[] cacheNames, String keySpEL, Method method, Object[] args, Object target) {
    Object key = generateKey(keySpEL, method, args, target);
    Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, keySpEL));
    for (String cacheName : cacheNames) {
      Collection<Cache> caches = cacheManager.getCache(cacheName);
      if (CollectionUtils.isEmpty(caches)) {
        // 如果没有找到Cache就新建一个默认的
        Cache cache =
            cacheManager.getCache(
                cacheName,
                new LayeringCacheSetting(
                    new FirstCacheSetting(), new SecondaryCacheSetting(), "默认缓存配置（删除时生成）"));
        cache.evict(key);
      } else {
        for (Cache cache : caches) {
          cache.evict(key);
        }
      }
    }
  }

  /**
   * 执行 CachePut 切面
   *
   * @param invoker 缓存注解的回调方法
   * @param cachePut {@link CachePut}
   * @param method {@link Method}
   * @param args 注解方法参数
   * @param target target
   * @return {@link Object}
   */
  private Object executePut(
      CacheOperationInvoker invoker,
      CachePut cachePut,
      Method method,
      Object[] args,
      Object target) {

    String[] cacheNames = cachePut.cacheNames();
    Assert.notEmpty(cachePut.cacheNames(), CACHE_NAME_ERROR_MESSAGE);
    // 解析SpEL表达式获取 key
    Object key = generateKey(cachePut.key(), method, args, target);
    Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, cachePut.key()));

    // 从解决中获取缓存配置
    FirstCache firstCache = cachePut.firstCache();
    SecondaryCache secondaryCache = cachePut.secondaryCache();
    FirstCacheSetting firstCacheSetting =
        new FirstCacheSetting(
            firstCache.initialCapacity(),
            firstCache.maximumSize(),
            firstCache.expireTime(),
            firstCache.timeUnit(),
            firstCache.expireMode());

    SecondaryCacheSetting secondaryCacheSetting =
        new SecondaryCacheSetting(
            secondaryCache.expireTime(),
            secondaryCache.preloadTime(),
            secondaryCache.timeUnit(),
            secondaryCache.forceRefresh(),
            secondaryCache.isAllowNullValue(),
            secondaryCache.magnification());

    LayeringCacheSetting layeringCacheSetting =
        new LayeringCacheSetting(firstCacheSetting, secondaryCacheSetting, cachePut.depict());

    // 指定调用方法获取缓存值
    Object result = invoker.invoke();

    for (String cacheName : cacheNames) {
      // 通过cacheName和缓存配置获取Cache
      Cache cache = cacheManager.getCache(cacheName, layeringCacheSetting);
      cache.put(key, result);
    }

    return result;
  }

  private CacheOperationInvoker getCacheOperationInvoker(ProceedingJoinPoint joinPoint) {
    return () -> {
      try {
        return joinPoint.proceed();
      } catch (Throwable ex) {
        throw new CacheOperationInvoker.ThrowableWrapperException(ex);
      }
    };
  }

  /**
   * 解析SpEL表达式，获取注解上的key属性值
   *
   * @return Object
   */
  private Object generateKey(String keySpEl, Method method, Object[] args, Object target) {

    // 获取注解上的key属性值
    Class<?> targetClass = getTargetClass(target);
    if (StringUtils.hasText(keySpEl)) {
      EvaluationContext evaluationContext =
          evaluator.createEvaluationContext(
              method, args, target, targetClass, CacheOperationExpressionEvaluator.NO_RESULT);

      AnnotatedElementKey methodCacheKey = new AnnotatedElementKey(method, targetClass);
      // 兼容传null值得情况
      Object keyValue = evaluator.key(keySpEl, methodCacheKey, evaluationContext);
      return Objects.isNull(keyValue) ? "null" : keyValue;
    }
    return this.keyGenerator.generate(target, method, args);
  }

  /**
   * 获取类信息
   *
   * @param target Object
   * @return targetClass
   */
  private Class<?> getTargetClass(Object target) {
    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
    if (targetClass == null) {
      targetClass = target.getClass();
    }
    return targetClass;
  }

  /**
   * 获取Method
   *
   * @param pjp ProceedingJoinPoint
   * @return {@link Method}
   */
  private Method getSpecificmethod(ProceedingJoinPoint pjp) {
    MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
    Method method = methodSignature.getMethod();
    // The method may be on an interface, but we need attributes from the
    // target class. If the target class is null, the method will be
    // unchanged.
    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(pjp.getTarget());
    if (targetClass == null && pjp.getTarget() != null) {
      targetClass = pjp.getTarget().getClass();
    }
    Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
    // If we are dealing with method with generic parameters, find the
    // original method.
    specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
    return specificMethod;
  }
}
