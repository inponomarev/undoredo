package ru.inponomarev.undoredo;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Before;
import org.junit.Test;

public class WorksheetTest {

	final static private String[][] TESTDATA = { { "qw", "we", "er", "rt" }, { "as", "sd", "df", "fg" },
			{ "zx", "xc", "cv", "vb" }, { "12", "23", "34", "45" } };

	private Worksheet ws;

	private void assertCellValue(String expected, int row, int col) {
		assertEquals(expected, ws.getCellValue(row, col));
	}

	@Before
	public void setUp() {
		ws = new Worksheet();
	}

	@Test
	public void testCellValues() {
		assertEquals("", ws.getCellValue(4, 5));
		ws.setCellValue(4, 5, "aaa");
		assertCellValue("aaa", 4, 5);
		ws.setCellValue(7, 1, "bbb");
		ws.setCellValue(4, 5, "ccc");
		ws.setCellValue(4, 5, "ddd");
		assertCellValue("bbb", 7, 1);
		assertCellValue("ddd", 4, 5);
		assertEquals(4, ws.getUndoStack().size());
		assertEquals(0, ws.getRedoStack().size());
		ws.undo();
		assertCellValue("ccc", 4, 5);
		ws.undo();
		assertCellValue("aaa", 4, 5);
		ws.undo();
		assertCellValue("aaa", 4, 5);
		ws.undo();
		assertCellValue("", 4, 5);
		assertCellValue("", 7, 1);
		assertEquals(0, ws.getUndoStack().size());
		assertEquals(4, ws.getRedoStack().size());
		ws.redo();
		assertCellValue("aaa", 4, 5);
		assertCellValue("", 7, 1);
		ws.redo();
		assertCellValue("aaa", 4, 5);
		assertCellValue("bbb", 7, 1);
		assertEquals(2, ws.getUndoStack().size());
		assertEquals(2, ws.getRedoStack().size());
		ws.setCellValue(7, 1, "eee");
		assertEquals(3, ws.getUndoStack().size());
		assertEquals(0, ws.getRedoStack().size());
	}

	@Test
	public void testInsertValues() {
		String[][] matrix1 = { { "1", "2", "3" }, { "4", "5", "6" }, { "7", "8", "9" } };
		String[][] matrix2 = { { "a", "b" }, { "c", "d" }, { "e", "f" } };

		ws.insertValues(0, 0, matrix1);
		assertEquals(1, ws.getUndoStack().size());
		assertCellValue("8", 2, 1);
		assertCellValue("9", 2, 2);
		ws.insertValues(1, 1, matrix2);
		assertCellValue("c", 2, 1);
		assertCellValue("d", 2, 2);
		assertEquals(2, ws.getUndoStack().size());
		ws.undo();
		assertCellValue("8", 2, 1);
		assertCellValue("9", 2, 2);
		ws.redo();
		assertCellValue("c", 2, 1);
		assertCellValue("d", 2, 2);
		// redo overrun
		ws.redo();
		assertCellValue("c", 2, 1);
		assertCellValue("d", 2, 2);

	}

	@Test
	public void testRowHColW() {
		assertEquals(Worksheet.DEFAULT_ROW_HEIGHT, ws.getRowHeight(7));
		assertEquals(Worksheet.DEFAULT_COL_WIDTH, ws.getColWidth(4));
		ws.setRowHeight(7, Worksheet.DEFAULT_ROW_HEIGHT + 5);
		ws.setColWidth(4, Worksheet.DEFAULT_COL_WIDTH - 2);
		assertEquals(Worksheet.DEFAULT_ROW_HEIGHT + 5, ws.getRowHeight(7));
		assertEquals(Worksheet.DEFAULT_COL_WIDTH - 2, ws.getColWidth(4));
		ws.undo();
		assertEquals(Worksheet.DEFAULT_ROW_HEIGHT + 5, ws.getRowHeight(7));
		assertEquals(Worksheet.DEFAULT_COL_WIDTH, ws.getColWidth(4));
		ws.undo();
		assertEquals(Worksheet.DEFAULT_ROW_HEIGHT, ws.getRowHeight(7));
		assertEquals(Worksheet.DEFAULT_COL_WIDTH, ws.getColWidth(4));
		// undo underrun
		ws.undo();
		assertEquals(Worksheet.DEFAULT_ROW_HEIGHT, ws.getRowHeight(7));
		assertEquals(Worksheet.DEFAULT_COL_WIDTH, ws.getColWidth(4));

		ws.redo();
		assertEquals(Worksheet.DEFAULT_ROW_HEIGHT + 5, ws.getRowHeight(7));
		assertEquals(Worksheet.DEFAULT_COL_WIDTH, ws.getColWidth(4));
		ws.redo();
		assertEquals(Worksheet.DEFAULT_ROW_HEIGHT + 5, ws.getRowHeight(7));
		assertEquals(Worksheet.DEFAULT_COL_WIDTH - 2, ws.getColWidth(4));
	}

	@Test
	public void testColumns() {

		ws.insertValues(0, 0, TESTDATA);

		assertCellValue("er", 0, 2);
		assertCellValue("45", 3, 3);

		ws.insertColumnLeft(2);
		assertCellValue(Worksheet.EMPTYVALUE, 0, 2);
		assertCellValue("er", 0, 3);
		assertCellValue("34", 3, 3);
		assertCellValue("45", 3, 4);

		ws.undo();
		assertCellValue("er", 0, 2);
		assertCellValue("45", 3, 3);

		ws.redo();
		assertCellValue(Worksheet.EMPTYVALUE, 0, 2);
		assertCellValue("er", 0, 3);
		assertCellValue("34", 3, 3);
		assertCellValue("45", 3, 4);
		assertCellValue("qw", 0, 0);
		ws.setCellValue(0, 2, "new");
		assertCellValue("we", 0, 1);
		ws.deleteColumn(1);
		assertCellValue("new", 0, 1);
		assertCellValue("df", 1, 2);
		ws.undo();
		assertCellValue("new", 0, 2);
		ws.undo();
		assertCellValue("we", 0, 1);
		assertCellValue("er", 0, 3);
		assertCellValue("34", 3, 3);
		assertCellValue("45", 3, 4);
		assertCellValue("qw", 0, 0);

	}

	@Test
	public void testRows() {

		ws.insertValues(0, 0, TESTDATA);

		assertCellValue("zx", 2, 0);
		assertCellValue("23", 3, 1);

		ws.insertRowAbove(1);
		assertCellValue("as", 2, 0);
		assertCellValue("xc", 3, 1);

		ws.undo();
		assertCellValue("zx", 2, 0);
		assertCellValue("23", 3, 1);
		assertCellValue("qw", 0, 0);

		ws.redo();
		assertCellValue("as", 2, 0);
		assertCellValue("xc", 3, 1);

		ws.deleteRow(1);
		assertCellValue("45", 3, 3);
	}

	@Test
	public void testMoveColumnRow() {

		ws.insertValues(0, 0, TESTDATA);
		assertCellValue("sd", 1, 1);
		assertCellValue("23", 3, 1);
		assertCellValue("rt", 0, 3);
		assertCellValue("fg", 1, 3);
		ws.setColWidth(1, 111);
		ws.moveColumn(1, 3);
		assertEquals(111, ws.getColWidth(3));
		assertCellValue(Worksheet.EMPTYVALUE, 1, 1);
		assertCellValue(Worksheet.EMPTYVALUE, 3, 1);

		assertCellValue("we", 0, 3);
		assertCellValue("sd", 1, 3);
		assertCellValue("23", 3, 3);

		ws.undo();
		assertCellValue("sd", 1, 1);
		assertCellValue("23", 3, 1);
		assertCellValue("rt", 0, 3);
		assertCellValue("fg", 1, 3);

		ws.redo();
		assertCellValue(Worksheet.EMPTYVALUE, 1, 1);
		assertCellValue(Worksheet.EMPTYVALUE, 3, 1);
		assertCellValue("we", 0, 3);
		assertCellValue("sd", 1, 3);
		assertCellValue("23", 3, 3);

		ws.moveRow(0, 1);
		assertCellValue("qw", 1, 0);
		ws.undo();
		assertCellValue("qw", 0, 0);
		ws.redo();
		assertCellValue("qw", 1, 0);

	}

	@Test
	public void testSave() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		assertFalse(ws.isModified());
		ws.insertValues(0, 0, TESTDATA);
		assertTrue(ws.isModified());
		ws.save(bos);
		assertFalse(ws.isModified());
		ws.setCellValue(0, 0, "");
		assertTrue(ws.isModified());

		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());

		Worksheet ws2 = Worksheet.load(bis);
		assertFalse(ws2.isModified());
		// TODO: uncomment when save() is implemented
		// for (int i = 0; i < 3; i++)
		// for (int j = 0; j < 3; j++)
		// assertEquals(ws.getCellValue(i, j), ws2.getCellValue(i, j));

	}
}
