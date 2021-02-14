
let sprites = {}, jsons = {};

function hjsonToJson(hjson){
	if(typeof hjson !== 'string') return hjson;
	try{
		return eval('(' + hjson + ')');
	}catch(e){};
	// hjson = hjson.replace(/\s/g, '')[0] != '{' ? '{' + hjson + '}' : hjson;
	try{
		let str = (new Packages.arc.util.serialization.JsonReader).parse(hjson.replace(/\s/g, '')[0] != '{' ? '{\n' + hjson + '}' : hjson);
		let obj = {};
		for(let i = 0; i < str.size; i++){
			let arr = (''+str.get(i)).split(': ');
			let value = arr.join('');
			obj[arr.splice(0, 1)] = value;
		}
		return obj;
	}catch(e){
		Vars.ui.showErrorMessage(e);
		return {};
	}
}
function find(mod, name){
	let error = Core.atlas.find('errors');
	if(!mod.exists()) return error;
	let all = sprites[mod.name()];
	return all != null && all.length != 0 && all.map(e => e.name()).indexOf(name + '.png') != -1 ? new Texture(all[index]) : error;
}

function showTextArea(text){
	let dialog = new Dialog('');
	let w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
	let text1 = text, text2 = dialog.cont.add(new TextArea(text.getText())).size(w * 0.85, h * 0.75).get();
	dialog.buttons.table(cons(t => {
		t.button('$back', Icon.left, run(() => dialog.hide())).size(w / 3 - 25, h * 0.05);
		t.button('$edit', Icon.edit, run(() => {
			let dialog = new Dialog('');
			dialog.addCloseButton();
			dialog.table(Tex.button, cons(t => {
				let style = Styles.cleart;
				t.defaults().size(280, 60).left();
				t.row();
				t.button("@schematic.copy.import", Icon.download, style, run(() => {
					dialog.hide();
					text2.setText(Core.app.getClipboardText());
				})).marginLeft(12);
				t.row();
				t.button("@schematic.copy", Icon.copy, style, run(() => {
					dialog.hide();
					Core.app.setClipboardText(text2.getText().replace(/\r/g, '\n'));
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
function TextField_JS(text, i, arr, t){
	arr.splice(i, 0, extend(TextField, {
		index:+i,
		toString(){
			return this.text;
		},
		/*paste(content, fireChangeEvent){
			if(content == null) return;
			let buffer = [];
			let textLength = this.text.length;
			if(this.hasSelection) textLength -= Math.abs(this.cursor - this.selectionStart);
			let data = style.font.getData();
			let field = this;
			let content = content.split(/\r|\n/);
			for(let i in content){
				if(i == 0) continue;
				TextField_JS(content[i], index + i, arr);
			}
			content = content[0];
			if(hasSelection) cursor = delete(fireChangeEvent);
			if(fireChangeEvent)
				this.changeText(this.text, this.insert(this.cursor, content, this.text));
			else
				this.text = this.insert(this.cursor, content, this.text);
			this.updateDisplayText();
			this.cursor += content.length;
		}*/
	}));
	arr[i].addListener(extend(InputListener, {
		enter(event, x, y, pointer, fromActor){
			TextField_JS('', this.index + 1, arr, t);
			arr.forEach((e, i) => i > this.index ? e.index++ : 0);
		}
	}));
	let style = new TextField.TextFieldStyle(Styles.defaultField);
	//style.messageFontColor = style.fontColor = style.focusedFontColor = style.disabledFontColor = Color.black;
	style.font = Fonts.def;
	style.cursor = Tex.whiteui.tint(1, 1, 1, .7);
	//style.background = Tex.whiteui.tint(1, 1, 1, 1);
	//style.focusedBackground = Tex.whiteui.tint(.1, .1, .1, .7);
	style.selection = Tex.whiteui.tint(.3, .3, 1, .7);
	arr[i].setStyle(style);
	arr[i].setText(text);
	arr[i].setWidth(Core.graphics.getWidth() * .8);
	t.addChildAt(i, arr[i]);
}
const Contents = Vars.content.blocks().toArray().concat(Vars.content.liquids().toArray()).concat(Vars.content.items());
const types = {
	'blocks':['Wall', 'Turret', 'ItemSource', 'LiquidSource', 'PowerSource', 'BaseTurret','ReloadTurret', 'ItemTurret', 'LaserTurret', 'Door', 'OreBlock', 'Floor', 'GenericCrafter', 'CoreBlock', 'Battery', 'ImpactReactor', 'NuclearReactor', 'SolarGenerator', 'SingleTypeGenerator', 'PowerGenerator', 'PowerNode', 'LaunchPad', 'TractorBeamTurret','ShockMine','PointDefenseTurret','OverdriveProjector','MendProjector','ForceProjector','LaunchPad'],
	'items':[],
	'liquids':[],
};
for(let k in types){
	types[k].unshift(k.split('').map((s, i) => i == 0 ? s.toUpperCase() : i == k.length - 1 ? '' : s).join(''));
}
const bundles = [
	'bundle',
	'bundle_be',
	'bundle_cs',
	'bundle_da',
	'bundle_de',
	'bundle_es',
	'bundle_et',
	'bundle_eu',
	'bundle_fi',
	'bundle_fil',
	'bundle_fr',
	'bundle_fr_BE',
	'bundle_hu',
	'bundle_in_ID',
	'bundle_it',
	'bundle_ja',
	'bundle_ko',
	'bundle_lt',
	'bundle_nl',
	'bundle_nl_BE',
	'bundle_pl',
	'bundle_pt_BR',
	'bundle_pt_PT',
	'bundle_ro',
	'bundle_ru',
	'bundle_sv',
	'bundle_th',
	'bundle_tk',
	'bundle_tr',
	'bundle_uk_UA',
	'bundle_zh_CN',
	'bundle_zh_TW'
];

elements[Core.bundle.get('makeMod.localizedName', 'makeMod')] = {
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
		build(){
			let _this = this;
			let table = this.table = extend(Table, {
				main:_this,
			});
			return this.addTable(table);
		},
		defined(){
			let obj = Object.assign(this, {
				type:'defined',
				color:Color.sky,
				name:'result',
				value:'"value"'
			});
			obj.buildChildren = function(table){
				table.add('$name');
				let _name = table.add(new TextField(this.name)).get();
				table.add(' = ');
				table.add('$value');
				let _value = table.add(new TextField(this.value)).get();
				table.update(run(() => {
					this.name = _name.getText();
					this.value = _value.getText();
				}));
			},
			obj.remove = function(){
				if(this.table != null) this.table.remove();
			},
			obj.toString = function(){
				return 'let ' + this.name + ' = ' + this.value + ';';
			}
			return obj;
		}
	},
	mods:[],
	modsPath: Vars.dataDirectory.child('mods(I hope...)'),
	importMod(file){
		try{
			file.copyTo(this.modsPath.child(file.name()));
			this.setup(this.ui);
		}catch(e){
			let err = '[red][' + Core.bundle.get(e.name, e.name) + '][]' + e.message;
			Vars.ui.showErrorMessage(err);
		}
	},
	doubleClick(t, _runs){
		let table = t;
		let runs = _runs;
		t.addListener(extend(ClickListener, {
			clicked(a,b,c){
				if(Time.millis() - this.visualPressedTime - this.visualPressedDuration * 1000 < 100){
					let listener;
					table.addListener(listener = extend(ClickListener, {
						clicked(a,b,c){
							if(Time.millis() - this.visualPressedTime - this.visualPressedDuration * 1000 < 100) runs[0].run();
							else runs[1].run();
							Time.clear();
							table.removeListener(listener);
						}
					}));
					Time.run(10, run(() => table.removeListener(listener) && runs[1].run()));
				}else runs[1].run();
			}
		}));
		return t;
	},
	editor(file, mod){
		Vars.ui.loadfrag.show();
		file.exists() || file.writeString('');
		let dialog = new BaseDialog('code编辑器');
		let cont = dialog.cont;
		cont.top().defaults().padTop(0).top();
		let w = Core.graphics.getWidth() * .8;
		let result = {};
		let fileName = file.extension() != 'properties' ? cont.table(cons(t => t.add('$fileName'))).get().add(new TextField('' + file.nameWithoutExtension())).get() : null;
		cont.row();
		cont.pane(cons(p => {
			switch(file.extension()){
				case'json':case'hjson':{
					let obj = result.value = hjsonToJson(file.readString());
					let parentname = file.parent().name();
					let parenttype = parentname[0].toUpperCase() + parentname.slice(1, -1);
					obj.type = obj.type != null ? obj.type : types[parentname] != null && types[parentname][0] != null ? types[parentname][0] : 'none';

					p.table(Tex.clear, cons(t => {
						t.add('$type').padRight(2);
						let button = t.add(Button(style[1])).size(w * 0.15, 40).get();
						button.label(() => '$' + obj.type.toLowerCase()).grow().row();
						button.image().color(Color.gray).fillX();
						button.clicked(run(() => showSelectTable(button, (p, hide, v) => {
							let arr = [];
							for(let k in types){
								types[k].forEach(e => arr.push(e));
							}
							arr.push('none');
							for(let type of arr){
								let t = type;
								p.button('$' + t.toLowerCase(), Styles.cleart, run(() => [obj.type = t, hide.run()])).pad(5).size(200, 65).disabled(obj.type == t).row();
							}
						}, false)));
					})).fillX().row();

					let tablesChange = (tables, table, index, remove, name, value) => {
						let ts = tables, t = table, i = index, k = name, v = value;
						if(!remove) ts.splice(i, 0, t.table(cons(t => {
							t.left().add('"' + Core.bundle.get(k, k) + '"').padLeft(2);
							t.add(': ');
							if(v instanceof Color || (() => {
								let color = v[0] == '#' ? v.slice(1) : v;
								if(color.length != 8) color = color + 'ff';
								if(color.length == 8) return false;
								try{
									return color == '' + eval('Color(0x' + color + ')');
								}catch(e){
									return false;
								}
							})()){
								let b = t.button('', Styles.cleart, run(() => Vars.ui.picker.show(eval('Color(0x' + (v[0] == '#' ? v.slice(1) : v) + ')'), cons(c => b.setText(obj[k] = c.toString()))))).width(100).get();
							}
							else if(k == 'requirements'){
								if(v instanceof String){
									try{
										v = eval('(' + v + ')');
									}catch(e){
										v = [];
									}
								}
								v = v || [];
								v = v.map(i => typeof i == 'string' ? i.split('/') : [i.item, i.amount]);
								t.add('[').row();
								let buttons = t.table().left().get();
								t.row();
								let lastI = -1, isTheMod = false;
								obj[k] = {
									toString(){
										return '[' + v.map(e => '"' + e[0] + '/' + e[1] + '"') + ']';
									}
								}
								var fun = (item, amount) => buttons.table(cons(t => {
									let i = ++lastI;
									v[i] = [];
									t.add('$item');
									let field = t.field('' + item, cons(text => {})).get();
									field.update(run(() => v[i][0] = field.getText()));
									let b = t.button('', Icon.pencilSmall, Styles.logict, run(() => {
										let t = new Table(Tex.button);
										t.defaults().size(480);
										let hitter = new Element;
										let hide = run(() => {
											hitter.remove();
											t.actions(Actions.fadeOut(0.3, Interp.fade), Actions.remove());
										});

										hitter.fillParent = true;
										hitter.clicked(hide);

										Core.scene.add(hitter);
										Core.scene.add(t);

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
										ItemSelection.buildTable(t, Seq(Vars.content.items().toArray()), prov(() => Vars.content.getByName(ContentType.item, (isTheMod ? mod.name + '-' : '') + field.getText())), cons(item => [hide.run(), field.setText(item.name == mod.name + '-' + item.localizedName ? [item.localizedName, (isTheMod = true)][0] : [item.name, (isTheMod = false)][0])]));
									})).size(40).padLeft(-1).get();
									t.add('$amount');
									let atf = t.field('' + amount, cons(text => {})).get();
									atf.update(run(() => v[i][1] = atf.getText()));
								})).left().row();
								t.button('$add', run(() => fun('', ''))).row();
								for(let i of v){
									fun(i[0], i[1]);
								}
								t.add(']');
							}
							else{
								t.add('"');
								obj[k] = v;
								t.field('' + v, cons(text => obj[k] = text));
								t.add('"');
							}
							t.label(() => i == tables.length ? '' : ',');
							t.button('', Icon.trash, Styles.cleart, run(() => tablesChange(ts, t, i, true, k, v)));
							if(Core.bundle.has(k + '.help')) t.add('// ' + Core.bundle.get(k + '.help')).padLeft(2);
						})).left().get());
						else{
							delete obj[k];
							ts.splice(i, 1)[0].remove();
						}
						table.row()
					}
					let table, tables = [];
					p.table(Tex.button, cons(t => {
						table = t.table(Tex.whiteui.tint(.8, 0, 1, 1)).fillX().pad(4).get();
						t.row();
						let content;
						let btn = t.button('$add', Icon.add, run(() => showSelectTable(btn, (p, hide, v) => {
							p.left().top().defaults().left().top();
							p.clearChildren();
							if(obj.type == 'none'){
								let name = p.table(cons(t => t.add('$name').growX().left().row())).get().add(new TextField).width(300).get();
								p.row();
								let value = p.table(cons(t => t.add('$value').growX().left().row())).get().add(new TextField).width(300).get();
								p.row();
								p.button('$ok', Styles.cleart, run(() => [tableChange(tables, tables.length, false, name.getText(), value.getText()), hide.run()])).fillX();
								return;
							}
							let cont = eval(obj.type);
							while(content == null){
								try{
									content = new JavaAdapter(cont, {}, 'unused_' + obj.type);
								}catch(e){
									Vars.content.removeLast();
								}
							}
							let reg = RegExp(v, 'i');
							let arr = Object.staticKeys(content, (k, obj) => typeof obj[k] != 'function' && !/id|self|stats|bars|inlineDescription|delegee|details|minfo|cicon|cacheLayer|region/i.test(k) && (reg.test(k) || reg.test(Core.bundle.get('content.' + k))), (k, obj) => {
								p.button(Core.bundle.get('content.' + k, k), Styles.cleart, run(() => [tablesChange(tables, table, tables.length, false, k, obj[k]), hide.run()])).size(Core.graphics.getWidth() * .2, 45).disabled(result.value[k] != null).row();
							});
						}, obj.type != 'none'))).fillX().growX().get();
						t.row();
						t.table(cons(t => {
							t.add('research:');
							let techs = TechTree.all.toArray();
							let btn = t.button(obj.research != null ? obj.research : '$none', Styles.cleart, run(() => showSelectTable(btn, (p, hide, v) => {
								p.button('$none', Styles.cleart, run(() => {
									delete obj.research;
									btn.setText('$none');
								}));
								let reg = RegExp(v, 'i');
								for(let tech of techs){
									let t = tech.content;
									if(reg.test(t.name) || reg.test(t.localizedName)) p.button(t.name, Styles.cleart, run(() => [btn.setText(obj.research = t.name.replace(modName + '-', '')), hide.run()])).height(40).growX().row();
								}
							}, true))).size(100, 60).get();
						})).fillX();
					})).fillX().row();
					for(var k in obj){
						if(k == 'type' || k == 'research') continue;
						tablesChange(tables, table, tables.length, false, k, obj[k]);
					}
				}
				break;
				case'properties':{
					let str = file.readString().split('\n');
					let obj = result.value = {from:[], to:[]};
					if(str.join('') == '') str.length = 0;
					let cont = p.table(Tex.whiteui.tint(.5, .6, .1, .8)).get();
					let fun = (from, to) => {
						let table = cont.table(Tex.button, cons(t => {
							obj.from.push(t.add(new TextField(from)).width(200).get());
							t.add(' = ', Color.gray);
							obj.to.push(t.add(new TextField(to)).width(200).get());
						})).get();
						let index = obj.from.length - 1;
						this.doubleClick(table, [run(() => {
							Vars.ui.showConfirm('$confirm', Core.bundle.format('confirm.remove', obj.from.splice(index, 1)), run(() => table.remove()));
						}), run(() => {})]);
						cont.row();
					}
					str.forEach(e => {
						let arr = e.split(' = ');
						fun(arr.splice(0, 1), arr.join(' = '));
					});
					p.row();
					p.button('$add', Icon.add, run(() => fun('', '')));
				}
				break;
				/*case'js':{
					p.top().defaults().top();
					let _k = {};
					let cont = p.add(new Table).get();
					p.row();
					let btn = [cont, p][cont.children.size ? 1 : 0].button('$add', Icon.add, run(() => {
						showSelectTable(btn, (p, hide) => {
							for(let k in this.scripts){
								if(k == 'addTable') continue;
								p.button('$' + k, Style.cleart, run(() => {
									let build = this.scripts[k]();
									cont.add(build.build()).row();
								})).size(Core.graphics.getWidth() * .2, 45);
							}
						});
					})).size(220, 75).get();
					p.row();
				}
				break;*/
				default:
					let area = result.value = p.area(file.readString(), cons(t => {
						
					})).size(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) - 200).get();
					/*area.changed(run(() => {
						nums
					}));*/
					//area.clicked(run(() => showTextArea(result.value)));
			}
		})).size(w, Core.graphics.getHeight() - 400).grow().row();
		dialog.buttons.button('$back', Icon.left, run(() => dialog.hide())).size(220, 70);
		dialog.buttons.button('$ok', Icon.ok, run(() => {
			switch(file.extension()){
				case'json':case'hjson':{
					let arr = [];
					function fun(obj){
						let arr = [];
						for(let k in obj){
							if((k == 'type' && /none|item|liquid|unit/i.test(obj[k])) || obj[k] == null || obj[k] == '') continue;
							arr.push('"' + k + '": ' + (typeof obj[k] == 'object' ? obj[k] : '"' + obj[k].replace(/\"/g, '\\"') + '"'));
						}
						return arr;
					}
					file = mod.child('content').child(result.value.type == 'none' ? 'blocks' : Object.keys(types).map(e => types[e].indexOf(result.value.type) != -1 ? e : '').join('')).child(file.name());
					file.writeString('{\n\t' + fun(result.value).join(',\n\t') + '\n}');
					/*let strs = [], obj = result.value;
					for(let k in obj){
						strs.push('\t"' + k + '": "' + obj[k] + '"');
					}
					file.writeString('{\n' + strs.join(',\n') + '\n}');*/
				}
				break;
				case'properties':{
					let str = [], obj = result.value;
					obj.from.forEach((e, i) => {
						str.push(e.getText() + ' = ' + obj.to[i].getText());
					});
					file.writeString(str.join('\n'));
				}
				break;
				/*case'js':{
					file.writeString(result.value.join('\n'));
				}*/
				break;
				default:
					file.writeString(result.value.getText().replace(/\r/g, '\n'));
			}
			if(fileName){
				let toFile = file.parent().child(fileName.getText() + '.' + file.extension());
				file.moveTo(toFile);
				this.showJson(toFile, mod);
			}
			dialog.hide();
		})).size(220, 70);
		dialog.show();
		Vars.ui.loadfrag.hide();
	},
	showMod(mod){
		Vars.ui.loadfrag.show();
		this._showMod(mod);
		Time.run(1, run(() => Vars.ui.loadfrag.hide()));
	},
	_showMod(mod){
		let json = jsons[mod.name()];
		let sprites = mod.child('sprites').exists() ? mod.child('sprites') : null;
		let displayName = json.displayName != null ? json.displayName : json.name;
		let w = Core.graphics.getWidth() * .8;
		let dialog = new BaseDialog(displayName);
		dialog.addCloseButton();
		dialog.cont.pane(cons(desc => {
			desc.center();
			desc.defaults().padTop(10).left();
			if(json == {}){
				desc.add('$error', Color.red);
				return;
			}
			desc.add('$editor.name', Color.gray).padRight(10).padTop(0).row();
			desc.add(displayName).growX().wrap().padTop(2).row();
			if(json.author != null){
				desc.add('$editor.author', Color.gray).padRight(10).row();
				desc.add(json.author).growX().wrap().padTop(2).row();
			}
			if(json.version != null){
				desc.add('$editor.version', Color.gray).padRight(10).row();
				desc.add(json.version).growX().wrap().padTop(2).row();
			}
			if(json.description != null){
				desc.add('$editor.description').padRight(10).color(Color.gray).top().row();
				desc.add(json.description).growX().wrap().padTop(2).row();
			}
			let _k = {};
			desc.table(cons(t => {
				t.button(cons(b => {
					b.add('$editor.content', Color.gold).padRight(10 + 5).growY().row();
					let image = b.image().size(w / 3 - 1, 4).growX().get();
					b.update(run(() => image.setColor(_k.value == 'content' ? Color.gold : Color.gray)));
				}), window.style[1], run(() => _setup('content'))).size(w / 3, 60).growY();
				t.button(cons(b => {
					b.add('$bundles', Color.pink).padRight(10 + 5).growY().row();
					let image = b.image().size(w / 3 - 1, 4).growX().get();
					b.update(run(() => image.setColor(_k.value == 'bundles' ? Color.pink : Color.gray)));
				}), window.style[1], run(() => _setup('bundles'))).size(w / 3, 60);
				t.button(cons(b => {
					b.add('$scripts', Color.sky).padRight(10 + 5).growY().row();
					let image = b.image().size(w / 3 - 1, 4).growX().get();
					b.update(run(() => image.setColor(_k.value == 'scripts' ? Color.sky : Color.gray)));
				}), window.style[1], run(() => _setup('scripts'))).size(w / 3, 60);
			})).fillX().row();
			let cont = desc.add(new Table).fillX().get();
			let _setup = str => {
				_k.value = str;
				cont.clear();
				switch(str){
					case'content':
						cont.table(Styles.none, cons(t => {
							t.center();
							t.defaults().padTop(10).left();
							let color = Color.valueOf('#ccccff');
							let content = mod.child('content');
							let files = content.findAll().toArray();
							let cont = new Table();
							cont.defaults().padTop(10).left();
							let setup = str => {
								cont.clear();
								let body = new Table();
								body.top().left();
								body.defaults().padTop(2).top().left();
								cont.pane(cons(p => p.add(body).left().grow().get().left())).fillX().minWidth(450).row();
								let files = str instanceof RegExp ? content.findAll().toArray().map(e => str.test(e) ? e : null) : str instanceof Cons ? content.findAll().toArray().map(e => str(e) ? e : null) : str == '' ? content.findAll().toArray() : content.child(str).findAll().toArray();
								for(let i = 0, len = files.length; i < len; i++){
									let json = files[i];
									if(json != null && json.extension() != 'json' && json.extension() != 'hjson') continue;

									body.button(cons(b => {
										b.left()
										b.image(find(mod, json.nameWithoutExtension())).size(32).padRight(6).left();
										if(!Vars.mobile) image.addListener(new HandCursorListener());
										b.add(json.name()).top();
										let clicked = () => {
											this.showJson(json, mod);
											dialog.hide();
										}
										b.addListener(extend(ClickListener, {
											clicked(a,b,c){
												if(Time.millis() - this.visualPressedTime - this.visualPressedDuration * 1000 > 800) Vars.ui.showConfirm('$confirm', Core.bundle.format('confirm.remove', json.nameWithoutExtension()), run(() => {
													json.delete();
													setup(str);
												}));
												else clicked();
											}
										}));
									}), Styles.defaultb, run(() => {})).fillX().minWidth(400).pad(2).left().row();
								}
							}

							t.add('$content.info').row();
							t.add(cont).grow().size(w).row();
							t.button('$add', Icon.add, run(() => {
								let ui = new Dialog('');
								let name = ui.cont.table(cons(t => t.add('$name'))).get().add(TextField()).get();
								ui.buttons.table(cons(t => t.button('$back', run(() => ui.hide())))).get().button('$ok', run(() => {
									let file = content.child('blocks').child(name.getText() + '.json');
									file.writeString('');
									dialog.hide();
									ui.hide();
									this.editor(file, mod);
								}));
								ui.show();
							})).fillX();
							setup('');
						})).width(w - 10).row();
					break;
					case'bundles':
						cont.table(Tex.whiteui.tint(1, .8, 1, .8), cons(t => {
							for(let _k in bundles){
								let k = _k;
								t.add('$bundle.' + bundles[k]).left();
								t.button(Icon.pencil, Styles.clearTransi, run(() => this.editor(mod.child('bundles').child(bundles[k] + '.properties'), mod))).right().pad(10).row();
							}
						})).minWidth(400).fillX().row();
					break;
					case'scripts':
						cont.table(Tex.whiteui.tint(.7, .7, 1, .8), cons(t => {
							let scripts = mod.child('scripts');
							let main = scripts.child('main.js')
							if(!main.exists()) main.writeString('');
							let cont = new Table;
							let setup = () => {
								cont.clear();
								let str = main.readString().split(/\s*require\s*\(\s*\'(?=[^\n\']*\'\)\;)/).map(e => e.slice(0, -3));
								for(let i in str) if(str[i] == '') str.splice(i, 1);

								let all = scripts.findAll().toArray();
								for(let i in all){
									if(all[i].name() == 'main.js') continue;
									let f = all[i];
									let name = f.nameWithoutExtension();
									let index = str.indexOf(name);
									cont.button(cons(b => {
										b.top().left();
										b.margin(12);
										b.defaults().left().top();
										b.table(cons(title => {
											title.left();
											title.image(Core.atlas.find(modName + '-js.file')).size(102).padTop(8).padLeft(-8).padRight(8);
											title.add(f.name()).wrap().width(170).growX().left();
											title.add().growX().left();
										}));
										b.table(cons(right => {
											right.right();
											right.add(index == -1 ? '$mod.disable' : '$mod.enable', index != -1 ? Color.gray : Color.red);
											right.button(index == -1 ? Icon.downOpen : Icon.upOpen, Styles.clearPartiali, run(() => {
												if(index == -1) str.splice(index, 0, name)
												else str.splice(index, 1);
												main.writeString(str.map(e => "require('" + e + "');").join('\n'));
												setup();
											})).size(50);
											right.button(Icon.trash, Styles.clearPartiali, run(() => Vars.ui.showConfirm('$confirm', Core.bundle.format('confirm.remove', f.name()), run(() => {
												f.delete();
												if(index != -1) str.splice(index, 1);
												main.writeString(str.map(e => "require('" + e + "');").join('\n'));
												setup();
											})))).size(50);
										})).grow();
									}), window.style[1], run(() => this.editor(f, mod))).fillX().row();
								}
								cont.table(cons(t => {
									t.button('$add', Icon.add, run(() => {
										let dialog = new Dialog('$add');
										dialog.cont.add('$fileName');
										let name = dialog.cont.add(new TextField('')).get();
										dialog.cont.row();
										let table = dialog.buttons;
										table.button('$back', Icon.left, run(() => dialog.hide()));
										table.button('$ok', Icon.ok, run(() => {
											if(name.getText() == 'main') return Vars.ui.showErrorMessage('文件名不能为[orange]main[]。');
											let toFile = scripts.child(name.getText() + '.js');
											function go(){
												toFile.writeString('');
												main.writeString(main.readString() + "\nrequire('" + name.getText() + "');");
												dialog.hide();
												setup();
											}
											if(toFile.exists()){
												Vars.ui.showConfirm('覆盖', '同名文件已存在\n是否要覆盖', run(() => go()));
											}else go();
										}));
										dialog.show();
									})).fillX();
									t.button('导入插件', Icon.download, run(() => {})).fillX();
									t.button('test', run(() => {
										//let o = Vars.mods.scripts.runConsole(main.readString());
										//if(o != null) Vars.ui.showInfo('' + o);
										Vars.mods.scripts.run(Vars.mods.locateMod(mod.name), main.readString());
									})).fillX();
								})).name('buttons').minWidth(200).fillX();
							}
							setup();
							t.add(cont);
						})).fillX().height(Core.graphics.getHeight() * .8);
				}
			}
			_setup('content');
		})).size(w + 30, Core.graphics.getHeight() * .8);
		dialog.show();
	},
	showJson(json, mod){
		if(!/hjson|json/.test(json.extension())) return;
		let dialog = new Dialog(json.name() != null ? json.name() : '');
		let w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
		let max = Math.min(w, h) - 200;
		dialog.cont.pane(cons(p => {
			p.center();
			p.defaults().padTop(10).left();
			p.add('$editor.sourceCode', Color.gray).padRight(10).padTop(0).row();
			p.table(cons(t => {
				t.right();
				t.button(Icon.download, Styles.clearPartiali, run(() => Vars.ui.showConfirm('粘贴', '是否要粘贴', run(() => {
					json.writeString(Core.app.getClipboardText());
					dialog.hide();
					this.showJson(json, mod);
				}))));
				t.button(Icon.copy, Styles.clearPartiali, run(() => Core.app.setClipboardText(json.readString())));
			})).growX().right().row();
			p.add(json.readString().replace(/\r/g, '\n').replace(/\t/g, '  '));
		})).size(max).grow().row();
		dialog.buttons.button('$back', Icon.left, Styles.defaultt, run(() => {
			this.showMod(mod);
			dialog.hide();
		})).size(max * 0.5, Math.min(w, h) * 0.1);
		dialog.buttons.button('$edit', Icon.edit, Styles.defaultt, run(() => {
			this.editor(json, mod);
			dialog.hide();
		})).size(max * 0.5, Math.min(w, h) * 0.1);
		dialog.show();
	},
	setup(dialog){
		Vars.ui.loadfrag.show();
		sprites.length = 0;
		dialog.cont.clear();
		dialog.buttons.clear();
		this.mods = this.modsPath.list();
		dialog.cont.add('$mod.advise').row();
		dialog.cont.table(Styles.none, cons(t => t.pane(cons(p => {
			if(this.mods.length == 0){
				p.table(Styles.black6, cons(t => t.add('$mods.none'))).height(80);
				return;
			}
			for(let i in this.mods){
				let mod = this.mods[i];
				let json = hjsonToJson(mod.child('mod.json').exists() ? mod.child('mod.json').readString() : mod.child('mod.hjson').exists() ? mod.child('mod.hjson').readString() : null);
				if(json == null) continue;
				let displayName = json.displayName != null ? json.displayName : json.name;
				jsons[mod.name()] = json;
				sprites[mod.name()] = mod.child('sprites').exists() ? mod.child('sprites').findAll().toArray() : null;

				p.button(cons(b => {
					b.top().left();
					b.margin(12);
					b.defaults().left().top();
					b.table(cons(title => {
						title.left();
						let image = extend(BorderImage, {});
						if(mod.child('icon.png').exists()){
							image.setDrawable(new TextureRegion(new Texture(mod.child('icon.png'))));
						}else{
							image.setDrawable(Tex.clear);
						}
						image.border(Pal.accent);
						title.add(image).size(102).padTop(-8).padLeft(-8).padRight(8);
						title.add(displayName + '\n[lightgray]v' + json.version).wrap().width(170).growX().left();
						title.add().growX().left();
					}));
					b.table(cons(right => {
						right.right().top();
						right.defaults().right().top();
						right.button(Icon.edit, Styles.clearPartiali, run(() => this.editModJson(mod.child('mod.json').exists() ? mod.child('mod.json') : mod.child('mod.hjson')))).size(50);
						right.button(Icon.trash, Styles.clearPartiali, run(() => Vars.ui.showConfirm('$confirm', '$mod.remove.confirm', run(() => {
							this.modsPath.list()[i].deleteDirectory();
							this.setup(this.ui);
						})))).size(50).row();
						right.button(Icon.upload, Styles.clearPartiali, run(() => {
							let file = Vars.modDirectory;
							if(file.child(mod.name()).exists()) Vars.ui.showConfirm('覆盖', '同名文件已存在\n是否要覆盖', run(() => mod.copyTo(file)));
							else mod.copyTo(file);
						})).size(50);
						right.button(Icon.link, Styles.clearPartiali, run(() => Core.app.openFolder(mod.absolutePath()))).size(50);
					})).grow();
				}), window.style[1], run(() => {
					this.showMod(mod);
				})).size(Core.graphics.getWidth() * .8, 120).row();
			}
		})))).grow().size(Core.graphics.getWidth() * .81, Core.graphics.getHeight() * .7).row();

		let style = Styles.defaultt, margin = 12, buttons = dialog.buttons;

		buttons.button('$back', Icon.left, style, run(() => dialog.hide())).margin(margin).size(Core.graphics.getWidth() * .4, 60);
		buttons.button('$mod.add', Icon.add, style, run(() => {
			let dialog = new BaseDialog('$mod.add'), bstyle = Styles.cleart;

			dialog.cont.table(Tex.button, cons(t => {
				t.defaults().left().size(300, 70);
				t.margin(12);

				t.button('$mod.import.file', Icon.file, bstyle, run(() => {
					dialog.hide();

					Vars.platform.showMultiFileChooser(file => {
						try{
							let obj = hjsonToJson(file.readString());
							this.modsPath.child(file.name()).child('mod.json').writeString(obj['mod.json'] != null ? obj['mod.json'] : obj['mod.hjson'] != null ? obj['mod.hjson'] : {});
							function reload(arr){
								for(let k in arr){
									let content = obj[arr[k]];
									for(let type in content){
										let contentType = content[type];
										for(let json in contentType) this.modsPath.child(file.name()).child(k).child(type).child(json + '.json').writeString(contentType[json]);
									}
								}
							}
							reload(['content', 'sprites', 'sprites-override', 'bundles', 'scripts', 'sounds']);
							this.importMod(file);
						}catch(e){Vars.ui.showErrorMessage(e);}
					}, 'json', 'hjson');
				})).margin(12).marginBottom(2).row();
				t.button('$mod.add', Icon.add, bstyle, run(() => {
					this.editModJson(this.modsPath.child('/tmp/').child('mod.hjson'));
				})).margin(12).marginBottom(2);
			}));
			dialog.addCloseButton();
			dialog.show();
		})).margin(margin).size(Core.graphics.getWidth() * .4, 60).row();
		buttons.button('$mods.openfolder', Icon.link, style, run(() => Core.app.openFolder(this.modsPath.absolutePath()))).margin(margin).size(Core.graphics.getWidth() * .4, 60);
		buttons.button('$quit', Icon.exit, style, run(() => Core.app.exit())).margin(margin).size(Core.graphics.getWidth() * .4, 60);

		Vars.ui.loadfrag.hide();
	},
	editModJson(file){
		let obj = hjsonToJson(file.exists() ? file.readString() : null);
		let isNull = obj == null;
		obj = isNull ? {} : obj;
		let ui = new Dialog(isNull ? '$mod.create' : '$edit');
		let arr = [ 'name', 'displayName', 'description', 'author', 'version', 'main'], cont = new Table();
		cont.add('$mod.fileName');
		let filename = obj.fileName = cont.add(new TextField(!isNull ? file.parent().nameWithoutExtension() : '')).get();
		filename.changed(run(() => {
			ok.setDisabled(filename.getText().replace(/\s/g, '') == '');
		}));
		cont.row();
		cont.add('$minGameVersion');
		let mingameversion = obj.minGameVersion = cont.add(new TextField(!isNull && obj.minGameVersion ? obj.minGameVersion : '105')).get();
		mingameversion.changed(run(() => {
			ok.setDisabled((mingameversion.getText() | 0) < 105 || (mingameversion.getText() | 0) > Version.build);
		}));
		cont.row();
		for(let i of arr){
			cont.add('$' + i);
			let field = new TextField(!isNull && obj[i] != null ? obj[i].replace(/\n|\r/g, '\\n') : '');
			field.clicked(run(function(){
				if(Time.millis() - this.visualPressedTime - this.visualPressedDuration * 1000 > 800) showTextArea(field);
			}));
			obj[i] = cont.add(field).get();
			cont.row();
		}
		ui.cont.add(cont).row();
		let func = mod => {
			delete obj.fileName;
			if(!isNull) file.moveTo(mod);
			let strArr = [];
			for(let k in obj){
				if(obj[k].getText() == '') continue;
				strArr.push('\t"' + k + '": "' + obj[k].getText().replace(/\n|\r/g, '\\n') + '"');
			}
			mod.child(isNull ? 'mod.json' : 'mod.' + file.extension()).writeString('{\n' + strArr.join(',\n') + ',\n}');
			this.setup(this.ui);
			ui.hide();
		}
		let buttons = new Table;
		let w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
		buttons.button('$back', Icon.left, run(() => ui.hide())).size(Math.max(w, h) * 0.1, Math.min(w, h) * 0.1);
		let ok = buttons.button('$ok', Icon.ok, run(() => {
			if((obj.minGameVersion.getText() | 0) < 105){
				return Vars.ui.showErrorMessage(Core.bundle.get('minGameVersion') + Core.bundle.get('cannot-be-less-than') + '105.');
			}
			let mod = this.modsPath.child(obj.fileName.getText());
			if(mod.path() != file.parent().path() && mod.exists()){
				Vars.ui.showConfirm('覆盖', '同名文件已存在\n是否要覆盖', run(() => {
					mod.deleteDirectory();
					func(mod);
				}));
				return;
			}
			func(mod);
		})).size(Math.max(w, h) * 0.1, Math.min(w, h) * 0.1).get();
		ui.cont.add(buttons);
		ui.show();
	},
	load(){
		this.ui = new BaseDialog(this.name);
	},
	buildConfiguration(table){
		this.setup(this.ui);
		this.ui.show();
	}
};

