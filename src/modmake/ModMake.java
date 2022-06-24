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
import modmake.util.ContentSeq;

import static modmake.IntUI.frag;
import static modmake.components.DataHandle.settings;

public class ModMake extends Mod {
	public ModMake() {
		Events.run(EventType.ClientLoadEvent.class, () -> {
			new SettingContent();
			new ModMakeContent();

			try {
//				LoadMod.init();
				ContentSeq.load();
			} catch (Exception e) {
				Vars.ui.showException("加载ContentSeq出现异常", e);
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