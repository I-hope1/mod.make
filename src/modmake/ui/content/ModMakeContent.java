package modmake.ui.content;

import modmake.util.BuildContent;
import modmake.util.Fields;

import static modmake.IntUI.*;

public class ModMakeContent extends Content {
	public ModMakeContent() {
		super("makeMod");

	}

	@Override
	public void load() {
		Fields.load((fields, table, key) -> BuildContent.build(fields.type, fields, table, key, fields.map.get(key)));
		modsDialog.load();
		editor.load();
		BuildContent.load();
//		load.run();
	}

	@Override
	public void build() {
		modsDialog.show();
	}
}
