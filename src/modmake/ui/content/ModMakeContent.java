package modmake.ui.content;

import modmake.util.BuildContent;
import modmake.util.Fields;

import static modmake.IntUI.editor;
import static modmake.IntUI.modsDialog;

public class ModMakeContent extends Content {
	public ModMakeContent() {
		super("makeMod");

	}

	@Override
	public void load() {
		Fields.load();
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
