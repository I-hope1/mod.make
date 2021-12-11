exports.cont = {
	name: 'showicon', needFi: false,
	load() {
		this.ui = new BaseDialog(this.name)
		this.ui.cont.pane(cons(p => {
			p.table(cons(t => {
				for (let k in Icon) {
					try {
						t.image(new TextureRegionDrawable(Icon[k])).size(32)
						t.add('' + k).row()
					} catch (e) { }
				}
			})).padBottom(6).row()
			p.image().color(Pal.accent).fillX().row()
			p.table(cons(t => {
				for (let k in Tex) {
					try {
						t.image(Tex[k]).size(32)
						t.add('' + k).row()
					} catch (e) { }
				}
			})).padBottom(6).row()
			p.image().color(Pal.accent).fillX().row()
			p.table(Tex.whiteui.tint(1, 0.6, 0.6, 1), cons(t => {
				for (let k in Styles) {
					try {
						if (!(Styles[k] instanceof ImageButton.ImageButtonStyle)) continue
						t.button(Icon.ok, Styles[k], () => {}).size(42)
						t.add('' + k).row()
					} catch (e) { }
				}
			}))
		})).fillX().fillY()
		this.ui.addCloseButton()
	},
	click() {
		this.ui.show()
	}
}