package modmake.components.highlight;

import arc.struct.*;
import modmake.components.TextAreaTable;

public class JsonSyntax extends JSSyntax {
	public JsonSyntax(TextAreaTable table) {
		super(table);
	}

	public static final IntMap<?> objectMap = IntMap.of('n', IntMap.of('u', IntMap.of('l', IntMap.of('l'))));

	{
		taskArr = new DrawTask[]{
				new DrawNumber(numberC),
				new DrawSymbol(JSSyntax.brackets, bracketsC),
				new DrawString(stringC),
				new DrawComment(commentC),
				new DrawWord(objectMap, objectsC),
		};
	}
}
