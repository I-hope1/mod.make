
const { caches: { types: typesIni } } = require('func/IniHandle')


exports.mod = Vars.mods.locateMod(modName)
/* 转换为可用的class */
exports.toClass = function (_class) {
	return Seq([_class]).get(0)
}

/* 一个文本域，可复制粘贴 */
exports.showTextArea = function (text) {
	let dialog = new BaseDialog('');
	dialog.title.remove()
	let area
	dialog.cont.add(area = new TextArea(text.getText().replace(/\\n/g, '\n'))).grow();
	if (Vars.mobile) area.removeInputDialog()

	dialog.addCloseListener();
	dialog.buttons.defaults().growX()
	dialog.buttons.button("@back", Icon.left, () => dialog.hide()).grow()

	dialog.buttons.button("@edit", Icon.edit, () => {
		let dialog = new Dialog('');
		dialog.addCloseButton();
		dialog.table(Tex.button, cons(t => {
			let style = Styles.cleart;
			t.defaults().size(280, 60).left();
			t.row();
			t.button("@schematic.copy.import", Icon.download, style, () => {
				dialog.hide();
				area.setText(Core.app.getClipboardText().replace(/\r/g, '\n'));
			}).marginLeft(12);
			t.row();
			t.button("@schematic.copy", Icon.copy, style, () => {
				dialog.hide();
				Core.app.setClipboardText(area.getText()
					.replace(
						/\r/g, '\n'));
			}).marginLeft(12);
		}));
		dialog.closeOnBack();
		dialog.show();
	}).grow();
	dialog.buttons.button("@ok", Icon.ok, () => {
		dialog.hide();
		text.setText(area.getText().replace(/\r|\n/g, '\\n'));
	}).grow();

	dialog.show();
}

exports.async = function (text, generator, callback) {
	Vars.ui.loadfrag.show(text);
	let ui = new Element()
	let v, t = 0;
	ui.update(() => {
		if (++t < 2) return;
		t = 0
		try {
			v = generator.next()
		} catch (err) {
			this.showException(err)
			v = null;
		}
		if (v == null || v.done) {
			ui.update(null)
			Vars.ui.loadfrag.hide()
			callback(v)
		}
	})
	Core.scene.add(ui);
}

exports.showException = function (err, text) {
	text = text || "";

	let ui = new Dialog("");
	let { cont } = ui
	let message = err.message;

	ui.setFillParent(true);
	cont.margin(15);
	cont.add("@error.title").colspan(2);
	cont.row();
	cont.image().width(300).pad(2).colspan(2).height(4).color(Color.scarlet);
	cont.row();
	cont.add((text.startsWith("@") ? Core.bundle.get(text.substring(1)) : text) + (message == null ? "" : "\n[lightgray](" + message + ")")).colspan(2).wrap().growX().center().get().setAlignment(Align.center);
	cont.row();

	let col = new Collapser(base => base.pane(t => t.margin(14).add(err.stack).color(Color.lightGray).left()), true);

	cont.button("@details", Styles.togglet, () => col.toggle()).size(180, 50).checked(b => !col.isCollapsed()).fillX().right();
	cont.button("@ok", () => ui.hide()).size(110, 50).fillX().left();
	cont.row();
	cont.add(col).colspan(2).pad(2);
	ui.closeOnBack();

	ui.show();
}


Events.run(ClientLoadEvent, () => exports.errorRegion = Core.atlas.find("error"))

// 查找图片
exports.find = function (mod, name) {
	let fi = mod.spritesFi();
	return fi != null ? this.findSprites(fi, name) : this.errorRegion
}
exports.findSprites = function (all, name) {
	let region = this.errorRegion
	try {
		all.walk(cons(f => {

			if (f.name() == name + ".png") {
				region = new TextureRegion(new Texture(f));
				throw '';
			}
		}))
	} catch (e) { }
	return region
}

/* 选择文件 */
exports.selectFile = function (open, purpose, ext, _cons) {
	purpose = /^\$|\@$/.test(purpose[0]) ? Core.bundle.get(purpose.substr(1), purpose) : purpose;

	Vars.platform.showFileChooser(open, purpose + ' (.' + ext + ')', ext, cons(fi => {
		try {
			_cons.get(fi);
		} catch (err) {
			Log.err('throw error when failed to select file: ', err);
		}
	}));
}

// 一个双击函数
exports.doubleClick = function (elem, runs) {
	elem.addListener(extend(ClickListener, {
		clickTime: 0,
		clicked(a, b, c) {
			if (this.tapCount == 2) {
				runs[0].run();
				this.tapCount = 0;
			} else if (this.tapCount == 1) {
				if (++this.clickTime == 2) {
					runs[1].run();
					this.tapCount = 0;
				}
			};
		}
	}));

	return elem;
};
// 长按事件
exports.longPress = function (elem, duration, func) {
	elem.addListener(extend(ClickListener, {
		clicked(a, b, c) {
			func(Time.millis() - this.visualPressedTime > duration)
		}
	}))

	return elem;
}

exports.searchTable = function (t, fun) {
	t.table(cons(t => {
		t.image(Icon.zoom);
		let text;
		t.add(text = new TextField).fillX();
		text.changed(() => fun(p, text.getText()));
		/* 自动聚焦到搜索框 */
		if (Core.app.isDesktop() && text != null) {
			Core.scene.setKeyboardFocus(text);
		}
	})).padRight(8).fillX().fill().top().row();

	let pane = t.top().pane(cons(p => fun(p.top(), ''))).pad(0).top().get();
	pane.setScrollingDisabled(true, false);

	let p = pane.getWidget();

	t.pack();
}

/**
 * 弹出一个小窗，自己设置内容
 * @param button 用于定位弹窗的位置
 * @param fun
 * * @param p 是Table，你可以添加元素
 * * @param hide 是一个函数，调用就会关闭弹窗
 * * @param text 如果 @param 为 true ，则启用。用于返回用户在搜索框输入的文本
 * @param searchable 可选，启用后会添加一个搜索框
 */
exports.showSelectTable = function (button, fun, searchable) {
	if (typeof fun != 'function') return null;
	let t = extend(Table, {
		getPrefHeight() {
			return Math.min(this.super$getPrefHeight(), Core.graphics.getHeight());
		},
		getPrefWidth() {
			return Math.min(this.super$getPrefWidth(), Core.graphics.getWidth())
		}
	});
	t.margin(4);
	t.setBackground(Tex.button);

	let b = button;
	let hitter = new Element;
	let hide = run(() => {
		hitter.remove()
		t.actions(Actions.fadeOut(0.3, Interp.fade), Actions.remove())
	});
	hitter.fillParent = true;
	hitter.clicked(hide);

	Core.scene.add(hitter);
	Core.scene.add(t);

	t.update(() => {
		if (b.parent == null || !b.isDescendantOf(Core.scene.root)) {
			return Core.app.post(() => {
				hitter.remove();
				t.remove();
			});
		}

		b.localToStageCoordinates(Tmp.v1.set(b.getWidth() / 2, b.getHeight() / 2));
		t.setPosition(Tmp.v1.x, Tmp.v1.y, Align.center);
		if (t.getWidth() > Core.scene.getWidth()) t.setWidth(Core.graphics.getWidth());
		if (t.getHeight() > Core.scene.getHeight()) t.setHeight(Core.graphics.getHeight());
		t.keepInStage();
		t.invalidateHierarchy();
		t.pack();
	});
	t.actions(Actions.alpha(0), Actions.fadeIn(0.3, Interp.fade));

	if (searchable) {
		t.table(cons(t => {
			t.image(Icon.zoom);
			let text;
			t.add(text = new TextField).fillX();
			text.changed(() => fun(p, hide, text.getText()));
			/* 自动聚焦到搜索框 */
			if (Core.app.isDesktop() && text != null) {
				Core.scene.setKeyboardFocus(text);
			}
		})).padRight(8).fillX().fill().top().row();
	}

	let pane = t.top().pane(cons(p => fun(p.top(), hide, ''))).pad(0).top().get();
	pane.setScrollingDisabled(true, false);

	let p = pane.getWidget();

	t.pack();

	return t;
}


exports.showSelectListTable = function (button, list, current, width, height, _cons, searchable) {
	if (!(list instanceof Seq)) throw TypeError("'" + list + "' isn't instanceof Seq")
	this.showSelectTable(button, (p, hide, text) => {
		p.clearChildren();

		let reg = new RegExp(text, 'i')
		list.each(boolf(item => reg.test(item)), cons(item => {
			p.button(typesIni.get(item + '') || item + '', Styles.cleart, () => {
				_cons.get(item)
				hide.run();
			}).size(width, height).disabled(current == item).row();
		}))
	}, searchable);
}

/**
 * 弹出一个可以选择内容的窗口（类似物品液体源的选择）
 * （需要提供图标）
 * @param content 用于展示可选的内容
 * @param icons 可选内容的图标
 * @param current 选中的内容，null就没有选中任何
 * @param size 每个内容的元素大小
 * @param imageSize 每个内容的图标大小
 * @param cons 选中内容就会调用
 * * @param cont 提供选中的内容
 * @param cols 一行的元素数量
 */
exports.showSelectImageTableWithIcons = function (button, items, icons, current, size, imageSize, cons, cols, searchable) {
	if (!(items instanceof Seq)) throw TypeError("'" + items + "' isn't a Seq")

	return this.showSelectTable(button, (p, hide, v) => {
		p.left();
		p.clearChildren();
		let group = new ButtonGroup();
		group.setMinCheckCount(0);
		p.defaults().size(size);

		let reg = RegExp(v, 'i');
		for (let i = 0; i < items.size;) {
			let cont = items.get(i)
			if (typeof current == 'string' && cont instanceof UnlockableContent && current == cont.name) current = cont;
			// 过滤不满足条件的
			if (v != '' && !(reg.test(cont.name) || reg.test(cont.localizedName))) return;

			let btn = p.button(Tex.whiteui, Styles.clearToggleTransi, imageSize, () => {
				cons.get(current = cont);
				hide.run();
			}).size(size).get()
			btn.getStyle().imageUp = icons[i];
			btn.update(() => btn.setChecked(cont == current))

			if (++i % cols == 0) p.row();
		}
	}, searchable);
}

// 弹出一个可以选择内容的窗口（无需你提供图标）
exports.showSelectImageTable = function (button, items, current, size, imageSize, _cons, cols, searchable) {
	if (!(items instanceof Seq)) throw TypeError("'" + items + "' isn't a Seq")

	let icons = []
	items.each(cons(item => {
		icons.push(new TextureRegionDrawable(item.uiIcon))
	}))
	return this.showSelectImageTableWithIcons(button, items, icons, current, size, imageSize, _cons, cols, searchable)
}

exports.selectionWithField = function (table, items, current, size, imageSize, cols, searchable) {
	let field = new TextField(current);
	table.add(field).fillX()
	let btn = table.button(Icon.pencilSmall, Styles.clearFulli, () => {
		this.showSelectImageTable(btn, items, current, size, imageSize, cons(item => field.setText(item.name)), cols, searchable);
	}).size(40).padLeft(-1).get();

	return prov(() => field.getText())
}


// const items = Vars.content.items().toArray(), liquids = Vars.content.liquids().toArray();
