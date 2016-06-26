package ru.inponomarev.undoredo;

abstract class Command {

	public abstract String getDescription();

	public abstract void execute();

	public abstract void undo();
}
