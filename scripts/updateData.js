
const IntSettings = require("content/settings");

var meta = Vars.mods.locateMod(modName).meta
Events.on(ClientLoadEvent, () => {
	// 更新日志弹窗
	if (IntSettings.getValue("base", "not_show_again")) return;

	let w = Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) / 3,
		h = w / 9 * 16;
	let t = new Dialog('');
	t.clear();
	t.add('来自mod(“' + modName + '[]”)作者的话:').row();
	t.table(cons(t => {
		t.defaults().left()
		t.add(
			'当前版本[gray](' + meta.version + ')[]仍在测试.\n[gray]这意味着:\n[]- JSON可能出错.\n- 很多内容没有做完.'
		).row()
		t.table(cons(t => {
			t.defaults().left()
			t.add('若发现bug可以到')
			t.add('github', ).color(Pal.accent).get().clicked(() => Core.app.openURI('https://github.com/I-hope1/mod.make/issues'));
			t.add('报告.')
		})).row()
		t.add(
			'\n[red]本mod未经允许禁止转载!!!\n[white]以下是更新日志'
		).row()
		t.image().fillX().color(Color.gray).padTop(2).padBottom(2).row();
		t.add('1. 修复了一堆bug.\n2. 优化了代码.').padBottom(2).row();
	})).minHeight(200)
		.maxHeight(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) * .75).padLeft(20)
	t.row();
	t.table(cons(_t => {
		_t.button('$ok', () => t.hide()).size(120, 50);
		_t.check(Core.bundle.get('not_show_again', 'not show again'),
			false,
			boolc(b => IntSettings.setValue("base", "not_show_again", b))
		);
	}));

	t.closeOnBack();
	t.show();
});