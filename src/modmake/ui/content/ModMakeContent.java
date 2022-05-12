package modmake.ui.content;

import mindustry.ui.dialogs.ModsDialog;

public class ModMakeContent extends Content {
	public Runnable load, build;
	public ModMakeContent(Runnable load, Runnable build) {
		super("makeMod");
		this.load = load;
		this.build = build;
	}

	@Override
	public void load() {
		load.run();
	}

	@Override
	public void build() {
		build.run();
	}
}
