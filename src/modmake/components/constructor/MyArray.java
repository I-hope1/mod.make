package modmake.components.constructor;

import arc.func.Cons;
import arc.func.Prov;
import arc.struct.Seq;

import java.util.ArrayList;
import java.util.StringJoiner;

public class MyArray<E> extends MyObject<Integer, E> {
	int j = -1;

	public MyArray() {
		super();
	}

	public MyArray(E... seq) {
		super();
		for (E e : seq) {
			put(e);
		}
	}

	public MyArray(Seq<E> seq) {
		super();
		seq.each(this::put);
	}

//	public E put(Integer i, E v) {
//		j = Math.max(i, j) + 1;
//		return super.put(i, v);
//	}

	public void put(E v) {
		super.put(++j, v);
//		Log.err(new Exception("" + (v instanceof Prov ? ((Prov<?>) v).get() : v)));
//		Log.info("put" + v);
	}

	public void each(Cons<E> cons) {
		super.each((i, v) -> cons.get(v));
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
	public MyArray<E> cpy() {
		MyArray<E> array = new MyArray<>();
		each(v -> array.put(v));
		return array;
	}

	public ArrayList<E> toArray() {
		var list = new ArrayList<E>();
		each(v -> list.add(v));
		return list;
	}
}

