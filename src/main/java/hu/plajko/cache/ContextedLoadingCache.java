package hu.plajko.cache;

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class ContextedLoadingCache<K, V, C> {

	private LoadingCache<ContextedKey<K, C>, V> cacheInstance = null;

	private static interface ContextedRemovalListenerIf<K, V, C> extends RemovalListener<ContextedKey<K, C>, V> {
		public void onRemoval(C context, K key, V value);
	}

	public static abstract class ContextedRemovalListener<K, V, C> implements ContextedRemovalListenerIf<K, V, C> {

		@Override
		public void onRemoval(RemovalNotification<ContextedKey<K, C>, V> notification) {
			onRemoval(notification.getKey().getContext(), notification.getKey().getKey(), notification.getValue());
		}

	}

	public static abstract class ContextedCacheLoader<K, V, C> extends CacheLoader<ContextedKey<K, C>, V> {

		public abstract V load(C context, K key) throws Exception;

		@Override
		public final V load(ContextedKey<K, C> key) throws Exception {
			return load(key.getContext(), key.getKey());
		}

		public Map<K, V> loadAll(final C context, final Iterable<? extends K> keys) throws Exception {
			return transformMapKeys(//
					super.loadAll(transformIterableValues(keys,//
							new ValueTransformer<K, ContextedKey<K, C>>() {
								@Override
								public ContextedKey<K, C> transform(K key) {
									return new ContextedKey<K, C>(key, context);
								}
							})),//
					new ValueTransformer<ContextedKey<K, C>, K>() {
						@Override
						public K transform(ContextedKey<K, C> value) {
							return value.getKey();
						}
					});
		}

		private class ContextHolder {
			private C context;

			public C getContext() {
				return context;
			}

			public void setContext(C context) {
				this.context = context;
			}
		}

		@Override
		public final Map<ContextedKey<K, C>, V> loadAll(Iterable<? extends ContextedKey<K, C>> keys) throws Exception {
			final ContextHolder cHolder = new ContextHolder();
			if (keys != null && keys.iterator().hasNext())
				cHolder.setContext(keys.iterator().next().getContext());
			return transformMapKeys(//
					loadAll(cHolder.getContext(), transformIterableValues(keys,//
							new ValueTransformer<ContextedKey<K, C>, K>() {
								@Override
								public K transform(ContextedKey<K, C> value) {
									return value.getKey();
								}
							})),//
					new ValueTransformer<K, ContextedKey<K, C>>() {
						@Override
						public ContextedKey<K, C> transform(K value) {
							return new ContextedKey<K, C>(value, cHolder.getContext());
						}
					});

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
			ContextedKey<K, C> other = (ContextedKey<K, C>) obj;
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
			this.context = new WeakReference<C>(context);
		}

		public K getKey() {
			return key;
		}

		public C getContext() {
			return context.get();
		}

		private K key;
		private WeakReference<C> context;
	}

	public ContextedLoadingCache(LoadingCache<ContextedKey<K, C>, V> cacheInstance) {
		this.cacheInstance = cacheInstance;
	}

	private static <T1, T2> Iterable<T2> transformIterableValues(final Iterable<? extends T1> keys, final ValueTransformer<T1, T2> valueTransofrmer) {
		return new Iterable<T2>() {
			@Override
			public Iterator<T2> iterator() {
				final Iterator<? extends T1> it = keys.iterator();
				return new Iterator<T2>() {
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public T2 next() {
						return valueTransofrmer.transform(it.next());
					}

					@Override
					public void remove() {
						it.remove();
					}
				};
			}
		};
	}

	private static interface ValueTransformer<T1, T2> {
		public T2 transform(T1 value);
	}

	private static <K1, K2, V> Map<K2, V> transformMapKeys(final Map<K1, V> map, final ValueTransformer<K1, K2> keyTransformer) {
		return new AbstractMap<K2, V>() {
			@Override
			public Set<Map.Entry<K2, V>> entrySet() {
				return new AbstractSet<Map.Entry<K2, V>>() {
					@Override
					public Iterator<Map.Entry<K2, V>> iterator() {
						final Iterator<Entry<K1, V>> it = map.entrySet().iterator();
						return new Iterator<Map.Entry<K2, V>>() {
							@Override
							public boolean hasNext() {
								return it.hasNext();
							}

							@Override
							public Map.Entry<K2, V> next() {
								final Entry<K1, V> oldEntry = it.next();
								return new Map.Entry<K2, V>() {
									@Override
									public K2 getKey() {
										return keyTransformer.transform(oldEntry.getKey());
									}

									@Override
									public V getValue() {
										return oldEntry.getValue();
									}

									@Override
									public V setValue(V value) {
										return oldEntry.setValue(value);
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

	public V get(C context, K key) throws ExecutionException {
		return cacheInstance.get(new ContextedKey<K, C>(key, context));
	}

	public Map<K, V> getAll(final C context, final Iterable<? extends K> keys) throws Exception {
		return transformMapKeys(//
				this.cacheInstance.getAll(transformIterableValues(keys,//
						new ValueTransformer<K, ContextedKey<K, C>>() {
							@Override
							public ContextedKey<K, C> transform(K key) {
								return new ContextedKey<K, C>(key, context);
							}
						})),//
				new ValueTransformer<ContextedKey<K, C>, K>() {
					@Override
					public K transform(ContextedKey<K, C> value) {
						return value.getKey();
					}
				});
	}

	public CacheStats stats() {
		return this.cacheInstance.stats();
	}

}