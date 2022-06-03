package modmake.components;

import arc.Core;
import arc.func.Prov;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.serialization.Json;
import mindustry.gen.Icon;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.world.consumers.Consume;
import modmake.IntUI;
import modmake.components.constructor.MyArray;
import modmake.components.constructor.MyObject;
import modmake.util.BuildContent;
import modmake.util.ContentSeq;
import modmake.util.Fields;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static modmake.components.DataHandle.*;
import static modmake.util.Tools.as;

public class AddFieldBtn extends TextButton {
	public static Seq<Class<?>> arrayClass = Seq.with(Seq.class, ObjectSet.class);
	public static ObjectMap<Class<?>, Object> caches = new ObjectMap<>();

	public AddFieldBtn(MyObject obj, Fields Fields, Prov<Class<?>> classProv) {
		super("@add");

		add(new Image(Icon.add));
		getCells().reverse();
		clicked(() -> {
			Class<?> cont = classProv.get();
			ObjectMap fields = null;
			if (caches.containsKey(cont)) {
				fields = as(caches.get(cont));
			} else if (cont != null) {
				var m = new OrderedMap(json.getFields(cont));
				if (cont == UnitType.class) {
					// Label仅是一种标识
					m.put("type", Label.class);
					m.put("controller", Label.class);
				}
				caches.put(cont, fields = m);
			}

			var table = new Table();
			AtomicReference<Pattern> pattern = new AtomicReference<>();
			AtomicReference<Runnable> hide = new AtomicReference<>();
			ObjectMap finalFields = fields;
			Runnable eachFields = () -> {
				table.clearChildren();

				assert finalFields != null;
				finalFields.each((key, meta) -> {
					Field field = null;
					if (meta != Label.class) {
						field = ((Json.FieldMetadata) meta).field;
						if (!filter(field, cont)) return;
					}
					String name = key + "";
					String displayName = content.get(name, () -> name);
					if (pattern.get() != null && !pattern.get().matcher(name).find() && !pattern.get().matcher(displayName).find())
						return;

					Field finalField = field;
					table.table(t -> {
						t.button(displayName, Styles.cleart, () -> {
							if (finalField == null) {
								Fields.add(null, name, null);
								hide.get().run();
								return;
							}
							Fields.add(null, name, defaultValue(finalField.getType()));

							hide.get().run();
						}).size(Core.graphics.getWidth() * .2f, 45).disabled(b -> obj.has(name));
						var help = content.get(name + ".help");
						if (help != null) {
							Button[] btn = {null};
							btn[0] = t.button("?", Styles.clearPartialt, () -> IntUI.showSelectTable(btn[0], (p, __, ___) -> {
								p.pane(p2 -> p2.add(help, 1.3f)).pad(4, 8, 4, 8).row();
							}, false)).size(8 * 5).padLeft(5).padRight(5).right().grow().get();
						}
					}).row();
				});
				if (table.getChildren().size == 0) {
					table.table(t -> t.add("$none")).size(Core.graphics.getWidth() * .2f, 45);
				}
			};

			IntUI.showSelectTable(this, (p, _hide, v) -> {
				p.clearChildren();
				hide.set(_hide);
				if (cont != null) {
					try {
						pattern.set(Pattern.compile(v, Pattern.CASE_INSENSITIVE));
					} catch (Exception e) {
						pattern.set(null);
					}
					eachFields.run();
					p.add(table);
				} else {
					p.left().top().defaults().left().top();
					TextField name = new TextField();
					p.table(t -> {
						t.add("$name").growX().left().row();
						t.add(name).width(300);
					}).pad(6, 8, 6, 8).row();
					TextField value = new TextField();
					p.table(t -> {
						t.add("$value").growX().left().row();
						t.add(value).width(300);
					}).pad(6, 8, 6, 8).row();

					p.button("$ok", Styles.cleart, () -> {
						Fields.add(null, name.getText(), value.getText());
						_hide.run();
					}).height(64).fillX();
				}

			}, cont != null);
		});
	}

	public static Pattern pattern = Pattern.compile(
			"^id|minfo|iconId|uiIcon|fullIcon|unlocked|stats|bars|timers|singleTarget|mapColor|buildCost|flags|timerDump|dumpTime|generator|capacities|region|legRegion|jointRegion|baseJointRegion|footRegion|legBaseRegion|baseRegion|cellRegion|softShadowRegion|outlineRegion|shadowRegion|heatRegion|edgeRegion|overlayRegion|canHeal$");
	public static boolean filter(Field field, Class<?> vType) {
		if (!settings.getBool("display_deprecated") && field.isAnnotationPresent(Deprecated.class)) return false;

		var type = field.getType();
		String name = field.getName();
		while (type.isArray() || arrayClass.contains(type)) {
			type = arrayClass.contains(type) ? ContentSeq.getGenericType(field).get(0) : type.getComponentType();
		}
		if (pattern.matcher(name).find()
				|| (type == TextureRegion.class && field.getType().isArray())
				|| (Consume.class.isAssignableFrom(vType) && "^(update|optional|booster)$".matches(name))) return false;
		if (type.isPrimitive() || type == String.class) return true;
//		if (type == TextureRegion.class && type.isAnnotationPresent())
		// 使用throw跳出循环
		try {
			Class<?> finalType = type;
			BuildContent.filterClass.each((k, v) -> {
				if (k.isAssignableFrom(finalType)) throw new RuntimeException();
			});
			BuildContent.filterKeys.each((k, v) -> {
				if (k.equals(name)) throw new RuntimeException();
			});
		} catch (Exception e) {
			return true;
		}
		return false;
	}

	public static Object defaultValue(Class<?> type) {
		return type.isArray() || arrayClass.contains(type) ? new MyArray()
				: type.getSimpleName().equalsIgnoreCase("boolean") ? false
				: type.isPrimitive() ? 0
				: type.getSimpleName().equals("String") ? ""
				: /* buildContent.make(type) */
				BuildContent.defaultClass.containsKey(type) ? BuildContent.defaultClass.get(type) : new MyObject();
	}
}
