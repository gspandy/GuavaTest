package hu.plajko;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class CacheTest {

	private static final int NUMBER_OF_REQUESTS = 100;
	private static final int PROCESSING_TIME = 5;
	private static final int LOADING_TIME = 200;

	private static final Logger log = LoggerFactory.getLogger(CacheTest.class);

	private static LoadingCache<Integer, Stopwatch> cache = CacheBuilder.newBuilder()//
			.expireAfterWrite(1, TimeUnit.SECONDS)//
			.removalListener(new RemovalListener<Integer, Stopwatch>() {
				public void onRemoval(RemovalNotification<Integer, Stopwatch> notification) {
					notification.getValue().stop();
					log.info("key removed: {}", notification);
				}
			})//
			.build(new CacheLoader<Integer, Stopwatch>() {
				public Stopwatch load(Integer key) throws Exception {
					log.info("loading key {}", key);
					Thread.sleep(LOADING_TIME);
					return new Stopwatch().start();
				}
			});

	public static void main(String[] args) throws InterruptedException, ExecutionException {

		// google bimap
		BiMap<String, String> map = HashBiMap.create();
		map.put("a", "alma");
		map.put("a", "alma2");
		log.info("{}", map.get("a"));
		log.info("{}", map.inverse().get("alma2"));

		// joda DateTime
		DateTime now = DateTime.now();
		log.info("US/Pacific: {}", now.withZone(DateTimeZone.forID("US/Pacific")));
		log.info("Europe/Budapest: {}", now.withZone(DateTimeZone.forID("Europe/Budapest")));

		// google cache test
		Stopwatch timer = new Stopwatch().start();
		Random r = new Random(0xcafe);
		for (int i = 0; i < NUMBER_OF_REQUESTS; i++) {
			Thread.sleep(PROCESSING_TIME);
			log.info("key age: {}", cache.get(r.nextInt(5)));
		}
		log.info("test took {} ms instead of {} ms", timer.elapsedMillis(), NUMBER_OF_REQUESTS * LOADING_TIME);
		log.info("{}", cache.stats());

	}

}
