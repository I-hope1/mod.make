
const findClass = require('func/findClass')
const { types: typesIni } = findClass("components.dataHandle");
const IntUI = findClass('IntUI')

exports.createDialog = IntUI.createDialog;
exports.doubleClick = IntUI.doubleClick;


exports.mod = Vars.mods.locateMod(modName)
/* 转换为可用的class */
exports.toClass = function (_class) {
	return Seq([_class]).get(0)
}

/* 一个文本域，可复制粘贴 */
exports.showTextArea = IntUI.showTextArea

exports.async = function (text, generator, callback, times) {
	Vars.ui.loadfrag.show(text);
	let el = new Element()
	times = times || 2;
	let v, t = 0;
	el.update(() => {
		if (t++ < times) return;
		try {
			v = generator.next()
		} catch (err) {
			this.showException(err)
			v = null;
		}
		if (v == null || v.done) {
			el.remove()
			el.update(null)
			Vars.ui.loadfrag.hide()
			callback(v)
		}
	})
	Core.scene.add(el);
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

// 长按事件
exports.longPress = IntUI.longPress

exports.searchTable = function (t, fun) {
	t.table(cons(t => {
		t.image(Icon.zoom);
		let text = new TextField();
		t.add(text).growX();
		text.changed(() => {
			fun(p, text.getText())
		});
		/* 自动聚焦到搜索框 */
		if (Core.app.isDesktop() && text != null) {
			Core.scene.setKeyboardFocus(text);
		}
	})).padRight(8).growX().fill().top().row();

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
	IntUI.showSelectTable(button, new Cons3({get: fun}), searchable);
}


exports.showSelectListTable = function (button, list, current, width, height, _cons, searchable) {
	return IntUI.showSelectListTable(button, list, () => current, _cons, width, height, searchable);
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
		let c = 0;
		for (let i = 0; i < items.size; i++) {
			let cont = items.get(i)
			if (typeof current == 'string' && cont instanceof UnlockableContent && current == cont.name) current = cont;
			// Log.info(v + "\n" + cont.name)
			// 过滤不满足条件的
			if (v != '' && !(reg.test(cont.name) || reg.test(cont.localizedName))) {
				continue;
			}

			let btn = p.button(Tex.whiteui, Styles.clearToggleTransi, imageSize, () => {
				cons.get(current = cont);
				hide.run();
			}).size(size).get()
			btn.getStyle().imageUp = icons[i];
			btn.update(() => btn.setChecked(cont == current))

			if (++c % cols == 0) p.row();
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
		this.showSelectImageTable(btn, items, field.getText(), size, imageSize, cons(item => field.setText(item.name)), cols, searchable);
	}).size(40).padLeft(-1).get();

	return prov(() => field.getText())
}


// const items = Vars.content.items().toArray(), liquids = Vars.content.liquids().toArray();

