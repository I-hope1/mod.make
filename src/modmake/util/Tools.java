package modmake.util;

import arc.func.*;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.struct.*;
import arc.util.Reflect;
import arc.util.serialization.Jval;
import mindustry.ctype.UnlockableContent;
import mindustry.mod.ClassMap;
import mindustry.type.Category;
import modmake.components.AddFieldBtn;

import java.lang.reflect.*;
import java.util.Objects;

import static modmake.util.MyReflect.unsafe;
import static modmake.util.load.ContentVars.otherTypes;

public class Tools {
	// 通过Jval转义
	public static String trope(String s) {
		return Jval.read('"' + s.replaceAll("\\\\\"", "\\\"") + '"').asString().replaceAll("[\\n\\r]", "\n").replaceAll("\"", "\\\"");
	}

	public static boolean isNum(String text) {
		try {
			Jval.read(text).asNumber();
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}
	public static boolean isStr(String text) {
		try {
			Jval.read(text).asString();
			return true;
		} catch (Throwable ignored) {
			return false;
		}
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
			throw new RuntimeException(thr);
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
		Class<?> cls  = o1.getClass();
		Jval     jval = Jval.newObject();
		Object   val1, val2;
		objectCaches.clear();
		while (cls != null && Object.class.isAssignableFrom(cls)) {
			if (!cls.isAnonymousClass() && !jval.has("type")) jval.put("type", cls.getSimpleName());
			Field[] fields = getFields(cls);
			for (Field f : fields) {
				int mod = f.getModifiers();
				if (Modifier.isStatic(mod) || !Modifier.isPublic(mod) || Modifier.isFinal(mod)) {
					continue;
				}
				if (o1 != o2) {
					val1 = f.get(o1);
					val2 = f.get(o2);
				} else val1 = val2 = f.get(o1);
				if (o1 == o2 || !Objects.equals(val1, val2)) {
					// if (display) Log.debug(f);
					addJval(jval, f, val2);
				}
			}
			cls = cls.getSuperclass();
		}
		return jval;
	}


	public static Jval handleJval(Object o) throws Exception {
		if (o == null) {
			return Jval.NULL;
		}
		if (Reflect.isWrapper(o.getClass())) {
			return Jval.read(String.valueOf(o));
		}
		label:
		if (o.getClass().isArray()) {
			if (objectCaches.containsKey(o)) break label;
			objectCaches.put(o, o);
			int  len    = Array.getLength(o);
			Jval newArr = Jval.newArray();
			for (int i = 0; i < len; i++) {
				newArr.add(handleJval(Array.get(o, i)));
			}
			return newArr;
		}
		if (o instanceof Seq) {
			Jval newArr = Jval.newArray();
			((Seq<?>) o).each(e -> {
				try {
					newArr.add(copyValue(e));
				} catch (Exception ex) {
					newArr.add(Jval.NULL);
				}
			});
			return newArr;
		}
		if (o instanceof String || o instanceof Color
				|| o instanceof TextureRegion || o instanceof Category) {
			return Jval.valueOf(String.valueOf(o));
		}
		if (o instanceof UnlockableContent) {
			return Jval.valueOf(((UnlockableContent) o).name);
		}
		if (objectCaches.containsKey(o)) return Jval.NULL;
		objectCaches.put(o, o);
		return copyValue(o);

	}

	public static void addJval(Jval jval, Field f, Object o) throws Exception {
		if (o == null) return;
		if (!AddFieldBtn.filter(f, o.getClass())) return;
		Jval res = handleJval(o);
		if (res != null) jval.put(f.getName(), res);
	}

	public static Jval copyValue(Object o) throws Exception {
		if (o == null) return Jval.NULL;
		Class<?> type = o.getClass();
		Class<?> cls  = type;
		/*if (cls.isPrimitive()) return Jval.read(String.valueOf(o));
		if (cls == String.class || TextureRegion.class.isAssignableFrom(cls)
				|| UnlockableContent.class.isAssignableFrom(cls) || Color.class.isAssignableFrom(cls))
			Jval.valueOf(String.valueOf(o));*/
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
		if (type != null) {
			while (type.isAnonymousClass()) type = type.getSuperclass();
			if (ClassMap.classes.containsValue(type, true)) {
				for (var c : otherTypes) {
					if (c.key.isAssignableFrom(type)) {
						jval.put("type", type.getSimpleName());
						break;
					}
				}
			}
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
		Class<?> type   = f.getType();
		long     offset = unsafe.objectFieldOffset(f);
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

	public static void __(boolean ignored) {}
}
