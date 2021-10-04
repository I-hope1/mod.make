const IntFunc = require('func/index');

exports.cont = {
	name: Core.bundle.get('select', 'select'), show: false, get disabled() { return Vars.state.isMenu() }, selection: {
		tiles: [],
		buildings: []
	},
	tables: [],
	select: {
		tile: Core.settings.get(modName + '-select-tile', false),
		building: Core.settings.get(modName + '-select-building', true),
		floor: Core.settings.get(modName + '-select-floor', false)
	},
	functionsWidth: 200, functionsHeight: 45,

	hide() {
		this.pane.visible = false;
		this.pane.touchable = Touchable.disabled;
	},
	loadSettings() {
		let settings = this.settingsUi = new BaseDialog('$settings')
		settings.cont.table(cons(t => {
			t.add(this.name).row();
			t.check('tile', this.select.tile, new Boolc({
				get: b => {
					if (b) this.tables[0].setup()
					else this.tables[0].clearChildren()
					Core.settings.put(modName + '-select-tile', b)
				}
			})).padLeft(6).row();
			t.check('building', this.select.building, new Boolc({
				get: b => {
					if (b) this.tables[1].setup()
					else this.tables[1].clearChildren()
					Core.settings.put(modName + '-select-building', b)
				}
			})).padLeft(6).row();
			t.check('floor', this.select.floor, new Boolc({
				get: b => {
					if (b) this.tables[2].setup()
					else this.tables[2].clearChildren()
					Core.settings.put(modName + '-select-floor', b)
				}
			})).padLeft(6);
		})).row()
		settings.addCloseButton()
	},
	load() {
		this.loadSettings()

		let elem = this.frag = extend(Dialog, {
			draw() {
				Lines.stroke(6);
				Draw.color(Pal.accent);
				Lines.rect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
			}
		}).background(Styles.none);
		elem.touchable = Touchable.enabled;
		elem.setFillParent(true);
		let _this = this;
		let [x1, y1, x2, y2] = [0, 0, 0, 0];

		Core.scene.addListener(extend(InputListener, {
			keyDown(event, keycode) {
				if (keycode.value == 'Mouse Right') {
					this.hide()
				}
			},
			touchDown(event, x, y, pointer, button) {
				x1 = x2 = x;
				y1 = y2 = y;
				this.move = false;
				Time.run(2, () => this.move = true);
				return _this.show;
			},
			touchDragged(event, x, y, pointer) {
				x2 = x;
				y2 = y;
			},
			touchUp(event, x, y, pointer, button) {
				if (!this.move) return;
				let [mx, my] = [x2, y2];
				if (x1 > x2) [x1, x2] = [x2, x1];
				if (y1 > y2) [y1, y2] = [y2, y1];

				_this.selection.tiles.length
					= _this.selection.buildings.length = 0;

				let v1 = Core.camera.unproject(x1, y1).cpy();
				let v2 = Core.camera.unproject(x2, y2).cpy();
				for (let y = v1.y; y < v2.y; y += Vars.tilesize) {
					for (let x = v1.x; x < v2.x; x += Vars.tilesize) {
						var tile = Vars.world.tileWorld(x, y);
						if (_this.select.tile || _this.select.floor) _this.selection.tiles.push(tile);
						if (_this.select.building && tile.build != null && !_this.selection.buildings.includes(tile.build)) _this.selection.buildings.push(tile.build);
					}
				}

				table.touchable = Touchable.enabled;
				table.visible = true;
				table.setPosition(
					Mathf.clamp(mx, W, Core.graphics.getWidth()),
					// 32是btn的高度
					Mathf.clamp(my, (maxH + 32) / 2, Core.graphics.getHeight() - (maxH + 32) / 2),
					Align.bottomRight
				);
				elem.hide();
				_this.show = false;
			}
		}));
		
		let W = this.functionsWidth, H = this.functionsHeight;

		let functions = new Table(Styles.black5)
		functions.defaults().width(W)

		let maxH = 400
		let table = this.pane = new Table(Styles.black5, cons(t => {
			t.table(cons(right => {
				right.right().defaults().right()
				right.button(Icon.settings, Styles.clearTransi, () => {
					this.settingsUi.show();
				}).size(32)
				right.button(Icon.cancel, Styles.clearTransi, () => {
					this.hide();
				}).size(32)
			})).fillX().right().row();
			let paneStyle = new ScrollPane.ScrollPaneStyle()
			paneStyle.background = Styles.none;

			t.pane(paneStyle, functions).size(W, maxH).get().setSize(W, maxH)
		}))
		table.right().defaults().width(W).right();
		table.update(() => Vars.state.isMenu() && this.hide())


		let tiles = this.tables[0] = extend(Table, {
			cont: null,
			setup() {
				this.add('tiles:').growX().left().row()
				this.add(this.cont).width(W)
			}
		});
		functions.add(tiles).get();
		functions.row()

		let buildings = this.tables[1] = extend(Table, {
			cont: null,
			setup() {
				/* 分隔 */
				this.image().color(Color.gray).height(1).padTop(3).padBottom(3).fillX().row()
				this.add('buildings:').growX().left().row()
				this.add(this.cont).width(W)
			}
		});
		functions.add(buildings).get();
		functions.row()

		let floors = this.tables[2] = extend(Table, {
			cont: null,
			setup() {
				/* 分隔 */
				this.image().color(Color.gray).height(1).padTop(3).padBottom(3).fillX().row()
				this.add('floors:').growX().left().row()
				this.add(this.cont).width(W)
			}
		});
		functions.add(floors).get();

		Core.scene.root.addChildAt(10, table);
		table.visible = false;


		/* tiles */
		tiles.cont = new Table(cons(t => {
			let setBtn = t.button('Set',
				() => IntFunc.showSelectImageTable(setBtn, Vars.content.blocks().toArray(), null, 40, 32, cons(block =>
					this.selection.tiles.forEach(t => t.setBlock(block, t.team()))
				), 6, true)
			).height(H).growX().right().get();
			t.row();
			t.button('Clear', () => {
				this.selection.tiles.forEach(t => t.setAir());
			}).height(H).growX().right().row();
		}))
		if (this.select.tile) tiles.setup()

		/* buildings */
		buildings.cont = new Table(cons(t => {
			t.button('Infinite health', () => {
				this.selection.buildings.forEach(b => b.health = Infinity);
			}).height(H).growX().right().row();
			t.button('Kill', () => {
				this.selection.buildings.forEach(b => b.kill());
			}).height(H).growX().right().row();
		}))
		if (this.select.building) buildings.setup()
		/* floors */
		floors.cont = new Table(cons(t => {
			let setBtn1 = t.button('Set Floor Reset Overlay',
				() => IntFunc.showSelectImageTable(setBtn1, Vars.content.blocks().toArray().filter(block => block instanceof Floor), null, 40, 32, cons(floor =>
					this.selection.tiles.forEach(t => t.setFloor(floor))
				), 6, true)
			).height(H).growX().right().get();
			t.row()
			let setBtn2 = t.button('Set Floor Preserving Overlay',
				() => IntFunc.showSelectImageTable(setBtn2, Vars.content.blocks().toArray().filter(block => block instanceof Floor && !(block instanceof OverlayFloor)), null, 40, 32, cons(floor =>
					this.selection.tiles.forEach(t => t.setFloorUnder(floor))
				), 6, true)
			).height(H).growX().right().get();
			t.row()
			let setBtn = t.button('Set Overlay',
				() => IntFunc.showSelectImageTable(setBtn, Vars.content.blocks().toArray().filter(block => block instanceof OverlayFloor), null, 40, 32, cons(overlay =>
					this.selection.tiles.forEach(t => t.setOverlay(overlay))
				), 6, true)
			).height(H).growX().right().get();
			t.row()
		}))
		if (this.select.floor) floors.setup()
	},
	click() {
		this.show = true;

		this.frag.show()
	}
}