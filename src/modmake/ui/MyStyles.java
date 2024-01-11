package modmake.ui;

import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import static mindustry.gen.Tex.flatDownBase;
import static mindustry.gen.Tex.pane;
import static mindustry.ui.Styles.*;

public class MyStyles {
	public static TextureRegionDrawable whiteui;
	public static ScrollPaneStyle       nonePane;
	public static ButtonStyle           clearb, clearpb;
	public static TextButtonStyle cleart, clearPartialt, clearTogglet;
	public static ImageButtonStyle clearFulli, clearPartiali, clearToggleTransi, clearTransi;

	public static void load() {
		whiteui = (TextureRegionDrawable) Tex.whiteui;
		nonePane = noBarPane;

		clearPartialt = new TextButtonStyle() {{
			down = flatOver;
			up = pane;
			over = flatDownBase;
			font = Fonts.def;
			fontColor = Color.white;
			disabledFontColor = Color.gray;
		}};
		clearTogglet = new TextButtonStyle() {{
			font = Fonts.def;
			fontColor = Color.white;
			checked = flatDown;
			down = flatDown;
			up = black;
			over = flatOver;
			disabled = black;
			disabledFontColor = Color.gray;
		}};
		clearToggleTransi = new ImageButtonStyle() {{
			down = flatDown;
			checked = flatDown;
			up = black6;
			over = flatOver;
		}};
		clearFulli = new ImageButtonStyle() {{
			down = whiteui;
			up = pane;
			over = flatDown;
		}};
		clearPartiali = new ImageButtonStyle() {{
			down = flatDown;
			up = none;
			over = flatOver;
			disabled = none;
			imageDisabledColor = Color.gray;
			imageUpColor = Color.white;
		}};
		clearTransi = new ImageButtonStyle() {{
			down = flatDown;
			up = black6;
			over = flatOver;
			disabled = black8;
			imageDisabledColor = Color.lightGray;
			imageUpColor = Color.white;
		}};

		clearb = new ButtonStyle(Styles.defaultb);
		clearb.up = Styles.none;
		clearb.down = clearb.over = flatOver;

		clearpb = new ButtonStyle(clearPartialt);

		cleart = new TextButtonStyle(Styles.cleart);
		cleart.up = whiteui.tint(0.5f, 0.5f, 0.3f, 1f);
	}
}
