elements['crashes'] = {
	load(){
		this.ui = new BaseDialog('crashes');
		this.ui.addCloseButton();
		this.all = Vars.dataDirectory.child('crashes').list();
		this.sort = this.all.slice(-1)[0];
	},
	buildConfiguration(table){
		this.ui.cont.clear();
		this.ui.cont.pane(cons(p => {
			p.left().defaults().left();
			for(let i in this.all){
				let fi = this.all[i];
				p.button(cons(b => {
					b.add(this.all[i].nameWithoutExtension());
					b.update(run(() => b.setDisabled(this.sort instanceof Packages.arc.files.Fi && this.sort.nameWithoutExtension() == b.children.get(0).getText())));
				}), style[1], run(() => this.sort = fi)).height(45).row();
			}
		})).pad(40).fillX().maxHeight(Core.graphics.getHeight() * 0.6).row();
		this.ui.cont.pane(cons(p => p.label(() => this.sort ? this.sort.readString() : ''))).fillX().height(Core.graphics.getHeight() * .2).get().setStyle(style[0]);
		this.ui.show();
	}
};