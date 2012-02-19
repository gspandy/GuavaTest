package hu.plajko;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class CacheTest2 {

	public static class LoaderContext {

		public String getValue() {
			return Thread.currentThread().getName();
		}
	}

	public static abstract class ContextedCacheLoader<K, V, C> extends CacheLoader<K, V> {

		private WeakReference<C> contextReference = null;

		private C getContext() {
			C context = this.contextReference.get();
			if (context == null)
				throw new IllegalStateException("context is null");
			return context;
		}

		@Override
		public Map<K, V> loadAll(Iterable<? extends K> keys) throws Exception {
			return loadAll(getContext(), keys);
		}

		@Override
		public V load(K key) throws Exception {
			return load(getContext(), key);
		}

		public Map<K, V> loadAll(C context, Iterable<? extends K> keys) throws Exception {
			return super.loadAll(keys);
		}

		public abstract V load(C context, K key) throws Exception;

		public void setContext(C context) {
			this.contextReference = new WeakReference<C>(context);
		}
	}

	public static abstract class ContextedLoadingCache<K, V, C> {
		private LoadingCache<K, V> cacheInstance = null;
		private ContextedCacheLoader<K, V, C> cacheLoader = null;

		public ContextedLoadingCache(final ContextedCacheLoader<K, V, C> cacheLoader) {
			this.cacheLoader = cacheLoader;
			this.cacheInstance = getCacheBuilder().build(cacheLoader);
		}

		public Map<K, V> getAll(C context, Iterable<? extends K> keys) throws Exception {
			this.cacheLoader.setContext(context);
			return this.cacheInstance.getAll(keys);
		}

		public V get(C context, K key) throws Exception {
			this.cacheLoader.setContext(context);
			return this.cacheInstance.get(key);
		}

		protected abstract CacheBuilder<K, V> getCacheBuilder();

		public CacheStats stats() {
			return this.cacheInstance.stats();
		}

	}

	private static ContextedLoadingCache<String, String, LoaderContext> CACHE = //
	new ContextedLoadingCache<String, String, LoaderContext>(//
			new ContextedCacheLoader<String, String, LoaderContext>() {

				@Override
				public Map<String, String> loadAll(LoaderContext context, Iterable<? extends String> keys) throws Exception {
					System.out.println(Thread.currentThread().getName() + " - " + "loadall " + keys);
					return super.loadAll(context, keys);
				}

				@Override
				public String load(LoaderContext context, String key) throws Exception {
					System.out.println(Thread.currentThread().getName() + " - " + "load " + key);
					Thread.sleep(100);
					return context.getValue();
				}

			}) {

		@Override
		protected CacheBuilder<String, String> getCacheBuilder() {
			return CacheBuilder.newBuilder()//
					.maximumSize(1000)//
					.expireAfterWrite(1, TimeUnit.SECONDS)//
					.removalListener(new RemovalListener<String, String>() {
						@Override
						public void onRemoval(RemovalNotification<String, String> notification) {
							System.out.println("remove " + notification);
						}
					});
		}
	};

	public static void main(String[] args) throws Exception {

		ExecutorService e = Executors.newFixedThreadPool(10, new ThreadFactory() {
			private int counter = 0;

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "session" + counter++);
			}
		});

		List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();
		for (int i = 0; i < 1000; i++) {
			FutureTask<String> task = new FutureTask<String>(new Callable<String>() {

				private LoaderContext context = new LoaderContext();

				@Override
				public String call() throws Exception {
					Random r = new Random();
					Thread.sleep(400 + r.nextInt(500));

					List<String> req = new ArrayList<String>();
					if (r.nextBoolean())
						req.add("a");

					if (r.nextBoolean())
						req.add("b");

					if (r.nextBoolean())
						req.add("c");

					if (r.nextBoolean())
						req.add("d");
					else
						req.add("e");

					System.out.println(Thread.currentThread().getName() + " - req: " + req);
					Stopwatch timer = new Stopwatch().start();
					String result = Thread.currentThread().getName() + " - result: " + CACHE.getAll(this.context, req);
					long time = timer.stop().elapsedMillis();
					return result + " (" + time + ")";
				}

			});
			e.execute(task);
			tasks.add(task);
		}

		for (FutureTask<String> task : tasks)
			System.out.println(task.get());

		e.shutdown();
		System.out.println(CACHE.stats());
	}
}
