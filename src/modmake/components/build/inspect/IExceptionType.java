package modmake.components.build.inspect;


import arc.graphics.Color;

public enum IExceptionType {
	ERROR(Color.red),
	SPELLING_MISTAKE(Color.blue),
	WARNING(Color.yellow),
	WEAK_WARNING(Color.yellow.cpy().a(0.7f)),
	NONE(Color.white);

	IExceptionType(Color color){
		this.color = color;
	}
	public Color color;
}
