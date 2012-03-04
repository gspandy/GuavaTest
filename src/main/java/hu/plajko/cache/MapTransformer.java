package hu.plajko.cache;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

class MapTransformer {

	static <K1, K2, V> Map<K2, V> transformMapKeys(final Map<K1, V> map, final Function<K1, K2> keyTransformer) {
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
										return keyTransformer.apply(oldEntry.getKey());
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
}
