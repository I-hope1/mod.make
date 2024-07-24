package modmake.util.tools;

import arc.func.*;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.struct.*;
import modmake.components.limit.LimitTable;

import java.util.function.Supplier;
import java.util.regex.Pattern;

public class FilterTable<R> extends LimitTable {
	public FilterTable() {}
	public FilterTable(Cons<FilterTable<R>> cons) {
		super((Cons) cons);
	}
	public FilterTable(Drawable background, Cons<FilterTable<R>> cons) {
		super(background, (Cons) cons);
		this.background = background;
	}
	Drawable                    background;
	ObjectMap<R, Seq<BindCell>> map;
	private Seq<BindCell> current;

	public void bind(R name) {
		if (map == null) map = new ObjectMap<>();
		current = map.get(name, Seq::new);
	}
	public void unbind() {
		current = null;
	}
	public <T extends Element> Cell<T> add(T element) {
		Cell<T> cell = super.add(element);
		if (current != null) current.add(new BindCell(cell));
		return cell;
	}
	public void addUpdateListener(Boolf<R> boolf) {
		update(() -> {
			filter(boolf);
			if (hasAny) {
				background(background);
			} else {
				background(null);
			}
		});
	}
	public void addUpdateListener(Supplier<Pattern> supplier) {
		Pattern[] last = {null};
		update(() -> {
			if (last[0] != supplier.get()) {
				last[0] = supplier.get();
				filter(name -> Tools.testP(last[0], (String) name));
				if (hasAny) {
					background(background);
				} else {
					background(null);
				}
			}
		});
	}
	public void clear() {
		super.clear();
		map.each((__, v) -> v.clear());;
	}
	/** 是否还有子元素 */
	public boolean hasAny = false;
	public void filter(Boolf<R> boolf) {
		hasAny = false;
		map.each((name, seq) -> {
			boolean b2 = boolf.get(name);
			seq.each(b2 ? BindCell::build : BindCell::remove);
			hasAny |= b2;
		});
	}
}
