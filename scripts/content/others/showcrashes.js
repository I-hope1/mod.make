
const IntStyles = require('scene/styles');

exports.cont = {
	name: 'crashes', needFi: true,
	load(){
		this.ui = new BaseDialog(this.name);
		this.ui.addCloseButton();

		this.all = Vars.dataDirectory.child('crashes').list();
		this.select = this.all[this.all.length - 1];
	},
	click(table){
		this.ui.cont.clear();

		this.ui.cont.pane(cons(p => {
			p.left().defaults().left();
			for(let i in this.all){
				let fi = this.all[i];
				p.button(cons(b => {
					b.add(this.all[i].nameWithoutExtension());
					b.update(run(() => b.setDisabled(this.select instanceof Packages.arc.files.Fi && this.select.nameWithoutExtension() == b.children.get(0).getText())));
				}), IntStyles.clearb, run(() => this.select = fi)).height(45).row();
			}
		})).pad(40).fillX().maxHeight(Core.graphics.getHeight() * 0.6).row();
		this.ui.cont.pane(cons(p => p.label(() => this.select ? this.select.readString() : ''))).fillX().height(Core.graphics.getHeight() * .2).style(IntStyles.nonePane);

		this.ui.show();
	}
}