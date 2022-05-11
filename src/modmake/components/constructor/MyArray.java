package modmake.components.constructor;

import arc.func.Prov;
import arc.struct.Seq;

import java.util.StringJoiner;

public class MyArray<E> extends MyObject<Integer, E> {

	public MyArray(){
		super();
	}
	public MyArray(E ...seq) {
		super();
		for (E e : seq) {
			put(e);
		}
	}
	public MyArray(Seq<E> seq) {
		super();
		seq.each(this::put);
	}

	public E put(Integer i, E v) {
		if (i < size) remove((int)i);
		super.put(i, v);
		return v;
	}

	public void put(E v){
		put(size, v);
	}

	@Override
	public boolean has(Integer i) {
		return get(i) != null;
	}

	public String toString() {
		var str = new StringJoiner(", ");
		each((i, item) -> {
			Object val = item instanceof Prov ? ((Prov<?>) item).get() : item;
			str.add(val + "");
		});
		return "[\n" + str + "\n]";
	}

	@Override
	public MyArray<E> cpy() {
		MyArray<E> array = new MyArray<>();
		each(array::put);
		return array;
	}
}

