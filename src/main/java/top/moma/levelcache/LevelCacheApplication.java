package top.moma.levelcache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;

/**
 * @author ivan
 */
@SpringBootApplication
public class LevelCacheApplication {

	public static void main(String[] args) {
		SpringApplication.run(LevelCacheApplication.class, args);
	}

}
