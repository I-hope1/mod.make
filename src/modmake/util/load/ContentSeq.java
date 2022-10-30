package modmake.util.load;

import arc.struct.*;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.entities.units.UnitController;
import mindustry.graphics.g3d.*;
import mindustry.mod.ContentParser;
import mindustry.type.Weapon;
import mindustry.world.draw.DrawBlock;
import modmake.util.*;

import java.lang.reflect.*;

import static modmake.components.DataHandle.settings;

public class ContentSeq {
	public static ContentParser parser;
	public static ObjectMap<ContentType, Object> parserObjectMap;
	public static ObjectMap<Class<?>, ContentType> contentTypes = new ObjectMap<>();
	// types -> type
	public static ObjectMap<String, String> cTypeMap = new ObjectMap<>();
	public static ObjectMap<String, Seq<Class<?>>> types = new ObjectMap<>();
	public static ObjectMap<Class<?>, Seq<Class<?>>> otherTypes = ObjectMap.of(
			BulletType.class, new Seq<>(),
			DrawBlock.class, new Seq<>(),
			Ability.class, new Seq<>(),
			Effect.class, new Seq<>(),
			Weapon.class, new Seq<>(),
			UnitController.class, new Seq<>(),
			ShootPattern.class, new Seq<>()
	);

	//	public static ObjectMap<String, Color> colors = new ObjectMap<>();

	public static void load() throws Throwable {
		parser = MyReflect.getValue(Vars.mods, "parser");
		parserObjectMap = MyReflect.getValue(parser, "parsers");
		ObjectMap<Class<?>, ContentType> map = MyReflect.getValue(parser, "contentTypes");
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

		/*Class<?> _TypeParser = Seq.with(ContentParser.class.getDeclaredClasses()).find(c -> c.getSimpleName().equals("TypeParser"));

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
				handler));*/


		Classes.each((k, type) -> {
			if (!settings.getBool("display_deprecated") && type.isAnnotationPresent(Deprecated.class)) return;
			if (Modifier.isAbstract(type.getModifiers())) return;
			var classes = contentTypes.keys().toSeq();
			for (int i = 0; i < classes.size; i += 1) {
				if (classes.get(i) != null && !classes.get(i).isAssignableFrom(type)) continue;
				var cType = contentTypes.get(classes.get(i));
				types.get(cType + "").add(type);
				break;
			}
			otherTypes.each((clazz, arr) -> {
				if (clazz.isAssignableFrom(type) && type != BulletType.class) {
					arr.add(type);
				}
			});
		});

		otherTypes.put(GenericMesh.class, Seq.with(
				NoiseMesh.class, MultiMesh.class, MatMesh.class));
	}


	public static Seq<Class<?>> getGenericType(Field field) {
		Seq<Class<?>> classes = new Seq<>();
		Type genericType = field.getGenericType();
		if (genericType instanceof ParameterizedType) {
			Type[] actualTypes = ((ParameterizedType) genericType).getActualTypeArguments();
			for (Type actualType : actualTypes) {
				if (actualType instanceof Class) {
					classes.add((Class<?>) actualType);
				} else if (actualType instanceof ParameterizedType) {
					classes.add((Class<?>) ((ParameterizedType) actualType).getRawType());
				} else if (actualType instanceof GenericArrayType) {
					Type componentType = ((GenericArrayType) actualType).getGenericComponentType();
					if (componentType instanceof Class)
						classes.add(Array.newInstance((Class<?>) componentType, 0).getClass());
				}
			}
		}
		return classes;
	}


}