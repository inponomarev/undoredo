package ru.inponomarev.undoredo;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class Worksheet {

	// Значение пустой ячейки.
	// TODO: заменить String на CellValue
	public final static String EMPTYVALUE = "";
	// Ширина колонки по умолчанию
	// TODO: заменить на изменяемое поле класса Worksheet
	final static int DEFAULT_COL_WIDTH = 50;
	// Высота столбца по умолчанию
	// TODO: заменить на изменяемое поле класса Worksheet
	final static int DEFAULT_ROW_HEIGHT = 12;

	private final TreeMap<Integer, Row> rows = new TreeMap<>();
	private final TreeMap<Integer, Column> columns = new TreeMap<>();
	private final LinkedList<Command> undoStack = new LinkedList<>();
	private final LinkedList<Command> redoStack = new LinkedList<>();

	private MacroCommand activeMacro;
	private Command lastSavedPoint;
	private boolean undoActive = true;

	/**
	 * Возвращает значение ячейки.
	 * 
	 * @param row
	 *            номер строки
	 * @param col
	 *            номер столбца
	 */
	public String getCellValue(int row, int col) {
		Row r = rows.get(row);
		Column c = columns.get(col);
		if (r != null && c != null) {
			Cell cell = r.getCells().get(c);
			if (cell != null) {
				return cell.getValue();
			}
		}
		return EMPTYVALUE;
	}

	/**
	 * Устанавливает значение ячейки.
	 * 
	 * @param row
	 *            номер строки
	 * @param col
	 *            номер столбца
	 * @param value
	 *            новое значение
	 */
	public void setCellValue(int row, int col, String value) {
		Command cmd = new SetCellValue(row, col, value);
		execute(cmd);
	}

	/**
	 * Вставляет фрагмент значений из двумерного массива.
	 * 
	 * @param top
	 *            номер строки ЛВУ диапазона для вставки
	 * @param left
	 *            номер столбца ЛВУ диапазона для вставки
	 * @param value
	 *            двумерный массив значений для вставки
	 */
	public void insertValues(int top, int left, String[][] value) {
		beginMacro("Вставка фрагмента таблицы");
		for (int i = 0; i < value.length; i++) {
			for (int j = 0; j < value[i].length; j++) {
				setCellValue(top + i, left + j, value[i][j]);
			}
		}
		endMacro();
	}

	void execute(Command cmd) {
		if (undoActive) {
			if (activeMacro != null)
				activeMacro.commands.push(cmd);
			else {
				undoStack.push(cmd);
				redoStack.clear();
			}
		}
		cmd.execute();
	}

	/**
	 * Начинает запись макрокоманды.
	 * 
	 * @param description
	 *            Описание макрокоманды
	 */
	public void beginMacro(String description) {
		if (!undoActive)
			return;
		MacroCommand m = new MacroCommand(description);
		m.previousMacro = activeMacro;
		activeMacro = m;
	}

	/**
	 * Оканчивает запись макрокоманды.
	 */
	public void endMacro() {
		if (!undoActive)
			return;
		if (activeMacro == null)
			throw new IllegalStateException();
		undoStack.push(activeMacro);
		activeMacro = activeMacro.previousMacro;
		redoStack.clear();
	}

	/**
	 * Отменить последнее действие.
	 */
	public void undo() {
		if (!undoActive)
			return;
		if (activeMacro != null)
			throw new IllegalStateException();
		if (undoStack.isEmpty())
			return;
		Command cmd = undoStack.pop();
		cmd.undo();
		redoStack.push(cmd);
	}

	/**
	 * Повторить последнее действие.
	 */
	public void redo() {
		if (!undoActive)
			return;
		if (activeMacro != null)
			throw new IllegalStateException();
		if (redoStack.isEmpty())
			return;
		Command cmd = redoStack.pop();
		cmd.execute();
		undoStack.push(cmd);
	}

	/**
	 * Возвращает undo-стек.
	 */
	public List<Command> getUndoStack() {
		return undoStack;
	}

	/**
	 * Возвращает redo-стек.
	 */
	public List<Command> getRedoStack() {
		return redoStack;
	}

	final class SetCellValue extends Command {

		private final int row;
		private final int col;
		private String val;
		
		public SetCellValue(int row, int col, String newValue) {
			this.row = row;
			this.col = col;
			this.val = newValue;
		}

		@Override
		public String getDescription() {
			return "Ввод";
		}

		private void changeVal() {
			String oldValue = getCellValue(row, col);
			Row r = rows.get(row);
			Column c = columns.get(col);
			
			if (EMPTYVALUE.equals(val)) {
				// Удалить значение из таблицы
				if (r != null)
					r.getCells().remove(c);
			} else {
				if (r == null) {
					r = new Row(Worksheet.this);
					rows.put(row, r);
				}
				if (c == null) {
					c = new Column(Worksheet.this);
					columns.put(col, c);
				}
				Cell cell = r.getCells().get(c);
				if (cell == null) {
					cell = new Cell();
					r.getCells().put(c, cell);
				}
				cell.setValue(val);
			}
			val = oldValue;
		}

		@Override
		public void execute() {
			changeVal();
		}

		@Override
		public void undo() {
			changeVal();
		}
	}

	final class MacroCommand extends Command {

		private final String description;

		private MacroCommand previousMacro;

		private LinkedList<Command> commands = new LinkedList<>();

		public MacroCommand(String description) {
			this.description = description;
		}

		@Override
		public void execute() {
			Iterator<Command> i = commands.descendingIterator();
			while (i.hasNext()) {
				Command cmd = i.next();
				cmd.execute();
			}

		}

		@Override
		public void undo() {
			for (Command cmd : commands) {
				cmd.undo();
			}
		}

		@Override
		public String getDescription() {
			return description;
		}
	}

	final class Insert<T extends AxisElement> extends Command {

		private final int num;
		private final TreeMap<Integer, T> map;

		Insert(TreeMap<Integer, T> map, int num) {
			this.map = map;
			this.num = num;
		}

		@Override
		public String getDescription() {
			return String.format("Вставка %s %d", map == columns ? "столбца" : "строки", num);
		}

		@Override
		public void execute() {
			internalInsert(map, num);
		}

		@Override
		public void undo() {
			internalDelete(map, num);
		}

	}

	final class Delete<T extends AxisElement> extends Command {
		private final int num;
		private final T deleted;
		private final TreeMap<Integer, T> map;

		Delete(TreeMap<Integer, T> map, int num) {
			this.num = num;
			this.map = map;
			deleted = map.get(num);
		}

		@Override
		public String getDescription() {
			return String.format("Удаление %s %d", map == columns ? "столбца" : "строки", num);
		}

		@Override
		public void execute() {
			internalDelete(map, num);
		}

		@Override
		public void undo() {
			internalInsert(map, num);
			map.put(num, deleted);
		}

	}

	private static <T extends AxisElement> void internalInsert(TreeMap<Integer, T> map, int num) {
		int i = map.lastKey();
		while (i >= num) {
			T e = map.remove(i);
			map.put(i + 1, e);
			i = map.lowerKey(i);
		}

	}

	private static <T extends AxisElement> void internalDelete(TreeMap<Integer, T> map, int colNum) {
		int i = colNum + 1;
		int max = map.lastKey();
		while (i < max) {
			T e = map.remove(i);
			map.put(i - 1, e);
			i = map.higherKey(i);
		}
		// last iteration
		T e = map.remove(i);
		map.put(i - 1, e);
	}

	final class Move<T extends AxisElement> extends Command {
		private final int from;
		private final int to;
		private final T replaced;
		private final TreeMap<Integer, T> map;

		Move(TreeMap<Integer, T> map, int from, int to) {
			this.from = from;
			this.to = to;
			this.map = map;
			this.replaced = map.get(to);
		}

		@Override
		public String getDescription() {
			return String.format("Перемещение %s", map == columns ? "столбца" : "строки");
		}

		@Override
		public void execute() {
			T e = map.remove(from);
			map.put(to, e);
		}

		@Override
		public void undo() {
			T e = map.remove(to);
			map.put(from, e);
			map.put(to, replaced);
		}
	}

	/**
	 * Возвращает ширину столбца.
	 * 
	 * @param col
	 *            Номер столбца
	 */
	public int getColWidth(int col) {
		Column c = columns.get(col);
		if (c == null) {
			return DEFAULT_COL_WIDTH;
		} else {
			return c.getWidth();
		}
	}

	/**
	 * Возвращает высоту строки.
	 * 
	 * @param row
	 *            Номер строки
	 */
	public int getRowHeight(int row) {
		Row r = rows.get(row);
		if (r == null) {
			return DEFAULT_ROW_HEIGHT;
		} else {
			return r.getHeight();
		}
	}

	/**
	 * Устанавливает высоту строки.
	 * 
	 * @param row
	 *            номер строки
	 * @param height
	 *            высота
	 */
	public void setRowHeight(int row, int height) {
		Row r = rows.get(row);
		if (r == null) {
			r = new Row(this);
			rows.put(row, r);
		}
		r.setSize(height);
	}

	/**
	 * Устанавливает ширину столбца
	 * 
	 * @param col
	 *            Номер столбца
	 * @param width
	 *            ширина
	 */
	public void setColWidth(int col, int width) {
		Column c = columns.get(col);
		if (c == null) {
			c = new Column(this);
			columns.put(col, c);
		}
		c.setSize(width);
	}

	public void insertColumnLeft(int colNum) {
		Command cmd = new Insert<Column>(columns, colNum);
		execute(cmd);
	}

	public void deleteColumn(int colNum) {
		Command cmd = new Delete<Column>(columns, colNum);
		execute(cmd);
	}

	public void insertRowAbove(int rowNum) {
		Command cmd = new Insert<Row>(rows, rowNum);
		execute(cmd);
	}

	public void deleteRow(int rowNum) {
		Command cmd = new Delete<Row>(rows, rowNum);
		execute(cmd);
	}

	public void moveRow(int from, int to) {
		Command cmd = new Move<Row>(rows, from, to);
		execute(cmd);
	}

	public void moveColumn(int from, int to) {
		Command cmd = new Move<Column>(columns, from, to);
		execute(cmd);
	}

	public boolean isModified() {
		return undoActive ? undoStack.peek() != lastSavedPoint : true;
	}

	public boolean isUndoActive() {
		return undoActive;
	}

	public void setUndoActive(boolean val) {
		if (undoActive && !val) {
			undoStack.clear();
			redoStack.clear();
		}
		undoActive = val;
	}

	public void save(OutputStream os) {
		lastSavedPoint = undoStack.peek();

		// TODO serialization of internal state

	}

	public static Worksheet load(InputStream is) {
		Worksheet result = new Worksheet();
		// TODO deserialization

		return result;
	}

}
