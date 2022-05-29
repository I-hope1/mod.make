package modmake.components;

import arc.Events;
import arc.files.Fi;
import arc.func.Func;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.serialization.Json;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.game.EventType;
import modmake.components.constructor.MyArray;
import modmake.components.constructor.MyObject;
import modmake.ui.dialog.MySettingsDialog;

import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static arc.util.serialization.Jval.Jformat;
import static mindustry.Vars.ui;
import static modmake.IntVars.data;
import static modmake.ui.dialog.MySettingsDialog.CheckSetting;

public class dataHandle {
	public static ObjectMap<String, ObjectMap<String, MyObject<Object, Object>>> framework;
	public static ObjectMap<String, String> types;
	public static StringMap content, settings;

	public final static JsonReader reader = new JsonReader();
	public final static Json json = new Json();

	public static class _Class {
		public MyObject<Object, Object> value;
		public boolean ok = false;
	}

	// framework
	static {
		framework = new ObjectMap<>();
		Fi[] fiList = data.child("framework").list();
		for (Fi fi : fiList) {
			var map = new ObjectMap<String, Prov<MyObject<Object, Object>>>();
			fi.walk(f -> {
				String str = f.readString();
				String parent = getParent(str);
				final var obj = new _Class();
				obj.value = parse(str);
				map.put(f.nameWithoutExtension(), () -> {
					if (parent.isEmpty()) return obj.value;
					var func = map.get(parent);
					if (func != null && !obj.ok) {
						obj.ok = true;
						func.get().each((k, v) -> {
							if (!obj.value.has(k)) {
								obj.value.put(k, v);
							}
						});
					}
					return obj.value;
				});
			});
			var tmp = new ObjectMap<String, MyObject<Object, Object>>();
			map.each((key, value) -> {
				tmp.put(key, value.get());
			});
			framework.put(fi.name(), tmp);
		}

	}

	// types
	static {
		types = new ObjectMap<>();
		Fi fi = data.child("types").child("default.ini");

		// [\u4e00-\u9fa5]为中文
		Matcher m = Pattern.compile("\\w+?\\s*=\\s*[^\n]+").matcher(fi.readString());
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
		content = new StringMap();
		Events.run(EventType.ClientLoadEvent.class, () -> {
			Fi file = data.child("content").child(ui.language.getLocale() + ".ini");
			if (file.exists()) {
				iniParse(content, file.readString());
			}
		});

	}

	// settings
	static {

		Fi fi = Vars.dataDirectory.child("mods(I hope...)").child("settings.txt");
		if (!fi.exists()) fi.writeString("");
		var map = StringMap.of();
		Matcher m = Pattern.compile("[\\w-]+?\\s*:\\s*\\w+").matcher(fi.readString());
		Seq<String> all = new Seq<>();
		// 将所有符合正则表达式的子串（电话号码）全部输出
		while (m.find()) {
			all.add(m.group());
		}
		all.each(type -> {
			var a = type.split("\\s*:\\s*");
			map.put(a[0], a[1]);
		});
		settings = new StringMap(map) {
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

	public static String getParent(String str) {
		if (str.startsWith("//")) {
			// [\u4e00-\u9fa5]为中文
			return str.replaceAll("//\\s+extend\\s+([\\u4e00-\\u9fa5]+)[^\\n]+", "$1");
		}
		return "";
	}

	public static StringMap iniParse(StringMap _map, String str) {
		// [\u4e00-\u9fa5]为中文
		var all = str.split("\n");
		var map = _map != null ? _map : new StringMap();
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
			return reader.parse(Jval.read(str).toString(Jformat.plain));
			// return reader.parse(str);
		} catch (Exception err) {
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
		MyObject output = value.isArray() ? new MyArray() : new MyObject<>();
		MyObject obj2 = output;
		ArrayList<JsonValue> valArr = new ArrayList<>();
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
		return formatPrint(cx, Format.valueOf(settings.get("format")));
	}

	public static String formatPrint(String cx, Format format) {
//		Log.info(cx);
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
