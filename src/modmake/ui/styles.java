package modmake.ui;

import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import mindustry.gen.Tex;
import mindustry.ui.Styles;

public class styles {
	public static TextureRegionDrawable whiteui;
	public static ScrollPane.ScrollPaneStyle nonePane;
	public static Button.ButtonStyle clearb, clearpb;
	public static TextButton.TextButtonStyle cleart;

	public static void load() {
		whiteui = (TextureRegionDrawable) Tex.whiteui;
		nonePane = new ScrollPane.ScrollPaneStyle();

		clearb = new Button.ButtonStyle(Styles.defaultb);
		clearb.up = Styles.none;
		clearb.down = clearb.over = Styles.flatOver;

		clearpb = new Button.ButtonStyle(Styles.clearPartialt);

		cleart = new TextButton.TextButtonStyle(Styles.cleart);
		cleart.up = whiteui.tint(0.5f, 0.5f, 0.3f, 1f);
	}
}
