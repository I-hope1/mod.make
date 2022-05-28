package modmake.util;

import arc.Core;
import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Func;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.ui.ImageButton;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.serialization.Json;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.*;
import mindustry.ui.Styles;
import mindustry.world.meta.Attribute;
import modmake.IntUI;
import modmake.components.*;
import modmake.components.constructor.MyArray;
import modmake.components.constructor.MyObject;
import modmake.ui.styles;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import static mindustry.Vars.ui;
import static modmake.components.AddFieldBtn.defaultValue;
import static modmake.components.AddFieldBtn.filter;
import static modmake.components.dataHandle.*;
import static modmake.util.ContentSeq.getGenericType;
import static modmake.util.ContentSeq.otherTypes;

public class BuildContent {
	public static Json json = dataHandle.json;
	public static ObjectMap<Class<?>, Object> defaultClass = ObjectMap.of(
			Effect.class, "none",
			UnitType.class, "mono",
			Item.class, "copper",
			Liquid.class, "water",
			ItemStack.class, "copper/0",
			LiquidStack.class, "water/0",
			Attribute.class, Attribute.all[0],
			BulletType.class, new MyObject<>(),
			Sector.class, 0,
			Planet.class, "serpulo"
	);

	// 折叠黑名单
	static Seq<Class<?>> foldBlackList = Seq.with(String.class, Color.class, Category.class,
			ItemStack.class, LiquidStack.class, UnitType.class, Item.class, Liquid.class,
			Sector.class, Planet.class);
	// 单位额外字段
	static Seq<String> UnitTypeExFields = Seq.with("requirements", "waves", "controller", "type");
	public static BuildKeys filterKeys;
	public static BuildClasses filterClass;

	public static void load() {
		filterClass = new BuildClasses();
		filterKeys = new BuildKeys();
	}

	public static int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}

	public static int parseInt(Object s) {
		return parseInt("" + s);
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

	public static <T> Seq<T> genericSeqByClass(Class<?> clazz, Func<Field, T> _func) {
		Seq<T> seq = new Seq<>();
		var fs = clazz.getFields();
		for (var field : fs) {
			seq.add(_func.get(field));
		}
		return seq;
	}


	public static Table[] foldTable() {
		var table = new Table();
		var content = new Table();
		var col = new Collapser(content, false);
		col.setDuration(0.3f);

		var btn = new ImageButton(Icon.rightOpen, Styles.clearTogglei);
		var style = btn.getStyle();
		btn.clicked(() -> {
			col.toggle();
			style.imageUp = col.isCollapsed() ? Icon.rightOpen : Icon.downOpen;
		});
		style.up = style.over = styles.whiteui.tint(0.6f, 0.8f, 0.8f, 1f);
		table.add(btn).padTop(1).padBottom(1).padRight(4).growY().width(32);
		table.add(col).growX().left();
		if (settings.getBool("auto_fold_code")) {
			btn.fireClick();
		}
		return new Table[]{table, content};
	}


	public static <T extends UnlockableContent> Prov<MyObject>
	buildOneStack(Table t, String type, Seq<T> stack, String content, String amount) {
		var output = new MyObject<String, Prov<?>>();

		t = t.table(Tex.pane).grow().get();

		t.add('$' + type);

		if (content == null) content = stack.get(0).name;
		var prov = IntUI.selectionWithField(t, stack, content, 42, 32, 6, true);
		output.put(type, prov);

		t.add("$amount");
		var atf = t.field("" + amount, __ -> {}).get();
		output.put("amount", () -> parseInt(atf.getText()));

		return () -> output;
	}


	public static void
	copyAndPaste(Table table, String key, Object value, Cons<MyObject> paste, Runnable catchF) {
		table.table(t -> {
			// 复制
			t.button("", Icon.copy, styles.cleart, () -> {
				Core.app.setClipboardText("" + value);
			}).padRight(2);
			// 粘贴
			t.button("", Icon.paste, styles.cleart, () -> ui.showConfirm(
					"粘贴", "是否要粘贴", () -> {
						String txt = Core.app.getClipboardText();
						try {
							paste.get(parse(txt));
						} catch (Exception e) {
							catchF.run();
							ui.showException("无法粘贴", e);
						}
					}));
		}).padRight(6);
	}


	public static Prov<String>
	tableWithListSelection(Table table, String value, Seq<String> seq, String defaultValue, boolean searchable) {
		String[] val = {value != null ? value : defaultValue};
		var btn = new TextButton(types.get(val[0], () -> val[0]), styles.cleart);
		btn.clicked(() -> IntUI.showSelectListTable(btn, seq, () -> val[0],
				type -> btn.setText(types.get(val[0] = type, () -> val[0])
				), 150, 55, searchable));
		table.add(btn).minWidth(100).height(45).get();
		return () -> val[0];
	}

	public static <T extends UnlockableContent> Prov<String>
	tableWithFieldImage(Table table, String value, Class<?> vType, Seq<T> seq) {
		value = value != null ? value : defaultClass.get(vType) + "";
		return IntUI.selectionWithField(table, seq, value, 42, 32, 6, true);
	}

	/*public static MyObject<?, ?>
	tableWithTypeSelection(Table table, MyObject value, Class<?> vType, String defaultValue) {
		table = table.table().get();
		value = or(value, new MyObject());
		String typeName = or(value.remove("type") + "", defaultValue);
		var selection = new TypeSelection(Classes.get(typeName), typeName, otherTypes.get(vType));
		table.add(selection.table).padBottom(4).row();
		var cont = table.table().name("cont").get();
		var map = fObject(cont, () -> selection.type(), value);
		return map;
	}*/


	public static <T> Prov<String>
	listWithType(Table table, Object value, Class<?> vType, String defaultType, Seq<T> list, Func<T, String> _func, Seq<Class<?>> blackList) {
		list = list.copy();
		Seq<String> tmpList = new Seq<>();
		tmpList.addAll(list.as());
		tmpList.add("自定义");
		value = or(value, defaultClass.get(vType));
		boolean isObject = value instanceof MyObject;
		MyObject val1 = isObject ? (MyObject) value : new MyObject();
		var table1 = new Table();
		String typeName = or(val1.remove("type") + "", defaultType);
		var selection = new TypeSelection(Classes.get(typeName), typeName, or(otherTypes.get(vType), new Seq<>(vType)));
		table1.add(selection.table).padBottom(4).row();
		var cont = table1.table().name("cont").get();
		var map = fObject(cont, selection::type, val1, or(blackList, new Seq<>()));

		String[] val2 = {isObject ? "自定义" : value + ""};
		Object[] retV = {val2};
		Cell<?>[] cell = {null};
		TextButton[] btn = {null};
		Seq<T> finalList = list;
		btn[0] = table.button(types.get(val2[0], () -> val2[0]), styles.cleart, () -> {
			IntUI.showSelectListTable(btn[0], tmpList, () -> val2[0], fx -> {
				btn[0].setText(types.get(fx, () -> fx));
				if ("自定义".equals(fx)) {
					cell[0].setElement(table1);
					retV[0] = map;
					val2[0] = "自定义";
				} else {
					cell[0].clearElement();
					retV[0] = val2[0] = _func.get(finalList.find(item -> item.equals(fx)));
				}
			}, 150, 55, true);
		}).width(200).height(45).get();
		table.row();
		cell[0] = table.add();

		if (isObject) {
			cell[0].setElement(table1);
			retV[0] = map;
		}

		return () -> (retV[0] instanceof Prov ? ((Prov<?>) retV[0]).get() : retV[0]) + "";
	}

	public static <T> Prov<String>
	listWithType(Table table, Object value, Class<?> vType, String defaultType, Seq<T> list, Func<T, String> _func) {
		return listWithType(table, value, vType, defaultType, list, _func, null);
	}

	public static Prov<MyObject> tableWithTypeSelection(Table table, MyObject value, Class<?> vType, String defaultType) {
		table = table.table().get();
		var obj = or(value, new MyObject<>());
		String typeName = or("" + value.remove("type"), defaultType);
		var selection = new TypeSelection(Classes.get(typeName), typeName, otherTypes.get(vType));
		table.add(selection.table).padBottom(4).row();
		Table cont = table.table().name("cont").get();
		var map = fObject(cont, selection::type, obj);
		return map;
	}

	public static Prov<MyObject>
	fObject(Table table, Prov<Class<?>> type, MyObject value) {
		return fObject(table, type, value, null, false);
	}

	public static Prov<MyObject>
	fObject(Table table, Prov<Class<?>> type, MyObject value, Seq<Class<?>> typeBlackList) {
		return fObject(table, type, value, typeBlackList, false);
	}

	public static Prov<MyObject>
	fObject(Table parent, Prov<Class<?>> type, MyObject value, Seq<Class<?>> typeBlackList, boolean all) {
		Table table = new Table(Tex.pane), children = new Table();
		Fields fields = new Fields(value, type, children);
		children.center().defaults().center().minWidth(100);
		table.add(children).row();
		parent.add(table);

		if (all) {
			Class<?> vType = type.get();
			for (Field f : vType.getFields()) {
				f.setAccessible(true);
				if (filter(f, vType)) {
					fields.add(null, f.getName(), defaultValue(f.getType()));
				}
			}
		} else {
			value.each((k, v) -> {
				if (!(v instanceof Method))
					fields.add(null, k + "");
			});
			table.add(new AddFieldBtn(value, fields, type)).fillX().growX().minWidth(100);
		}
		return () -> {
			if (typeBlackList == null || !typeBlackList.contains(type.get()))
				value.put("type", type.get().getSimpleName());
			return value;
		};
	}


	public static Prov<MyArray>
	fArray(Table parent, Class<?> vType, MyArray v) {
		Table table = new Table(), children = new Table();
		var fields = new Fields(v = or(v, new MyArray<>()), () -> vType, children);
		children.center().defaults().center().minWidth(100);
		table.add(children).name("cont").row();
		parent.add(table);
		Cons2<Integer, Object> addItem = (i, value) -> {
			var table1 = new Table();
			build(vType, fields, table1, i + "", value, true);
			fields.add(table1, i + "");
		};
		v.cpy().each((i, val) -> {
			addItem.get((int) i, val);
		});
		table.button("@add", () -> {
			addItem.get(-1, AddFieldBtn.defaultValue(vType));
		}).growX().minWidth(100);

		MyArray finalV = v;
		return () -> finalV;
	}

	static class Type {
		public Field field;
		public Class<?> vType;

		public Type(Class<?> type, String k, boolean isArray) {
			try {
				field = isArray ? null : type.getField(k);
				vType = isArray ? type : field.getType();
			} catch (Exception e) {
				// ui.showException(e);
			}

		}
	}

	// 处理字符串和失败的对象
	public static Prov<String> fail(Table t, Object v, Class<?> vType) {
		if (vType == null) vType = String.class;
		var field = new TextField(("" + v).replaceAll("[\\n\\r]", "\\n").replaceAll("\\t", "\\t"));
		if (String.class.isAssignableFrom(vType)) IntUI.longPress(field, 600, longPress -> {
			if (longPress) IntUI.showTextArea(field);
		});
		if (Vars.mobile) field.removeInputDialog();
		t.add(field).growX();
		Class<?> finalVType = vType;
		return () -> {
			var txt = !field.getText().replace("\\s*", "").isBlank() ? field.getText() : "";
			// 通过Jval转义
			txt = (Jval.read('"' + txt.replaceAll("\\\\\"", "\\\"") + '"') + "").replaceAll("[\\n\\r]", "\n").replaceAll("\"", "\\\"");
			return finalVType.isPrimitive() ? txt : '"' + txt + '"';
		};
	}

	/* 构建table */
	public static void
	build(Class<?> type, Fields fields, Table table, String k, Object v, boolean isArray) {
		if (type == null) return;
		boolean unknown = false;
		Object[] value = {v};
		if (!isArray && (type != UnitType.class || !UnitTypeExFields.contains(k)) && settings.getBool("point_out_unknown_field") && !json.getFields(type).containsKey(k)) {
			table.table(Tex.pane, left -> left.add("未知", Color.yellow)).padRight(5);
			unknown = true;
		}

		var map = fields.map;

		var NewType = new Type(type, k, isArray);
		Field field = NewType.field;
		Class<?> vType = NewType.vType;

		// 折叠代码
		Table[] foldT = null;
		if (vType != null && !vType.isPrimitive() && !foldBlackList.contains(vType)) {
			foldT = foldTable();
		}
		// 不是数组的话，添加key
		if (!isArray) {
			table.add((unknown ? k : content.get(k, () -> k)) + ':').fillX().left().padLeft(2).padRight(6);
		}

		Table[] finalTable = {null};
		if (foldT != null) {
			table.add(foldT[0]);
			finalTable[0] = foldT[1];
		} else {
			finalTable[0] = table;
		}

		Prov<?> output = ((Prov<Prov<?>>) () -> {

			try {
				if (vType == null || vType == String.class) {
					return null;
				}
				if (vType.isPrimitive()) {
					if (vType.getSimpleName().equalsIgnoreCase("boolean")) {
						boolean[] val = {Boolean.parseBoolean(value[0] + "")};
						StringMap obj = StringMap.of(
								"true", "是",
								"false", "否");
						var btn = new TextButton(obj.get("" + val[0]), styles.cleart);
						btn.clicked(() -> btn.setText(obj.get((value[0] = (val[0] = !val[0])) + "")));
						finalTable[0].add(btn).minWidth(100).height(45).get();
						return () -> val[0];
					}
					return null;
				}

				if ((vType.isArray() || AddFieldBtn.arrayClass.contains(vType)) && v instanceof MyArray && !Objects.equals(k, "upgrades")) {
					return fArray(finalTable[0], AddFieldBtn.arrayClass.contains(vType) ? getGenericType(field).get(0) : vType.getComponentType(), (MyArray) v);
				}
				if (ObjectMap.class.isAssignableFrom(vType)) {
					var classes = getGenericType(field);
					return filterClass.get(vType).get(finalTable[0], v, vType, classes);
				}
				if (filterClass.containsKey(vType)) {
					return filterClass.get(vType).get(finalTable[0], v, vType, null);
				}
			} catch (Exception e) {
				Log.info(type);
				Log.err(e);
			}
			if (filterKeys.containsKey(k)) {
				return filterKeys.get(k).get(finalTable[0], v, type);
			}
			return null;
		}).get();
		var res = or(output, () -> fail(table, value[0], vType));
		if (isArray) {
			((MyArray)map).put(res);
		} else map.put(k, res);

		// 右边
		Table[] finalFoldT = foldT;
		boolean finalUnknown = unknown;
		table.table(right -> {
			right.right().defaults().right();
			if (finalFoldT != null) copyAndPaste(right, k, value[0], v2 -> {
				map.put(k, v2);
				fields.setTable(k, Fields.json(fields, 0, k));
			}, () -> {
				map.put(k, value[0]);
				fields.setTable(k, table);
			});
			// 帮助按钮
			if (!isArray && !finalUnknown && content.containsKey(k + ".help")) {
				var btn = new TextButton("?", Styles.clearPartialt);
				right.add(btn).size(8 * 5).padLeft(5).padRight(5).right().grow().get();
				btn.clicked(() -> IntUI.showSelectTable(btn, (p, __, ___) -> {
					p.pane(help -> help.add(content.get(k + ".help"), 1.3f)).pad(4, 8, 4, 8);
				}, false));
			}
			// 删除按钮
			right.button("", Icon.trash, styles.cleart, () -> fields.remove(k));
		}).padLeft(4).growX().right();

		table.row();
	}

	public static void
	build(Class<?> type, Fields fields, Table table, String k, Object v) {
		build(type, fields, table, k, v, fields.map instanceof MyArray<?>);
	}
}
