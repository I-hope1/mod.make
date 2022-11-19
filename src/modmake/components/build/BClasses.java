package modmake.components.build;

import arc.Core;
import arc.func.*;
import arc.graphics.Color;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.ctype.ContentType;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.gen.*;
import mindustry.graphics.g3d.GenericMesh;
import mindustry.type.*;
import mindustry.world.blocks.Attributes;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.consumers.*;
import mindustry.world.draw.DrawBlock;
import mindustry.world.meta.Attribute;
import modmake.components.constructor.*;
import modmake.ui.MyStyles;
import modmake.util.*;

import java.lang.reflect.Field;

import static modmake.util.BuildContent.*;
import static modmake.util.Tools.*;

public class BClasses extends ObjectMap<Class<?>, BClasses.ClassInterface> {

	Seq<String> effects = genericSeqByClass(Fx.class, Field::getName);
	Seq<String> bullets = genericSeqByClass(Bullets.class, Field::getName);
	Seq<Attribute> attributes = new Seq<>(Attribute.all);

	public BClasses() {
		put(Attribute.class, (table, value, __, ___) -> tableWithListSelection(
				table, "" + value, attributes.as(), "" + defaultClass.get(Attribute.class).get(), false));
		put(Attributes.class, (table, value, __, ___) -> {
			checkMyObject(value);
			MyObject<Object, Object> map = new MyObject<>();
			var cont = new Table(Tex.button);
			var children = new Table();
			cont.add(children).fillX().row();
			table.add(cont).fillX();
			final int[] i = {0};
			Cons2<Object, Object> add = (k, v) -> children.add(Fields.build(i[0]++, t -> {
				var key = get(Attribute.class).get(t, or(k, defaultClass.get(Attribute.class)), null, null);
				map.put(key, field(t, v, Double.TYPE));
				t.table(right -> {
					right.button("", Icon.trash, MyStyles.cleart, () -> {
						map.remove(key);
						t.remove();
					});
				}).padLeft(4).growX().right();
			})).growX().row();
			var obj = or(as(value), MyObject::new);
			obj.each(add);

			cont.button("@add", Icon.add, () -> add.get(null, 0)).growX().minWidth(100);

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
			button.clicked(() -> Vars.ui.picker.show(color[0], c -> {
				image.color(color[0] = c.cpy());
				field.setText("" + c);
			}));

			table.add(button);

			return () -> "\"" + color[0] + "\"";
		});
		put(Sector.class, (table, value, __, ___) -> field(table, parseInt(value), Double.TYPE));
		put(BulletType.class, (table, value, __, ___) -> listWithType(
				table, value, BulletType.class, "BasicBulletType", bullets, b -> "" + b));
		put(StatusEffect.class, (table, value, __, ___) -> listWithType(
				table, value, StatusEffect.class, "StatusEffect",
				Vars.content.statusEffects(), s -> s.name, new Seq<>(StatusEffect.class)));
		put(Weather.class, (table, value, __, ___) -> listWithType(
				table, value, Weather.class, "ParticleWeather",
				Vars.content.<Weather>getBy(ContentType.weather), w -> w.name));
		// AmmoType, (table, value) -> {},
		put(DrawBlock.class, (table, value, vType, __) -> {
			if (value instanceof String) {
				return tableWithTypeSelection(table, as(MyObject.of("type", value)), vType, "DrawBlock");
			}
			return tableWithTypeSelection(table, as(value), vType, "DrawBlock");
		});
		put(Ability.class, (table, value, vType, __) -> tableWithTypeSelection(table,
				as(value), vType, "Ability"));
		put(Weapon.class, (table, value, vType, __) -> tableWithTypeSelection(table,
				as(value), vType, "Weapon"));

		put(ItemStack.class, (table, value, __, ___) -> {
			String[][] stack = {{}};
			if (value instanceof String) {
				stack[0] = (value + "").split("/");
			} else if (value instanceof MyObject) {
				var obj = (MyObject<Object, Object>) value;
				stack[0] = new String[]{"" + obj.get("item"), Tools.toString(obj.get("amount"))};
			} else {
				stack[0] = new String[]{"copper", "0"};
			}

			// to do...
			//		if (isNaN(stack[0][1])) throw new IllegalArgumentException("'" + stack[0][1] + "' isn't a number");
			return buildOneStack(table, "item", Vars.content.items(), stack[0][0], stack[0][1]);
		});
		// like ItemStack
		put(LiquidStack.class, (table, value, __, ___) -> {
			String[][] stack = {{}};
			if (value instanceof String) {
				stack[0] = (value + "").split("/");
			} else if (value instanceof MyObject) {
				var obj = (MyObject) value;
				stack[0] = new String[]{"" + or(obj.get("liquid"), defaultClass.get(Liquid.class).get()), Tools.toString(or(obj.get("amount"), 0))};
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
				var tab = Fields.build(i[0]++, t -> {
					Prov<?> key = get(classes.get(0)).get(t, or(k, () -> defaultClass.get(classes.get(0)).get()), null, null);
					Table[] foldT = foldTable();
					t.add(foldT[0]);
					map.put(
							key, get(classes.get(1)).get(foldT[1], v, null, null)
					);
					Runnable remove = () -> {
						map.remove(key);
						t.remove();
					};
					t.table(right -> {
						copyAndPaste(right, k, v, newV -> {
							remove.run();
							add[0].get(k, newV);
						}, () -> {});
						right.button("", Icon.trash, MyStyles.cleart, remove);
					}).padLeft(4).growX().right();
				});
				tab.defaults().growX();
				group.add(tab).growX().row();
			};
			ObjectMap obj = or((ObjectMap) value, MyObject::new);
			obj.each(add[0]);

			cont.button("@add", Icon.add, () -> add[0].get(null, null)).growX().minWidth(100);

			return () -> map;
		});
		put(ShootPattern.class, (table, value, vType, __) -> {
			checkMyObject(value);
			var prov = tableWithTypeSelection(table, as(value), ShootPattern.class, "ShootPattern");
			return () -> prov.get().toString();
		});

		put(UnitFactory.UnitPlan.class, (table, value, __, ___) -> {
			MyObject<Object, Object> map = or(as(value), MyObject::new);
			Table cont = new Table(Tex.button);
			table.add(cont).fillX();
			cont.add(Core.bundle.get("unit", "unit"));
			map.put("unit", get(UnitType.class).get(cont, map.get("unit", defaultClass.get(UnitType.class).get()), null, null));
			cont.row();
			cont.add(Core.bundle.get("time", "time"));
			map.put("time", field(cont, map.get("time", 0), Double.TYPE));
			cont.row();
			cont.add(Core.bundle.get("requirements"));
			Table[] foldT = foldTable();
			cont.add(foldT[0]).row();
			map.put(
					"requirements", fArray(foldT[1], ItemStack.class, as(map.get("requirements", new MyArray<>())))
			);

			return () -> map;
		});

		put(GenericMesh.class, (table, value, __, ___) -> {
			checkMyObject(value);
			var prov = tableWithTypeSelection(table, as(value), GenericMesh.class, "NoiseMesh");
			return () -> prov.get().toString();
		});

		// 以下是consumes
		Seq<Class<?>> consumeFilter = Seq.with(
				ConsumeItemCharged.class,
				ConsumeItemFlammable.class,
				ConsumeItemRadioactive.class,
				ConsumeItemExplosive.class,
				ConsumeItemExplode.class,
				ConsumeItems.class,
				ConsumeLiquidFlammable.class,
				ConsumeLiquid.class,
				ConsumeLiquids.class,
				ConsumeCoolant.class,
				ConsumePower.class
		);
		ClassInterface def = (table, value, clazz, ___) -> {
			checkMyObject(value);
			var prov = fObject(table, () -> clazz, as(value), consumeFilter, true);
			return () -> prov.get().toString();
		};
		put(ConsumeItemCharged.class, def);
		put(ConsumeItemFlammable.class, def);
		put(ConsumeItemRadioactive.class, def);
		put(ConsumeItemExplosive.class, def);
		put(ConsumeItemExplode.class, def);
		put(ConsumeItems.class, def);
		put(ConsumeLiquidFlammable.class, def);
		put(ConsumeLiquid.class, def);
		put(ConsumeLiquids.class, def);
		put(ConsumeCoolant.class, def);
		put(ConsumePower.class, def);


		/*put(TextureRegion.class, (table, value, __, ___) -> {
			final String[] v = {"" + value};
			Button[] btn = {null};
			TextureRegion region = new TextureRegion();
			region.texture = IntVars.find(v[0]).texture;

			table.button(b -> {
				btn[0] = b;
				b.image(region).size(45, 45);
				b.label(() -> v[0]);
			}, Styles.defaultb, () -> {
				MyMod mod = modDialog.currentMod;
				IntUI.createDialog("__",
						"选择图像", "从图片库选择图片", Icon.zoom, (Runnable) () -> {
							IntUI.showSelectImageTableWithFunc(btn[0], mod.keys1, () -> v[0],
									val -> {
										v[0] = val;
										region.texture = IntVars.find(val).texture;
									}, 50, 42, 5,
									val -> mod.sprites1.containsKey(val) ? new TextureRegionDrawable(IntVars.wrap(mod.sprites1.get(val))) : new TextureRegionDrawable(IntVars.error), true);
						});
			});
			return () -> v[0];
		});*/
		/*put(TextureRegion.class, (table, value, __, ___) -> {

		});*/
	}

	public static void checkMyObject(Object value) {
		if (!(value instanceof MyObject))
			throw new IllegalArgumentException("value(" + value + ") must be MyObject.");
	}

	public interface ClassInterface {
		Prov<?> get(Table t, Object o, Class<?> clazz, Seq<Class<?>> classes);
	}
}
