package modmake.util.img;

import modmake.components.DataHandle;
import modmake.ui.img.ImgEditor;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;

import static modmake.IntUI.view;

public class Stack {
	private final ImgEditor imgEditor;
	public static int maxSize = DataHandle.settings.getInt("max_stack_buffer_size", 15);
	public ByteBuffer tmp = null;
	public ArrayDeque<ByteBuffer> undoes = new ArrayDeque<>();
	public ArrayDeque<ByteBuffer> redoes = new ArrayDeque<>();

	public Stack(ImgEditor imgEditor) {
		this.imgEditor = imgEditor;
	}

	public void addUndo(ByteBuffer pixels) {
		undoes.add(pixels);
		while (undoes.size() > maxSize) {
			undoes.pop().clear();
		}
	}

	public void addRedo(ByteBuffer pixels) {
		redoes.add(pixels);
		while (redoes.size() > maxSize) {
			redoes.pop().clear();
		}
	}

	public void clear() {
		undoes.forEach(ByteBuffer::clear);
		redoes.forEach(ByteBuffer::clear);
		undoes.clear();
		redoes.clear();
	}

	public void undo() {
		if (canUndo()) {
			ByteBuffer pixmap = undoes.removeLast();
			addRedo(setPixmap(pixmap));
		}
	}

	public boolean canUndo() {
		return undoes.size() > 0;
	}

	public void redo() {
		if (canRedo()) {
			ByteBuffer pixels = redoes.removeLast();
			addUndo(setPixmap(pixels));
		}
	}

	public ByteBuffer setPixmap(ByteBuffer newPixels) {
		ByteBuffer old = imgEditor.pixmap().pixels;

		//			view.cont.texture.draw(pixmap());
		imgEditor.pixmap().pixels = newPixels;
		view.rebuildCont();
		return old;
	}


	public boolean canRedo() {
		return redoes.size() > 0;
	}
}
