package ru.inponomarev.undoredo;

/**
 * Базовый класс для строк (Rows) и столбцов (Columns).
 */
abstract class AxisElement {
	private int size = -1;
	private final Worksheet ws;

	AxisElement(Worksheet ws) {
		this.ws = ws;
	}

	protected int getSize() {
		return size < 0 ? getDefaultSize() : size;
	}

	protected abstract int getDefaultSize();

	protected void setSize(int newSize) {
		Command cmd = new SetSize(newSize);
		ws.execute(cmd);
	}



	final class SetSize extends Command {

		private final int newValue;
		private final int oldValue;

		public SetSize(int newValue) {
			this.newValue = newValue == getDefaultSize() ? -1 : newValue;
			this.oldValue = size;
		}

		@Override
		public String getDescription() {
			return "Изменение размера";
		}

		@Override
		public void execute() {
			size = newValue;
		}

		@Override
		public void undo() {
			size = oldValue;
		}

	}

}
