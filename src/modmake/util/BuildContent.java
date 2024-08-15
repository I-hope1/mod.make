package modmake.util;

import arc.Core;
import arc.func.*;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.ui.Styles;
import mindustry.world.meta.Attribute;
import modmake.IntUI;
import modmake.IntUI.*;
import modmake.components.*;
import modmake.components.build.*;
import modmake.components.build.inspect.*;
import modmake.components.constructor.*;
import modmake.ui.MyStyles;
import modmake.ui.dialog.Editor;
import modmake.util.tools.FilterTable;

import java.lang.reflect.*;
import java.util.Objects;
import java.util.function.BiFunction;

import static mindustry.Vars.ui;
import static modmake.components.AddFieldBtn.*;
import static modmake.components.DataHandle.types;
import static modmake.components.DataHandle.*;
import static modmake.util.Tools.*;
import static modmake.util.load.ContentVars.*;

public class BuildContent {
	public static Json json = DataHandle.json;

	/** 默认类实例 */
	public static ObjectMap<Class<?>, Prov<?>> defaultClassIns  = new ObjectMap<>();
	/** 默认类名 */
	static        ObjectMap<Seq, String>       defaultTypeNames = new ObjectMap<>();

	public static final String selfDefined = Core.bundle.get("modmake.selfdefined");


	public static void registerDefTypeName(Seq seq, String s) {
		defaultTypeNames.put(seq, s);
	}


	static {
		defaultClassIns.put(Effect.class, () -> "none");
		defaultClassIns.put(UnitType.class, () -> "mono");
		defaultClassIns.put(Item.class, () -> "copper");
		defaultClassIns.put(Liquid.class, () -> "water");
		defaultClassIns.put(ItemStack.class, () -> "copper/0");
		defaultClassIns.put(LiquidStack.class, () -> "water/0");
		defaultClassIns.put(Attribute.class, () -> Attribute.all[0]);
		defaultClassIns.put(BulletType.class, MyObject::new);
		defaultClassIns.put(Sector.class, () -> 0);
		defaultClassIns.put(Planet.class, () -> "serpulo");
	}

	/** @see mindustry.mod.ContentParser  */
	public static ObjectMap<String, Prov<?>> defaultKey = ObjectMap.of(
	 "consumes", (Prov<?>) MyObject::new,
	 "controller", (Prov<?>) MyObject::new,
	 "research", (Prov<?>) () -> "none"
	);

	// 折叠黑名单
	static Seq<Class<?>> foldBlackList = Seq.with(
	 String.class, Color.class, Category.class,
	 ItemStack.class, LiquidStack.class, UnitType.class, Item.class, Liquid.class,
	 Sector.class, Planet.class
	);

	// 单位额外字段
	static Seq<String> UnitTypeExFields = Seq.with(
	 "requirements", "waves", "controller", "type");

	public static BKeys    filterKeys;
	public static BClasses filterClass;

	public static IKeys                       inspectKeys;
	public static IClasses                    inspectClass;
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
		var    fs  = clazz.getFields();
		for (var field : fs) {
			seq.add(_func.get(field));
		}
		return seq;
	}

	public static Table[] foldTable() {
		var table   = new Table();
		var content = new Table();
		var col     = new Collapser(content, false);
		col.setDuration(0.1f);

		var btn   = new ImageButton(Icon.rightOpen, Styles.clearTogglei);
		var style = btn.getStyle();
		btn.clicked(() -> {
			col.toggle();
			style.imageUp = col.isCollapsed() ? Icon.rightOpen : Icon.downOpen;
		});
		style.up = style.over = MyStyles.whiteui.tint(0.6f, 0.8f, 0.8f, 1f);
		table.add(btn).padTop(1).padBottom(1).padRight(4).size(32);
		table.add(col).growX().left();
		if (dsettings.getBool("auto_fold_code")) {
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

		t.add("@amount");
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
			t.button("", Icon.copy, MyStyles.cleart, () -> {
				Core.app.setClipboardText("" + value);
				ui.showInfoFade("已复制");
			}).padRight(2);
			// 粘贴
			t.button("", Icon.paste, MyStyles.cleart, () -> ui.showConfirm(
			 "@paste", "@confirm.paste", () -> {
				 String txt = Core.app.getClipboardText();
				 try {
					 paste.get(parse(txt));
					 ui.showInfoFade("Ok");
				 } catch (Exception e) {
					 catchF.run();
					 ui.showException(Core.bundle.get("unable.paste"), e);
				 }
			 }));
		}).padRight(6);
	}

	public static <T> Prov<String>
	tableWithListSelection(
	 Table table, String value, Seq<T> seq, String defaultValue,
	 boolean searchable) {
		return tableWithListSelection(table, value, seq, defaultValue, String::valueOf, searchable);
	}
	public static <T> Prov<String>
	tableWithListSelection(
	 Table table, String value, Seq<T> seq, String defaultValue,
	 Func<T, String> func
	 , boolean searchable) {
		String[] val = {value != null ? value : defaultValue};
		var      btn = new TextButton(types.get(val[0], () -> val[0]), MyStyles.cleart);
		btn.clicked(() -> IntUI.showSelectListTable(btn, seq, () -> val[0],
		 type -> btn.setText(types.get(val[0] = func.get(type), () -> val[0])
		 ), 150, 55, searchable));
		table.add(btn).minWidth(100).height(45).get();
		return () -> val[0];
	}

	public static <T extends UnlockableContent> Prov<String>
	tableWithFieldImage(Table table, String value, Class<?> vType, Seq<T> seq) {
		value = value != null ? value : defaultClassIns.get(vType).get() + "";
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
		tmpList.add(selfDefined);
		value = Tools.or(value, () -> defaultClassIns.get(vType).get());
		boolean                  isObject = value instanceof MyObject;
		MyObject<Object, Object> val1     = isObject ? as(value) : new MyObject<>();
		assert val1 != null;
		var    table1   = new Table();
		String typeName = "" + Tools.or(val1.remove("type"), defaultType);
		var selection = new TypeSelection(or(Classes.get(typeName), () -> {
			Time.runTask(0, () -> ui.showException(new ClassNotFoundException("无法找到类: " + typeName)));
			return Object.class;
		}), typeName, Tools.or(otherTypes.get(vType), new Seq<>(vType)));
		table1.add(selection.table).padBottom(4).row();
		var cont = table1.table().name("cont").get();
		var map  = fObject(cont, selection::type, val1, Tools.or(blackList, new Seq<>()));

		Object[]      retV       = {isObject ? selfDefined : value};
		Object[]      val2       = {retV[0]};
		Cell<?>[]     cell       = {null};
		ImageButton[] btn        = {null};
		Seq<T>        finalItems = items;
		Func<Object, Drawable> getImg = o -> {
			if (o instanceof UnlockableContent) return new TextureRegionDrawable(((UnlockableContent) o).uiIcon);
			return Icon.add;
		};
		btn[0] = table.button(Tex.whiteui, Styles.logici, () -> {
			IntUI.showSelectImageTableWithFunc(btn[0], tmpList, () -> val2[0], obj -> {
				btn[0].getStyle().imageUp = getImg.get(obj);
				if (selfDefined.equals(obj)) {
					cell[0].setElement(table1);
					retV[0] = map;
					val2[0] = selfDefined;
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
	listWithType(Table table, Object value, Class<?> vType,
							 String defaultType, Seq<T> list, Func<T, String> _func,
							 Seq<Class<?>> blackList) {
		return buildMulti(table, value instanceof MyObject ? 0 : 1, new BuildElement((t, self) -> {
			MyObject val;
			if (!(value instanceof MyObject)) val = new MyObject<>();
			else val = (MyObject) value;
			String typeName = "" + Tools.or(val.remove("type"), defaultType);
			var selection = new TypeSelection(or(Classes.get(typeName), () -> {
				Time.runTask(0, () -> ui.showException(new ClassNotFoundException("无法找到类: " + typeName)));
				return Object.class;
			}), typeName, Tools.or(otherTypes.get(vType), new Seq<>(vType)));
			t.add(selection.table).padBottom(4).row();
			var cont = t.table().name("cont").get();
			return fObject(cont, selection::type, val, Tools.or(blackList, new Seq<>()));
		}), new BuildElement((t, self) -> {
			return tableWithListSelection(t, String.valueOf(value), list, null, _func, true);
		}));
		/*list = list.copy();
		Seq<String> tmpList = new Seq<>();
		tmpList.addAll(list.as());
		tmpList.add(selfDefined);
		value = Tools.or(value, () -> defaultClassIns.get(vType).get());
		boolean                  isObject = value instanceof MyObject;
		MyObject<Object, Object> val1     = isObject ? as(value) : new MyObject<Object, Object>();
		assert val1 != null;
		var    table1   = new Table();
		String typeName = "" + Tools.or(val1.remove("type"), defaultType);
		var selection = new TypeSelection(or(Classes.get(typeName), () -> {
			Time.runTask(0, () -> ui.showException(new ClassNotFoundException("无法找到类: " + typeName)));
			return Object.class;
		}), typeName, Tools.or(otherTypes.get(vType), new Seq<>(vType)));
		table1.add(selection.table).padBottom(4).row();
		var cont = table1.table().name("cont").get();
		var map  = fObject(cont, selection::type, val1, Tools.or(blackList, new Seq<>()));

		Object[]     retV      = {isObject ? selfDefined : value};
		String[]     val2      = {"" + retV[0]};
		Cell<?>[]    cell      = {null};
		TextButton[] btn       = {null};
		Seq<T>       finalList = list;
		btn[0] = table.button(types.get(val2[0], () -> val2[0]), MyStyles.cleart, () -> {
			IntUI.showSelectListTable(btn[0], tmpList, () -> val2[0], fx -> {
				btn[0].setText(types.get(fx, () -> fx));
				if (selfDefined.equals(fx)) {
					cell[0].setElement(table1);
					retV[0] = map;
					val2[0] = selfDefined;
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

		return () -> (retV[0] instanceof Prov ? ((Prov<?>) retV[0]).get() : retV[0]) + "";*/
	}

	public static <T> Prov<String>
	listWithType(Table table, Object value, Class<?> vType,
							 String defaultType, Seq<T> list, Func<T, String> _func) {
		return listWithType(table, value, vType, defaultType, list, _func, null);
	}
	public static <T> Prov<String>
	listWithType(Table table, Object value, Class<?> vType, Seq<T> list, Func<T, String> _func) {
		return listWithType(table, value, vType, defaultTypeNames.get(list), list, _func, null);
	}

	public static Prov<MyObject<Object, Object>> tableWithTypeSelection(Table table, MyObject<Object, Object> value,
																																			Class<?> vType, String
																																			 defaultType) {
		table = table.table().get();
		var    obj      = Tools.or(value, MyObject::new);
		String typeName = "" + Tools.or(value.remove("type"), defaultType);
		var selection = new TypeSelection(or(Classes.get(typeName), () -> {
			Time.runTask(1, () -> ui.showException(new ClassNotFoundException("cannot find class typeName: " + typeName)));
			return Object.class;
		}), typeName, otherTypes.get(vType));
		Fields[] fields = {null};

		selection.switchType = cl -> {
			if (obj.size == 0) {
				var v = defaultObjValue(cl);
				if (v == null) return;
				// Log.info(fields[0].type);
				// 等待更新
				Time.runTask(1f, () -> v.each((k, _v) -> {
					fields[0].add(null, k, _v);
				}));
			}
		};

		table.add(selection.table).padBottom(4).row();
		Table cont = table.table().name("cont").get();
		var   fObj = fObjectInternal(cont, selection::type, as(obj), null, false);
		fields[0] = fObj.fields;
		return fObj.prov;
	}

	public static class BuildElement {
		private final BiFunction<Table, BuildElement, Prov<?>> func;

		private Prov<?> prov;
		public BuildElement(BiFunction<Table, BuildElement, Prov<?>> func) {
			this.func = func;
		}
		public void build(Table t) {
			prov = func.apply(t, this);
		}
		public Prov<?> getProv() {
			return prov;
		}
	}
	public static <T> Prov<T> buildMulti(Table t, BuildElement... elements) {
		return buildMulti(t, 0, elements);
	}
	public static <T> Prov<T> buildMulti(Table t, int i0, BuildElement... elements) {
		int[] index = {i0};
		t.defaults().growY();
		t.button(Icon.leftOpen, Styles.clearNonei, () -> --index[0])
		 .disabled(__ -> index[0] == 0);

		FilterTable<Integer> cont = new FilterTable<>();
		t.add(cont).grow();
		for (int i = 0, elementsLength = elements.length; i < elementsLength; i++) {
			BuildElement element = elements[i];
			cont.bind(i);
			cont.table(element::build);
			cont.unbind();
		}
		cont.addUpdateListener(s -> s == index[0]);

		t.button(Icon.rightOpen, Styles.clearNonei, () -> ++index[0])
		 .disabled(__ -> index[0] == elements.length - 1);
		return () -> (T) elements[index[0]].getProv().get();
	}

	// obj
	public static Prov<MyObject<Object, Object>>
	fObject(Table table, Prov<Class<?>> type, MyObject<Object, Object> value) {
		return fObject(table, type, value, null, false);
	}
	public static Prov<MyObject<Object, Object>>
	fObject(Table table, Prov<Class<?>> type, MyObject<Object, Object> value, Seq<Class<?>> typeBlackList) {
		return fObject(table, type, value, typeBlackList, false);
	}
	public static class FObject {
		public Prov<MyObject<Object, Object>> prov;
		public Fields                         fields;
		public FObject(Prov<MyObject<Object, Object>> prov, Fields fields) {
			this.prov = prov;
			this.fields = fields;
		}
	}
	public static Prov<MyObject<Object, Object>>
	fObject(Table parent, Prov<Class<?>> type, MyObject<Object, Object> value,
					Seq<Class<?>> typeBlackList,
					boolean all) {
		return fObjectInternal(parent, type, value, typeBlackList, all).prov;
	}
	public static FObject
	fObjectInternal(Table parent, Prov<Class<?>> type, MyObject<Object, Object> value,
									Seq<Class<?>> typeBlackList,
									boolean all) {
		Table  table  = new Table(Tex.pane), children = new Table();
		table.left().defaults().left();

		Fields fields = new Fields(value, type, children);
		children.center().defaults().center().minWidth(100);
		Cell<?>         cell   = parent.add(table).left();
		final boolean[] resize = {false};

		var      pane   = new ScrollPane(null);
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
				if (!(v instanceof Method)) {
					fields.add(null, k);
				}
			});
			table.add(new AddFieldBtn(value, fields, type)).fillX().growX().minWidth(100);
		}
		return new FObject(() -> {
			if (typeBlackList == null || !typeBlackList.contains(type.get()))
				nullCheck(type.get(), t -> value.put("type", t.getSimpleName()));
			return value;
		}, fields);
	}


	// array
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
			// table1.bind(i);
			build(vType, null, fields, table1, i, value, true);
			// table1.unbind();
			// table1.addUpdateListener(() -> IntUI.editor.pattern);
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
		// Class<?> finalVType = vType;
		return () -> {
			var txt = !field.getText().replace("\\s*", "").isBlank() ? field.getText() : "";
			txt = trope(txt);
			return isNum(txt) ? txt : '"' + txt + '"';
		};
	}

	/**
	 * 构建table
	 *
	 * @param isArray fields是否为数组
	 */
	public static void
	build(Class<?> type, Field field, Fields fields, Table table,
				Object k, Object v, boolean isArray) {
		if (type == null || table == null) return;
		final boolean[] unknown = {false};
		Runnable        removeR = () -> fields.remove(k);

		String      strK  = "" + k;
		Object[]    value = {v};
		AddFieldBtn addF  = null;
		if (!isArray) {
			addF = new AddFieldBtn(fields.map, fields, () -> fields.type, name -> {
				fields.map.remove(k);
				fields.map.put(name, v);
				fields.setTable(k, Fields.json(fields, fields.nextId(), name));
			});
			addF.bind = addF;
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

		var      map   = fields.map;
		Class<?> vType = isArray ? type : field == null ? null : field.getType();

		// 折叠代码
		Table[] foldT = null;
		if (vType != null && !vType.isPrimitive() && !foldBlackList.contains(vType)) {
			foldT = foldTable();
		}

		// 不是数组的话，添加key
		if (!isArray) {
			// Time.runTask(0, () -> Log.info(table.parent));
			IntUI.addShowMenuListener(
			 table.add(new Label(() -> (unknown[0] ? strK : dcontent.get(strK, () -> strK)) + ':'))
				.fillX().left().padLeft(2).padRight(6)
				.update(label -> {
					unknown[0] = (type != UnitType.class || !UnitTypeExFields.contains(strK))
											 && dsettings.getBool("point_out_unknown_field") && !caches.get(type).containsKey(strK);
					label.setColor(unknown[0] ? Pal.remove : Color.white);
				})
				.get(), new ConfirmList(Icon.trash, "delete", "are you sure?", removeR),
			 new MenuList(null, "rename", addF.runnable),
			 new MenuList(Icon.up, "up", () -> {
				 var el    = table.parent;
				 var cells = ((Table) el.parent).getCells();
				 for (int i = 0; i < cells.size; i++) {
					 if (cells.get(i).get() == el) {
						 // Log.info(i);
						 if (i == 0) break;
						 cells.swap(i, i - 1);
						 el.parent.layout();
						 break;
					 }
				 }
				 // .getCell().setZIndex(Math.max(table.parent.getZIndex() - 1, 0))
			 }),
			 new MenuList(Icon.down, "down", () -> {
				 // table.parent.setZIndex(table.parent.getZIndex() + 1)
				 var el    = table.parent;
				 var cells = ((Table) el.parent).getCells();
				 for (int i = 0; i < cells.size; i++) {
					 if (cells.get(i).get() == el) {
						 if (i == cells.size - 1) break;
						 cells.swap(i, i + 1);
						 el.parent.layout();
						 break;
					 }
				 }
			 })
			);
		}

		Table[] finalTable = {null};
		if (foldT != null) {
			table.add(foldT[0]);
			finalTable[0] = foldT[1];
		} else {
			finalTable[0] = table;
		}

		Prov<?> output = resolve(type, field, k, v, isArray, strK, value, vType, finalTable);

		Prov<?> res = output != null ? output : field(table, value[0], vType);
		//		if (isArray) {
		//			Log.info(output);
		//			((MyArray) map).put(res);
		//		} else {
		map.put(k, res);
		//		}

		// 右边
		Table[] finalFoldT   = foldT;
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
			if (!isArray && !finalUnknown && dcontent.containsKey(k + ".help")) {
				var btn = new TextButton("?", MyStyles.clearPartialt);
				right.add(btn).size(8 * 5).padLeft(5).padRight(5).right().grow().get();
				btn.clicked(() -> IntUI.showSelectTable(btn, (p, __, ___) -> {
					p.pane(help -> help.add(dcontent.get(k + ".help"), 1.3f)).pad(4, 8, 4, 8);
				}, false));
			}
			// 删除按钮
			right.button("", Icon.trash, MyStyles.cleart, removeR);
		}).padLeft(4).growX().right();
		table.row();
	}
	private static Prov<?> resolve(Class<?> type, Field field,
																 Object k, Object v, boolean isArray, String strK, Object[] value,
																 Class<?> vType, Table[] finalTable) {
		label:
		try {
			if (vType == String.class) return null;
			if (vType == null) {
				break label;
			}
			if (vType.isPrimitive()) {
				if (vType == boolean.class) {
					boolean[] val = {Boolean.parseBoolean(value[0] + "")};
					StringMap obj = StringMap.of(
					 "true", types.get("true"),
					 "false", types.get("false"));
					var btn = new TextButton(obj.get("" + val[0]), MyStyles.cleart);
					btn.clicked(() -> btn.setText(obj.get((value[0] = val[0] = !val[0]) + "")));
					finalTable[0].add(btn).minWidth(100).height(45).get();
					return () -> val[0];
				}
				return null;
			}

			// Log.info(vType.isArray());
			if ((vType.isArray() || arrayClass.contains(vType))
					&& v instanceof MyArray && !Objects.equals(k, "upgrades")) {
				return fArray(finalTable[0],
				 getvType(field, vType), (MyArray) v);
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
			// Log.info(type);
			// Log.err(e);
		}
		// 尝试根据keys解析
		if (filterKeys.containsKey(strK)) {
			var ret = filterKeys.get(strK).get(finalTable[0], v, type);
			//	if (ret != null) inspectMap.put(k, inspectKeys.get(strK));
			return ret;
		}
		// 尝试解析对象
		if (field == null || !Object.class.isAssignableFrom(field.getType())) return null;
		if (value[0] instanceof MyArray) {
			return fArray(finalTable[0], field.getType(), as(value[0]));
		} else if (value[0] instanceof MyObject) {
			return fObject(finalTable[0], field::getType, as(value[0]));
		}
		return null;
	}
	private static Class<?> getvType(Field field, Class<?> vType) {
		return arrayClass.contains(vType) ? getGenericType(field).get(0)
		 : vType.getComponentType();
	}

	public static Field getField(Class<?> cls, Object key) {
		try {
			return cls.getField((String) key);
		} catch (Exception e) {
			return null;
		}
	}

	public static void
	build(Class<?> type, Field field, Fields fields, Table table,
				Object k, Object v) {
		build(type, field, fields, table, k, v, fields.map instanceof MyArray);
	}

	/** 必须修改 */
	public static Editor getEditor(Element element) {
		while (!(element instanceof Editor)) {
			element = element.parent;
			if (element == null) throw new RuntimeException();
		}
		return (Editor) element;
	}
	;

	public static class MultiClasses {
		public Seq<Class<?>> classSeq;
		public MultiClasses(Seq<Class<?>> classSeq) {
			this.classSeq = classSeq;
		}
	}
}

