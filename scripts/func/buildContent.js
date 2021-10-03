
const IntFunc = require('func/index');
/* 构建table */
exports.method = function(obj, arr) {
	let [ts, t, i, k, v] = arr;
	let type = obj.type;
	
	t.left().add(Core.bundle.get('content.' + k, k) + ':').fillX().left().padLeft(2).padRight(2);

	if (/color/i.test(k) && (v == null || v instanceof Color ||
		(() => {
			// 去除前面的#
			let color = ('' + v).replace(/^#?/, '');
			let i = 0, reg = /[\da-f]/;
			for (; reg.test(color); i++) {
				color = color.replace(reg, '');
			}
			return /^(3|6|8)$/.test(i + '');
		}))) {
		let color = Color.valueOf(obj[k]);
		let button = new Button;		
		/* 使用原本自带的采色器 */	
		button.clicked(() => Vars.ui.picker.show(color, cons(c => {
			image.color(color = c);
			field.setText(color + '');
		})));

		t.add(button);
	
		let image = button.image().size(30).color(color);
		let field = button.add('' + color).get();
	
		obj[k] = {
			toString:() => '"' + color + '"'
		}
	}
	else if (k == 'requirements') {
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
		t.add('[').fillX().left().row();
	
		IntFunc.buildMultipleStack(Styles.black5, 'item', Vars.content.items().toArray(), v, t);
	
		obj[k] = {
			toString:() => '[' + v.map(e => '"' + e[0] + '/' + e[1] + '"') + ']'
		}
	
		t.add(']').fillX().left();
	}
	
	else if (k == 'outputItem') {
		v = v == null || v instanceof ItemStack ? {item:'copper', amount:1} : v;
		
		t.row();
		let stack;
		t.table(cons(t => {
			t.left();
			stack = IntFunc.buildOneStack(t, 'item', Vars.content.items().toArray(), v.item, v.amount);
		})).fillX().left();
	
		obj[k] = {
			toString:() => '{"item": "' + stack[0] + '", "amount": ' + stack[1] + '}'
		}
	}

	else if (k == 'outputLiquid') {
		v = v == null || v instanceof LiquidStack ? {liquid:'water', amount:1} : v;
		
		t.row();
		let stack;
		t.table(cons(t => {
			t.left();
			stack = IntFunc.buildOneStack(t, 'liquid', Vars.content.liquids().toArray(), v.liquid, v.amount);
		})).fillX().left();
	
		obj[k] = {
			toString:() => '{"liquid": "' + stack[0] + '", "amount": ' + stack[1] + '}'
		}
	}

	else if (/^unit(Type)?$/.test(k)) {
		v = '' + v || 'mono';
	
		let field = t.field(v, cons(text => {})).get();
		field.update(run(() => v = field.getText()));
	
		let btn = t.button('', Icon.pencilSmall, Styles.logict, run(() => {
			IntFunc.showSelectImageTable(btn, Vars.content.units().toArray(), v, 40, 32, cons(u => {
				field.setText(v = u.name);
			}), 6, true);
		})).size(40).padLeft(-1).get();
	
		obj[k] = {
			toString:() => '"' + v + '"'
		}
	}

	/* else if (k == 'ammoTypes') {
		v = v.toArray()

		let contents = Vars.content[type == 'LiquidTurret' ? 'liquids' : 'items']().toArray();
		let btn = t.button('$add', () => IntFunc.showSelectImageTable(
			btn, contents, null, 40, 32, cons(item => {
			v[item.name] = {}
		}), 6, true)).get();

		obj[k] = {
			toString() {
				t.clear();
				t.remove();
				return JSON.stringify(v)
			}
		};
	} */
	
	else if (k == 'consumes') {
		v = v == null || v instanceof Consumers ? {
			items: {
				items: []
			},
			liquid: {}
		} : v;

		let liquidEnab = true;
		if (v.liquid == null) {
			v.liquid = {}
			liquidEnab = false;
		}

		t.row();
		t.table(cons(t => {
			t.left().add(Core.bundle.get('item') + ': {').row();
		})).left().row();

		let stack = v.items.items.map(i => typeof i == 'string' ? i.split('/') : [i.item, i.amount]);
		IntFunc.buildMultipleStack(Styles.black5, 'item', Vars.content.items().toArray(), stack, t);
		t.update(() => {
			v.items.items = stack.map(e => e[0] + '/' + e[1]);
		});
	
		t.add('}').fillX().left().row();
		
		t.table(cons(t => {
			t.left().add(Core.bundle.get('liquid') + ': {').row();
		})).left().row();

		t.table(cons(t => {
			t.left();
			t.check('$mod.enabled', liquidEnab, new Boolc({get:b => liquidEnab = b})).row();
			t.update(() => {
				if (!liquidEnab) return;
				if (stack[0] != '') {
					v.liquid.liquid = stack[0];
					v.liquid.amount = stack[1];
				} else v.liquid = {};
			});

			let stack;
			t.table(
				cons(t => stack = IntFunc.buildOneStack(t, 'liquid',
				Vars.content.liquids().toArray(), v.liquid.liquid || 'water', v.liquid.amount || 0))
			).visible(boolp(() => liquidEnab));
		})).fillX().left().row();
	
		t.add('}').fillX().left().row();
		obj[k] = {
			toString() {
				t.clear();
				t.remove();
				if (!liquidEnab) {
					delete v.liquid;
				}
				return JSON.stringify(v)
			}
		};
	}

	else if (k == 'category') {
		let category = obj[k] || 'distribution';
		let all = Category.all;
		let btn = t.button(category, Styles.cleart, run(() => {
			IntFunc.showSelectTable(btn, (p, hide) => {
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
	}

	else {
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
