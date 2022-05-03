package modmake.components.constructor;

import java.util.function.Consumer;

public interface MyInterface<K, V, C> {
	boolean has(K k);

	int size();

	V put(K k, V v);

	void each(C cons);

	String toString();

	V removeKey(K k);

	MyInterface<K, V, C> cpy();
}
