package top.moma.levelcache.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * JacksonHelper
 *
 * @author Created by ivan on 2020/6/22 .
 * @version 1.0
 */
public class JacksonHelper {
  private static ObjectMapper objectMapper;

  static {
    objectMapper = getObjectMapper(new ObjectMapper());
  }

  /**
   * getObjectMapper
   *
   * <p>return static Object Mapper
   *
   * @author Created by ivan at 下午4:25 2020/1/10.
   * @return com.fasterxml.jackson.databind.ObjectMapper
   */
  public static ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /**
   * getObjectMapper
   *
   * <p>return static Object Mapper base on third Mapper
   *
   * @author Created by ivan at 下午4:25 2020/1/10.
   * @return com.fasterxml.jackson.databind.ObjectMapper
   */
  public static ObjectMapper getObjectMapper(ObjectMapper thirdMapper) {
    if (Objects.isNull(thirdMapper)) {
      return objectMapper;
    }
    return configureObjectMapper(thirdMapper);
  }

  /**
   * setLowCamelCaseMapper
   *
   * <p>Set JacksonHelper as LowCamelCase
   *
   * @author Created by ivan at 下午4:26 2020/1/10.
   */
  public static void setLowCamelCaseMapper() {
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
  }

  /**
   * setSnakeCaseMapper
   *
   * <p>Set JacksonHelper as SnakeCase
   *
   * @author Created by ivan at 下午4:29 2020/1/10.
   */
  public static void setSnakeCaseMapper() {
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
  }

  /**
   * configureObjectMapper
   *
   * <p>Configure Object Mapper
   *
   * @see <a href=
   *     "https://github.com/FasterXML/jackson-databind/wiki/Deserialization-Features">Deserialization
   *     Features</a>
   * @see <a href=
   *     "https://github.com/FasterXML/jackson-databind/wiki/Serialization-Features">Serialization
   *     features</a>
   * @see <a href="https://github.com/FasterXML/jackson-databind/wiki/Mapper-Features">Mapper
   *     Features</a>
   * @author Created by ivan at 上午10:47 2020/1/10.
   * @return com.fasterxml.jackson.databind.ObjectMapper
   */
  static ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
    // Set Naming Strategy
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
    // Set Feature
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
    objectMapper.enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature());

    // Handle Java Time & JSR310
    registerJavaTime(objectMapper);

    // Handler Long 2 String
    registerLongSerializer(objectMapper);

    return objectMapper;
  }

  /**
   * registerJavaTime
   *
   * <p>Handle Time Format to JSR310
   *
   * @author Created by ivan at 下午2:52 2020/1/10.
   * @return com.fasterxml.jackson.databind.ObjectMapper
   */
  public static void registerJavaTime(ObjectMapper objectMapper) {
    // Handle Java Time
    objectMapper.registerModule(new JavaTimeModule());

    // Handle JSR310 Time
    SimpleModule jsr310Module = new SimpleModule();
    jsr310Module.addSerializer(
        LocalDateTime.class,
        new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    jsr310Module.addSerializer(
        LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    jsr310Module.addSerializer(
        LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
    jsr310Module.addDeserializer(
        LocalDateTime.class,
        new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    jsr310Module.addDeserializer(
        LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    jsr310Module.addDeserializer(
        LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
    objectMapper.registerModule(jsr310Module);
  }

  /**
   * registerLongSerializer
   *
   * <p>Handle Long Format to String
   *
   * @author Created by ivan at 下午2:53 2020/1/10.
   */
  public static void registerLongSerializer(ObjectMapper objectMapper) {
    SimpleModule long2StringModule = new SimpleModule();
    // Handle long
    long2StringModule.addSerializer(Long.class, ToStringSerializer.instance);
  }

  /**
   * parse
   *
   * <p>read String to Object
   *
   * @author Created by ivan at 下午4:30 2020/1/10.
   * @return java.lang.Object
   */
  public static Object readValue(String jsonString) {
    Object object = null;
    try {
      object = objectMapper.readValue(jsonString, Object.class);
    } catch (Exception ignore) {
    }
    return object;
  }

  /**
   * readValue
   *
   * <p>read json into simple class
   *
   * @author Created by ivan at 下午5:54 2020/1/10.
   * @return T
   */
  public static <T> T readValue(String json, Class<T> clazz) {
    T t = null;
    try {
      t = objectMapper.readValue(json, clazz);
    } catch (Exception ignore) {
      throw new RuntimeException("Unable to read JSON value: " + json, ignore);
    }
    return t;
  }

  /**
   * readValue
   *
   * <p>read json into complex class
   *
   * @author Created by ivan at 下午5:55 2020/1/10.
   * @return T
   */
  public static <T> T readValue(String json, TypeReference<T> valueTypeRef) {
    T t;
    try {
      t = objectMapper.readValue(json, valueTypeRef);
    } catch (Exception ignore) {
      throw new RuntimeException(ignore);
    }
    return t;
  }

  /**
   * toJson
   *
   * <p>read object into json
   *
   * @author Created by ivan at 下午5:59 2020/1/10.
   * @return java.lang.String
   */
  public static String toJson(Object object) {
    if (Objects.nonNull(object) && CharSequence.class.isAssignableFrom(object.getClass())) {
      return object.toString();
    }
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException ignore) {
      throw new RuntimeException(ignore);
    }
  }
}
