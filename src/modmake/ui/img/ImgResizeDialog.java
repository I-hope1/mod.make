package modmake.ui.img;

import arc.Core;
import arc.func.Intc2;
import arc.math.Mathf;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import mindustry.ui.dialogs.BaseDialog;

import static modmake.IntUI.imgDialog;
import static modmake.IntUI.imgEditor;
import static rhino.ScriptRuntime.isNaN;
import static rhino.ScriptRuntime.toNumber;

public class ImgResizeDialog extends BaseDialog {
	// just advice
	public static int minSize = 1, maxSize = Mathf.pow(2, 12);

	int width, height;

	public ImgResizeDialog(Intc2 cons) {
		super("重设大小");

		closeOnBack();
		shown(() -> {
			cont.clear();
			width = imgEditor.width();
			height = imgEditor.height();

			Table table = new Table();

			for (boolean w : Mathf.booleans) {
				table.add(w ? "@width" : "@height").padRight(8f);
				table.defaults().height(60f).padTop(8);

				table.field((w ? width : height) + "", TextField.TextFieldFilter.digitsOnly, value -> {
					int val = (int) toNumber(value);
					if (w) width = val;
					else height = val;
				}).valid(value -> {
					if (isNaN(toNumber(value))) return false;
					int i = (int) toNumber(value);
					return i <= maxSize && i >= minSize;
				}).maxTextLength(4);

				table.row();
			}
			cont.row();
			cont.add(table);

		});

		hidden(() -> {
			Core.scene.setKeyboardFocus(imgDialog);
		});

		buttons.defaults().size(200f, 50f);
		buttons.button("@cancel", this::hide);
		buttons.button("@ok", () -> {
			cons.get(width, height);
			hide();
		});
	}
}
