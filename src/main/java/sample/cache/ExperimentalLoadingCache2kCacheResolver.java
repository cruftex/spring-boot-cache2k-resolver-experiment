package sample.cache;

import org.cache2k.Cache2kBuilder;
import org.cache2k.integration.CacheLoader;
import org.springframework.cache.Cache;
import sample.springframework.cache.Cache2kCache;
import sample.springframework.cache.Cache2kCacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EXPERIMENTAL: the idea is not working as soon as other annotations like @CachePut or @CacheEvict are used
 * and those are called first. It would be nice to create a loading cache at class load time.
 *
 * <p>This is a cache resolver that creates a loading cache which is backed by the annotated method.
 * This only works for the simple case that there is a single method annotated with {@code Cacheable}
 * and this method has only one parameter which is used as the cache key.
 *
 * <p>Using the loader has the benefits that the cache2k features
 * <a href="https://cache2k.org/docs/1.0/user-guide.html#refresh-ahead">refresh ahead</a>
 * and <a href="https://cache2k.org/docs/1.0/user-guide.html#resilience-and-exceptions">resilience</a></a>
 * can be used.
 *
 * <p>Supporting more then one argument would require a reverse mapping of the cache key.
 * That would need an extension of the {@link org.springframework.cache.interceptor.KeyGenerator}.
 *
 * @author Jens Wilke
 */
public class ExperimentalLoadingCache2kCacheResolver implements CacheResolver {

	final private Cache2kCacheManager manager;
	final private Map<Set<String>, ResolvedCachesInContextLoader> names2caches =
			new ConcurrentHashMap<Set<String>, ResolvedCachesInContextLoader>();

	public ExperimentalLoadingCache2kCacheResolver(final Cache2kCacheManager mgr) {
		manager = mgr;
	}

	@Override
	public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
		ResolvedCachesInContextLoader resolved = resolveCachesWithLoader(context);
		if (resolved.cachableMethod != context.getMethod() ||
				resolved.target != context.getTarget()) {
			throw new IllegalArgumentException(ExperimentalLoadingCache2kCacheResolver.class.getSimpleName() +
					" can only be used with a unique name on a single cacheable method and target instance");
		}
		return resolved.caches;
	}

	@SuppressWarnings("unchecked")
	private ResolvedCachesInContextLoader resolveCachesWithLoader(CacheOperationInvocationContext<?> context) {
		Set<String> names = context.getOperation().getCacheNames();
		ResolvedCachesInContextLoader cachesAndContext = names2caches.get(names);
		if (cachesAndContext != null) {
			return cachesAndContext;
		}
		final Map<String, Cache> cacheMap = manager.getCacheMap();
		synchronized (cacheMap) {
			cachesAndContext = names2caches.get(names);
			if (cachesAndContext != null) {
				return cachesAndContext;
			}
			// actually I would like to check also whether there is a custom key generator in use.
			// unfortunately, the key generator is not exposed via the interface.
			if (context.getMethod().getParameterCount() != 1) {
				throw new IllegalArgumentException(
						ExperimentalLoadingCache2kCacheResolver.class.getSimpleName() +
						" only supports methods with a single parameter (cache key)");
			}
			Collection<Cache2kCache> caches = new ArrayList<Cache2kCache>();
			cachesAndContext = new ResolvedCachesInContextLoader(caches, context.getTarget(), context.getMethod());
			for (String name : names) {
				if (cacheMap.containsKey(name)) {
					throw new IllegalStateException("cache '" + name + "' already existing");
				}
				org.cache2k.Cache<Object,Object> cache =
						Cache2kCacheManager.configureCache(Cache2kBuilder.forUnknownTypes(), name)
								.loader(cachesAndContext)
								.build();
				Cache2kCache wrappedCache = new Cache2kCache(cache, true);
				cacheMap.put(name, wrappedCache);
				caches.add(wrappedCache);
			}
			names2caches.put(names, cachesAndContext);
		}
		return cachesAndContext;
	}

	private static class ResolvedCachesInContextLoader extends CacheLoader<Object,Object> {

		final Object target;
		final Method cachableMethod;
		final Collection<? extends Cache> caches;

		public ResolvedCachesInContextLoader(final Collection<? extends Cache> caches, Object target, Method method) {
			this.caches = caches;
			this.target = target;
			this.cachableMethod = method;
		}

		@Override
		public Object load(final Object key) throws Exception {
			return cachableMethod.invoke(target, key);
		}

	}

}
