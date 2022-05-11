
const IntFunc = require("func/index")
const findClass = require('func/findClass')
const { settings } = findClass("components.dataHandle");


var meta = IntFunc.mod.meta
Events.run(ClientLoadEvent, () => {
	// 更新日志弹窗

	let w = Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) / 3,
		h = w / 9 * 16;
	let dialog = new Dialog();
	dialog.clear();
	dialog.add('来自mod(“' + modName + '[]”)作者的话:').row();
	dialog.table(cons(t => {
		t.defaults().left()
		t.add(
			'当前版本[gray](' + meta.version + ')[]仍在测试.\n[gray]这意味着:\n[]- JSON可能出错.\n- 很多内容没有做完.'
		).row()
		t.table(cons(t => {
			t.defaults().left()
			t.add('若发现bug可以到')
			t.add('github',).color(Pal.accent).get().clicked(() => Core.app.openURI('https://github.com/I-hope1/mod.make/issues'));
			t.add('报告.')
		})).row()
		t.add(
			'\n[red]本mod未经允许禁止转载!!!\n[white]以下是更新日志'
		).row()
		t.image().fillX().color(Color.gray).padTop(2).padBottom(2).row();
		t.add('1. 修复了一堆bug.\n2. 优化了代码.\n3. 添加了新功能').padBottom(2).row();
	})).minHeight(200)
		.maxHeight(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) * .75).padLeft(20)
	dialog.row();
	dialog.table(cons(_t => {
		_t.button('$ok', () => dialog.hide()).size(120, 50);
		_t.check(Core.bundle.get('not_show_again', 'not show again'),
			false,
			boolc(b => settings.put("not_show_again", b))
		);
	}));

	dialog.closeOnBack();
	dialog.show();
});