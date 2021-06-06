
exports.forIn = function(_obj, cons, fun) {
	let arr = [];
	let obj = Object.assign(_obj != null ? _obj : {});
	for (let k in obj) {
		if (k == 'factory') continue;
		try {
			if (cons != null && !cons(k, obj)) continue;
			let type = typeof obj[k];
			obj[k] = type == 'boolean' ? !!obj[k] : type == 'string' ? '' + obj[k] : type == 'number' ? +obj[k] :
				type == 'function' ? (() => {}) : obj[k];
		} catch (e) {
			continue;
		}
		if (fun != null) fun(k, obj);
		arr.push(k);
	}
	return arr;
}

/* 一个文本域，可复制粘贴 */
exports.showTextArea = function(text) {
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

/* hjson解析（暂时没啥用，约等于eval） */
exports.HjsonParse = function(str) {
	if (typeof str !== 'string') return str;
	if (str == '') return {};
	try {
		return eval('(' + str + ')');
	} catch (err) {
		Vars.ui.showErrorMessage(err);
		return {};
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

/* 选择文件 */
exports.selectFile = function(open, purpose, ext, cons){
	purpose = /^\$|\@$/.test(purpose[0]) ? Core.bundle.get(purpose.substr(1), purpose) : purpose;

	Vars.platform.showFileChooser(open, purpose + ' (.' + ext + ')', ext, new Cons({get:fi => {
		try {
			cons.get(fi);
		} catch (err) {
			Log.err('thorw error when failed to select file: ', err);
		}
	}}));
}

// 一个双击函数
exports.doubleClick = function(table, runs) {
	table.addListener(extend(ClickListener, {
		clickTimes: 0,
		clicked(a, b, c) {
			if (++this.clickTimes == 2) {
				runs[0].run();
			} else if (this.clickTimes == 1) Time.runTask(24, run(() => {
			 	this.clickTimes = 0;
			 	runs[1].run();
			}));
		}
	}));

	return table;
}; 


/* 顾名思义 */
exports.showSelectTable = function(button, fun, searchable) {
	if (typeof fun != 'function') return null;
	let t = extend(Table, {
		getPrefHeight(){
			return Math.min(this.super$getPrefHeight(), Core.graphics.getHeight());
		},
		getPrefWidth(){
			return Math.min(this.super$getPrefWidth(), Core.graphics.getWidth());
		}
	});
	t.margin(4);
	t.setBackground(Tex.button);

	let b = button;
	let hitter = new Element;
	let hide = run(() => {
		hitter.remove();
		t.actions(Actions.fadeOut(0.3, Interp.fade), Actions.remove());
		t.remove();
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
		})).padRight(8).fillX().fill().top().row();
	}

	let pane = t.top().pane(cons(p => fun(p.top(), hide, ''))).pad(0).top().get();
	pane.setScrollingDisabled(true, false);

	let p = pane.getWidget();

	t.pack();

	return t;
}

/* 顾名思义 */
exports.showSelectImageTable = function(button, content, current, size, imageSize, cons, cols, searchable){
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

			let b = p.button(new TextureRegionDrawable(cont.icon(Cicon.small)), Styles.cleari, imageSize, run(() => {
				cons.get(current = cont);
				hide.run();
			})).size(size).checked(boolf(() => cont == current)).get();

			if (++c % cols == 0) p.row();
		}
	}, searchable);
}


/* 构建一个物品/液体堆 */
exports.buildOneStack = function(t, type, stack, content, amount){
	let output = [];

	t.add('$' + type);
	
	let field = t.field(content || (stack[0] instanceof UnlockableContent ? stack[0].name : stack[0]), cons(text => {})).get();
	field.update(run(() => output[0] = field.getText()));
	let btn = t.button('', Icon.pencilSmall, Styles.logict, run(() => {
		this.showSelectImageTable(btn, stack, output[0], 40, 32, cons(item => {
			field.setText(item.name);
		}), 6, true);
	})).size(40).padLeft(-1).get();
		
	t.add('$amount');
	let atf = t.field('' + (amount | 0), cons(t => {})).get();
	atf.update(() => output[1] = atf.getText() | 0);

	return output;
}

/* 构建n个物品/液体堆 */
exports.buildMultipleStack =  function(name, stack, v, t){
	let lastI = -1;

	let buttons = t.table().left().get();
	t.row();

	var fun = (item, amount) => buttons.table(cons(t => {
		let i = ++lastI;

		v[i] = this.buildOneStack(t, name, stack, item, amount);
	
		t.button('', Icon.trash, Styles.cleart, run(() => {
			t.remove();
			v.splice(i, 1);
		}));
	})).left().row();

	t.button('$add', run(() => fun('', 0))).row();

	for (let i of v) {
		fun(i[0], i[1]);
	}
}


// const items = Vars.content.items().toArray(), liquids = Vars.content.liquids().toArray();

/* 构建table */
exports.buildContent = function(obj, arr) {
	let [ts, t, i, k, v] = arr;
	let type = obj.type;

	switch (true) {
		case(/color/i.test(k) && (v == null || v instanceof Color ||
			/^#?([\da-f][\da-f][\da-f][\da-f][\da-f][\da-f]|[\da-f][\da-f][\da-f][\da-f][\da-f][\da-f][\da-f][\da-f])$/i
			.test('' + v))):
			let color = Color.valueOf(obj[k]);
			let button = t.button(cons(b => {}), run(() => Vars.ui.picker.show(color, cons(c => {
				image.color(color = c);
				field.setText(color + '');
			})))).get();

			let image = button.image().size(30).color(color);
			let field = button.add('' + color).get();

			obj[k] = {
				toString:() => '"' + color + '"'
			}
			break;
		case(k == 'requirements'):
			// 建造消耗
			/* if (v instanceof String) {
				try {
					v = eval('(' + v + ')')
				} catch (e) {
					v = [];
				}
			} */
			v = v || [];
			v = v.map(i => typeof i == 'string' ? i.split('/') : [i.item, i.amount]);
			t.add('[').row();

			this.buildMultipleStack('item', Vars.content.items().toArray(), v, t);

			obj[k] = {
				toString:() => '[' + v.map(e => '"' + e[0] + '/' + e[1] + '"') + ']'
			}

			t.add(']');
			break;

		case(k == 'outputItem'):{
			v = v == null || v instanceof ItemStack ? {item:'copper', amount:1} : v;
			
			t.row();
			let stack;
			t.table(cons(t => {
				t.left();
				stack = this.buildOneStack(t, 'item', Vars.content.items().toArray(), v.item, v.amount);
			})).fillX().left();

			obj[k] = {
				toString:() => '{"item": "' + stack[0] + '", "amount": ' + stack[1] + '}'
			}

			break;
		}
		case(k == 'outputLiquid'):{
			v = v == null || v instanceof LiquidStack ? {liquid:'water', amount:1} : v;
			
			t.row();
			let stack;
			t.table(cons(t => {
				t.left();
				stack = this.buildOneStack(t, 'liquid', Vars.content.liquids().toArray(), v.liquid, v.amount);
			})).fillX().left();
		
			obj[k] = {
				toString:() => '{"liquid": "' + stack[0] + '", "amount": ' + stack[1] + '}'
			}
		
			break;
		}

		case(/^unit(Type)?$/.test(k)):{
			v = '' + v || 'mono';

			let field = t.field(v, cons(text => {})).get();
			field.update(run(() => v = field.getText()));

			let btn = t.button('', Icon.pencilSmall, Styles.logict, run(() => {
				this.showSelectImageTable(btn, Vars.content.units().toArray(), v, 40, 32, cons(u => {
					field.setText(v = u.name);
				}), 6, true);
			})).size(40).padLeft(-1).get();

			obj[k] = {
				toString:() => '"' + v + '"'
			}
			break;
		}

		/* case(k == 'ammoTypes'):{

		} */

		case(k == 'consumes'):
			v = v == null || v instanceof Consumers ? {
				items: {
					items: []
				},
				liquid: {}
			} : v;

			t.row();
			t.table(cons(t => {
				t.left().add('$item');
				t.add(': {').row();
			})).left().row();

			t.table(cons(t => {
				let stack = v.items.items.map(i => typeof i == 'string' ? i.split('/') : [i.item, i.amount]);
				this.buildMultipleStack('item', Vars.content.items().toArray(), stack, t);
				t.update(() => {
					v.items.items = stack.map(e => e[0] + '/' + e[1]);
				})
			})).left().row();

			t.add('}').row();
			
			t.table(cons(t => {
				t.left().add('$liquid');
				t.add(': {').row();
			})).left().row();

			t.table(cons(t => {
				t.left();
				t.update(() => {
					if (stack[0] != '') {
						v.liquid.liquid = stack[0];
						v.liquid.amount = stack[1];
					}else v.liquid = {};
				});

				let stack = this.buildOneStack(t, 'liquid', Vars.content.liquids().toArray(), v.liquid.liquid || 'water', v.liquid.amount || 0);
			})).fillX().left().row();

			t.add('}').row();
			obj[k] = {
				toString:() => JSON.stringify(v)
			};

			break;
		case(k == 'category'):
			let category = obj[k] || 'distribution';
			let all = Category.all;
			let btn = t.button(category, Styles.cleart, run(() => {
				this.showSelectTable(btn, (p, hide) => {
					p.clearChildren();

					for (let i = 0; i < all.length; i++) {
						let cat = all[i];
						p.button(cat, Styles.cleart, run(() => {
							btn.setText(category = cat);
							hide.run();
						})).size(130, 50).disabled(category == cat).row();
					}
				}, false);
			})).size(130, 45).get();
			obj[k] = {
				toString:() => '"' + category + '"'
			}
			break;
		default:
			/* t.add('"');
			obj[k] = '"' + v + '"';
			t.field('' + v, cons(text => obj[k] = '"' + text + '"'));
			t.add('"'); */

			v = typeof v == 'string' ? v : JSON.stringify(v);
			obj[k] = {
				toString() {
					let str = ('' + v).replace(/^\s||\s$/g, '');
					if ((str[0] == '[' && v[str.length - 1] == ']') || (str[0] == '{' && v[str.length - 1] == '}')) {
						try {
							v = eval('(' + str + ')');
						} catch(e) {};
					}
					return JSON.stringify(v);
				}
			}
			t.field(v, cons(text => v = text));
	}
}

