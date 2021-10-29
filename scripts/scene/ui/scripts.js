
function build(){
	let table = this.table = extend(Table, {
		main: this,
	});
	table.table(Tex.whiteui, cons(t => {
		let main = table.main;
		if (main == null) return null;
		t.color.set(main.color);
		t.margin(6);
		t.add('$' + main.type).style(Styles.outlineLabel).color(Color.white).padRight(8);
		t.image().height(1).growX().color(Color.gray);
		t.button(Icon.copy, Styles.logici, run(() => t.parent.add(main.build()))).padRight(6);
		t.button(Icon.cancel, Styles.logici, run(() => main.remove()));
		t.row();
		t.table(Styles.black, cons(t => {
			t.left();
			t.marginLeft(4);
			t.marginBottom(7);
			main.buildChildren(t);
		})).width(Core.graphics.getWidth() * .6).pad(4).padTop(3).left().growX();
	})).width(Core.graphics.getWidth() * .7).pad(4).padTop(2).left().growX();
}

exports.defined = function () {
	build.call(this)
	this.build.call(this);
	this.type = 'defined';
	this.color = Color.sky;
	this.name = 'result';
	this.value = '"value"';
	this.toString = function () {
		return 'let ' + this.name + ' = ' + this.value + ';';
	};
	this.buildChildren = function (table) {
		table.add('$name');
		let _name = table.add(new TextField(this.name)).get();
		table.add(' = ');
		table.add('$value');
		let _value = table.add(new TextField(this.value)).get();
		table.update(run(() => {
			this.name = _name.getText();
			this.value = _value.getText();
		}));
	};
	this.remove = function () {
		if (this.table != null) this.table.remove();
	}
}