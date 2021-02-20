package top.moma.levelcache.cache.redis;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;
import top.moma.levelcache.support.CacheConstants;
import top.moma.m64.core.constants.StringConstants;
import top.moma.m64.core.helper.StringHelper;

import java.util.Arrays;

/**
 * RedisKeyHelper
 *
 * @author Created by ivan on 2020/7/9 .
 * @version 1.0
 */
public class RedisKey {
  private String prefix;
  private boolean usePrefix = false;

  private final Object keyBody;
  private final RedisSerializer keySerializer;

  public RedisKey(Object key, RedisSerializer keySerializer) {
    Assert.notNull(key, "Key can not be Null");
    Assert.notNull(keySerializer, "Serializer can not be null");
    this.keyBody = key;
    this.keySerializer = keySerializer;
  }

  public RedisKey(String prefix, boolean usePrefix, Object key, RedisSerializer keySerializer) {

    Assert.notNull(key, "Key can not be Null");
    Assert.notNull(keySerializer, "Serializer can not be null");
    this.prefix = prefix;
    this.usePrefix = usePrefix;
    this.keyBody = key;
    this.keySerializer = keySerializer;
  }

  public RedisKey prefix(boolean usePrefix, String keyPrefix) {
    this.usePrefix = usePrefix;
    if (StringHelper.isEmpty(keyPrefix)) {
      this.prefix = CacheConstants.DEFAULT_PREFIX;
    } else {
      this.prefix = keyPrefix;
    }
    return this;
  }

  public String getRedisKey() {
    if (usePrefix) {
      byte[] bodyKey = this.serializeKeyBody();
      byte[] prefixKey = this.serializeKeyPrefix();
      byte[] finalKey = Arrays.copyOf(prefixKey, prefixKey.length + bodyKey.length);
      System.arraycopy(bodyKey, 0, finalKey, prefixKey.length, bodyKey.length);
      return StringHelper.toString(finalKey, StringConstants.UTF_8);
    }
    return StringHelper.toString(this.serializeKeyBody(), StringConstants.UTF_8);
  }

  private byte[] serializeKeyBody() {
    return keySerializer.serialize(keyBody);
  }

  private byte[] serializeKeyPrefix() {
    RedisSerializer<String> prefixSerializer = new StringRedisSerializer();
    if (StringHelper.isNotEmpty(prefix)) {
      return prefixSerializer.serialize(prefix);
    } else {
      return new byte[0];
    }
  }
}
