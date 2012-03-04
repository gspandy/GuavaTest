package hu.plajko.cache;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Function;
import com.google.common.cache.ForwardingLoadingCache.SimpleForwardingLoadingCache;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;

public class ContextedLoadingCache<K, V, C> extends SimpleForwardingLoadingCache<ContextedKey<K, C>, V> {

	public ContextedLoadingCache(LoadingCache<ContextedKey<K, C>, V> delegate) {
		super(delegate);
	}

	// special get method
	public V get(C context, K key) throws ExecutionException {
		return delegate().get(new ContextedKey<K, C>(key, context));
	}

	// special getAll method
	public Map<K, V> getAll(final C context, final Iterable<? extends K> keys) throws Exception {
		return MapTransformer.transformMapKeys(//
				delegate().getAll(Iterables.transform(keys, new Function<K, ContextedKey<K, C>>() {
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

}