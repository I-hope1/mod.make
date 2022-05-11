package modmake.components.constructor;

public interface MyInterface<K, V, C> {
	boolean has(K k);

	V put(K k, V v);

	void each(C cons);

	String toString();

	V removeKey(K k);

	MyInterface<K, V, C> cpy();
}
