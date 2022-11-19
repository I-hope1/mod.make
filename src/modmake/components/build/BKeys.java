package modmake.components.build;


import arc.func.*;
import arc.scene.style.Drawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.Log;
import mindustry.content.TechTree;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.units.UnitController;
import mindustry.gen.Icon;
import mindustry.type.*;
import mindustry.ui.Styles;
import mindustry.world.consumers.*;
import modmake.IntUI;
import modmake.components.constructor.*;
import modmake.ui.MyStyles;

import java.lang.reflect.Field;
import java.util.Objects;

import static mindustry.Vars.ui;
import static modmake.util.BuildContent.*;
import static modmake.util.Tools.*;
import static modmake.util.load.ContentSeq.otherTypes;

public class BKeys extends ObjectMap<String, Func3<Table, Object, Class<?>, Prov<String>>> {
	Seq<Category> categories = new Seq<>(Category.all);
	Seq<Drawable> categoriesIcon = new Seq<>();
	Seq<String> AISeq = new Seq<>();
	Seq<Class<?>> AIBlackList = Seq.with(UnitController.class);
	Seq<String> unitType = Seq.with("none", "flying", "mech", "legs", "naval", "payload");

	public BKeys() {
		otherTypes.get(UnitController.class).each(ai -> {
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
			String[] val = {"" + or(categories.find(c -> c.name().equals(value + "")),
					Category.distribution)};

			var btn = new ImageButton(Styles.none, new ImageButton.ImageButtonStyle(Styles.cleari));
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
		put("consumes", (table, value, type) -> {
			if (!(value instanceof MyObject))
				throw new IllegalArgumentException("value(" + value + ") must be MyObject.");
			var prov = fObject(table, () -> ConsumesClass.class, as(value), Seq.with(ConsumesClass.class));
			return () -> {
				return prov.get().toString();
			};
			/*var value = _value instanceof MyObject ? (MyObject) _value : new MyObject<>();
			var cont = table.table(Tex.button).get();

			Consumer.object = value;
			Consumer.cont = cont;
			var all = Consumer.all;
			new Consumer<>("power", "power", value.get("power"), (t, v) -> {
				var field = new TextField(myIsNaN(v) ? "0" : toNumber(v) + "");
				t.add(field).row();
				t.image().fillX().color(Pal.accent);
				return () -> myIsNaN(field.getText()) ? 0f : toNumber(field.getText());
			}, box -> all.get("powerBuffered") != null && all.get("powerBuffered").enable);

			new Consumer<>("powerBuffered", "powerBuffered", value.get("powerBuffered"), (t, v) -> {
				var field = new TextField(myIsNaN(v) ? "0" : "" + toNumber(v));
				t.add(field).row();
				t.image().fillX().color(Pal.accent);
				return () -> myIsNaN(field.getText()) ? 0 : toNumber(field.getText());
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
			};*/
		});
		put("upgrades", (table, _value, __) -> {
			if (!(_value instanceof MyArray)) return null;
			MyArray<Object> value = as(_value);
			assert value != null;
			table = table.table().fillX().get();
			var cont = new Table();
			//.name("upgrades-cont");
			table.add(cont).row();
			Cons<Object> build = _item -> {
				if (!(_item instanceof MyArray)) {
					ui.showException("upgrades解析错误", new IllegalArgumentException("item isn't a MyArray"));
					return;
				}
				MyArray<Object> item = as(_item);
				assert item != null;
				var list = item.toArray();
				item.clear();
				var __table = cont.table().get();
				cont.row();
				var unitType1 = filterClass.get(UnitType.class).get(__table, or(list.get(0), () -> defaultClass.get(UnitType.class).get()), null, null);
				item.put(0, unitType1);
				__table.add("-->");
				var unitType2 = filterClass.get(UnitType.class).get(__table, or(list.get(1), () -> defaultClass.get(UnitType.class).get()), null, null);
				item.put(1, unitType2);
				__table.button("", Icon.trash, MyStyles.cleart, () -> {
					value.removeValue(item);
					__table.remove();
				}).marginLeft(4);
			};
			value.each(build);
			table.button("@add", () -> {
				var array = new MyArray<>();
				value.put(array);
				build.get(array);
			}).growX().minWidth(100);

			return () -> "" + value;
		});
		put("controller", (table, value, type) -> {
			if (UnitType.class.isAssignableFrom(type)) {
				Log.debug("ok");
				return tableWithListSelection(table, value + "", AISeq, "FlyingAI", false);
			}
			return null;
		});
		put("research", (table, o, __) -> {
			MyObject<Object, Object> researchObj = o instanceof MyObject ? as(o) : o instanceof String ? MyObject.of("parent", "" + o) : null;
			Object value = researchObj != null ? researchObj.get("parent") : null;

			var techs = TechTree.all;
			Seq<UnlockableContent> all = new Seq<>();
			techs.each(node -> all.add(node.content));

			final UnlockableContent[] content = {all.find(f -> f.name.equals(value))};
			var btn = new TextButton(!Objects.equals(value, "") ?
					"" + or(nullCheck(content[0], c -> c.localizedName), value) :
					"@none", Styles.flatt);
			btn.clicked(() -> IntUI.allContentSelection(btn, all, () -> content[0], c -> {
				content[0] = c;
				btn.setText(c.localizedName);
			}, 42, 32, true));
			table.add(btn).size(150, 60);
			return () -> content[0].name;
		});
	}

	/*static class Consumer<V, T> {
		public final static ObjectMap<String, Consumer> all = new ObjectMap<>();
		public static Table cont = null;
		public Table table;
		public static MyObject object = null;
		public boolean enable;
		public final String name;
		public final Cell<?> cell;
		public final CheckBox box;

		public Consumer(String name, String key, V obj, Func2<Table, V, Prov<T>> _func, Boolf<CheckBox> disabledProv) {
			this.enable = obj != null;
			this.name = name;
			table = new Table();
			table.defaults().growX().left();

			Cell<CheckBox> c = cont.check(Core.bundle.get("consumes." + name, name), enable, this::setup);
			box = c.get();
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
			if (box.isDisabled()) object.remove(name);
		}

	}*/

	// from ContentParser
	static class ConsumesClass {
		public Item item;
		public ConsumeItemCharged itemCharged;
		public ConsumeItemFlammable itemFlammable;
		public ConsumeItemRadioactive itemRadioactive;
		public ConsumeItemExplosive itemExplosive;
		public ConsumeItemExplode itemExplode;
		public ConsumeItems items;
		public ConsumeLiquidFlammable liquidFlammable;
		public ConsumeLiquid liquid;
		public ConsumeLiquids liquids;
		public ConsumeCoolant coolant;
		public ConsumePower power;
		public float powerBuffered;
	}

	static Field[] consumeFields = ConsumesClass.class.getFields();

	/*public boolean myIsNaN(Object obj) {
		return myIsNaN("" + obj);
	}

	public boolean myIsNaN(String str) {
		try {
			double d = toNumber(str);
			return isNaN(d);
		} catch (Exception ignored) {}
		return false;
	}*/
}