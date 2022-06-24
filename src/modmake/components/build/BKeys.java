package modmake.components.build;


import arc.Core;
import arc.func.*;
import arc.scene.style.Drawable;
import arc.scene.ui.CheckBox;
import arc.scene.ui.ImageButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.ai.types.FormationAI;
import mindustry.entities.units.AIController;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.LiquidStack;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.world.blocks.power.NuclearReactor;
import mindustry.world.consumers.ConsumeCoolant;
import modmake.IntUI;
import modmake.components.constructor.MyArray;
import modmake.components.constructor.MyObject;
import modmake.ui.styles;
import modmake.util.Classes;
import modmake.util.Tools;

import static mindustry.Vars.ui;
import static modmake.IntVars.modName;
import static modmake.util.BuildContent.*;
import static modmake.util.ContentSeq.otherTypes;
import static rhino.ScriptRuntime.isNaN;
import static rhino.ScriptRuntime.toNumber;

public class BKeys extends ObjectMap<String, Func3<Table, Object, Class<?>, Prov<String>>> {
	Seq<Category> categories = new Seq<>(Category.all);
	Seq<Drawable> categoriesIcon = new Seq<>();
	Seq<String> AISeq = new Seq<>();
	Seq<Class<?>> AIBlackList = Seq.with(FormationAI.class);
	Seq<String> unitType = Seq.with("none", "flying", "mech", "legs", "naval", "payload");

	public BKeys() {
		otherTypes.get(AIController.class).each(ai -> {
			if (!AIBlackList.contains(ai)) {
				AISeq.add(ai.getSimpleName());
			}
		});
		categories.each(cat -> {
			categoriesIcon.add(ui.getIcon(cat.name()));
		});
		setup();
	}

	public void setup() {
		put("category", (table, value, __) -> {
			String[] val = {"" + Tools.or(Category.valueOf(value + ""), Category.distribution)};

			var btn = new ImageButton(Styles.none, new ImageButton.ImageButtonStyle(Styles.clearPartial2i));
			var style = btn.getStyle();
			style.imageUp = ui.getIcon(val[0]);
			btn.clicked(() -> {
				IntUI.showSelectImageTableWithIcons(btn, categories, categoriesIcon, () -> Category.valueOf(val[0]), cat -> style.imageUp = ui.getIcon(val[0] = "" + cat), 42, 32, 2, false);
			});
			table.add(btn).size(45, 45);
			return () -> val[0];
		});
		put("type", (table, value, type) -> {
			if (type == UnitType.class) {
				return tableWithListSelection(table, value + "", unitType, "none", false);
			}
			return null;
		});
		put("consumes", (table, _value, type) -> {
			var value = _value instanceof MyObject ? (MyObject) _value : new MyObject<>();
			var cont = table.table(Tex.button).get();

			Consumer.object = value;
			Consumer.cont = cont;
			var all = Consumer.all;
			new Consumer<>("power", "power", "" + value.get("power"), (t, v) -> {
				var field = new TextField(myIsNaN(v) ? "0" : Float.parseFloat(v) + "");
				t.add(field).row();
				t.image().fillX().color(Pal.accent);
				return () -> myIsNaN(field.getText()) ? 0f : Float.parseFloat(field.getText());
			}, box -> all.get("powerBuffered") != null && all.get("powerBuffered").enable);

			new Consumer<>("powerBuffered", "powerBuffered", "" + value.get("powerBuffered"), (t, v) -> {
				var field = new TextField(myIsNaN(v) ? "0" : "" + Float.parseFloat(v));
				t.add(field).row();
				t.image().fillX().color(Pal.accent);
				return () -> myIsNaN(field.getText()) ? 0 : Float.parseFloat(field.getText());
			}, box -> all.get("power") != null && all.get("power").enable);

			var itemObj = ((Prov<MyObject>) () -> {
				Object item = value.get("item");
				if (item == null) return (MyObject) value.get("items");
				var arr = new MyArray<>(MyObject.of("item", item, "amount", 1));
				var obj = MyObject.of("items", arr);
				return obj;
			}).get();
			new Consumer<>("item", "items", itemObj, (t, _obj) -> {
				var obj = _obj != null ? _obj : new MyObject<>();
				obj.put("items", fArray(t, Classes.get("ItemStack"), (MyArray) obj.get("items", new MyArray<>())));
				t.row();
				t.table(t1 -> {
					t1.check(Core.bundle.get(modName + ".consumes-optional", "optional"), (boolean) obj.get("optional", false), b -> obj.put("optional", b));
					t1.check(Core.bundle.get(modName + ".consumes-booster", "booster"), (boolean) obj.get("booster", false), b -> obj.put("booster", b));
				}).fillX().row();
				t.image().fillX().color(Pal.accent);

				return () -> obj;
			}, box -> type == NuclearReactor.class);
			if (type == NuclearReactor.class) all.get("item").setup(true);

			new Consumer<>("liquid", "liquid", value.get("liquid"), (t, _obj) -> {
				var obj = _obj instanceof MyObject ? (MyObject) _obj : new MyObject<>();
				var tab = new Table();
				t.add(tab).fillX();
				var p = filterClass.get(LiquidStack.class).get(tab, obj, null, null);
				MyObject v = Tools.as(p.get());
				t.row();
				MyObject finalObj = obj;
				t.table(t1 -> {
					t1.check(Core.bundle.get(modName + ".consumes-booster", "booster"), (boolean) finalObj.get("booster", false), b -> v.put("booster", b));
					t1.check(Core.bundle.get(modName + ".consumes-optional", "optional"), (boolean) finalObj.get("optional", false), b -> v.put("optional", b));
				}).fillX().row();
				return () -> v;
			}, box -> all.get("coolant") != null && all.get("coolant").enable || type == NuclearReactor.class);
			if (type == NuclearReactor.class) all.get("liquid").setup(true);

			new Consumer<>("coolant", "coolant", value.get("coolant"), (t, _obj) -> {
				var obj = _obj instanceof MyObject ? (MyObject) _obj : new MyObject<>();
				var opt = obj.get("optional", false);
				var boost = obj.get("booster", false);
				var _prov = fObject(t, () -> ConsumeCoolant.class, obj, new Seq<>(), true);
				t.row();
				var v = new MyObject<>();
				v.put("update", false);
				t.table(t1 -> {
					t1.check(Core.bundle.get(modName + ".consumes-optional", "optional"), (boolean) opt, b -> v.put("optional", b));
					t1.check(Core.bundle.get(modName + ".consumes-booster", "booster"), (boolean) boost, b -> v.put("booster", b));
				}).fillX().row();
				return () -> {
					var out = _prov.get();
					v.each(out::put);
					return out;
				};
			}, box -> all.get("liquid") != null && all.get("liquid").enable || type == NuclearReactor.class);
			if (type == NuclearReactor.class) all.get("coolant").setup(false);

			return () -> {
				for (var v : all.values()) {
					v.check();
				}
				return value + "";
			};
		});
		put("upgrades", (table, _value, __) -> {
			if (!(_value instanceof MyArray)) return null;
			var value = (MyArray) _value;
			table = table.table().fillX().get();
			var cont = new Table();
			//.name("upgrades-cont");
			table.add(cont).row();
			Cons<Object> build = _item -> {
				if (!(_item instanceof MyArray)) {
					ui.showException("upgrades解析错误", new IllegalArgumentException("item isn't a MyArray"));
					return;
				}
				var item = (MyArray) _item;
				var list = item.toArray();
				item.clear();
				var __table = cont.table().get();
				cont.row();
				var unitType1 = filterClass.get(UnitType.class).get(__table, Tools.or(list.get(0), () -> defaultClass.get(UnitType.class)), null, null);
				item.put(0, unitType1);
				__table.add("-->");
				var unitType2 = filterClass.get(UnitType.class).get(__table, Tools.or(list.get(1), () -> defaultClass.get(UnitType.class)), null, null);
				item.put(1, unitType2);
				__table.button("", Icon.trash, styles.cleart, () -> {
					value.removeValue(item);
					__table.remove();
				}).marginLeft(4);
			};
			value.each(build);
			table.button("$add", () -> {
				var array = new MyArray<>();
				value.put(array);
				build.get(array);
			}).growX().minWidth(100);

			return () -> "" + value;
		});
		put("controller", (table, value, type) -> {
			if (type == UnitType.class) {
				return tableWithListSelection(table, value + "", AISeq, "FlyingAI", false);
			}
			return null;
		});
	}

	static class Consumer<V, T> {
		public final static ObjectMap<String, Consumer> all = new ObjectMap<>();
		public static Table cont = null;
		public Table table;
		public static MyObject object = null;
		public boolean enable;
		public final String name;
		public final Cell<?> cell;

		public Consumer(String name, String key, V obj, Func2<Table, V, Prov<T>> _func, Boolf<CheckBox> disabledProv) {
			this.enable = obj != null;
			this.name = name;
			table = new Table();
			table.defaults().growX().left();

			Cell<CheckBox> c = cont.check(Core.bundle.get("consumes." + name, name), enable, this::setup);
			if (disabledProv != null) c.disabled(disabledProv);
			c.row();
			var cell = cont.add().growX().left().padLeft(10);
			cont.row();
			this.cell = cell;

			object.put(key, _func.get(table, obj));
			setup(enable);
			all.put(name, this);
		}

		public Consumer(String name, String key, V obj, Func2<Table, V, Prov<T>> _func) {
			this(name, key, obj, _func, null);
		}

		public void setup(boolean enable) {
			this.enable = enable;
			if (enable) {
				cell.setElement(table);
			} else {
				cell.clearElement();
			}
		}

		public void check() {
			if (!enable) object.remove(name);
		}

	}

	public boolean myIsNaN(String str) {
		try {
			double d = toNumber(str);
			return isNaN(d);
		} catch (Exception ignored) {}
		return false;
	}
}
