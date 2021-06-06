
function showSelectTable(button, fun, find){
	if(typeof fun != 'function') return null;
	let t = new Table, b = button;
	let hitter = new Element;
	let hide = run(() => {
		hitter.remove();
		t.actions(Actions.fadeOut(0.3, Interp.fade), Actions.remove());
		Time.run(2, run(() => t.remove()));
	});
	hitter.fillParent = t.fillParent = true;
	hitter.clicked(hide);
	Core.scene.add(hitter);
	Core.scene.add(t);
	t.actions(Actions.alpha(0), Actions.fadeIn(0.3, Interp.fade));
	t.update(run(() => {
		if(b.parent == null || !b.isDescendantOf(Core.scene.root)){
			return Core.app.post(run(() => {
				hitter.remove();
				t.remove();
			}));
		}
		b.localToStageCoordinates(Tmp.v1.set(b.getWidth() / 2, b.getHeight() / 2));
		t.setPosition(Tmp.v1.x, Tmp.v1.y, Align.center);
		if(t.getWidth() > Core.scene.getWidth()) t.setWidth(Core.graphics.getWidth());
		if(t.getHeight() > Core.scene.getHeight()) t.setHeight(Core.graphics.getHeight());
		t.keepInStage();
	}));
	t.table(Tex.button, cons(t => {
		if(find){
			t.table(cons(t => {
				t.image(Icon.zoom);
				let text
				t.add(text = new TextField).fillX();
				text.changed(run(() => fun(p, hide, text.getText())));
			})).padRight(8).fillX().fill().top().row();
		}

		let p = t.pane(cons(p => fun(p, hide, ''))).maxSize(Core.graphics.getWidth() * .7, Core.graphics.getHeight() * .8).get().getWidget();
	}));
	return t;
}

elements[Core.bundle.get('effectConstructor.localizedName', '特效制作')] = {
	scripts:{
		'addTable':table => table.table(Tex.whiteui, cons(t => {
			let main = table.main;
			if(main == null) return null;
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
		})).width(Core.graphics.getWidth() * .7).pad(4).padTop(2).left().growX().get().parent,
		constructor(type){
			let obj = Object.assign(this);
			obj.type = type;
			obj.color = Color.valueOf('#ccccff');
			obj._type = Object.keys(eval(type))[0];
			let arr = eval(type)[obj._type].toString().replace(/function [^\/]*\/\*\n|\n\*\/\}/g, '').split('\n').map(e => e.replace(/void[^\(]*|\)/g, ''));
			obj.__type = arr[0];
			obj.build = function(){
				return this.addTable(this.table = extend(Table, {
					main:this
				}));
			};
			obj.buildChildren = function(table){
				table.add('$type');
				table.button('$' + this._type, Styles.logict, run(() => {
					
				}));
				this.values = Array(Math.max.apply(1, arr.map(e => e.split(',').length))).fill(new TextField);
				table.row();
				table.button(cons(b => {
					b.label(() => '$' + this.__type);
				}), IntStyles[1], run(() => {
					showSelectTable(table, (p, hide) => {
						for(let i in arr){
							let value = arr[i];
							p.button(value.split(',').map(e => Core.bundle.get(e, e)).join('\n'), style[1], run(() => {
								this.__type = value;
								hide.run();
							})).width(400);
						}
					});
				})).row();
				switch(this._type){
					case'color':
						switch(this.__type){
							case'float,float,float,float':
								eval(Array(4).fill('table.add(this.values[' + i + ']);').join(''));
						}
				}
			};
			obj.remove = function(){
				if(this.table != null) this.table.remove();
			};
			obj.toString = function(){
				return this.type + '.' + this._type + '(' + this.values.splice(0, this.__type.split(',').length) + ');';
			}
			return obj;
		}
	},
	load(){
		let ui = this.ui = new BaseDialog(this.name);
		let canvas = new Table;
		ui.cont.pane(cons(p => p.add(extend(Table, {
			draw(){
				try{
					eval((() => {
						let str = [];
						canvas.children.each(cons(() => str.push(str)));
						return str.join('');
					})());
				}catch(e){
					this.error = e;
				}
			}
		})))).size(300).fillX().row();
		ui.cont.pane(cons(p => p.label(() => this.error != null ? '' + this.error : ''))).row();
		ui.cont.pane(cons(p => p.add(canvas))).height(400).fillX().row();
		let arr = ['Draw', 'Lines', 'Fill', 'Drawf'];
		ui.cont.button(Icon.add, run(() => {
			showSelectTable(canvas, (p, hide) => {
				for(let i in arr){
					let value = arr[i];
					p.button(value.toLowerCase(), Styles.cleart, run(() => {
						let obj = this.scripts.constructor(value);
						obj.buildChildren(canvas);
						hide.run();
					})).size(200, 60).row();
				}
			});
		})).height(60).fillX();
		ui.addCloseButton();
	},
	buildConfiguration(table){
		this.ui.show();
	},
	evalMessage(){
		try{
			let print = text => log(this.block.localizedName, text);
			this.log = '' + eval(this.message);
		}catch(e){
			let str = e.message.replace(/\([^]*/, '');
			let arr = str.split(' '), arr2 = [];
			for(let i in arr){
				try{
					if(/number|string/i.test(typeof eval(arr[i]))) arr2.push(arr.splice(i, 1));
				}catch(e){continue;};
			}
			this.log = '[red][' + Core.bundle.get(e.name, e.name) + '][gray]:[white] ' + (!Core.bundle.has(arr.join('-')) ? str : arr2.length ? Core.bundle.format(arr.join('-'), eval('' + arr2)) : Core.bundle.get(arr.join('-'))).split(': ').map(e => Core.bundle.get(e, e)).join(': '); + '[#ccccff](#' + e.lineNumber + ')[]';
		}
	},
	read(stream, revision){
		this.super$read(stream, revision);
		this.message = stream.str();
	},
	write(stream){
		this.super$write(stream);
		stream.str('' + this.message);
	}
}
