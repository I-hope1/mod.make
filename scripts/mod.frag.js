Time.run(1, run(() => {
	let style = new ScrollPane.ScrollPaneStyle(Styles.defaultPane);
	style.vScroll = style.vScrollKnob = style.corner = Styles.none;
	let _style = new Button.ButtonStyle(Styles.defaultb);
	_style.up = Styles.black5;
	_style.down = _style.over = Styles.flatOver;
	if(!Core.settings.get(modName + '-不再显示', false)){
		function table(fun){
			let ui = new Dialog('');
			ui.cont.table(cons(t => fun(t))).row();
			ui.cont.button('$ok', run(() => ui.hide())).size(120, 50);
			return ui.show();
		}
		let w = Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) / 3, h = w / 9 * 16;
		let t = new Dialog('');
		t.clear();
		t.add('来自mod(“' + modName + '[]”)作者的话:').row();
		t.pane(cons(t => {
			t.add('当前版本[gray](beta2.0)[]仍在测试.\n[gray]这意味着:\n[]- JSON生成可能出错.\n- 很多内容没有做完.\n若发现请及时告诉作者[gray](I hope...).\n\n[red]本mod未经允许禁止转载!!!\n[white]以下是更新日志').row();
			t.image().fillX().color(Color.gray).row();
			t.button('beta1.0', run(() => table(t => {
				t.add('此版本为最初版本，只能编辑“mod.json”.').row();
				t.pane(cons(p => {
					for(var i = 0, image = Core.atlas.find(modName + '-update-beta1.0-' + ++i); image.toString() != 'error'; image = Core.atlas.find(modName + '-update-beta1.0-' + ++i)) p.image(image).size(w, h);
				})).width(Core.graphics.getWidth() * .9);
			}))).fillX().row();
			t.button('beta1.01', run(() => Vars.ui.showInfo('此版本为内测版.\n更新内容:\n1.添加了所有类的预览.\n2.修复了一些bug.\n3.优化了mod.'))).fillX().row();
			t.button('beta2.0', run(() => table(t => {
				t.add('更新内容:\n1.添加语言和js脚本编辑.\n2.修复了一堆bug.\n3.优化了mod，样式化了ui.').row();
				t.pane(cons(p => {
					for(var i = 0, image = Core.atlas.find(modName + '-update-beta2.0-' + ++i); (image + '') != 'error'; image = Core.atlas.find(modName + '-update-beta2.0-' + ++i)) p.image(image).size(w, h);
				})).width(Core.graphics.getWidth() * .9);
			}))).fillX().row()
			t.button('beta2.2', run(() => table(t => {
				t.add('更新内容:\n1.重置了json编辑.\n2.修复了一堆bug.\n3.优化了mod，样式化了ui.').row();
				t.pane(cons(p => {
					for(var i = 0, image = Core.atlas.find(modName + '-update-beta2.0-' + ++i); (image + '') != 'error'; image = Core.atlas.find(modName + '-update-beta2.2-' + ++i)) p.image(image).size(w, h);
				})).width(Core.graphics.getWidth() * .9);
			}))).fillX();
		})).width(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) - 120).maxHeight(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) * .75).padLeft(20).style(style);
		t.row();
		t.table(cons(_t => {
			_t.button('$ok', run(() => t.hide())).size(120, 50);
			_t.check('不再显示', new Boolc({get:b => Core.settings.put(modName + '-不再显示', b)}));
		}));
		t.show();
	}

	const frag = extend(Table, {
		cont:new Table,
		lastx:0, lasty:0
	});
	frag.image().color(Color.sky).margin(0).pad(0).padBottom(-4).fillX().height(40).get().addListener(extend(InputListener, {
		touchDown(event, x, y, pointer, button){
			this.bx = x;
			this.by = y;
			return true;
		},
		touchDragged(event, x, y, pointer){
			let v = frag.localToStageCoordinates(Tmp.v1.set(x, y));
			frag.lastx = -this.bx + v.x;
			frag.lasty = -this.by + v.y;
		}
	}));
	frag.row();
	frag.table(Tex.button, cons(t => {
		for(let i in elements){
			let k = i;
			elements[k].name = k;
			if('load' in elements[k]) elements[k].load();
			elements[k].style = [style, _style];
			t.button(k, Styles.cleart, run(() => {
				frag.cont.clear();
				elements[k].buildConfiguration(frag.cont);
			})).size(120, 40).row();
		}
	})).row();
	frag.table(Styles.black3, cons(t => t.add(frag.cont))).fillX();
	frag.left().bottom().margin(10);
	frag.lastx = frag.x;
	frag.lasty = frag.y;
	frag.update(run(() => {
		/*frag.color.a = +Vars.state.isMenu() ^ 1;
		frag.touchable = Vars.state.isMenu() ? Touchable.disabled : Touchable.enabled;*/
		frag.setPosition(frag.lastx < 0 ? 0 : frag.lastx > Core.graphics.getWidth() * .8 ? Core.graphics.getWidth() - Object.keys(elements).length * 120 : frag.lastx, frag.lasty < 0 ? 0 : frag.lasty > Core.graphics.getHeight() - 100 ? Core.graphics.getHeight() * .8 : frag.lasty);
	}));
	Core.scene.add(frag);
	window.style = [style, _style];
}))