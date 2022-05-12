package modmake.util;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.units.AIController;
import mindustry.mod.ContentParser;
import mindustry.mod.Mods;
import mindustry.type.Weapon;
import mindustry.world.draw.DrawBlock;

import java.lang.reflect.Modifier;

import static modmake.components.dataHandle.settings;

public class ContentSeq {
	public static ContentParser parser;
	public static ObjectMap<ContentType, ?> parserObjectMap;
	public static ObjectMap<Class<?>, ContentType> contentTypes = new ObjectMap<>();
	public static ObjectMap<String, String> cTypeMap = new ObjectMap<>();
	public static ObjectMap<String, Seq<Class<?>>> types = new ObjectMap<>();
	public static ObjectMap<Class<?>, Seq<Class<?>>> otherTypes = ObjectMap.of(
			BulletType.class, new Seq<>(),
			DrawBlock.class, new Seq<>(),
			Ability.class, new Seq<>(),
			Effect.class, new Seq<>(),
			Weapon.class, new Seq<>(),
			AIController.class, new Seq<>()
	);

	public static void load() throws Exception {
		var field = Reflect.getField(Mods.class, "parser");
		parser = (ContentParser) field.get(Vars.mods);
		field = Reflect.getField(ContentParser.class, "parsers");
		parserObjectMap = (ObjectMap<ContentType, ?>) field.get(parser);
		field = Reflect.getField(ContentParser.class, "contentTypes");
		contentTypes.putAll((ObjectMap<Class<?>, ContentType>) field.get(parser));
		contentTypes.each((clazz, cType) -> {
			/*if (!parserObjectMap.containsKey(type)) {
				contentTypes.remove(contentTypes.findKey(type, false));
			}*/

			if (parserObjectMap.containsKey(cType)) {
				String _type = cType + "";
				String type_s = _type.endsWith("s") ? _type : cType + "s";
				cTypeMap.put(type_s, _type);
			}
//			if (parserObjectMap.containsKey(contentType)) {
				types.put(cType + "", new Seq<>());
//			}
		});

		Classes.each((k, type) -> {
			if (!settings.getBool("display_deprecated") && type.isAnnotationPresent(Deprecated.class)) return;
			var classes = contentTypes.keys().toSeq();
			for (int i = 0; i < classes.size; i += 1) {
				if (classes.get(i) != null && !classes.get(i).isAssignableFrom(type)) continue;
				var cType = contentTypes.get(classes.get(i));
				types.get(cType + "").add(type);
				break;
			}
			otherTypes.each((clazz, arr) -> {
				if (clazz.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers()) && type != BulletType.class) {
					arr.add(type);
				}
			});
		});
	}
}