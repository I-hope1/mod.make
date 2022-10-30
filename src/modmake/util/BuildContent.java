package modmake.util;

import arc.Core;
import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Func;
import arc.func.Prov;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Time;
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
import modmake.components.AddFieldBtn;
import modmake.components.DataHandle;
import modmake.components.MyTextField;
import modmake.components.TypeSelection;
import modmake.components.build.BClasses;
import modmake.components.build.BKeys;
import modmake.components.build.inspect.IClasses;
import modmake.components.build.inspect.IKeys;
import modmake.components.build.inspect.Inspect;
import modmake.components.constructor.MyArray;
import modmake.components.constructor.MyObject;
import modmake.ui.styles;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import static mindustry.Vars.ui;
import static modmake.components.AddFieldBtn.*;
import static modmake.components.DataHandle.*;
import static modmake.util.Tools.*;
import static modmake.util.load.ContentSeq.getGenericType;
import static modmake.util.load.ContentSeq.otherTypes;

public class BuildContent {
	public static Json json = DataHandle.json;
	public static ObjectMap<Class<?>, Prov<?>> defaultClass = new ObjectMap<>();

	static {
		defaultClass.put(Effect.class, () -> "none");

		defaultClass.put(UnitType.class, () -> "mono");
		defaultClass.put(Item.class, () -> "copper");
		defaultClass.put(Liquid.class, () -> "water");
		defaultClass.put(ItemStack.class, () -> "copper/0");
		defaultClass.put(LiquidStack.class, () -> "water/0");
		defaultClass.put(Attribute.class, () -> Attribute.all[0]);
		defaultClass.put(BulletType.class, MyObject::new);
		defaultClass.put(Sector.class, () -> 0);
		defaultClass.put(Planet.class, () -> "serpulo");
	}

	public static ObjectMap<String, Prov<?>> defaultKey = ObjectMap.of(
			"consumes", (Prov) MyObject::new
	);

	// 折叠黑名单
	static Seq<Class<?>> foldBlackList = Seq.with(String.class, Color.class, Category.class,
			ItemStack.class, LiquidStack.class, UnitType.class, Item.class, Liquid.class,
			Sector.class, Planet.class);
	// 单位额外字段
	static Seq<String> UnitTypeExFields = Seq.with("requirements", "waves", "controller", "type");
	public static BKeys filterKeys;
	public static BClasses filterClass;
	public static IKeys inspectKeys;
	public static IClasses inspectClass;
	public static OrderedMap<Object, Inspect> inspectMap = new OrderedMap<>();

	public static void load() {
		filterClass = new BClasses();
		filterKeys = new BKeys();
		//		inspectKeys = new IKeys();
		//		inspectClass = new IClasses();
	}

	public static int parseInt(String s) {
		try {
			return Jval.read(s).asInt();
		} catch (Exception e) {
			return 0;
		}

	}

	public static int parseInt(Object s) {
		return parseInt("" + s);
	}

	public static String packString(String str) {
		return str.replaceAll("[\n\r]", "\\\\n").replaceAll("\t", "\\\\t");
	}

	public static String unpackString(String str) {
		return str.replace("\\n", "\n");
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
		col.setDuration(0.1f);

		var btn = new ImageButton(Icon.rightOpen, Styles.clearTogglei);
		var style = btn.getStyle();
		btn.clicked(() -> {
			col.toggle();
			style.imageUp = col.isCollapsed() ? Icon.rightOpen : Icon.downOpen;
		});
		style.up = style.over = styles.whiteui.tint(0.6f, 0.8f, 0.8f, 1f);
		table.add(btn).padTop(1).padBottom(1).padRight(4).size(32);
		table.add(col).growX().left();
		if (settings.getBool("auto_fold_code")) {
			btn.fireClick();
		}
		return new Table[]{table, content};
	}


	public static <T extends UnlockableContent> Prov<MyObject<String, Object>>
	buildOneStack(Table t, String type, Seq<T> stack, String content, String amount) {
		var output = new MyObject<String, Object>();

		t = t.table(Tex.pane).grow().get();

		t.add('$' + type);

		if (content == null) content = stack.get(0).name;
		var prov = IntUI.selectionWithField(t, stack, content, 42, 32, 6, true);

		t.add("$amount");
		var atf = new TextField("" + amount);
		t.add(atf);

		return () -> {
			output.put("item", prov.get());
			output.put("amount", parseInt(atf.getText()));
			return output;
		};
	}


	public static void
	copyAndPaste(Table table, Object key, Object value, Cons<MyObject> paste, Runnable catchF) {
		table.table(t -> {
			// 复制
			t.button("", Icon.copy, styles.cleart, () -> {
				Core.app.setClipboardText("" + value);
				ui.showInfoFade("已复制");
			}).padRight(2);
			// 粘贴
			t.button("", Icon.paste, styles.cleart, () -> ui.showConfirm(
					"粘贴", "是否要粘贴", () -> {
						String txt = Core.app.getClipboardText();
						try {
							paste.get(parse(txt));
							ui.showInfoFade("已粘贴");
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
		value = value != null ? value : defaultClass.get(vType).get() + "";
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


	public static <T extends UnlockableContent> Prov<String>
	itemSelectionWithType(Table table, Object value, Class<?> vType, Seq<T> items,
	                      Class<?> defaultType, Func<T, String> _func, Seq<Class<?>> blackList) {
		items = items.copy();
		Seq<Object> tmpList = new Seq<>();
		tmpList.addAll(items.as());
		tmpList.add("自定义");
		value = Tools.or(value, () -> defaultClass.get(vType).get());
		boolean isObject = value instanceof MyObject;
		MyObject<Object, Object> val1 = isObject ? as(value) : new MyObject<Object, Object>();
		assert val1 != null;
		var table1 = new Table();
		String typeName = "" + Tools.or(val1.remove("type"), defaultType);
		var selection = new TypeSelection(or(Classes.get(typeName), () -> {
			Time.runTask(0, () -> ui.showException(new ClassNotFoundException("无法找到类: " + typeName)));
			return Object.class;
		}), typeName, Tools.or(otherTypes.get(vType), new Seq<>(vType)));
		table1.add(selection.table).padBottom(4).row();
		var cont = table1.table().name("cont").get();
		var map = fObject(cont, selection::type, val1, Tools.or(blackList, new Seq<>()));

		Object[] retV = {isObject ? "自定义" : value};
		Object[] val2 = {retV[0]};
		Cell<?>[] cell = {null};
		ImageButton[] btn = {null};
		Seq<T> finalItems = items;
		Func<Object, Drawable> getImg = o -> {
			if (o instanceof UnlockableContent) return new TextureRegionDrawable(((UnlockableContent) o).uiIcon);
			return Icon.add;
		};
		btn[0] = table.button(Tex.whiteui, Styles.logici, () -> {
			IntUI.showSelectImageTableWithFunc(btn[0], tmpList, () -> val2[0], obj -> {
				btn[0].getStyle().imageUp = getImg.get(obj);
				if ("自定义".equals(obj)) {
					cell[0].setElement(table1);
					retV[0] = map;
					val2[0] = "自定义";
				} else {
					cell[0].clearElement();
					retV[0] = val2[0] = _func.get(finalItems.find(item -> item.equals(obj)));
				}
			}, 150, 42, 6, getImg, true);
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
	listWithType(Table table, Object value, Class<?> vType, String
			defaultType, Seq<T> list, Func<T, String> _func, Seq<Class<?>> blackList) {
		list = list.copy();
		Seq<String> tmpList = new Seq<>();
		tmpList.addAll(list.as());
		tmpList.add("自定义");
		value = Tools.or(value, () -> defaultClass.get(vType).get());
		boolean isObject = value instanceof MyObject;
		MyObject<Object, Object> val1 = isObject ? as(value) : new MyObject<Object, Object>();
		assert val1 != null;
		var table1 = new Table();
		String typeName = "" + Tools.or(val1.remove("type"), defaultType);
		var selection = new TypeSelection(or(Classes.get(typeName), () -> {
			Time.runTask(0, () -> ui.showException(new ClassNotFoundException("无法找到类: " + typeName)));
			return Object.class;
		}), typeName, Tools.or(otherTypes.get(vType), new Seq<>(vType)));
		table1.add(selection.table).padBottom(4).row();
		var cont = table1.table().name("cont").get();
		var map = fObject(cont, selection::type, val1, Tools.or(blackList, new Seq<>()));

		Object[] retV = {isObject ? "自定义" : value};
		String[] val2 = {"" + retV[0]};
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
	listWithType(Table table, Object value, Class<?> vType, String
			defaultType, Seq<T> list, Func<T, String> _func) {
		return listWithType(table, value, vType, defaultType, list, _func, null);
	}

	public static Prov<MyObject<Object, Object>> tableWithTypeSelection(Table table, MyObject<Object, Object> value, Class<?> vType, String
			defaultType) {
		table = table.table().get();
		var obj = Tools.or(value, MyObject::new);
		String typeName = "" + Tools.or(value.remove("type"), defaultType);
		var selection = new TypeSelection(or(Classes.get(typeName), () -> {
			Time.runTask(1, () -> ui.showException(new ClassNotFoundException("cannot find class typeName: " + typeName)));
			return Object.class;
		}), typeName, otherTypes.get(vType));
		table.add(selection.table).padBottom(4).row();
		Table cont = table.table().name("cont").get();
		return fObject(cont, selection::type, as(obj));
	}

	public static Prov<MyObject<Object, Object>>
	fObject(Table table, Prov<Class<?>> type, MyObject<Object, Object> value) {
		return fObject(table, type, value, null, false);
	}

	public static Prov<MyObject<Object, Object>>
	fObject(Table table, Prov<Class<?>> type, MyObject<Object, Object> value, Seq<Class<?>> typeBlackList) {
		return fObject(table, type, value, typeBlackList, false);
	}

	public static Prov<MyObject<Object, Object>>
	fObject(Table parent, Prov<Class<?>> type, MyObject<Object, Object> value, Seq<Class<?>> typeBlackList, boolean all) {
		Table table = new Table(Tex.pane), children = new Table();
		Fields fields = new Fields(value, type, children);
		children.center().defaults().center().minWidth(100);
		Cell<?> cell = parent.add(table);
		final boolean[] resize = {false};
		var pane = new ScrollPane(null);
		Button[] button = {null};
		pane.addListener(new InputListener() {
			@Override
			public boolean keyUp(InputEvent event, KeyCode keycode) {
				if (keycode == KeyCode.escape) button[0].fireClick();
				return false;
			}
		});
		button[0] = table.button(Icon.resize, Styles.clearTogglei, 32, () -> {
			if (resize[0]) {
				resize[0] = false;
				pane.setWidget(null);
				cell.setElement(table);
				Core.scene.setKeyboardFocus(parent);
				Core.scene.root.removeChild(pane);
			} else {
				pane.fillParent = true;
				cell.clearElement();
				pane.setWidget(table);
				resize[0] = true;
				Core.scene.setKeyboardFocus(pane);
				Core.scene.add(pane);
			}
		}).size(45).get();
		table.row();
		table.add(children).row();

		if (all && value.size == 0) {
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
					fields.add(null, k);
			});
			table.add(new AddFieldBtn(value, fields, type)).fillX().growX().minWidth(100);
		}
		return () -> {
			if (typeBlackList == null || !typeBlackList.contains(type.get()))
				nullCheck(type.get(), t -> value.put("type", t.getSimpleName()));
			return value;
		};
	}


	public static Prov<MyArray>
	fArray(Table parent, Class<?> vType, MyArray v) {
		Table table = new Table(), children = new Table();
		//		MyArray newV = new MyArray<>();
		var fields = new Fields(v = Tools.or(v, MyArray::new), () -> vType, children);
		children.center().defaults().center().minWidth(100);
		table.add(children).name("cont").row();
		parent.add(table);
		Cons2<Integer, Object> addItem = (i, value) -> {
			var table1 = new Table();
			build(vType, fields, table1, i, value, true);
			fields.add(table1, i);
		};
		v.cpy().each((i, val) -> {
			addItem.get((Integer) i, val);
		});
		MyArray finalV = v;
		table.button("@add", () -> {
			addItem.get(finalV.nextId(), AddFieldBtn.defaultValue(vType));
		}).growX().minWidth(100);

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
	public static Prov<String> field(Table t, Object v, Class<?> vType) {
		if (vType == null) vType = String.class;
		//		Log.info(v);
		var field = new MyTextField(packString(Tools.toString(v)));
		if (String.class.isAssignableFrom(vType)) IntUI.longPress(field, 600, longPress -> {
			if (longPress) IntUI.showTextArea(field);
		});
		if (Vars.mobile) field.removeInputDialog();
		t.add(field);
		Class<?> finalVType = vType;
		return () -> {
			var txt = !field.getText().replace("\\s*", "").isBlank() ? field.getText() : "";
			txt = trope(txt);
			return finalVType.isPrimitive() ? txt : '"' + txt + '"';
		};
	}

	/* 构建table */
	public static void
	build(Class<?> type, Fields fields, Table table, Object k, Object v, boolean isArray) {
		if (type == null || table == null) return;
		final boolean[] unknown = {false};
		//		String[] extensionText = {""};
		String strK = "" + k;
		Object[] value = {v};
		if (!isArray) {
			TextButton[] button = {null};
			AddFieldBtn[] addF = {null};
			button[0] = table.button("未知", Styles.flatBordert, () -> {
				addF[0].runnable.run();
			}).width(64).growY().update(b -> {
				unknown[0] = (type != UnitType.class || !UnitTypeExFields.contains(strK))
						&& settings.getBool("point_out_unknown_field") && !caches.get(type).containsKey(strK);
				button[0].getLabel().setText(unknown[0] ? "未知" : "重命名");
				button[0].getLabelCell().color(unknown[0] ? Color.yellow : Color.sky);
			}).get();
			addF[0] = new AddFieldBtn(fields.map, fields, () -> fields.type, name -> {
				fields.map.remove(k);
				fields.map.put(name, v);
				fields.setTable(k, Fields.json(fields, fields.nextId(), name));
			});
			addF[0].bind = button[0];
		}
		/*table.table(Tex.pane, left -> {
			left.add("未知", Color.yellow).visible(() -> {
				return unknown[0] = !isArray && (type != UnitType.class
						|| !UnitTypeExFields.contains(strK)) && settings.getBool("point_out_unknown_field")
						&& !json.getFields(type).containsKey(strK);
			}).padRight(5);
			left.label(() -> extensionText[0]).padRight(5).visible(() -> {
				var inspect = inspectMap.get(k);
				if (inspect == null) return true;
				var ex = inspect.get(v);
				if (ex == null) return true;
				extensionText[0] = ex.message;
				return ex.type != IExceptionType.NONE;
			}).padRight(5);;
		}).padRight(5).visible(() -> unknown[0]);*/

		var map = fields.map;

		var NewType = new Type(type, strK, isArray);
		Field field = NewType.field;
		Class<?> vType = NewType.vType;

		// 折叠代码
		Table[] foldT = null;
		if (vType != null && !vType.isPrimitive() && !foldBlackList.contains(vType)) {
			foldT = foldTable();
		}
		// 不是数组的话，添加key
		if (!isArray) {
			table.add((unknown[0] ? strK : content.get(strK, () -> strK)) + ':').fillX().left().padLeft(2).padRight(6);
		}

		Table[] finalTable = {null};
		if (foldT != null) {
			table.add(foldT[0]);
			finalTable[0] = foldT[1];
		} else {
			finalTable[0] = table;
		}

		Prov<?> output = ((Prov<Prov<?>>) () -> {
			label:
			try {
				if (vType == String.class) return null;
				if (vType == null) {
					break label;
				}
				if (vType.isPrimitive()) {
					if (vType.getSimpleName().equalsIgnoreCase("boolean")) {
						boolean[] val = {Boolean.parseBoolean(value[0] + "")};
						StringMap obj = StringMap.of(
								"true", "是",
								"false", "否");
						var btn = new TextButton(obj.get("" + val[0]), styles.cleart);
						btn.clicked(() -> btn.setText(obj.get((value[0] = val[0] = !val[0]) + "")));
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
					var ret = filterClass.get(vType).get(finalTable[0], v, vType, null);
					//					if (ret != null) inspectMap.put(k, inspectClass.get(vType));
					return ret;
				}
			} catch (Exception e) {
				Log.info(type);
				Log.err(e);
			}
			if (filterKeys.containsKey(strK)) {
				var ret = filterKeys.get(strK).get(finalTable[0], v, type);
				//	if (ret != null) inspectMap.put(k, inspectKeys.get(strK));
				return ret;
			}
			// 尝试解析对象
			if (!unknown[0] && value[0] instanceof MyObject && !isArray) {
				return fObject(finalTable[0], field::getType, as(value[0]));
			}
			return null;
		}).get();
		Prov<?> res = output != null ? output : field(table, value[0], vType);
		//		if (isArray) {
		//			Log.info(output);
		//			((MyArray) map).put(res);
		//		} else {
		map.put(k, res);
		//		}

		// 右边
		Table[] finalFoldT = foldT;
		boolean finalUnknown = unknown[0];
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
				var btn = new TextButton("?", styles.clearPartialt);
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
	build(Class<?> type, Fields fields, Table table, Object k, Object v) {
		build(type, fields, table, k, v, fields.map instanceof MyArray);
	}
}

