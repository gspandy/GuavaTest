package hu.plajko.cache;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class ContextedLoadingCache<K, V, C> {

	public static interface ContextedRemovalListener<K, V, C> {
		public void onRemoval(C context, K key, V value);
	}

	public static abstract class ContextedCacheLoader<K, V, C> extends CacheLoader<ContextedKey<K, C>, V> {

		public abstract V load(C context, K key) throws Exception;

		@Override
		public final V load(ContextedKey<K, C> key) throws Exception {
			return load(key.getContext(), key.getKey());
		}

		public Map<K, V> loadAll(final C context, final Iterable<? extends K> keys) throws Exception {
			return transformMap(context, loadAll(transformIterable(context, keys)));
		}

	}

	private static class ContextedKey<K, C> {
		@Override
		public int hashCode() {
			return (key == null) ? 0 : key.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			ContextedLoadingCache.ContextedKey<K, C> other = (ContextedLoadingCache.ContextedKey<K, C>) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}

		public ContextedKey(K key, C context) {
			super();
			this.key = key;
			this.context = context;
		}

		public K getKey() {
			return key;
		}

		public C getContext() {
			return context;
		}

		private K key;
		private C context;
	}

	private LoadingCache<ContextedKey<K, C>, V> cacheInstance = null;

	@SuppressWarnings("unchecked")
	public ContextedLoadingCache(@SuppressWarnings("rawtypes") CacheBuilder builder, ContextedCacheLoader<K, V, C> cacheLoader, final ContextedRemovalListener<K, V, C> removalListener) {
		this.cacheInstance = builder//
				.removalListener(new RemovalListener<ContextedKey<K, C>, V>() {

					@Override
					public void onRemoval(RemovalNotification<ContextedKey<K, C>, V> notification) {
						removalListener.onRemoval(notification.getKey().getContext(), notification.getKey().getKey(), notification.getValue());

					}
				})//
				.build(cacheLoader);
	}

	public V get(C context, K key) throws ExecutionException {
		return cacheInstance.get(new ContextedKey<K, C>(key, context));
	}

	private static <K, V, C> Iterable<ContextedKey<K, C>> transformIterable(final C context, final Iterable<? extends K> keys) {
		return new Iterable<ContextedKey<K, C>>() {
			@Override
			public Iterator<ContextedKey<K, C>> iterator() {
				final Iterator<? extends K> it = keys.iterator();
				return new Iterator<ContextedKey<K, C>>() {
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public ContextedKey<K, C> next() {
						return new ContextedKey<K, C>(it.next(), context);
					}

					@Override
					public void remove() {
						it.remove();
					}
				};
			}
		};
	}

	private static <K, V, C> Map<K, V> transformMap(final C context, final Map<ContextedKey<K, C>, V> map) {
		return new AbstractMap<K, V>() {
			@Override
			public Set<Map.Entry<K, V>> entrySet() {
				return new AbstractSet<Map.Entry<K, V>>() {
					@Override
					public Iterator<Map.Entry<K, V>> iterator() {
						final Iterator<Entry<ContextedKey<K, C>, V>> it = map.entrySet().iterator();
						return new Iterator<Map.Entry<K, V>>() {
							@Override
							public boolean hasNext() {
								return it.hasNext();
							}

							@Override
							public Map.Entry<K, V> next() {
								final Entry<ContextedKey<K, C>, V> contextedEntry = it.next();
								return new Map.Entry<K, V>() {
									@Override
									public K getKey() {
										return contextedEntry.getKey().getKey();
									}

									@Override
									public V getValue() {
										return contextedEntry.getValue();
									}

									@Override
									public V setValue(V value) {
										return contextedEntry.setValue(value);
									}
								};
							}

							@Override
							public void remove() {
								it.remove();
							}
						};
					}

					@Override
					public int size() {
						return map.size();
					}
				};
			}
		};
	}

	public Map<K, V> getAll(final C context, final Iterable<? extends K> keys) throws Exception {
		return transformMap(context, this.cacheInstance.getAll(transformIterable(context, keys)));
	}

	public CacheStats stats() {
		return this.cacheInstance.stats();
	}

}