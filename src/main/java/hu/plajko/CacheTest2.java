package hu.plajko;

import hu.plajko.cache.ContextedLoadingCache;
import hu.plajko.cache.ContextedLoadingCache.ContextedCacheLoader;
import hu.plajko.cache.ContextedLoadingCache.ContextedRemovalListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;

public class CacheTest2 {

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

	private static ContextedLoadingCache<String, ValueClass, LoaderContext> CACHE = //
	new ContextedLoadingCache<String, ValueClass, LoaderContext>(//
			CacheBuilder.newBuilder()//
					.maximumSize(1000)//
					.expireAfterWrite(1, TimeUnit.SECONDS),//
			new ContextedCacheLoader<String, ValueClass, LoaderContext>() {
				@Override
				public ValueClass load(LoaderContext context, String key) throws Exception {
					System.out.println(Thread.currentThread().getName() + " - " + "load " + key);
					return new ValueClass(context.getValue());
				}

				// optional
				public Map<String, ValueClass> loadAll(LoaderContext context, Iterable<? extends String> keys) throws Exception {
					System.out.println(Thread.currentThread().getName() + " - " + "loadall [" + Joiner.on(", ").join(keys) + "]");
					return super.loadAll(context, keys);
				}

			},//
			new ContextedRemovalListener<String, ValueClass, LoaderContext>() {

				@Override
				public void onRemoval(LoaderContext context, String key, ValueClass value) {
					System.out.println("remove " + key + "=" + value);

				}
			});

	public static void main(String[] args) throws Exception {

		ExecutorService e = Executors.newFixedThreadPool(10, new ThreadFactory() {
			private int counter = 0;

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "user" + counter++);
			}
		});

		final List<String> requestStrings = Arrays.asList(new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" });

		List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();
		for (int i = 0; i < 200; i++) {
			FutureTask<String> task = new FutureTask<String>(new Callable<String>() {

				private LoaderContext context = new LoaderContext();

				@Override
				public String call() throws Exception {
					Random r = new Random();
					Thread.sleep(200 + r.nextInt(500));

					Collections.shuffle(requestStrings);
					List<String> req = requestStrings.subList(0, 1 + r.nextInt(requestStrings.size() - 1));

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
