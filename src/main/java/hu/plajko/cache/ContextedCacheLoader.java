package hu.plajko.cache;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Iterables;

public abstract class ContextedCacheLoader<K, V, C> extends CacheLoader<ContextedKey<K, C>, V> {

	// must be implemented
	public abstract V load(C context, K key) throws Exception;

	@Override
	public final V load(ContextedKey<K, C> key) throws Exception {
		return load(key.getContext(), key.getKey());
	}

	// default implementation, can be overridden
	public Map<K, V> loadAll(final C context, final Iterable<? extends K> keys) throws Exception {
		return MapTransformer.transformMapKeys(//
				super.loadAll(Iterables.transform(keys, new Function<K, ContextedKey<K, C>>() {
					@Override
					public ContextedKey<K, C> apply(K key) {
						return new ContextedKey<K, C>(key, context);
					}
				})),//
				new Function<ContextedKey<K, C>, K>() {
					@Override
					public K apply(ContextedKey<K, C> contextedKey) {
						return contextedKey.getKey();
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
		return MapTransformer.transformMapKeys(//
				loadAll(cHolder.getContext(), Iterables.transform(keys, new Function<ContextedKey<K, C>, K>() {
					@Override
					public K apply(ContextedKey<K, C> contextedKey) {
						return contextedKey.getKey();
					}
				})),//
				new Function<K, ContextedKey<K, C>>() {
					@Override
					public ContextedKey<K, C> apply(K value) {
						return new ContextedKey<K, C>(value, cHolder.getContext());
					}
				});

	}
}