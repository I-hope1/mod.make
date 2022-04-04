exports.clearb = exports.nonePane = {};
Events.run(ClientLoadEvent, () => {
	exports.nonePane = new ScrollPane.ScrollPaneStyle;

	exports.clearb = new Button.ButtonStyle(Styles.defaultb);
	exports.clearb.up = Styles.none;
	exports.clearb.down = exports.clearb.over = Styles.flatOver;

	exports.clearpb = new Button.ButtonStyle(Styles.clearPartialt);
})