package modmake;

import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import modmake.ui.UpdateData;
import modmake.ui.content.ModMakeContent;
import modmake.ui.content.SettingContent;
import modmake.ui.styles;
import modmake.util.MyReflect;
import modmake.util.load.ContentSeq;
import modmake.util.load.LoadMod;

import static modmake.IntUI.frag;
import static modmake.components.DataHandle.settings;

public class ModMake extends Mod {
	public static Runnable runnable;

	public ModMake() {
		MyReflect.load();

		Events.run(EventType.ClientLoadEvent.class, () -> {
//			if (Vars.ui != null) throw new RuntimeException("");
//			DataHandle.load();

			new SettingContent();
			new ModMakeContent();

			try {
//				LoadMod.init();
				ContentSeq.load();
			} catch (Throwable e) {
				Vars.ui.showException("加载ContentSeq出现异常", e);
			}
			try {
				LoadMod.init();
			} catch (Throwable throwable) {
				Log.err(throwable);
			}
			styles.load();
			frag.load();
			if (!settings.getBool("not_show_again")) {
				Log.info("load updateData");
				new UpdateData().show();
			}
			// debug
//			imgDialog.beginEditImg(Vars.dataDirectory.child("tmp.png"));
		});
	}
}