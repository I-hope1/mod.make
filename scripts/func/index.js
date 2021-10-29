
const IntCons = require('func/constructor');

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
		t.button('$back', Icon.left, run(() => dialog.hide())).size(w / 3 - 25, h * 0.05);
		t.button('$edit', Icon.edit, run(() => {
			let dialog = new Dialog('');
			dialog.addCloseButton();
			dialog.table(Tex.button, cons(t => {
				let style = Styles.cleart;
				t.defaults().size(280, 60).left();
				t.row();
				t.button("@schematic.copy.import", Icon.download, style, run(
					() => {
						dialog.hide();
						text2.setText(Core.app.getClipboardText());
					})).marginLeft(12);
				t.row();
				t.button("@schematic.copy", Icon.copy, style, run(() => {
					dialog.hide();
					Core.app.setClipboardText(text2.getText()
						.replace(
							/\r/g, '\n'));
				})).marginLeft(12);
			}));
			dialog.show();
		})).size(w / 3 - 25, h * 0.05);
		t.button('$ok', Icon.ok, run(() => {
			dialog.hide();
			text1.setText(text2.getText().replace(/\r/g, '\\n'));
		})).size(w / 3 - 25, h * 0.05);
	}));
	dialog.show();
}

/* hjson解析 (使用arc的JsonReader) */
exports.HjsonParse = function (str) {
	if (typeof str !== 'string') return str;
	if (str == '') return new IntCons.Object();
	if (str.replace(/^\s+/, '')[0] != '{') str = '{' + str + '}'
	try {
		let obj1 = (new JsonReader).parse(str), arr = [],
			obj2 = output = new IntCons.Object();
		while (true) {
			for (let i = obj1.size; --i >= 0;) {
				let child = obj1.get(i);
				if (child.isArray()) {
					let array = new IntCons.Array()
					if (obj2 instanceof Array) obj2.push(array)
					else obj2.put(child.name, array)
					arr.push(child, array);
					continue
				}
				if (child.isObject()) {
					let obj = new IntCons.Object()
					if (obj2 instanceof Array) obj2.push(obj)
					else obj2.put(child.name, obj)
					arr.push(child, obj);
					continue
				}

				let value = obj1.getString(i)
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
		return new IntCons.Object();
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
		for (var f of all) {
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
			Log.err('thorw error when failed to select file: ', err);
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
exports.longPress = function (elem, duration, func) {
	elem.addListener(extend(ClickListener, {
		clicked(a, b, c) {
			func(Time.millis() - this.visualPressedTime > duration)
		}
	}));

	return elem;
}


/* 顾名思义 */
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
			return Core.app.post(run(() => {
				hitter.remove();
				t.remove();
			}));
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
			text.changed(run(() => fun(p, hide, text.getText())));
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


exports.showSelectListTable = function(button, list, current, width, height, cons, searchable){
	this.showSelectTable(button, (p, hide) => {
		p.clearChildren();

		for (let i = 0; i < list.length; i++) {
			let item = list[i];
			p.button('' + item, Styles.cleart, run(() => {
				cons.get(item)
				hide.run();
			})).size(width, height).disabled(current == item).row();
		}
	}, searchable);
}

/* 顾名思义 */
exports.showSelectImageTable = function (button, content, current, size, imageSize, cons, cols, searchable) {
	return this.showSelectTable(button, (p, hide, v) => {
		p.left();
		p.clearChildren();

		let reg = RegExp(v, 'i');
		let c = 0;
		for (let i = 0; i < content.length; i++) {
			let cont = content[i];
			if (typeof current == 'string' && current == cont.name) current = cont;
			// 过滤不满足条件的
			if (!(reg.test(cont.name) || reg.test(cont.localizedName))) continue;

			p.button(new TextureRegionDrawable(cont.icon(Cicon.small)), Styles.cleari, imageSize, run(() => {
				cons.get(current = cont);
				hide.run();
			})).size(size).checked(boolf(() => cont == current))

			if (++c % cols == 0) p.row();
		}
	}, searchable);
}

exports.selectionWithField = function(table, items, current, size, imageSize, func, cols, searchable){
	let field = new TextField(current);
	table.add(field).fillX()
	let btn = table.button('', Icon.pencilSmall, Styles.logict, run(() => {
		IntFunc.showSelectImageTable(btn, items, current, size, imageSize, cons(item => field.setText(func.get(item))), cols, searchable);
	})).size(40).padLeft(-1).get();

	return prov(() => field.getText())
}

let cont = Packages.mindustry.ctype.UnlockableContent
/* 构建一个物品/液体堆 */
exports.buildOneStack = function (t, type, stack, content, amount) {
	let output = [];

	t.add('$' + type);

	content = content || stack[0]
	let field = t.field(content instanceof cont ? content.name : '' + content, cons(text => { })).get();
	output[0] = prov(() => field.getText());
	let btn = t.button(Icon.pencilSmall, Styles.logici, run(() => {
		this.showSelectImageTable(btn, stack, output[0].get(), 40, 32, cons(item => {
			field.setText(item.name);
		}), 6, true);
	})).size(40).padLeft(-1).get();

	t.add('$amount');
	let atf = t.field('' + (amount | 0), cons(t => { })).get();
	output[1] = prov(() => atf.getText() | 0);

	return output;
}

/* 构建n个物品/液体堆 */
exports.buildMultipleStack = function (background, name, stack, v, t) {
	let lastI = -1, output = [];

	let _t = t.table(background instanceof Drawable ? background : Styles.none).get();
	let buttons = _t.table().left().get();
	_t.row();

	var fun = (item, amount) => buttons.table(cons(t => {
		let i = ++lastI;

		output[i] = this.buildOneStack(t, name, stack, item, amount);

		t.button('', Icon.trash, Styles.cleart, run(() => {
			t.remove();
			output.splice(i, 1);
		}));
	})).left().row();

	_t.button('$add', run(() => fun('', 0))).growX().row();

	for (let i of v) {
		fun(i[0], i[1]);
	}

	return output;
}


// const items = Vars.content.items().toArray(), liquids = Vars.content.liquids().toArray();
