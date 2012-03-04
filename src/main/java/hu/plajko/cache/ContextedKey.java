package hu.plajko.cache;

public class ContextedKey<K, C> {

	private K key;
	private C context;

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
}