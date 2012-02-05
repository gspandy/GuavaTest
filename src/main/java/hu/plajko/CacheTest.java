package hu.plajko;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class CacheTest {

	private static final int NUM = 500;
	private static final int PROCESS = 10;
	private static final int LOAD = 200;

	private static final Logger log = LoggerFactory.getLogger(CacheTest.class);

	private static LoadingCache<Integer, DateTime> cache = CacheBuilder.newBuilder()//
			.expireAfterWrite(2, TimeUnit.SECONDS)//
			.removalListener(new RemovalListener<Integer, DateTime>() {
				public void onRemoval(RemovalNotification<Integer, DateTime> notification) {
					log.info("remove: " + notification);
				}
			})//
			.build(new CacheLoader<Integer, DateTime>() {
				public DateTime load(Integer key) throws Exception {
					log.info("load: " + key);
					Thread.sleep(LOAD);
					return DateTime.now();
				}
			});

	public static void main(String[] args) throws InterruptedException, ExecutionException {

		Stopwatch timer = new Stopwatch().start();
		for (int i = 0; i < NUM; i++) {
			Thread.sleep(PROCESS);

			log.info("key age: " + Seconds.secondsBetween(cache.get(new Random().nextInt(5)), DateTime.now()).get(DurationFieldType.seconds()));
		}
		log.info("time: " + timer.elapsedMillis() + "[" + (NUM * LOAD) + "]");
		log.info("" + cache.stats());

	}

}
