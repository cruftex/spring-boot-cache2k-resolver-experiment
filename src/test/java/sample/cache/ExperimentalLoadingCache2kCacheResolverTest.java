package sample.cache;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;
import org.springframework.cache.Cache;
import sample.springframework.cache.Cache2kCache;
import sample.springframework.cache.Cache2kCacheManager;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jens Wilke
 */
public class ExperimentalLoadingCache2kCacheResolverTest {

        /**
         * Run each test within a unique test manager.
         */
        Cache2kCacheManager manager = new Cache2kCacheManager();
        ExperimentalLoadingCache2kCacheResolver resolver =
          new ExperimentalLoadingCache2kCacheResolver(manager);

        @After
        public void tearDown() {
                manager.getNativeCacheManager().close();
        }

        @Test
        public void testResolverWiresInLoader() throws Exception {
                assertFalse(manager.getNativeCacheManager().getActiveCaches().iterator().hasNext());
                Object target = new DummyTarget();
                Method method = target.getClass().getMethod("retrieve", Object.class);
                CacheOperationInvocationContext<BasicOperation> ctx =
                                createDummyInvokationContext(new String[]{"hello2", "asdf2"}, target, method);
                Collection<? extends Cache> cache = resolver.resolveCaches(ctx);
                assertNotNull(cache);
                assertEquals(2, cache.size());
                Cache[] caches = cache.toArray(new Cache[cache.size()]);
                assertEquals("hello", caches[0].get("hello").get());
                assertTrue(((Cache2kCache) manager.getCache("asdf2")).isLoaderPresent());
        }

        @Test
        public void testRejectTwoArguments() throws Exception {
                Object target = new DummyTarget();
                Method method = target.getClass().getMethod("retrieve2arg", Object.class,  Object.class);
                CacheOperationInvocationContext<BasicOperation> ctx =
                                createDummyInvokationContext(new String[]{"hello", "asdf"}, target, method);
                try {
                        resolver.resolveCaches(ctx);
                        fail();
                } catch (IllegalArgumentException ex) {
                        // expected
                }
        }

        @Test
        public void testRejectSecondMethod() throws Exception {
                Object target = new DummyTarget();
                Method method = target.getClass().getMethod("retrieve", Object.class);
                CacheOperationInvocationContext<BasicOperation> ctx =
                                createDummyInvokationContext(new String[]{"hello", "asdf"}, target, method);
                resolver.resolveCaches(ctx);
                Method method2 = target.getClass().getMethod("retrieveSecond", Object.class);
                CacheOperationInvocationContext<BasicOperation> ctx2 =
                                createDummyInvokationContext(new String[]{"hello", "asdf"}, target, method2);
                try {
                        resolver.resolveCaches(ctx2);
                        fail();
                } catch (IllegalArgumentException ex) {
                        // expected
                }
        }


        private CacheOperationInvocationContext<BasicOperation> createDummyInvokationContext(
                        final String[] names, final Object target, final Method method) {
                final Set<String> namesSet = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(names)));
                return new CacheOperationInvocationContext<BasicOperation>() {
                        @Override
                        public BasicOperation getOperation() {
                                return new BasicOperation() {
                                        @Override
                                        public Set<String> getCacheNames() {
                                                return namesSet;
                                        }
                                };
                        }

                        @Override
                        public Object getTarget() {
                                return target;
                        }

                        @Override
                        public Method getMethod() {
                                return method;
                        }

                        @Override
                        public Object[] getArgs() {
                                return new Object[0];
                        }
                };
        }


        public static class DummyTarget {

                public Object retrieveSecond(Object key) { return key; }
                public Object retrieve(Object key) { return key; }
                public Object retrieve2arg(Object key1, Object key2) { return null; }

        }


}
