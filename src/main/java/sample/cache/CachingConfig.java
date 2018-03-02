package sample.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import sample.springframework.cache.Cache2kCacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * Defines the cache manager explicitly. There is no auto configure support for cache2k in
 * spring boot yet.
 *
 * @author Jens Wilke
 */
@Configuration
public class CachingConfig {

  @Bean
  public CacheManager cacheManager() {
    return new Cache2kCacheManager() {
      @Nullable
      @Override
      public Cache getCache(final String name) {
        // Thread.dumpStack();
        return super.getCache(name);
      }
    };
  }

  @Bean
  public CacheResolver loadingCacheResolver(Cache2kCacheManager mgr) {
    return new ExperimentalLoadingCache2kCacheResolver(mgr);
  }

}
