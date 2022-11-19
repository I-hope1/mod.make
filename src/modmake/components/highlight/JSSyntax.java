package modmake.components.highlight;

import arc.graphics.Color;
import arc.struct.*;
import modmake.components.TextAreaTable;

import java.util.regex.Pattern;

public class JSSyntax extends Syntax {
	public static Pattern
			keywordP = Pattern.compile("\\b(break|c(?:ase|atch|onst|ontinue)|d(?:efault|elete|o)|else|f(?:inally|or|unction)|i[fn]|instranceof|let|new|return|switch|this|t(?:hrow|ry|ypeof)|v(?:ar|oid)|w(?:hile|ith)|yield)\\b", Pattern.COMMENTS),
	// 数字和true|false
	numberP = Pattern.compile("\\b([+-]?\\d+(?:\\.\\d*)?(?:[Ee]\\d+)?)\\b"),
			commentP = Pattern.compile("(//.*|/\\*[\\s\\S]*?\\*/|/\\*[^(*/)]*$)"),
	/**
	 * 我也不知道为什么这么慢
	 **/
	functionsP = Pattern.compile("([a-z_$][\\w$]*)\\s*\\(", Pattern.CASE_INSENSITIVE),

	objectsP = Pattern.compile("\\b(null|undefined|true|false|arguments)\\b", Pattern.COMMENTS)
			// others = Pattern.compile("([a-z$]+)", Pattern.CASE_INSENSITIVE)
			;
	public static Seq<Pattern> patternSeq = Seq.with(
			whiteSpaceP, stringP, keywordP, numberP, commentP,
			bracketsP, operatCharP/*, functionsP*/, objectsP
	);
	public static Seq<Color> colorSeq = Seq.with(
			Color.clear, stringC, keywordC, numberC, commentC,
			bracketsC, operatCharC/*, functionsC*/, objectsC
	);



	public JSSyntax(TextAreaTable table) {
		super(table);
	}

	/*public JSSyntax() {

	}*/

	/*static JsonValue
			keywords = reader.parse("{\"b\":{\"r\":{\"e\":{\"a\":{\"k\":false}}}},\"c\":{\"a\":{\"s\":{\"e\":false},\"t\":{\"c\":{\"h\":false}}},\"o\":{\"n\":{\"s\":{\"t\":false},\"t\":{\"i\":{\"n\":{\"u\":{\"e\":false}}}}}}},\"d\":{\"e\":{\"f\":{\"a\":{\"u\":{\"l\":{\"t\":false}}}},\"l\":{\"e\":{\"t\":{\"e\":false}}}},\"o\":false},\"e\":{\"l\":{\"s\":{\"e\":false}}},\"f\":{\"i\":{\"n\":{\"a\":{\"l\":{\"l\":{\"y\":false}}}}},\"o\":{\"r\":false},\"u\":{\"n\":{\"c\":{\"t\":{\"i\":{\"o\":{\"n\":false}}}}}}},\"i\":{\"f\":false,\"n\":{\"s\":{\"t\":{\"r\":{\"a\":{\"n\":{\"c\":{\"e\":{\"o\":{\"f\":false}}}}}}}}}},\"l\":{\"e\":{\"t\":false}},\"n\":{\"e\":{\"w\":false}},\"r\":{\"e\":{\"t\":{\"u\":{\"r\":{\"n\":false}}}}},\"s\":{\"w\":{\"i\":{\"t\":{\"c\":{\"h\":false}}}}},\"t\":{\"h\":{\"i\":{\"s\":false},\"r\":{\"o\":{\"w\":false}}},\"r\":{\"y\":false},\"y\":{\"p\":{\"e\":{\"o\":{\"f\":false}}}}},\"v\":{\"a\":{\"r\":false},\"o\":{\"i\":{\"d\":false}}},\"w\":{\"h\":{\"i\":{\"l\":{\"e\":false}}},\"i\":{\"t\":{\"h\":false}}},\"y\":{\"i\":{\"e\":{\"l\":{\"d\":false}}}}}"),
			objects = reader.parse("{\"n\":{\"u\":{\"l\":{\"l\":false}}},\"u\":{\"n\":{\"d\":{\"e\":{\"f\":{\"i\":{\"n\":{\"e\":{\"d\":false}}}}}}}},\"t\":{\"r\":{\"u\":{\"e\":false}}},\"f\":{\"a\":{\"l\":{\"s\":{\"e\":false}}}},\"a\":{\"r\":{\"g\":{\"u\":{\"m\":{\"e\":{\"n\":{\"t\":{\"s\":false}}}}}}}}}");*/

	static IntMap<?>
			keywordMap = IntMap.of('b', IntMap.of('r', IntMap.of('e', IntMap.of('a', IntMap.of('k', new IntMap<>())))), 'c', IntMap.of('a', IntMap.of('s', IntMap.of('e', new IntMap<>()), 't', IntMap.of('c', IntMap.of('h', new IntMap<>()))), 'o', IntMap.of('n', IntMap.of('s', IntMap.of('t', new IntMap<>()), 't', IntMap.of('i', IntMap.of('n', IntMap.of('u', IntMap.of('e', new IntMap<>()))))))), 'd', IntMap.of('e', IntMap.of('f', IntMap.of('a', IntMap.of('u', IntMap.of('l', IntMap.of('t', new IntMap<>())))), 'l', IntMap.of('e', IntMap.of('t', IntMap.of('e', new IntMap<>())))), 'o', new IntMap<>()), 'e', IntMap.of('l', IntMap.of('s', IntMap.of('e', new IntMap<>()))), 'f', IntMap.of('i', IntMap.of('n', IntMap.of('a', IntMap.of('l', IntMap.of('l', IntMap.of('y', new IntMap<>()))))), 'o', IntMap.of('r', new IntMap<>()), 'u', IntMap.of('n', IntMap.of('c', IntMap.of('t', IntMap.of('i', IntMap.of('o', IntMap.of('n', new IntMap<>()))))))), 'i', IntMap.of('f', new IntMap<>(), 'n', IntMap.of('s', IntMap.of('t', IntMap.of('r', IntMap.of('a', IntMap.of('n', IntMap.of('c', IntMap.of('e', IntMap.of('o', IntMap.of('f', new IntMap<>())))))))))), 'l', IntMap.of('e', IntMap.of('t', new IntMap<>())), 'n', IntMap.of('e', IntMap.of('w', new IntMap<>())), 'r', IntMap.of('e', IntMap.of('t', IntMap.of('u', IntMap.of('r', IntMap.of('n', new IntMap<>()))))), 's', IntMap.of('w', IntMap.of('i', IntMap.of('t', IntMap.of('c', IntMap.of('h', new IntMap<>()))))), 't', IntMap.of('h', IntMap.of('i', IntMap.of('s', new IntMap<>()), 'r', IntMap.of('o', IntMap.of('w', new IntMap<>()))), 'r', IntMap.of('y', new IntMap<>()), 'y', IntMap.of('p', IntMap.of('e', IntMap.of('o', IntMap.of('f', new IntMap<>()))))), 'v', IntMap.of('a', IntMap.of('r', new IntMap<>()), 'o', IntMap.of('i', IntMap.of('d', new IntMap<>()))), 'w', IntMap.of('h', IntMap.of('i', IntMap.of('l', IntMap.of('e', new IntMap<>()))), 'i', IntMap.of('t', IntMap.of('h', new IntMap<>()))), 'y', IntMap.of('i', IntMap.of('e', IntMap.of('l', IntMap.of('d', new IntMap<>()))))),
			objectMap = IntMap.of('n', IntMap.of('u', IntMap.of('l', IntMap.of('l', new IntMap<>()))), 'u', IntMap.of('n', IntMap.of('d', IntMap.of('e', IntMap.of('f', IntMap.of('i', IntMap.of('n', IntMap.of('e', IntMap.of('d', new IntMap<>())))))))), 't', IntMap.of('r', IntMap.of('u', IntMap.of('e', new IntMap<>()))), 'f', IntMap.of('a', IntMap.of('l', IntMap.of('s', IntMap.of('e', new IntMap<>())))), 'a', IntMap.of('r', IntMap.of('g', IntMap.of('u', IntMap.of('m', IntMap.of('e', IntMap.of('n', IntMap.of('t', IntMap.of('s', new IntMap<>())))))))));


	/*static {
		StringBuffer sb = new StringBuffer();
		append(sb, objects);
		Log.info(sb);
	}

	static void append(StringBuffer sb, JsonValue value) {
		sb.append("IntMap.of(");
		StringJoiner sj = new StringJoiner(",");
		for (JsonValue entry = value; entry != null; entry = entry.next) {
			sj.add("'" + entry.name + "'");
			if (entry.child == null) sj.add("new IntMap<>()");
			else {
				StringBuffer sb2 = new StringBuffer();
				append(sb2, entry.child);
				sj.add(sb2);
			}
		}
		sb.append(sj);
		sb.append(")");
	}*/ {
		taskArr = new DrawTask[]{
				new DrawWord(JSSyntax.keywordMap, keywordC),
				new DrawWord(JSSyntax.objectMap, objectsC),
				new DrawString(stringC),
				new DrawComment(commentC),
				new DrawSymbol(JSSyntax.operats, operatCharC),
				new DrawNumber(numberC),
				new DrawSymbol(JSSyntax.brackets, bracketsC),
		};
	}

	public static IntMap<Object> operats = new IntMap<>();
	public static IntMap<Object> brackets = new IntMap<>();

	static {
		String s;
		s = "~|,+-=*/<>!%^&";
		for (int i = 0, len = s.length(); i < len; i++) {
			operats.put(s.charAt(i), null);
		}
		s = "()[]{}";
		for (int i = 0, len = s.length(); i < len; i++) {
			brackets.put(s.charAt(i), null);
		}
	}
}
