package modmake.util;

import arc.struct.ObjectMap;
import arc.util.Log;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Reflect {
	static ObjectMap<String, Field> map = new ObjectMap<>();
	public static Unsafe unsafe;
	public static MethodHandles.Lookup lookup;
	public static MethodHandle modifiers;
//	public static Reflect self = new Reflect();

	public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
		if (map.containsKey(clazz.getName() + "." + name)) return map.get(clazz.getName() + "." + name);
		Field f = clazz.getDeclaredField(name);
		f.setAccessible(true);
		map.put(clazz.getName() + "." + name, f);
		return f;
	}

	public static void removeFinal(Field field) throws Throwable {
		modifiers.invoke(field, field.getModifiers() & ~Modifier.FINAL);
	}

	public static void setValue(Object o, String name, Object val) throws Throwable {
		lookup.findSetter(o.getClass(), name, val.getClass()).invoke(o, val);
//		unsafe.putObject(o, Modifier.isStatic(field.getModifiers()) ? unsafe.staticFieldOffset(field) : unsafe.objectFieldOffset(field), val);
	}

	public static Object getValue(Object o, String name, Class<?> clazz) throws Throwable {
		return lookup.findGetter(o.getClass(), name, clazz).invoke(o);
//		return unsafe.getObject(o, Modifier.isStatic(field.getModifiers()) ? unsafe.staticFieldOffset(field) : unsafe.objectFieldOffset(field));
	}

	{
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			unsafe = (Unsafe) theUnsafe.get(null);

			Field module = Class.class.getDeclaredField("module");
			long offset = unsafe.objectFieldOffset(module);
			unsafe.putObject(Reflect.class, offset, Object.class.getModule());

			Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
			offset = unsafe.staticFieldOffset(field);
			lookup = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, offset);

			modifiers = lookup.findSetter(Field.class, "modifiers", int.class);

//			Scriptable scope = Vars.mods.getScripts().scope;
//			ScriptableObject.putProperty(scope, "aaa", Context.javaToJS(unsafe, scope));
		} catch (Exception e) {
			Log.err(e);
		}
	}
}
