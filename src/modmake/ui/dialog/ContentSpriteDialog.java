package modmake.ui.dialog;

import arc.Core;
import mindustry.ui.dialogs.BaseDialog;

public class ContentSpriteDialog extends BaseDialog {
	public ContentSpriteDialog() {
		super(Core.bundle.get("content.sprite.dialog", "Content图集"));

		addCloseButton();
	}
}
