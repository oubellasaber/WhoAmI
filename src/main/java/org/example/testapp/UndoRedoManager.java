package org.example.testapp;

import java.util.Stack;

public class UndoRedoManager {
  private final Stack<Command> undoStack = new Stack<>();
  private final Stack<Command> redoStack = new Stack<>();
  private final int maxHistorySize;

  public UndoRedoManager(int maxHistorySize) {
    this.maxHistorySize = maxHistorySize;
  }

  public void executeCommand(Command command) {
    command.execute();
    undoStack.push(command);
    redoStack.clear();
    trimHistory();
  }

  public void undo() {
    if (!undoStack.isEmpty()) {
      Command command = undoStack.pop();
      command.undo();
      redoStack.push(command);
    }
  }

  public void redo() {
    if (!redoStack.isEmpty()) {
      Command command = redoStack.pop();
      command.execute();
      undoStack.push(command);
    }
  }

  public boolean canUndo() {
    return !undoStack.isEmpty();
  }

  public boolean canRedo() {
    return !redoStack.isEmpty();
  }

  public void clear() {
    undoStack.clear();
    redoStack.clear();
  }

  private void trimHistory() {
    while (undoStack.size() > maxHistorySize) {
      undoStack.remove(0);
    }
  }

  public interface Command {
    void execute();

    void undo();
  }
}
