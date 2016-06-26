package ru.inponomarev.undoredo;

import java.util.HashMap;
import java.util.Map;

public final class Row extends AxisElement {
	private final HashMap<Column, Cell> cells = new HashMap<>();

	Row(Worksheet ws) {
		super(ws);
	}

	public int getHeight() {
		return getSize();
	}

	@Override
	protected int getDefaultSize() {
		return Worksheet.DEFAULT_ROW_HEIGHT;
	}

	Map<Column, Cell> getCells() {
		return cells;
	}
}
