
const IntStyles = require('styles');
Events.on(ClientLoadEvent, e => {
	// 更新日志弹窗
	if (Core.settings.get(modName + '-不再显示', false)) return;
	// 更新日志函数
	function fun(table, versoin, text, hasImage) {
		table.add('' + versoin).size(20).growX().left().row();
		table.table(Tex.button, cons(t => {
			t.add('' + text).row();
			if (hasImage) t.pane(cons(p => {
				let i = 1,
					image = Core.atlas.find(modName + '-update-' + versoin + '-' + i);
				while (image.toString() != 'error') {
					p.add(Image(image)).size(w, h);
					image = Core.atlas.find(modName +
						'-update-' + versoin + '-' + ++i
					);
				}
			})).fillX();
		})).fillX().row();
	}
	let w = Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) / 3,
		h = w / 9 * 16;
	let t = new Dialog('');
	t.clear();
	t.add('来自mod(“' + modName + '[]”)作者的话:').row();
	t.pane(cons(t => {
		t.add(
			'当前版本[gray](beta2.4)[]仍在测试.\n[gray]这意味着:\n[]- JSON成可能出错.\n- 很多内容没有做完.\n若发现bug请及时告诉作者[gray](Ihope...).\n\n[red]本mod未经允许禁止转载!!!\n[white]以下是更新日志'
		).row()
		t.image().fillX().color(Color.gray).row();
		t.add('1. 修复了一堆bug.\n2. 优化了代码.').row();
		t.button('历史更新内容', run(() => {
			let dialog = new BaseDialog('历史更新日志');
			dialog.cont.pane(cons(p => {
				fun(p, 'beta1.0', '此版本为最初版本，只能编辑“mod.json”.', true)
				fun(p, 'beta1.01',
					'此版本为内测版.\n更新内容:\n1.添加了所有类的预览.\n2.修复一些bug.\n3.优化了mod.',
					false);
				fun(p, 'beta2.0',
					'更新内容:\n1.添加语言和js脚本编辑.\n2.修复了一堆bug.\n3.化了mod，样式化了ui.',
					true);
				fun(p, 'beta2.2',
					'更新内容:\n1.重置了json编辑.\n2.修复了一堆bug.\n3.优化mod，样式化了ui.',
					true);
				fun(p, 'beta2.21', '更新内容：修复了bug');
				fun(p, 'beta2.23', '更新内容：\n1.优化了代码.\n2.添加一些jso代码编辑程序，让你能更轻松的写json代码.\n3.优化代码编辑.\n4.修了一些bug.', true);
				fun(p, 'beta2.24', '更新内容：1. 优化ui.\n2. 修复bug.\n3. 完善select.');
			})).fillX().fillY();
			dialog.addCloseButton();
			dialog.show();
		})).fillX().row();
	})).width(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) - 120).minHeight(200)
		.maxHeight(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) * .75).padLeft(20)
		.style(IntStyles[0].cont);
	t.row();
	t.table(cons(_t => {
		_t.button('$ok', run(() => t.hide())).size(120, 50);
		_t.check(Core.bundle.get('not_show_again', 'not show again'), new Boolc({
			get: b => Core.settings.put(modName + '-not_show_again', b)
		}));
	}));
	t.show();
});