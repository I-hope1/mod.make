Time.run(1, run(() => {
	const frag = extend(Table, {
		lastx:0, lasty:0
	});
	frag.image().color(Color.sky).margin(0).pad(0).padBottom(-4).fillX().height(40).get().addListener(extend(InputListener, {
		touchDown(event, x, y, pointer, button){
			this.bx = x;
			this.by = y;
			return true;
		},
		touchDragged(event, x, y, pointer){
			let v = frag.localToStageCoordinates(Tmp.v1.set(x, y));
			frag.lastx = -this.bx + v.x;
			frag.lasty = -this.by + v.y;
		}
	}));
	frag.row();
	frag.table(Tex.button, cons(t => {
		t.button('解锁', run(() => {
			
		}));
	})).row();
	frag.left().bottom().margin(10);
	frag.lastx = frag.x;
	frag.lasty = frag.y;
	frag.update(run(() => {
		frag.color.a = Vars.state.isMenu() ? 0 : 1;
		frag.touchable = Vars.state.isMenu() ? Touchable.disabled : Touchable.enabled;
		frag.setPosition(frag.lastx < 0 ? 0 : frag.lastx > Core.graphics.getWidth() * .8 ? Core.graphics.getWidth() - Object.keys(elements).length * 120 : frag.lastx,  frag.lasty < 0 ? 0 : frag.lasty > Core.graphics.getHeight() - 100 ? Core.graphics.getHeight() * .8 : frag.lasty);
	}));
	Core.scene.add(frag);
}))