package modmake.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import mindustry.graphics.Pal;
import mindustry.mod.Mods;
import modmake.IntVars;
import modmake.components.Window;

import static modmake.IntVars.*;
import static modmake.components.DataHandle.dsettings;

// 更新日志弹窗
public class UpdateData extends Window {
	Mods.ModMeta meta = IntVars.mod.meta;

	float w = Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) / 3f,
	 h      = w / 9 * 16;

	public UpdateData() {
		super("");
		top.clear();
		fillParent = false;
	}

	@Override
	public Window show() {
		Table cont = this.cont;
		cont.add("来自mod(“" + modName + "[]”)作者的话:").row();
		cont.pane(t -> {
			t.defaults().left();
			t.add(
			 "当前版本[gray](" + meta.version + ")[]仍在测试.\n[gray]这意味着:\n[]- JSON可能出错.\n- 很多内容没有做完."
			).row();
			t.table(t1 -> {
				t1.defaults().left();
				t1.add("若发现bug可以到");
				t1.add("github").color(Pal.accent).get().clicked(() -> Core.app.openURI("https://github.com/I-hope1/mod.make/issues"));
				t1.add("报告.");
			}).row();
			t.add(
			 "\n[red]本mod未经允许禁止转载!!!\n[white]以下是更新日志"
			).row();
			t.image().fillX().color(Color.gray).padTop(2).padBottom(2).row();
			t.add(mod.root.child("更新日志.txt").readString()).padBottom(2).row();
		}).padTop(10f);
		cont.row();
		cont.table(_t -> {
			_t.button("@ok", this::hide).size(120, 50);
			_t.check(Core.bundle.get("not_show_again", "not show again"),
			 false, b -> dsettings.put("not_show_again", String.valueOf(b))
			);
		});
		cont.pack();

		return super.show();
	}
}
