package hu.plajko.cache;

import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public abstract class ContextedRemovalListener<K, V, C> implements RemovalListener<ContextedKey<K, C>, V> {

	@Override
	public final void onRemoval(RemovalNotification<ContextedKey<K, C>, V> notification) {
		onRemoval(notification.getKey().getContext(), notification.getKey().getKey(), notification.getValue(), notification.getCause());
	}

	// RemovalNotification cannot be used, it's final
	public abstract void onRemoval(C context, K key, V value, RemovalCause cause);

}