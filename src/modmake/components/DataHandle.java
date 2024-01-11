package modmake.components;

import arc.Core;
import arc.files.Fi;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.Vars;
import modmake.components.constructor.*;
import modmake.ui.dialog.MySettingsDialog;
import modmake.util.load.ContentVars;

import java.util.*;
import java.util.regex.*;

import static arc.util.serialization.Jval.Jformat;
import static mindustry.Vars.locales;
import static modmake.IntVars.data;
import static modmake.ui.dialog.MySettingsDialog.CheckSetting;

public class DataHandle {
	static Pattern getP = Pattern.compile("^//\\s+extend\\s+([\\u4e00-\\u9fa5]+)");

	public static ObjectMap<String, ObjectMap<String, Jval>> framework;
	public static ObjectMap<String, String>                  types;
	public static StringMap                                  dcontent, dsettings;

	public static JsonReader reader = new JsonReader();
	public static Json       json   = Reflect.get(ContentVars.parser, "parser");

	/**
	 * 数据目录
	 */
	public static final Fi dataDirectory = Vars.dataDirectory.child("b0kkihope");

	public static class _Class {
		public Jval    value;
		public boolean ok = false;
	}

	// framework
	static {
		framework = new ObjectMap<>();
		Fi[] fiList = data.child("framework").list();
		for (Fi fi : fiList) {
			var map = new ObjectMap<String, Prov<Jval>>();
			fi.walk(f -> {
				String    str    = f.readString();
				String    parent = getParent(str);
				final var obj    = new _Class();
				obj.value = Jval.read(str);
				map.put(f.nameWithoutExtension(), () -> {
					if (parent.isEmpty()) return obj.value;
					var func  = map.get(parent);
					var value = obj.value;
					if (func != null && !obj.ok) {
						obj.ok = true;
						var parentValue = func.get();
						if (value.isObject()) {
							for (var entry : parentValue.asObject()) {
								if (!value.has(entry.key)) value.put(entry.key, parentValue.get(entry.key));
							}
						}
					}
					return obj.value;
				});
			});
			var tmp = new ObjectMap<String, Jval>();
			map.each((key, value) -> {
				tmp.put(key, value.get());
			});
			framework.put(fi.name(), tmp);
		}

	}

	static String getLocate() {
		String loc = Core.settings.getString("locale");

		if (loc.equals("default")) {
			return findClosestLocale();
		}
		return loc;
	}

	static String findClosestLocale() {
		//check exact locale
		for (Locale l : locales) {
			if (l.equals(Locale.getDefault())) {
				return l.toString();
			}
		}

		//find by language
		for (Locale l : locales) {
			if (l.getLanguage().equals(Locale.getDefault().getLanguage())) {
				return l.toString();
			}
		}

		return "en";
	}

	// types
	static {
		types = new ObjectMap<>();
		Fi fi = data.child("types").child(getLocate() + ".ini");
		if (fi.exists()) parseType(fi);
		else {
			fi = data.child("types").child("default.ini");
			parseType(fi);
		}
	}

	static void parseType(Fi fi) {
		// [\u4e00-\u9fa5]为中文
		Matcher     m   = Pattern.compile("\\w+?\\s*=\\s*[^\n]+").matcher(fi.readString());
		Seq<String> seq = new Seq<>();
		// 将所有符合正则表达式的子串（电话号码）全部输出
		while (m.find()) {
			seq.add(m.group());
		}
		seq.each(type -> {
			var a = type.split("\\s*=\\s*");
			types.put(a[0], a[1]);
		});

	}

	// content
	static {
		dcontent = new StringMap();/* {
			public boolean hasChanged;
			public final StringMap nullMap = new StringMap();

			@Override
			public String get(String key) {
				if (containsKey(key)) return super.get(key);
				nullMap.put(key, null);
				hasChanged = true;
				return null;
			}

			final Fi fi = dataDirectory.child("未翻译的接口.txt");

			{
				if (fi.exists()) for (var str : fi.readString().split("\n")) {
					if (str.isBlank()) continue;
					nullMap.put(str, null);
				}

				Events.run(Trigger.update, () -> {
					if (!hasChanged) return;
					StringBuilder sb = new StringBuilder();
					for (var entry : nullMap) {
						sb.append(entry.key).append('\n');
					}
					fi.writeString(sb.toString());
				});
			}
		};*/
		Fi file = data.child("content").child(getLocate() + ".ini");
		if (!file.exists()) file = data.child("types").child("default.ini");
		iniParse(dcontent, file.readString());
	}

	// settings
	static {
		try {
			Fi fi = Vars.dataDirectory.child("mods(I hope...)");
			if (fi.exists() && fi.isDirectory()) {
				fi.copyFilesTo(dataDirectory);
				fi.deleteDirectory();
			}
		} catch (Throwable ignored) {}
		Fi fi = dataDirectory.child("settings.txt");
		if (!fi.exists()) fi.writeString("");
		var map = StringMap.of();
		for (var entry : Jval.read(fi.readString()).asObject()) {
			map.put(entry.key, String.valueOf(entry.value));
		}
		/*Matcher m = Pattern.compile("[\\w-]+?\\s*:\\s*\\w+").matcher(fi.readString());
		Seq<String> all = new Seq<>();
		// 将所有符合正则表达式的子串全部输出
		while (m.find()) {
			all.add(m.group());
		}
		all.each(type -> {
			var a = type.split("\\s*:\\s*");
			map.put(a[0], a[1]);
		});*/
		dsettings = new StringMap(map) {
			public String put(String k, String v) {
				super.put(k, v);
				var str = new StringJoiner("\n");
				each((key, value) -> {
					str.add(key + ":" + value);
				});
				fi.writeString(str + "");
				return k;
			}

			@Override
			public boolean getBool(String name) {
				var cr = MySettingsDialog.all.get(name);
				return (cr.bp == null || !cr.bp.get()) && (containsKey(name) ? super.getBool(name) : cr instanceof CheckSetting && ((CheckSetting) cr).def);
			}
		};

	}

	// to load static
	public static void load() {}

	public static String getParent(String str) {
		if (str.startsWith("//")) {
			// [\u4e00-\u9fa5]为中文
			Matcher m = getP.matcher(str);
			if (m.find()) return m.group(1);
		}
		return "";
	}

	public static StringMap iniParse(StringMap map, String str) {
		// [\u4e00-\u9fa5]为中文
		var all = str.split("\n");
		map = map != null ? map : new StringMap();
		for (var row : all) {
			if (row.isEmpty() || row.matches("(#|//)[^\n]+")) continue;
			var arr = row.split("\\s+=\\s+");
			if (arr.length <= 1) continue;
			map.put(arr[0], arr[1].replaceAll("\\\\n", "\n"));
		}

		return map;
	}

	public static JsonValue toJsonValue(Jval jval) {
		return reader.parse(jval.toString());
	}

	public static Jval hjsonParse(Number num) {
		return Jval.valueOf((Double) num);
	}

	/* hjson解析 (使用arc的JsonReader) */
	public static JsonValue hjsonParse(String str) {

		//		if (!Pattern.compile("^\\s*[\\[{]").matcher(str).find()) str = "{\n" + str + "\n}";
		try {
			return json.fromJson(null, Jval.read(str).toString(Jformat.plain));
			// return reader.parse(str);
		} catch (Exception err) {
			Log.info(str);
			Log.err(err);
			return null;
		}
	}

	public static MyObject parse(String str) {
		try {
			return toIntObject(hjsonParse(str));
		} catch (Exception e) {
			Log.err(e);
			return new MyObject<>();
		}
	}

	/*public static MyObject<?, ?> toIntObject(String s){
		return json.fromJson(MyObject.class, s);
	}*/

	public static MyObject toIntObject(JsonValue value) {
		if (value == null) return new MyObject<>();
		MyObject                  output = value.isArray() ? new MyArray() : new MyObject<>();
		MyObject                  obj2   = output;
		ArrayList<JsonValue>      valArr = new ArrayList<>();
		ArrayList<MyObject<?, ?>> objArr = new ArrayList<>();

		while (true) {
			for (JsonValue child = value.child; child != null; child = child.next) {
				Object result = ((Func<JsonValue, Object>) _child -> {
					if (_child.isArray()) {
						var array = new MyArray<>();
						valArr.add(_child);
						objArr.add(array);
						return array;
					}
					if (_child.isObject()) {
						var obj = new MyObject<>();
						valArr.add(_child);
						objArr.add(obj);
						return obj;
					}

					if (_child.isNumber()) return _child.asDouble();
					if (_child.isBoolean()) return _child.asBoolean();
					return _child.asString();
				}).get(child);
				if (child.name == null && obj2 instanceof MyArray) {
					((MyArray) obj2).put(result);
				} else {
					obj2.put(child.name, result);
				}
			}
			if (objArr.isEmpty() || valArr.isEmpty()) break;
			value = valArr.remove(0);
			obj2 = objArr.remove(0);
		}
		//		 Log.info(output + "");
		return output;
	}

	public static String formatPrint(String cx) {
		return formatPrint(cx, Format.valueOf(dsettings.get("format")));
	}

	public static String formatPrint(String cx, Format format) {
		switch (format) {
			case hjsonMin:
				return Jval.read(cx).toString(Jformat.hjson);
			case hjson:
				return json.prettyPrint(cx);
			case jsonMin:
				return Jval.read(cx).toString(Jformat.plain);
			case json:
				return Jval.read(cx).toString(Jformat.formatted);
			default:
				throw new IllegalArgumentException("not found format '" + format + "'.");
		}
	}

	public enum Format {
		hjsonMin, hjson, jsonMin, json
	}
}
