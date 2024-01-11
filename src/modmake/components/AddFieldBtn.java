package modmake.components;

import arc.Core;
import arc.func.*;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.serialization.Json;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.effect.MultiEffect;
import mindustry.gen.Icon;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.consumers.Consume;
import mindustry.world.draw.DrawMulti;
import modmake.IntUI;
import modmake.components.constructor.*;
import modmake.ui.MyStyles;
import modmake.util.*;
import modmake.util.load.ContentVars;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static modmake.components.DataHandle.*;
import static modmake.util.Tools.as;
import static modmake.util.tools.Tools.*;

public class AddFieldBtn extends TextButton {
	public static Seq<Class<?>> arrayClass = Seq.with(
	 Seq.class, ObjectSet.class);

	public static ObjectMap<Class<?>, ObjectMap> caches = new ObjectMap<>() {
		public ObjectMap get(Class<?> key) {
			if (key == null) return null;
			var val = super.get(key);
			l:
			if (val == null) {
				if (key.getSuperclass() == null) {
					put(key, val = new ObjectMap());
					break l;
				}
				var m = new OrderedMap(json.getFields(key));
				if (UnitType.class.isAssignableFrom(key)) {
					// Label仅是一种标识
					m.put("type", Label.class);
					m.put("controller", Label.class);
				}
				if (UnlockableContent.class.isAssignableFrom(key)) {
					m.put("research", Label.class);
				}
				if (Block.class.isAssignableFrom(key)) {
					m.put("consumes", Label.class);
				}
				put(key, val = m);
			}
			return val;
		}
	};

	public Runnable runnable;

	public AddFieldBtn(MyObject obj, Fields Fields, Prov<Class<?>> classProv) {
		this(obj, Fields, classProv, null);
	}

	public AddFieldBtn(MyObject obj, Fields Fields, Prov<Class<?>> classProv, Cons<String> listener) {
		super("@add");

		add(new Image(Icon.add));
		getCells().reverse();
		clicked(runnable = () -> {
			Class<?>  cont   = classProv.get();
			ObjectMap fields = as(caches.get(cont));

			var                      table   = new Table();
			AtomicReference<Pattern> pattern = new AtomicReference<>();
			Runnable[]               hide    = {null};
			Runnable eachFields = () -> {
				table.clearChildren();

				assert fields != null;
				fields.each((key, meta) -> {
					Field field = null;
					if (meta != Label.class) {
						field = ((Json.FieldMetadata) meta).field;
						if (!filter(field, cont)) return;
					}
					String name        = key + "";
					String displayName = dcontent.get(name, () -> name);
					if (!(testP(pattern.get(), name) ||
								testP(pattern.get(), displayName)))
						return;

					Field finalField = field;
					table.table(t -> {
						t.button(displayName, Styles.cleart, () -> {
							if (listener != null) {
								listener.get(name);
								return;
							}
							Fields.add(null, finalField, name,
							 finalField == null ? defaultValue(name) : defaultValue(finalField.getType()));
							hide[0].run();
						}).size(Core.graphics.getWidth() * .2f, 45).disabled(b -> obj.has(name));
						var help = dcontent.get(name + ".help");
						if (help != null) {
							Button[] btn = {null};
							btn[0] = t.button("?", MyStyles.clearPartialt, () -> IntUI.showSelectTable(btn[0], (p, __, ___) -> {
								p.pane(p2 -> p2.add(help, 1.3f)).pad(4, 8, 4, 8).row();
							}, false)).size(8 * 5).padLeft(5).padRight(5).right().grow().get();
						}
					}).row();
				});
				if (table.getChildren().size == 0) {
					table.table(t -> t.add("@none")).size(Core.graphics.getWidth() * .2f, 45);
				}
			};

			showSelection(Fields, cont, table, pattern, hide, eachFields);
		});
	}
	private void showSelection(Fields Fields, Class<?> cont, Table table, AtomicReference<Pattern> pattern,
														 Runnable[] hide,
														 Runnable eachFields) {
		IntUI.showSelectTable(bind, (p, _hide, v) -> {
			p.clearChildren();
			hide[0] = _hide;
			if (cont != null) {
				pattern.set(compileRegExp(v));
				eachFields.run();
				p.add(table);
			} else {
				p.left().top().defaults().left().top();
				TextField name = new TextField();
				p.table(t -> {
					t.add("@name").growX().left().row();
					t.add(name).width(300);
				}).pad(6, 8, 6, 8).row();
				TextField value = new TextField();
				p.table(t -> {
					t.add("@value").growX().left().row();
					t.add(value).width(300);
				}).pad(6, 8, 6, 8).row();

				p.button("@ok", Styles.cleart, () -> {
					Fields.add(null, name.getText(), value.getText());
					_hide.run();
				}).height(64).fillX();
			}
			//				Log.debug("ok");
		}, cont != null);
	}

	public Button bind = this;

	public static ObjectMap<String, ?> map = Seq.with(
	 "id", "minfo", "iconId", "uiIcon", "fullIcon", "unlocked", "stats", "bars", "timers", "singleTarget", "mapColor", "buildCost", "flags", "timerDump", "dumpTime", "generator", "capacities", "region", "legRegion", "jointRegion", "baseJointRegion", "footRegion", "legBaseRegion", "baseRegion", "cellRegion", "softShadowRegion", "outlineRegion", "shadowRegion", "heatRegion", "edgeRegion", "overlayRegion", "canHeal",
	 "itemFilter", "liquidFilter", "solarSystem", "children", "autoFindTarget"
	).asMap(k -> k, k -> null);
	// public static Pattern pattern = Pattern.compile("^$");

	public static boolean filter(Field field, Class<?> vType) {
		if (!dsettings.getBool("display_deprecated") && field.isAnnotationPresent(Deprecated.class)) return false;

		Class<?> type = field.getType();
		String   name = field.getName();
		while (type.isArray() || arrayClass.contains(type)) {
			type = arrayClass.contains(type) ? ContentVars.getGenericType(field).get(0) : type.getComponentType();
		}
		if (map.containsKey(name)
				|| (type == TextureRegion.class && field.getType().isArray())
				|| (Consume.class.isAssignableFrom(vType) && "^(update|optional|booster)$".matches(name))) return false;
		if (type.isPrimitive() || type == String.class) return true;
		// if (type == TextureRegion.class && type.isAnnotationPresent()) ;
		for (var entry : BuildContent.filterClass) {
			if (entry.key.isAssignableFrom(type)) return true;
		}
		return BuildContent.filterKeys.containsKey(name);
	}

	public static Object defaultValue(String key) {
		return BuildContent.defaultKey.get(key, () -> () -> null).get();
	}

	public static Object defaultValue(Class<?> type) {
		return type.isArray() || arrayClass.contains(type) ? new MyArray<>()
		 : type.getSimpleName().equalsIgnoreCase("boolean") ? false
		 : type.isPrimitive() ? 0
		 : type.getSimpleName().equals("String") ? ""
		 : /* buildContent.make(type) */
		 BuildContent.defaultClassIns.get(type, () -> MyObject::new).get();
	}

	public static ObjectMap<Class<?>, Prov<MyObject>> defValue = new ObjectMap<>();

	static {
		defValue.put(DrawMulti.class, () -> MyObject.of("drawers", new MyArray<>()));
		defValue.put(MultiEffect.class, () -> MyObject.of("effects", new MyArray<>()));
	}

	public static MyObject defaultObjValue(Class<?> type) {
		// Log.info(type);
		return defValue.containsKey(type) ? defValue.get(type).get() : null;
	}

}
