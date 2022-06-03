package modmake.util;

import arc.func.Cons2;
import arc.struct.Seq;

import static mindustry.mod.ClassMap.classes;

public class Classes {
	static Seq<Class<?>> values = classes.values().toSeq();
	public static void each(Cons2<String, Class<?>> cons) {
		classes.each(cons);
	}

	public static Class<?> get(String name) {
		return values.find(clazz -> clazz.getSimpleName().equalsIgnoreCase(name));
	}
}
