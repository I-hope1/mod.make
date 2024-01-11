package modmake.ui.content;

import modmake.IntUI;

public class SettingContent extends Content {
	public SettingContent() {
		super("settings");
	}

	@Override
	public void build() {
		IntUI.settingsDialog.show();
	}
}
