
const buildContent = require('func/buildContent');
const Editor = require('ui/dialogs/Editor');
const IntModsDialog = require('ui/dialogs/ModsDialog')

// const Contents = Vars.content.blocks().toArray().concat(Vars.content.liquids().toArray()).concat(Vars.content.items());

// 暂时没用
function TextField_JS(text, i, arr, t) {
	arr.splice(i, 0, extend(TextField, {
		index: +i,
		toString() {
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
		enter(event, x, y, pointer, fromActor) {
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

exports.cont = {
	o: this,
	name: Core.bundle.get('makeMod.localizedName', 'makeMod'), needFi: true,

	load() {
		IntModsDialog.load(this.name)

		buildContent.load()
		Editor.load()
	},
	buildConfiguration(table) {
		IntModsDialog.constructor()
	}
};
