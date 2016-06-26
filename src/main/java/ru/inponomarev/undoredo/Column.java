package ru.inponomarev.undoredo;

public class Column extends AxisElement {

	Column(Worksheet ws) {
		super(ws);
	}
	
	public int getWidth() {
		return getSize();
	}

	@Override
	protected int getDefaultSize() {
		return Worksheet.DEFAULT_COL_WIDTH;
	}

}
