package modmake.util;

import arc.func.Func;
import arc.func.Prov;

public class Tools {
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
		} catch (Exception ignored){}
		return defaultValue;
	}
	public static <T, R> R as(Object obj, Func<T, R> func) {
		return as(obj, func, null);
	}
}
