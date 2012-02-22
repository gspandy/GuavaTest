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

	public static class LoaderContext {

		public String getValue() {
			return Thread.currentThread().getName();
		}
	}

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

	private static LoadingCache<String, ValueClass> cache = //
	CacheBuilder.newBuilder()//
			.maximumSize(1000)//
			.expireAfterWrite(2, TimeUnit.SECONDS)//
			.removalListener(//
					new RemovalListener<String, ValueClass>() {
						@Override
						public void onRemoval(RemovalNotification<String, ValueClass> notification) {
							log.info("remove {}", notification);
						}
					})//
			.build(//
			new CacheLoader<String, ValueClass>() {
				@Override
				public ValueClass load(String key) throws Exception {
					log.info("{} - load {}", Thread.currentThread().getName(), key);
					return new ValueClass("static");
				}

				// optional
				public Map<String, ValueClass> loadAll(Iterable<? extends String> keys) throws Exception {
					log.info("{} - loadAll [{}]", Thread.currentThread().getName(), Joiner.on(", ").join(keys));
					return super.loadAll(keys);
				}
			});

	private static ContextedLoadingCache<String, ValueClass, LoaderContext> contextedCache = //
	new ContextedLoadingCache<String, ValueClass, LoaderContext>(//
			CacheBuilder.newBuilder()//
					.maximumSize(1000)//
					.expireAfterWrite(2, TimeUnit.SECONDS)//
					.removalListener(//
							new ContextedRemovalListener<String, ValueClass, LoaderContext>() {
								@Override
								public void onRemoval(LoaderContext context, String key, ValueClass value) {
									log.info("remove {}={}", key, value);
								}
							})//
					.build(//
					new ContextedCacheLoader<String, ValueClass, LoaderContext>() {
						@Override
						public ValueClass load(LoaderContext context, String key) throws Exception {
							log.info("{} - load {}", Thread.currentThread().getName(), key);
							return new ValueClass(context.getValue());
						}

						// optional
						public Map<String, ValueClass> loadAll(LoaderContext context, Iterable<? extends String> keys) throws Exception {
							log.info("{} - loadAll [{}]", Thread.currentThread().getName(), Joiner.on(", ").join(keys));
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
				return contextedCache.getAll(new LoaderContext(), request);
			}
		});
	}

	private static void test(final CacheTester<String, ValueClass> tester) throws InterruptedException, ExecutionException {
		ExecutorService e = Executors.newFixedThreadPool(10, new ThreadFactory() {
			private int counter = 0;

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "user" + counter++);
			}
		});

		final List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			keys.add("key" + i);
		}

		List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();
		for (int i = 0; i < 100; i++) {
			FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
				@Override
				public String call() throws Exception {
					Random r = new Random();
					Thread.sleep(100 + r.nextInt(300));

					Collections.shuffle(keys);
					List<String> req = keys.subList(0, 1 + r.nextInt(keys.size() - 1));

					log.info("{} - req: {}", Thread.currentThread().getName(), req);
					Stopwatch timer = new Stopwatch().start();
					String result = Thread.currentThread().getName() + " - result: " + tester.doGetAll(req);
					long time = timer.stop().elapsedMillis();
					return result + " (" + time + ")";
				}

			});
			e.execute(task);
			tasks.add(task);
		}

		for (FutureTask<String> task : tasks)
			log.info(task.get());

		e.shutdown();
		log.info("{}", tester.getStats());
	}
}
