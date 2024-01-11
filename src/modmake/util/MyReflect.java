package modmake.util;

import arc.util.Log;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class MyReflect {
	static HashMap<String, Field> map = new HashMap<>();

	public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
		if (map.containsKey(clazz.getName() + "." + name)) return map.get(clazz.getName() + "." + name);
		Field f = clazz.getDeclaredField(name);
		f.setAccessible(true);
		map.put(clazz.getName() + "." + name, f);
		return f;
	}

	public static void setValue(Object o, Field field, Object val) {
		long offset = Modifier.isStatic(field.getModifiers()) ? unsafe.staticFieldOffset(field) : unsafe.objectFieldOffset(field);
		unsafe.putObject(o, offset, val);
	}
	public static void setValue(Object o, String name, Object val) throws Exception {
		Field f = getField(o.getClass(), name);
		f.set(o, val);
	}

	public static <T> T getValue(Object o, String name/*, Class<?> clazz*/) throws Throwable {
		Field f = getField(o.getClass(), name);
		//		if (f.getType() != clazz) return null;
		return (T) f.get(o);
	}
	public static <T> T getValue(Object o, Field field) {
		long offset = Modifier.isStatic(field.getModifiers()) ? unsafe.staticFieldOffset(field) : unsafe.objectFieldOffset(field);
		return (T) unsafe.getObject(o, offset);
	}

	public static Unsafe unsafe;

	// init
	public static void load() {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			unsafe = (Unsafe) theUnsafe.get(null);
		} catch (Exception e) {
			Log.err(e);
		}
	}
}
