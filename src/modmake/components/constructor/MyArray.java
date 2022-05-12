package modmake.components.constructor;

import arc.func.Prov;
import arc.struct.Seq;
import arc.util.Log;

import java.util.StringJoiner;

public class MyArray<E> extends MyObject<Integer, E> {
	int j = -1;

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
		j++;
		if (i == -1) {
			i = j;
		}

		return super.put(i, v);
	}

	public void put(E v){
		put(-1, v);
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

