
const { MyObject, MyArray } = require('func/constructor');

const forIn = exports.forIn = function (obj, str, method) {
	var jg = [], method = method instanceof Function ? method : str => str;
	for (var k in obj) {
		try {
			let str = (obj[k] instanceof Array ? '[ ' + obj[k] + ' ]' : obj[k])
			jg.push(k + ': ' + method(str))
		} catch (e) {
			Log.err(k)
		}
	}
	return typeof str == 'string' ? jg.join(str) : jg;
}


/* 转换为可用的class */
exports.toClass = function (_class) {
	return Seq([_class]).get(0)
}

/* 一个文本域，可复制粘贴 */
exports.showTextArea = function (text) {
	let dialog = new Dialog('');
	let w = Core.graphics.getWidth(),
		h = Core.graphics.getHeight();
	let text1 = text,
		text2 = dialog.cont.add(new TextArea(text.getText())).size(w * 0.85, h * 0.75).get();
	dialog.buttons.table(cons(t => {
		t.button('$back', Icon.left, () => dialog.hide()).size(w / 3 - 25, h * 0.05);
		t.button('$edit', Icon.edit, () => {
			let dialog = new Dialog('');
			dialog.addCloseButton();
			dialog.table(Tex.button, cons(t => {
				let style = Styles.cleart;
				t.defaults().size(280, 60).left();
				t.row();
				t.button("@schematic.copy.import", Icon.download, style, () => {
					dialog.hide();
					text2.setText(Core.app.getClipboardText());
				}).marginLeft(12);
				t.row();
				t.button("@schematic.copy", Icon.copy, style, () => {
					dialog.hide();
					Core.app.setClipboardText(text2.getText()
						.replace(
							/\r/g, '\n'));
				}).marginLeft(12);
			}));
			dialog.show();
		}).size(w / 3 - 25, h * 0.05);
		t.button('$ok', Icon.ok, () => {
			dialog.hide();
			text1.setText(text2.getText().replace(/\r/g, '\\n'));
		}).size(w / 3 - 25, h * 0.05);
	}));
	dialog.show();
}

/* hjson解析 (使用arc的JsonReader) */
exports.HjsonParse = function (str) {
	if (typeof str !== 'string') return str;
	if (str == '') return new MyObject();
	if (str.replace(/^\s+/, '')[0] != '{') str = '{' + str + '}'
	try {
		return (new JsonReader).parse(str)
		let obj1 = (new JsonReader).parse(str), arr = [];
		let output, obj2 = output = new MyObject();
		while (true) {
			for (let child = obj1.child; child != null; child = child.next) {
				if (child.isArray()) {
					let array = new MyArray()
					if (obj2 instanceof Array) obj2.push(array)
					else obj2.put(child.name, array)
					arr.push(child, array);
					continue
				}
				if (child.isObject()) {
					let obj = new MyObject()
					if (obj2 instanceof Array) obj2.push(obj)
					else obj2.put(child.name, obj)
					arr.push(child, obj);
					continue
				}

				let value = child.asString()
				if (child.isNumber()) value *= 1
				if (child.isBoolean()) value = value == 'true'
				if (obj2 instanceof Array) obj2.push(value)
				else obj2.put(child.name, value)
			}
			if (arr.length == 0) break
			obj1 = arr.shift()
			obj2 = arr.shift()
		}
		return output;
	} catch (err) {
		Vars.ui.showErrorMessage(err);
		return new MyObject();
	};
	// hjson = hjson.replace(/\s/g, '')[0] != '{' ? '{' + hjson + '}' : hjson;
	/* try {
		let string = (new Packages.arc.util.serialization.JsonReader).parse(str.replace(/\s/g, '')[0] != '{' ?
			'{\n' +
			str + '}' : str);
		let obj = {};
		for (let i = 0; i < string.size; i++) {
			let arr = ('' + string.get(i)).split(': ');
			let value = arr.join('');
			obj[arr.splice(0, i)] = value;
		}
		return obj;
	} catch (e) {
		Vars.ui.showErrorMessage(e);
		return {};
	} */
}

// 查找图片
exports.find = function (mod, name) {
	let error = Core.atlas.find('error');
	let all = mod.spritesAll();
	return all.length != 0 && (() => {
		for (let f of all) {
			if (f.name() == name + '.png') return new TextureRegion(new Texture(f));
		}
	})() || error;
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
	elem.clicked(cons(l => { }), cons(l => {
		func(Time.millis() - l.visualPressedTime > duration)
	}))

	return elem;
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
			// /* 自动聚焦到搜索框 */
			// text.fireClick();
		})).padRight(8).fillX().fill().top().row();
	}

	let pane = t.top().pane(cons(p => fun(p.top(), hide, ''))).pad(0).top().get();
	pane.setScrollingDisabled(true, false);

	let p = pane.getWidget();

	t.pack();

	return t;
}


exports.showSelectListTable = function (button, list, current, width, height, cons, searchable) {
	this.showSelectTable(button, (p, hide, text) => {
		p.clearChildren();

		let reg = new RegExp(text, 'i')
		list.each(boolf(item => reg.test(item)), new Cons({
			get: item => {
				p.button('' + item, Styles.cleart, () => {
					cons.get(item)
					hide.run();
				}).size(width, height).disabled(current == item).row();
			}
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
			if (typeof current == 'string' && current == cont.name) current = cont;
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
