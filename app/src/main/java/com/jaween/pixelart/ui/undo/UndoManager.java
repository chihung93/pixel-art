package com.jaween.pixelart.ui.undo;

import java.util.LinkedList;

/**
 * Manages the undo and redo stacks.
 *
 * Used by first constructing an instance with a max undo stack size. When the user performs an
 * undoable operation, an instance of UndoItem must be created that contains both the type of
 * operation and the data needed to undo the operation. This is passed the function pushUndoItem().
 * When performing the undo, call the function popUndoItem() which will first push the UndoItem to
 * the redo stack and also return it to the caller. The details of unrolling an action is left up to
 * the receiver. To redo an action, call popRedoItem() to have it pushed to the UndoStack and have
 * it returned.

 */
public class UndoManager {

    private LinkedList<UndoItem> undoItems = new LinkedList<UndoItem>();
    private LinkedList<UndoItem> redoItems = new LinkedList<UndoItem>();

    private int maxUndos;

    public UndoManager(int maxUndos) {
        this.maxUndos = maxUndos;
    }

    /**
     * Pushes an item onto the undo stack. If there isn't enough space, removes the first/oldest item
     * on the stack
     * @param undoItem The item to be stacked
     */
    public void pushUndoItem(UndoItem undoItem) {
        // Loses any future commands that were in the redo stack
        if (redoItems.size() > 0) {
            redoItems.clear();
        }

        // Maintains the maximum number of undos (and hence the maximum number of redos)
        undoItems.push(undoItem);
        if (undoItems.size() > maxUndos) {
            undoItems.pollLast();
        }
    }

    /**
     * Pops the top of the undo stack and pushes it to the redo stack
     * @return The UndoItem on the top of the stack or null if there was no such element
     */
    public UndoItem popUndoItem() {
        UndoItem undoItem = undoItems.pollFirst();

        // Pushes item onto redo stack
        if (undoItem != null) {
            redoItems.push(undoItem);
        }
        return undoItem;
    }

    /**
     * Pops the top of the redo stack and pushes it to the undo stack
     * @return The UndoItem on the top of the stack or null if there was no such element
     */
    public UndoItem popRedoItem() {
        UndoItem redoItem = redoItems.pollFirst();

        // Pushes item onto undo stack
        if (redoItem != null) {
            undoItems.push(redoItem);
        }
        return redoItem;
    }
}
