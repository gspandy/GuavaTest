package hu.plajko;

import hu.plajko.cache.ContextedLoadingCache;
import hu.plajko.cache.ContextedLoadingCache.ContextedCacheLoader;
import hu.plajko.cache.ContextedLoadingCache.ContextedRemovalListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class CacheTest2 {

	private static final Logger log = LoggerFactory.getLogger(CacheTest2.class);

	// a sample context class
	public static class LoaderContext {

		private String constructorThread = Thread.currentThread().getName();

		public String getValue() {
			return constructorThread;
		}
	}

	// a sample value class for the cache
	public static class ValueClass {
		private Stopwatch timer = null;
		private String name = null;

		public ValueClass(String name) {
			this.name = name;
			this.timer = new Stopwatch().start();
		}

		@Override
		public String toString() {
			return name + "(" + timer + ")";
		}
	}

	private static final int MAXSIZE = 1000;
	private static final int TIME = 30;
	private static final TimeUnit TIMEUNIT = TimeUnit.MILLISECONDS;

	// original LoadingCache construction
	private static LoadingCache<String, ValueClass> cache = //
	CacheBuilder.newBuilder()//
			.maximumSize(MAXSIZE)//
			.expireAfterWrite(TIME, TIMEUNIT)//
			.removalListener(// optional removal listener
					new RemovalListener<String, ValueClass>() {
						@Override
						public void onRemoval(RemovalNotification<String, ValueClass> notification) {
							log.debug("remove {}", notification);
						}
					})//
			.build(//
			new CacheLoader<String, ValueClass>() {
				@Override
				public ValueClass load(String key) throws Exception {
					log.debug("{} - load {}", Thread.currentThread().getName(), key);
					return new ValueClass("???");
				}

				// optional override of the loadAll method
				@Override
				public Map<String, ValueClass> loadAll(Iterable<? extends String> keys) throws Exception {
					log.debug("{} - loadAll [{}]", Thread.currentThread().getName(), Joiner.on(", ").join(keys));
					return super.loadAll(keys);
				}
			});

	// "contexted" LoadingCache construction, it's a wrapper class
	private static ContextedLoadingCache<String, ValueClass, LoaderContext> contextedCache = //
	new ContextedLoadingCache<String, ValueClass, LoaderContext>(//
			CacheBuilder.newBuilder()//
					.maximumSize(MAXSIZE)//
					.expireAfterWrite(TIME, TIMEUNIT)//
					.removalListener(// optional removal listener
							new ContextedRemovalListener<String, ValueClass, LoaderContext>() {
								@Override
								public void onRemoval(LoaderContext context, String key, ValueClass value) {
									log.debug("remove {}={}", key, value);
								}
							})//
					.build(//
					new ContextedCacheLoader<String, ValueClass, LoaderContext>() {
						@Override
						public ValueClass load(LoaderContext context, String key) throws Exception {
							log.debug("{} - load {}", Thread.currentThread().getName(), key);
							return new ValueClass(context.getValue());
						}

						// optional override of the loadAll method
						@Override
						public Map<String, ValueClass> loadAll(LoaderContext context, Iterable<? extends String> keys) throws Exception {
							log.debug("{} - loadAll [{}]", Thread.currentThread().getName(), Joiner.on(", ").join(keys));
							return super.loadAll(context, keys);
						}
					}));

	private static interface CacheTester<K, V> {
		public CacheStats getStats();

		public Map<K, ? extends ValueClass> doGetAll(Iterable<? extends K> request) throws Exception;
	}

	public static void main(String[] args) throws Exception {

		test(new CacheTester<String, ValueClass>() {
			@Override
			public CacheStats getStats() {
				return cache.stats();
			}

			@Override
			public Map<String, ? extends ValueClass> doGetAll(Iterable<? extends String> request) throws Exception {
				// cannot pass object(s) to the loader
				return cache.getAll(request);
			}
		});

		test(new CacheTester<String, ValueClass>() {
			@Override
			public CacheStats getStats() {
				return contextedCache.stats();
			}

			@Override
			public Map<String, ? extends ValueClass> doGetAll(Iterable<? extends String> request) throws Exception {
				// possible to pass a complex object to the loader
				return contextedCache.getAll(new LoaderContext(), request);
			}
		});
		
	}

	private static void test(final CacheTester<String, ValueClass> tester) throws InterruptedException, ExecutionException {
		// do parallel testing
		ExecutorService executor = Executors.newFixedThreadPool(10, new ThreadFactory() {
			private int counter = 0;

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "user" + counter++);
			}
		});

		// create a collection of keys
		final List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 100; i++) {
			keys.add("key" + i);
		}

		// create tasks for testing the cache
		List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();
		for (int i = 0; i < 500; i++) {
			FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
				@Override
				public String call() throws Exception {
					Random r = new Random();

					// generate a random request
					Collections.shuffle(keys);
					List<String> req = keys.subList(0, 1 + r.nextInt(keys.size() - 1));

					log.debug("{} - req: {}", Thread.currentThread().getName(), req);
					Stopwatch timer = new Stopwatch().start();
					StringBuilder sb = //
					new StringBuilder(Thread.currentThread().getName())//
							.append(" - result: ")//
							.append(tester.doGetAll(req));
					return sb.append(" (").append(timer.stop().elapsedMillis()).append(")").toString();
				}

			});
			executor.execute(task);
			tasks.add(task);
		}

		// wait for worker threads for results
		for (FutureTask<String> task : tasks)
			log.debug(task.get());

		executor.shutdown();
		log.info("{}", tester.getStats());
	}
}
