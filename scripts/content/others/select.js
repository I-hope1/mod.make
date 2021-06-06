exports.cont = {
	name: 'select', show: false, get disabled(){return Vars.state.isMenu()},
	load() {
		let elem = this.ui = extend(Table, {
			selection: [], selectBuild: true,
			draw() {
				Lines.stroke(6);
				Draw.color(Pal.accent);
				Lines.rect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
			}
		});
		elem.touchable = Touchable.enabled;
		elem.setFillParent(true);
		let _this = this;
		let [x1, y1, x2, y2] = [0, 0, 0, 0];

		Core.scene.addListener(extend(InputListener, {
			keyDown(event, keycode) {
				if (keycode.value == 'Mouse Right') functions.visible = false;
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

				elem.selection.length = 0;
				if (elem.selectBuild) {
					let v1 = Core.camera.unproject(x1, y1).cpy();
					let v2 = Core.camera.unproject(x2, y2).cpy();
					for (let y = v1.y; y < v2.y; y += Vars.tilesize) {
						for (let x = v1.x; x < v2.x; x += Vars.tilesize) {
							var build = Vars.world.buildWorld(x, y);
							if (build != null && !elem.selection.includes(build)) elem.selection.push(build);
						}
					}
					// print([x1, y1, x2, y2, v1, v2].join(', '))

				}
				print(elem.selection);
				functions.visible = true;
				functions.setPosition(mx, my, Align.top);
				elem.remove();
				_this.show = false;
			}
		}));

		let functions = this.functions = new Table(Styles.black5);
		Core.scene.root.addChildAt(0, functions);
		functions.visible = false;
		functions.button('NaN health', () => {
			elem.selection.forEach(b => b.health = NaN);
		}).width(60);
		functions.button('clear', () => {
			elem.selection.forEach(b => b.tile.removeNet());
		})
	},
	click() {
		this.show = true;

		Core.scene.root.addChildAt(0, this.ui);
	}
}