package modmake.components.input.area;

import arc.graphics.g2d.Font;
import arc.math.Mathf;
import arc.scene.Scene;
import arc.scene.style.Drawable;
import arc.scene.ui.TextField;
import arc.util.Time;


public class AutoTextField extends TextField {
	public AutoTextField(String text) {
		super(text);
	}

	{
		changed(() -> {
			Time.runTask(0, () -> setWidth(getPrefWidth()));
			parent.invalidateHierarchy();
		});
	}

	public AutoTextField() {
	}

	@Override
	public float getPrefWidth() {
		return Mathf.clamp(layout.width + 30, 100, 440);
	}

	Drawable getBack() {
		Scene   stage   = getScene();
		boolean focused = stage != null && stage.getKeyboardFocus() == this;
		return (disabled && style.disabledBackground != null) ? style.disabledBackground
		 : (!isValid() && style.invalidBackground != null) ? style.invalidBackground
		 : ((focused && style.focusedBackground != null) ? style.focusedBackground : style.background);
	}

	@Override
	protected void drawCursor(Drawable cursorPatch, Font font, float x, float y) {
		cursorPatch.draw(
		 x + textOffset + glyphPositions.get(cursor) - glyphPositions.get(visibleTextStart) + fontOffset + font.getData().cursorX,
		 y - textHeight - font.getDescent(), cursorPatch.getMinWidth(), textHeight);
	}
	/*public static class MyTextFieldStyle extends TextFieldStyle {
		public MyTextFieldStyle() {
			font = luculent;
		}
	}*/

	/*public static Font luculent;

	public static void load() {
		Core.assets.load("luculent", Font.class, new FreetypeFontLoader.FreeTypeFontLoaderParameter(IntVars.data.child("data").child("luculent.ttf").path(), new FreeTypeFontGenerator.FreeTypeFontParameter() {{
			size = 30;
			characters = "\0";
		}})).loaded = f -> luculent = f;
	}*/
}
