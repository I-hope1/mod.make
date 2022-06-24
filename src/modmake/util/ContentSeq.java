package modmake.util;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
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

import java.lang.reflect.*;

import static modmake.components.DataHandle.settings;

public class ContentSeq {
	public static ContentParser parser;
	public static ObjectMap<ContentType, Object> parserObjectMap;
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
		parserObjectMap = (ObjectMap<ContentType, Object>) field.get(parser);
		field = Reflect.getField(ContentParser.class, "contentTypes");
		var map = (ObjectMap<Class<?>, ContentType>) field.get(parser);
		// 手动init
		if (contentTypes.isEmpty()) {
			Method init = ContentParser.class.getDeclaredMethod("init");
			init.setAccessible(true);
			init.invoke(parser);
		}
		contentTypes.putAll(map);
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

		Class<?> _TypeParser = Seq.with(ContentParser.class.getDeclaredClasses()).find(c -> c.getSimpleName().equals("TypeParser"));

		Object target = new Object();
		InvocationHandler handler = (proxyx, method, args) -> {
			if (method.getDeclaringClass() == Object.class) {
				String methodName = method.getName();
				if (methodName.equals("equals")) {
					Object other = args[0];
					return proxyx == other;
				}

				if (methodName.equals("hashCode")) {
					return target.hashCode();
				}

				if (methodName.equals("toString")) {
					return "Proxy[" + target + "]";
				}
			}
			Log.info(proxyx + "\n" + method);
			return null;
//                return adapter.invoke(cf, target, topScope, proxyx, method, args);
		};
		parserObjectMap.put(ContentType.planet, Proxy.newProxyInstance(_TypeParser.getClassLoader(),
				new Class<?>[]{_TypeParser},
				handler));

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



	public static Seq<Class<?>> getGenericType(Field field){
		Seq<Class<?>> classes = new Seq<>();
		Type genericType = field.getGenericType();
        if(genericType instanceof ParameterizedType){
            Type[] actualTypes = ((ParameterizedType)genericType).getActualTypeArguments();
            for (Type actualType : actualTypes) {
                if(actualType instanceof Class){
					classes.add((Class<?>)actualType);
                } else if(actualType instanceof ParameterizedType) {
	                classes.add((Class<?>) ((ParameterizedType) actualType).getRawType());
                } else if(actualType instanceof GenericArrayType){
                    Type componentType = ((GenericArrayType)actualType).getGenericComponentType();
                    if(componentType instanceof Class)
                        classes.add(Array.newInstance((Class<?>)componentType, 0).getClass());
                }
            }
        }
        return classes;
	}
}