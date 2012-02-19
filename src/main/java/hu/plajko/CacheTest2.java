package hu.plajko;

import hu.plajko.cache.ContextedLoadingCache;
import hu.plajko.cache.ContextedLoadingCache.ContextedCacheLoader;
import hu.plajko.cache.ContextedLoadingCache.ContextedRemovalListener;

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

public class CacheTest2 {

	public static class LoaderContext {

		public String getValue() {
			return Thread.currentThread().getName();
		}
	}

	private static ContextedLoadingCache<String, String, LoaderContext> CACHE2 = //
	new ContextedLoadingCache<String, String, LoaderContext>(//
			CacheBuilder.newBuilder()//
					.maximumSize(1000)//
					.expireAfterWrite(1, TimeUnit.SECONDS),//
			new ContextedCacheLoader<String, String, LoaderContext>() {
				@Override
				public String load(LoaderContext context, String key) throws Exception {
					System.out.println(Thread.currentThread().getName() + " - " + "load " + key);
					return context.getValue();
				}

				public Map<String, String> loadAll(LoaderContext context, Iterable<? extends String> keys) throws Exception {
					System.out.println(Thread.currentThread().getName() + " - " + "loadall " + keys);
					return super.loadAll(context, keys);
				}

			},//
			new ContextedRemovalListener<String, String, LoaderContext>() {

				@Override
				public void onRemoval(LoaderContext context, String key, String value) {
					System.out.println("remove " + key + "=" + value);

				}
			});

	public static void main(String[] args) throws Exception {

		ExecutorService e = Executors.newFixedThreadPool(10, new ThreadFactory() {
			private int counter = 0;

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "session" + counter++);
			}
		});

		List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();
		for (int i = 0; i < 200; i++) {
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
					String result = Thread.currentThread().getName() + " - result: " + CACHE2.getAll(this.context, req);
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
		System.out.println(CACHE2.stats());

	}
}
