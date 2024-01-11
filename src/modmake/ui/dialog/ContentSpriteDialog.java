package modmake.ui.dialog;

import arc.Core;
import modmake.components.Window;

public class ContentSpriteDialog extends Window {
	public ContentSpriteDialog() {
		super(Core.bundle.get("content.sprite.dialog", "Content图集")
		 , 120, 80, true, false);

		// addCloseButton();
	}
}
