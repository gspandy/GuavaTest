package hu.plajko;

import java.util.*;
import java.util.concurrent.*;

import org.joda.time.*;
import org.slf4j.*;

import com.google.common.base.*;
import com.google.common.cache.*;
import com.google.common.collect.*;

public class CacheTest {

	private static final int NUM = 100;
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

		BiMap<String, String> map = HashBiMap.create();

		map.put("a", "alma");
		map.put("a", "alma2");
		
		log.info(DateTime.now(DateTimeZone.forID("US/Pacific"))+"");
		log.info(DateTime.now(DateTimeZone.forID("Europe/Budapest"))+"");

		log.info("" + map.get("a"));
		log.info("" + map.inverse().get("alma2"));
		
		Stopwatch timer = new Stopwatch().start();
		for (int i = 0; i < NUM; i++) {
			Thread.sleep(PROCESS);
			log.info("key age: " + Seconds.secondsBetween(cache.get(new Random().nextInt(5)), DateTime.now()).getSeconds());
		}
		log.info("time: " + timer.elapsedMillis() + "[" + (NUM * LOAD) + "]");
		log.info("" + cache.stats());

	}

}