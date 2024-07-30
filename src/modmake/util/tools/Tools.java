package modmake.util.tools;

import arc.func.*;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.util.Timer;
import arc.util.*;
import arc.util.Timer.Task;
import rhino.ScriptRuntime;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;
import java.util.jar.*;
import java.util.regex.Pattern;


public class Tools {
	public static boolean validPosInt(String text) {
		return text.matches("^\\d+(\\.\\d*)?([Ee]\\d+)?$");
	}

	// public static Pattern nothingP = Pattern.compile("");
	public static Pattern compileRegExp(String text) {
		try {
			return Pattern.compile(text, Pattern.CASE_INSENSITIVE);
		} catch (Throwable e) {
			return null;
			// return nothingP;
		}
	}

	public static boolean isNum(String text) {
		try {
			return !ScriptRuntime.isNaN(ScriptRuntime.toNumber(text));
		} catch (Throwable ignored) {
			return false;
		}
	}
	public static int asInt(String text) {
		return (int) Float.parseFloat(text);
	}

	// 去除颜色
	public static String format(String s) {
		return s.replaceAll("\\[(\\w+?)\\]", "[\u0001$1]");
	}
	public static int len(String s) {
		return s.split("").length - 1;
	}
	public static Vec2 getAbsPos1(Element el) {
		return el.localToStageCoordinates(Tmp.v1.set(el.getWidth() / -2f, el.getHeight() / -2f));
	}
	public static Vec2 getAbsPos(Element el) {
		if (true) return el.localToStageCoordinates(Tmp.v1.set(0, 0));
		Vec2 vec2 = new Vec2(el.x, el.y);
		while (el.parent != null) {
			el = el.parent;
			vec2.add(el.x, el.y);
		}
		return vec2;
	}

	public static void forceRun(Boolp boolp) {
		// Log.info(Time.deltaimpl);
		Timer.schedule(new Task() {
			@Override
			public void run() {
				try {
					if (boolp.get()) cancel();
				} catch (Exception e) {
					Log.err(e);
					cancel();
				}
			}
		}, 0, 1, -1);
		/*Runnable[] run = {null};
		run[0] = () -> {
			Time.runTask(0, () -> {
				try {
					toRun.run();
				} catch (Exception e) {
					run[0].run();
				}
			});
		};
		run[0].run();*/
	}

	/**
	 * @param pack 包名
	 *
	 * @return Set数组
	 **/
	public static Set<Class<?>> getClasses(String pack) {
		// 第一个class类的集合
		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		// 是否循环迭代
		boolean recursive = true;
		// 获取包的名字 并进行替换
		String packageDirName = pack.replace('.', '/');
		// 定义一个枚举的集合 并进行循环来处理这个目录下的things
		Enumeration<URL> dirs;
		try {
			dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
			// 循环迭代下去
			while (dirs.hasMoreElements()) {
				// 获取下一个元素
				URL url = dirs.nextElement();
				// 得到协议的名称
				String protocol = url.getProtocol();
				// 如果是以文件的形式保存在服务器上
				if ("file".equals(protocol)) {
					// 获取包的物理路径
					String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
					// 以文件的方式扫描整个包下的文件 并添加到集合中
					findClassesInPackageByFile(pack, filePath, recursive, classes);
				} else if ("jar".equals(protocol)) {
					// 如果是jar包文件
					// 定义一个JarFile
					System.out.println("jar类型的扫描");
					JarFile jar;
					try {
						// 获取jar
						jar = ((JarURLConnection) url.openConnection()).getJarFile();
						// 从此jar包 得到一个枚举类
						Enumeration<JarEntry> entries = jar.entries();
						findClassesInPackageByJar(pack, entries, packageDirName, recursive, classes);
					} catch (IOException e) {
						// log.error("在扫描用户定义视图时从jar包获取文件出错");
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return classes;
	}
	/* 以文件的形式来获取包下的所有Class
	 *
	 * @param packageName
	 * @param packagePath
	 * @param recursive
	 * @param classes
	 */
	private static void findClassesInPackageByFile(String packageName, String packagePath, final boolean recursive,
																								 Set<Class<?>> classes) {
		// 获取此包的目录 建立一个File
		File dir = new File(packagePath);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) {
			// log.warn("用户定义包名 " + packageName + " 下没有任何文件");
			return;
		}
		// 如果存在 就获取包下的所有文件 包括目录
		// 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
		File[] dirfiles = dir.listFiles(file ->
		 (recursive && file.isDirectory()) || file.getName().endsWith(".class"));
		// 循环所有文件
		assert dirfiles != null;
		for (File file : dirfiles) {
			// 如果是目录 则继续扫描
			if (file.isDirectory()) {
				findClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes);
			} else {
				// 如果是java类文件 去掉后面的.class 只留下类名
				String className = file.getName().substring(0, file.getName().length() - 6);
				try {
					// 添加到集合中去
					// classes.add(Class.forName(packageName + '.' +
					// className));
					// 经过回复同学的提醒，这里用forName有一些不好，会触发static方法，没有使用classLoader的load干净
					classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
				} catch (ClassNotFoundException e) {
					// log.error("添加用户自定义视图类错误 找不到此类的.class文件");
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 以jar的形式来获取包下的所有Class
	 *
	 * @param packageName    包名
	 * @param entries        ？？？
	 * @param packageDirName ？？？
	 * @param recursive      ？？？
	 * @param classes        ？？？
	 */
	private static void findClassesInPackageByJar(String packageName, Enumeration<JarEntry> entries,
																								String packageDirName, final boolean recursive,
																								Set<Class<?>> classes) {
		// 同样的进行循环迭代
		while (entries.hasMoreElements()) {
			// 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
			JarEntry entry = entries.nextElement();
			String   name  = entry.getName();
			// 如果是以/开头的
			if (name.charAt(0) == '/') {
				// 获取后面的字符串
				name = name.substring(1);
			}
			// 如果前半部分和定义的包名相同
			if (name.startsWith(packageDirName)) {
				int idx = name.lastIndexOf('/');
				// 如果以"/"结尾 是一个包
				if (idx != -1) {
					// 获取包名 把"/"替换成"."
					packageName = name.substring(0, idx).replace('/', '.');
				}
				// 如果可以迭代下去 并且是一个包
				if ((idx != -1) || recursive) {
					// 如果是一个.class文件 而且不是目录
					if (name.endsWith(".class") && !entry.isDirectory()) {
						// 去掉后面的".class" 获取真正的类名
						String className = name.substring(packageName.length() + 1, name.length() - 6);
						try {
							// 添加到classes
							classes.add(Class.forName(packageName + '.' + className));
						} catch (ClassNotFoundException e) {
							// .error("添加用户自定义视图类错误 找不到此类的.class文件");
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public static Class<?> box(Class<?> type) {
		if (!type.isPrimitive()) return type;
		if (type == boolean.class) return Boolean.class;
		if (type == byte.class) return Byte.class;
		if (type == char.class) return Character.class;
		if (type == short.class) return Short.class;
		if (type == int.class) return Integer.class;
		if (type == float.class) return Float.class;
		if (type == long.class) return Long.class;
		if (type == double.class) return Double.class;
		return type;
		// return TO_BOX_MAP.get(type, type);
	}
	public static Class<?> unbox(Class<?> type) {
		if (type.isPrimitive()) return type;
		if (type == Boolean.class) return boolean.class;
		if (type == Byte.class) return byte.class;
		if (type == Character.class) return char.class;
		if (type == Short.class) return short.class;
		if (type == Integer.class) return int.class;
		if (type == Float.class) return float.class;
		if (type == Long.class) return long.class;
		if (type == Double.class) return double.class;
		// it will not reach
		return type;
	}

	public static <T> T as(Object o) {
		//noinspection unchecked
		return (T) o;
	}

	public static <T> T or(T t1, T t2) {
		return t1 == null ? t2 : t1;
	}
	public static <T> T or(T t1, Prov<T> t2) {
		return t1 == null ? t2.get() : t1;
	}

	/** 对value处理并返回 */
	public static <T> T srr(T value, Consumer<T> cons) {
		cons.accept(value);
		return value;
	}
	public static boolean testP(Pattern pattern, String text) {
		return pattern == null || pattern.matcher(text).find();
	}
	public static void __(Object __) {}
	public static <T> void checknull(T t, Consumer<T> cons) {
		if (t != null) cons.accept(t);
	}
	public static <T> void checknull(T t, Runnable run) {
		if (t != null) run.run();
	}

	public interface SRI<T> {
		SRI none = new SRI() {
			public SRI reset(Function func) {
				return this;
			}

			public SRI isInstance(Class cls, Consumer consumer) {
				return this;
			}

			public SRI cons(Consumer consumer) {
				return this;
			}

			public Object get() {
				return null;
			}
		};

		SRI<T> reset(Function<T, T> func);

		<R> SRI<T> isInstance(Class<R> cls, Consumer<R> consumer);

		SRI<T> cons(Consumer<T> consumer);

		T get();
	}


	// Reflection
	public static Object invoke(Method m, Object obj, Object... args) {
		try {
			return m.invoke(obj, args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}

