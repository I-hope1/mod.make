package modmake.components.constructor;

import arc.func.Cons2;
import arc.func.Prov;
import arc.struct.Seq;
import arc.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.function.Consumer;

public class MyArray<E> extends ArrayList<E>
		implements MyInterface<Integer, E, Consumer<? super E>> {

	public MyArray(){
		super();
	}
	public MyArray(E ...seq) {
		super();
		for (E e : seq) {
			add(e);
		}
	}
	public MyArray(Seq<E> seq) {
		super();
		seq.each(this::add);
	}

	public E put(Integer i, E v) {
		if (i < size()) remove((int)i);
		add(i, v);
		return v;
	}

	public boolean put(E v){
		return add(v);
	}

	public void each(Consumer<? super E> cons) {
		forEach(cons);
	}
	public void each(Cons2<Integer, E> c) {
		int s = size();
		for (int i = 0; i < s; i++) {
			Log.info(i);
			c.get(i, get(i));
		};
	}

	@Override
	public boolean has(Integer i) {
		return get((int)i) != null;
	}

	public String toString() {
		var str = new StringJoiner(", ");
		each(item -> {
			Object val = item instanceof Prov ? ((Prov<?>) item).get() : item;
			str.add(val + "");
		});
		return "[\n" + str + "\n]";
	}

	@Override
	public E removeKey(Integer i) {
		E e = get((int)i);
		remove((int)i);
		return e;
	}

	@Override
	public MyArray<E> cpy() {
		MyArray<E> array = new MyArray<E>();
		each(e -> array.add(e));
		return array;
	}
}

