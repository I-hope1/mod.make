package modmake.components.constructor;

import arc.func.Cons2;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;

import java.util.StringJoiner;

public class MyObject<K, V> extends OrderedMap<K, V>
		implements MyInterface<K, V, Cons2<K, V>> {
	public static <K1, V1> MyObject<K1, V1> of(Object... args) {
		var m = new MyObject<K1, V1>();
		for (int i = 0; i < args.length; i += 2) {
			m.put((K1)args[i], (V1)args[i + 1]);
		}
		return m;
	}
	public MyObject(){}
	public MyObject(ObjectMap<K, V> map) {
		super((OrderedMap<? extends K, ? extends V>) map);
	}

	public V put(K k, V v){
//		Log.info(k + "\n" + v);
//		Log.info("res: " + this);
		return super.put(k, v);
	}

	public V removeValue(V value) {
		K k = findKey(value, false);
		if (k != null) return remove(k);
		return null;
	}

	@Override
	public boolean has(K k) {
		return containsKey(k);
	}

	@Override
	public String toString() {
		var str = new StringJoiner("\n");
		orderedKeys().each(k -> {
			var v = get(k);
			var key = k instanceof Prov ? ((Prov<?>) k).get() : k;
			var value = v instanceof Prov ? ((Prov<?>) v).get() : v;
			str.add(key + ": " + (value.equals("") ? "\"\"" : value));
		});
		return "{\n" + str + "\n}";
	}

	@Override
	public MyObject<K, V> cpy() {
		return new MyObject<>(this);
	}
}
