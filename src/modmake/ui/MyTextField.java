package modmake.ui;

import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import mindustry.ui.Fonts;


public class MyTextField extends TextField {
	public MyTextField(String text) {
		super(text);
//		setStyle(new MyTextFieldStyle());
		mySetWidth();
		changed(this::mySetWidth);
	}

	public void mySetWidth() {
		String s = getText();
		int totalWidth = 0;
		for (int i = 0, len = s.length(); i < len; i++) {
			totalWidth += Fonts.getGlyph(Fonts.def, s.charAt(i)).getMinWidth();
		}
		setWidth(totalWidth + Scl.scl(23f));
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
