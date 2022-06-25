package modmake.util;

import arc.util.Log;
import modmake.ModMake;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class Reflect {
	static HashMap<String, Field> map = new HashMap<>();
	public static MethodHandles.Lookup lookup;
	public static MethodHandle modifiers;

	public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
		if (map.containsKey(clazz.getName() + "." + name)) return map.get(clazz.getName() + "." + name);
		Field f = clazz.getDeclaredField(name);
		f.setAccessible(true);
		map.put(clazz.getName() + "." + name, f);
		return f;
	}

	public static void setValue(Object o, String name, Object val) throws Throwable {
//		lookup.findSetter(o.getClass(), name, val.getClass()).invoke(o, val);
		Field f = getField(o.getClass(), name);
		f.set(o, val);
	}

	public static <T> T getValue(Object o, String name, Class<?> clazz) throws Throwable {
//		return (T)lookup.findGetter(o.getClass(), name, clazz).invoke(o);
		Field f = getField(o.getClass(), name);
		if (f.getType() != clazz) return null;
		return (T) f.get(o);
	}

	public static Unsafe unsafe;

	public static void removeFinal(Field field) throws Throwable {
		unsafe.putObject(field, unsafe.objectFieldOffset(field), field.getModifiers() & ~Modifier.FINAL);
	}

	// init
	public static void load() {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			unsafe = (Unsafe) theUnsafe.get(null);

			Field module = Class.class.getDeclaredField("module");
			long offset = unsafe.objectFieldOffset(module);
			unsafe.putObject(ModMake.class, offset, Object.class.getModule());

		} catch (Exception e) {
			Log.err(e);
		}

	}
}
