
Time.run(1, run(() => {
	/* 浮窗 */
	const frag = extend(Table, {
		cont: new Table,
		lastx: 0,
		lasty: 0,
	});
	frag.image().color(Color.sky).margin(0).pad(0).padBottom(-4).fillX().height(40)
	.get().addListener(extend(InputListener, {
		touchDown(event, x, y, pointer, button) {
			this.bx = x;
			this.by = y;
			return true;
		},
		touchDragged(event, x, y, pointer) {
			let v = frag.localToStageCoordinates(Tmp.v1.set(x, y));
			frag.lastx = -this.bx + v.x;
			frag.lasty = -this.by + v.y;
		}
	}));
	frag.row();
	frag.table(Tex.button, cons(t => {
		contArr.forEach(cont => {
			if (cont == null) return;
			if (cont.load instanceof Function) cont.load();

			cont.btn = t.button(cont.name, Styles.cleart, run(() => {
				frag.cont.clearChildren();
				cont.buildConfiguration(frag.cont);
			})).size(120, 40).get();
			t.row();
		})
	})).row();
	frag.table(Styles.black3, cons(t => t.add(frag.cont))).fillX();
	frag.left().bottom();
	frag.lastx = frag.x;
	frag.lasty = frag.y;
	frag.update(run(() => {
		/* frag.color.a = +Vars.state.isMenu() ^ 1;
		frag.touchable = Vars.state.isMenu() ? Touchable.disabled : Touchable.enabled; */
		frag.setPosition(
			Mathf.clamp(0, frag.lastx, Core.graphics.getWidth() - frag.width),
			Mathf.clamp(0, frag.lasty, Core.graphics.getHeight() - frag.height),
			Align.top
		);
	}));
	Core.scene.add(frag);
}));
