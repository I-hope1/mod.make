package modmake.components;

import arc.Core;
import arc.func.Cons2;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Bullets;
import mindustry.content.Fx;
import mindustry.ctype.ContentType;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.*;
import mindustry.world.blocks.Attributes;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.draw.DrawBlock;
import mindustry.world.meta.Attribute;
import modmake.components.constructor.MyArray;
import modmake.components.constructor.MyObject;
import modmake.ui.styles;
import modmake.util.Fields;

import java.lang.reflect.Field;

import static modmake.util.BuildContent.*;

public class BuildClasses extends ObjectMap<Class<?>, BuildClasses.ClassInterface> {

	Seq<String> effects = genericSeqByClass(Fx.class, Field::getName);
	Seq<String> bullets = genericSeqByClass(Bullets.class, Field::getName);
	Seq<Attribute> attributes = new Seq<>(Attribute.all);

	public BuildClasses() {
		put(Attribute.class, (table, value, __, ___) -> tableWithListSelection(
				table, "" + value, attributes.as(), "" + defaultClass.get(Attribute.class), false));
		put(Attributes.class, (table, value, __, ___) -> {
			if (!(value instanceof MyObject<?, ?>)) throw new IllegalArgumentException("value isn't MyObject");
			MyObject map = new MyObject<>();
			var cont = new Table(Tex.button);
			var children = new Table();
			cont.add(children).fillX().row();
			table.add(cont).fillX();
			final int[] i = {0};
			Cons2 add = (k, v) -> children.add(Fields.build(i[0]++, t -> {
				var key = get(Attribute.class).get(t, k, null, null);
				map.put(
						key, fail(t, v, Double.TYPE));
				t.table(right -> {
					right.button("", Icon.trash, styles.cleart, () -> {
						map.remove(key);
						t.remove();
					});
				}).padLeft(4).growX().right();
			})).growX().row();
			var obj = or((MyObject) value, new MyObject<>());
			obj.each(add);

			cont.button("$add", Icon.add, () -> add.get(null, 0)).growX().minWidth(100);

			return () -> map;
		});
		put(Color.class, (table, value, __, ___) -> {
			Color[] color = {null};
			try {
				color[0] = Color.valueOf("" + value);
			} catch (Exception e) {
				color[0] = new Color();
			}
			var button = new Button();

			var image = button.image().size(30).color(color[0]);
			var field = button.add("" + color[0]).get();
			/* 使用原本自带的采色器 */
			button.clicked(() -> Vars.ui.picker.show(color[0], (c -> {
				image.color(color[0] = c.cpy());
				field.setText("" + c);
			})));

			table.add(button);

			return () -> "\"" + color[0] + "\"";
		});
		put(Sector.class, (table, value, __, ___) -> fail(table, parseInt(value), Double.TYPE));
		put(BulletType.class, (table, value, __, ___) -> listWithType(
				table, value, BulletType.class, "BasicBulletType", bullets, b -> "" + b));
		put(StatusEffect.class, (table, value, __, ___) -> listWithType(
				table, value, StatusEffect.class, "StatusEffect",
				Vars.content.statusEffects(), s -> s.name, new Seq<>(StatusEffect.class)));
		put(Weather.class, (table, value, __, ___) -> listWithType(
				table, value, Weather.class, "ParticleWeather",
				Vars.content.getBy(ContentType.weather).<Weather>as(), w -> w.name));
		// AmmoType, (table, value) -> {},
		put(DrawBlock.class, (table, value, vType, __) -> {
			if (value instanceof String) {
				return tableWithTypeSelection(table, MyObject.of("type", value), vType, "DrawBlock");
			}
			return tableWithTypeSelection(table, (MyObject) value, vType, "DrawBlock");
		});
		put(Ability.class, (table, value, vType, __) -> tableWithTypeSelection(table,
				(MyObject) value, vType, "Ability"));
		put(Weapon.class, (table, value, vType, __) -> tableWithTypeSelection(table,
				(MyObject) value, vType, "Weapon"));

		put(ItemStack.class, (table, value, __, ___) -> {
			String[][] stack = {{null, null}};
			if (value instanceof String) {
				stack[0] = (value + "").split("/");
			} else if (value instanceof MyObject) {
				var obj = (MyObject) value;
				stack[0] = new String[]{"" + obj.get("item"), "" + obj.get("amount")};
			} else {
				stack[0] = new String[]{"copper", "0"};
			}

			// to do...
//		if (isNaN(stack[0][1])) throw new IllegalArgumentException("'" + stack[0][1] + "' isn't a number");
			return buildOneStack(table, "item", Vars.content.items(), stack[0][0], stack[0][1]);
		});
		// like ItemStack
		put(LiquidStack.class, (table, value, __, ___) -> {
			String[][] stack = {{null, null}};
			if (value instanceof String) {
				stack[0] = (value + "").split("/");
			} else if (value instanceof MyObject) {
				var obj = (MyObject) value;
				stack[0] = new String[]{"" + obj.get("liquid"), "" + obj.get("amount")};
			} else {
				stack[0] = new String[]{"liquid", "0"};
			}

			// to do...
//		if (isNaN(stack[0][1])) throw new IllegalArgumentException("'" + stack[0][1] + "' isn't a number");
			return buildOneStack(table, "liquid", Vars.content.liquids(), stack[0][0], stack[0][1]);
		});
		put(Effect.class, (table, value, __, ___) -> listWithType(table, value, Effect.class,
				"ParticleEffect", effects, e -> "" + e));
		put(UnitType.class, (table, value, vType, __) -> tableWithFieldImage(table,
				"" + value, vType, Vars.content.units()));
		put(Item.class, (table, value, vType, __) -> tableWithFieldImage(table,
				"" + value, vType, Vars.content.items()));
		put(Liquid.class, (table, value, vType, __) -> tableWithFieldImage(table,
				"" + value, vType, Vars.content.liquids()));
		put(Planet.class, (table, value, vType, __) -> tableWithFieldImage(table,
				"" + value, vType, Vars.content.planets()));
		put(ObjectMap.class, (table, value, vType, classes) -> {
			MyObject map = new MyObject<>();
			Table cont = new Table(Tex.button);
			Table group = new Table();
			cont.add(group).fillX().row();
			table.add(cont).fillX();
			final int[] i = {0};
			Cons2<String, Object>[] add = new Cons2[]{null};
			add[0] = (k, v) -> {
				var tab = Fields.build(i[0]++, (t -> {
					Prov<?> key = get(classes.get(0)).get(t, or(k, defaultClass.get(classes.get(0))), null, null);
					Table[] foldt = foldTable();
					t.add(foldt[0]);
					map.put(
							key, get(classes.get(1)).get(foldt[1], v, null, null)
					);
					Runnable remove = () -> {
						map.remove(key);
						t.remove();
					};
					t.table((right -> {
						copyAndPaste(right, k, v, (newV -> {
							remove.run();
							add[0].get(k, newV);
						}), () -> {});
						right.button("", Icon.trash, styles.cleart, remove);
					})).padLeft(4).growX().right();
				}));
				tab.defaults().growX();
				group.add(tab).growX().row();
			};
			ObjectMap obj = or((ObjectMap) value, new MyObject<>());
			obj.each(add[0]);

			cont.button("$add", Icon.add, () -> add[0].get(null, null)).growX().minWidth(100);

			return () -> map;
		});

		put(UnitFactory.UnitPlan.class, (table, value, __, ___) -> {
			MyObject map = or((MyObject) value, new MyObject<>());
			Table cont = new Table(Tex.button);
			table.add(cont).fillX();
			cont.add(Core.bundle.get("unit", "unit"));
			map.put("unit", get(UnitType.class).get(cont, map.get("unit", defaultClass.get(UnitType.class)), null, null));
			cont.row();
			cont.add(Core.bundle.get("time", "time"));
			map.put("time", fail(cont, map.get("time", 0), Double.TYPE));
			cont.row();
			cont.add(Core.bundle.get("requirements"));
			Table[] foldt = foldTable();
			cont.add(foldt[0]).row();
			map.put(
					"requirements", fArray(foldt[1], ItemStack.class, (MyArray) map.get("requirements", new MyArray<>()))
			);

			return () -> map;
		});
	}

	public interface ClassInterface {
		Prov<?> get(Table t, Object o, Class<?> clazz, Seq<Class<?>> classes);
	}
}
