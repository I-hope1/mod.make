package modmake.util;

import java.lang.reflect.Field;

public class Reflect {
	public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
		Field f = clazz.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}
}
