package modmake.components.input.highlight;

import arc.graphics.Color;
import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.Effect;
import mindustry.graphics.Pal;
import mindustry.mod.ClassMap;
import mindustry.type.Category;
import modmake.components.input.area.TextAreaTable;
import rhino.ScriptRuntime;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import static modmake.components.input.highlight.JSSyntax.brackets;
import static modmake.util.tools.Tools.srr;

public class JSONSyntax extends Syntax {
	public JSONSyntax(TextAreaTable table) {
		super(table);
	}

	ObjectMap<ObjectSet<String>, Color> TOKEN_MAP = ObjectMap.of(
	 ClassMap.classes.keys().toSeq().asSet(), Pal.accent,
	 srr(new ObjectSet<>(), set -> {
		 TechTree.all.each(t -> set.add(t.content.name));
	 }), Color.sky,
	 srr(new ObjectSet<>(), set -> {
		 eachFields(Fx.class, Effect.class, f -> set.add(f.getName()));
	 }), Color.sky,
	 new Seq<>(Category.all).asSet(), Color.green,
	 ObjectSet.with("true", "false"), objectsC
	);

	public static <T> void eachFields(Class<?> parent, Class<T> type, Consumer<Field> consumer) {
		for (Field f : parent.getFields()) {
			if (type.isAssignableFrom(f.getType()))
				consumer.accept(f);
		}
	}

	protected final DrawSymbol
	 operatesSymbol = new DrawSymbol(srr(new IntSet(), set -> {
		String s = ":,";
		for (int i = 0, len = s.length(); i < len; i++) {
			set.add(s.charAt(i));
		}
	}), operatCharC),
	 bracketsSymbol = new DrawSymbol(brackets, bracketsC);


	public TokenDraw[] tokenDraws = {task -> {
		if (lastTask != operatesSymbol || operatesSymbol.lastSymbol != ':') return null;
		for (var entry : TOKEN_MAP) {
			if (entry.key.contains(task.token)) {
				return entry.value;
			}
		}
		return null;
	}, task -> {
		return ScriptRuntime.isNaN(ScriptRuntime.toNumber(task.token)) && !task.token.equals("NaN")
		 ? null : numberC;
	}};


	private final DrawTask[] taskArr0 = {
	 new DrawString(stringC),
	 bracketsSymbol,
	 new DrawComment(commentC),
	 operatesSymbol,
	 // new DrawWord(keywordMap, keywordC),
	 // new DrawWord(objectMap, objectsC),
	 new DrawToken(tokenDraws),
	 // new DrawNumber(numberC),
	};

	{
		taskArr = taskArr0;
	}
}
