package modmake;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.TextButton;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;
import modmake.ui.ImgEditor;
import modmake.ui.ImgEditorDialog;

public class IntUI {
	public static ImgEditor imgEditor = new ImgEditor();
	public static ImgEditorDialog imgDialog = new ImgEditorDialog();
	/**
	 * Argument format:
	 * 0) button name
	 * 1) description
	 * 2) icon name
	 * 3) listener
	 */
	public static void createDialog(String title, Object... arguments) {
		BaseDialog dialog = new BaseDialog(title);

		float h = 90f;

		dialog.cont.defaults().size(360f, h).padBottom(5).padRight(5).padLeft(5);

		for (int i = 0; i < arguments.length; i += 4) {
			String name = (String) arguments[i];
			String description = (String) arguments[i + 1];
			Drawable icon = arguments[i + 2] != null ? (Drawable) arguments[i + 2] : Tex.clear;
			Runnable listenable = (Runnable) arguments[i + 3];

			TextButton button = dialog.cont.button(name, () -> {
				listenable.run();
				dialog.hide();
			}).left().margin(0).get();

			button.clearChildren();
			button.image(icon).padLeft(10);
			button.table(t -> {
				t.add(name).growX().wrap();
				t.row();
				t.add(description).color(Color.gray).growX().wrap();
			}).growX().pad(10f).padLeft(5);

			button.row();

			dialog.cont.row();
		}

		dialog.addCloseButton();
		dialog.show();
	}
}
