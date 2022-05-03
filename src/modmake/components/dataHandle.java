package modmake.components;

import arc.Events;
import arc.files.Fi;
import arc.func.Func;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import mindustry.Vars;
import mindustry.game.EventType;
import modmake.components.constructor.MyArray;
import modmake.components.constructor.MyInterface;
import modmake.components.constructor.MyObject;

import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static modmake.IntVars.data;

public class dataHandle {
	public static ObjectMap<String, ObjectMap<String, MyObject<Object, Object>>> framework;
	public static ObjectMap<String, String> types, settings;
	public static StringMap content;
	private final static JsonReader reader = new JsonReader();

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
				obj.value = toIntObject(hjsonParse(str));
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
			Fi file = data.child("content").child(Vars.ui.language.getLocale() + ".ini");
			if (file.exists()) {
				iniParse(content, file.readString());
			}
		});

	}

	// settings
	static {

		Fi fi = Vars.dataDirectory.child("mods(I hope...)").child("settings.txt");
		if (!fi.exists()) fi.writeString("");
		var map = new ObjectMap<String, String>();
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
		};

	}

	public static String getParent(String str) {
		if (str.substring(0, 2).equals("//")) {
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
			map.put(arr[0], arr[1].replaceAll("\\n", "\n"));
		}

		return map;
	}

	public static JsonValue hjsonParse(Number num) {
		return new JsonValue((Double) num);
	}

	public static JsonValue hjsonParse(Fi fi) {
		return hjsonParse(fi.readString());
	}

	/* hjson解析 (使用arc的JsonReader) */
	public static JsonValue hjsonParse(String str) {
		if (!"{".equals("" + str.replaceAll("^\\s+", "").charAt(0))) {
			str = "{\n" + str + "\n}";
		}
		try {
			return reader.parse(str);
			// return reader.parse(str);
		} catch (Exception err) {
			Log.err(err);
			return null;
		}
	}

	/*public static MyObject<?, ?> toIntObject(String s){
		return json.fromJson(MyObject.class, s);
	}*/

	public static MyObject<Object, Object> toIntObject(JsonValue value) {
		if (value == null) return new MyObject<>();
		MyObject<Object, Object> output = new MyObject<>();
		MyInterface obj2 = output;
		ArrayList<JsonValue> valArr = new ArrayList<>();
		ArrayList<MyInterface> objArr = new ArrayList<>();

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
					((MyArray<Object>)obj2).put(result);
				} else {
					obj2.put(child.name, result);
				}
			}
			if (objArr.isEmpty() || valArr.isEmpty()) break;
			value = valArr.remove(0);
			obj2 = objArr.remove(0);
		}
		// Log.info(output + "")
		return output;
	}
}
