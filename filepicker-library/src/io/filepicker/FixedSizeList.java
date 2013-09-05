package io.filepicker;

import java.util.LinkedList;

class FixedSizeList<T> {
	private final int maxSize;
	private final LinkedList<T> list = new LinkedList<T>();

	public FixedSizeList(int maxSize) {
		this.maxSize = maxSize < 0 ? 0 : maxSize; // don't make bigger than 0
	}

	public T add(T t) {
		list.add(t);
		return list.size() > maxSize ? list.remove() : null;
	}
}
