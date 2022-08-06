package modmake.util;

import arc.func.*;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.util.serialization.Jval;
import mindustry.ctype.UnlockableContent;
import modmake.components.AddFieldBtn;

import java.lang.reflect.*;
import java.util.Objects;

import static modmake.util.MyReflect.unsafe;

public class Tools {
	// 通过Jval转义
	public static String trope(String s) {
		return Jval.read('"' + s.replaceAll("\\\\\"", "\\\"") + '"').asString().replaceAll("[\\n\\r]", "\n").replaceAll("\"", "\\\"");
	}

	public static <T, R> R nullCheck(T obj, Func<T, R> func) {
		if (obj != null) return func.get(obj);
		return null;
	}

	public static <T> T or(T arg1, T arg2) {
		return arg1 != null ? arg1 : arg2;
	}

	public static <T> T or(T arg1, Prov<T> arg2) {
		return arg1 != null ? arg1 : arg2.get();
	}

	public static <T> T as(Object obj) {
		try {
			return (T) obj;
		} catch (Throwable thr) {
			return null;
		}
	}

	public static <T, R> R as(Object obj, Func<T, R> func, R defaultValue) {
		try {
			if (obj != null) return func.get((T) obj);
		} catch (Exception ignored) {}
		return defaultValue;
	}

	public static <T, R> R as(Object obj, Func<T, R> func) {
		return as(obj, func, null);
	}

	public static ObjectMap<Object, Object> objectCaches = new ObjectMap<>();

	/**
	 * @param o1 新的实例
	 * @param o2 原本的
	 **/
	public static <T> Jval copyJval(T o1, T o2) throws Exception {
		Class<?> cls = o1.getClass();
		Jval jval = Jval.newObject();
		Object val1, val2;
		objectCaches.clear();
		while (cls != null && Object.class.isAssignableFrom(cls)) {
			if (!cls.isAnonymousClass() && !jval.has("type")) jval.put("type", cls.getSimpleName());
			Field[] fields = getFields(cls);
			for (Field f : fields) {
				int mod = f.getModifiers();
				if (!Modifier.isStatic(mod) && Modifier.isPublic(mod) && !Modifier.isFinal(mod)) {
					long offset = unsafe.objectFieldOffset(f);
					val1 = f.get(o1);
					val2 = f.get(o2);
					if (!Objects.equals(val1, val2) && AddFieldBtn.filter(f, cls)) {
						// if (display) Log.debug(f);
						addJval(jval, f, val2);
					}
				}
			}
			cls = cls.getSuperclass();
		}
		return jval;
	}

	public static Jval addJval(Jval jval, Field f, Object o) throws Exception {
		String key = f.getName();
		label:
		if (o == null) {
			jval.put(key, Jval.NULL);
		} else if (f.getType().isArray()) {
			if (objectCaches.containsKey(o)) break label;
			objectCaches.put(o, o);
			if (!(o instanceof Object[])) break label;
			int len = Array.getLength(o);
			Jval newArr = Jval.newArray();
			for (int i = 0; i < len; i++) {
				newArr.asArray().add(copyValue(Array.get(o, i)));
			}
			jval.put(key, newArr);
		} else if (f.getType().isPrimitive()) {
			jval.put(key, Jval.read(String.valueOf(o)));
		} else if (f.getType() == String.class) {
			jval.put(key, Jval.valueOf(String.valueOf(o)));
		} else if (TextureRegion.class.isAssignableFrom(f.getType())) {
		} else if (UnlockableContent.class.isAssignableFrom(f.getType())) {
			jval.put(key, String.valueOf(o));
		} else {
			if (objectCaches.containsKey(o)) break label;
			objectCaches.put(o, o);
			jval.put(key, copyValue(o));
		}
		return jval;
	}

	public static Jval copyValue(Object o) throws Exception {
		if (o == null) return Jval.NULL;
		Class cls = o.getClass();
		if (cls.isPrimitive()) return Jval.read(String.valueOf(o));
		if (cls == String.class || TextureRegion.class.isAssignableFrom(cls)
				|| UnlockableContent.class.isAssignableFrom(cls)) Jval.valueOf(String.valueOf(o));
		Jval jval = Jval.newObject();
		while (cls != null && Object.class.isAssignableFrom(cls)) {
			Field[] fields = getFields(cls);
			for (Field f : fields) {
				int mod = f.getModifiers();
				if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod) && Modifier.isPublic(mod)) {
					// if (display) Log.debug(f);
					addJval(jval, f, f.get(o));
				}
			}
			cls = cls.getSuperclass();
		}
		return jval;
	}

	public static ObjectMap<Class, Field[]> fieldsCaches = new ObjectMap<>();

	public static Field[] getFields(Class cls) {
		return fieldsCaches.get(cls, () -> {
			Field[] fields = cls.getDeclaredFields();
			for (Field f : fields) {
				f.setAccessible(true);
			}
			return fields;
		});
	}

	public static <T> void setValue(Field f, T from, T to) {
		Class<?> type = f.getType();
		long offset = unsafe.objectFieldOffset(f);
		if (int.class.equals(type)) {
			unsafe.putInt(to, offset, unsafe.getInt(from, offset));
		} else if (float.class.equals(type)) {
			unsafe.putFloat(to, offset, unsafe.getFloat(from, offset));
		} else if (double.class.equals(type)) {
			unsafe.putDouble(to, offset, unsafe.getDouble(from, offset));
		} else if (long.class.equals(type)) {
			unsafe.putLong(to, offset, unsafe.getLong(from, offset));
		} else if (char.class.equals(type)) {
			unsafe.putChar(to, offset, unsafe.getChar(from, offset));
		} else if (byte.class.equals(type)) {
			unsafe.putByte(to, offset, unsafe.getByte(from, offset));
		} else if (short.class.equals(type)) {
			unsafe.putShort(to, offset, unsafe.getShort(from, offset));
		} else if (boolean.class.equals(type)) {
			unsafe.putBoolean(to, offset, unsafe.getBoolean(from, offset));
		} else {
			Object o = unsafe.getObject(from, offset);
			/*if (f.getType().isArray()) {
				o = Arrays.copyOf((Object[]) o, Array.getLength(o));
			}*/
			unsafe.putObject(to, offset, o);
		}
	}


	public static String toString(Object obj) {
		if (obj instanceof Number) {
			return String.valueOf(obj).replaceAll("\\.0$", "");
		}
		return String.valueOf(obj);
	}
}
