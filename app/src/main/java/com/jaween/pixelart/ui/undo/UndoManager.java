package com.jaween.pixelart.ui.undo;

import java.util.LinkedList;

/**
 * Manages the undo and redo lists.
 *
 * Used by first constructing the object with the initial state Bitmap and the max undo limit.
 * The user draws on the Bitmap and passes that bitmap to modifiedBitmap().
 * It XORs the Bitmap with the previous frame and stores the compressed the result.
 * The undo() function will then decompress that result, XOR it with the given bitmap to retrieve
 * the previous frame. Finally it blits that bitmap back onto the given Bitmap.
 *
 * TODO: Run in separate thread
 */
public class UndoManager {

    private LinkedList<UndoItem> undoItems = new LinkedList<UndoItem>();
    private LinkedList<UndoItem> redoItems = new LinkedList<UndoItem>();

    private int maxUndos;

    //private static UndoLayerListener undoLayerListener = null;

    public UndoManager(int maxUndos) {
        //drawOpManager = new DrawOpManager(initialBitmap);
        this.maxUndos = maxUndos;
    }

   /* public static void undo(List<Bitmap> layers) {
        long startTime = System.currentTimeMillis();

        // There must be an item to undo
        if (undoItems.size() <= 0) {
            return;
        }

        UndoItem undoItem = undoItems.pop();
        redoItems.push(undoItem);

        switch (undoItem.getType()) {
            case DRAW_OP:
                drawOpManager.undo(layers, undoItem);
                break;
            case LAYER:
                LayerUndoData layerUndoData = (LayerUndoData) undoItem.getData();
                switch (layerUndoData.getType()) {
                    case ADD:
                        if (undoLayerListener != null) {
                            Log.d("UndoManager", "Undoing addition of layer (deleting)");
                            undoLayerListener.onDeleteLayerFromUndo(layerUndoData.getLayerIndex());
                        }
                        break;
                    case DELETE:
                        if (undoLayerListener != null) {
                            Log.d("UndoManager", "Undoing deletion of layer (adding)");
                            undoLayerListener.onAddLayerFromUndo(layerUndoData.getLayerIndex(), layerUndoData.getDeletedLayer());
                        }
                        break;
                }
                break;
        }

        Log.d("UndoRedoTracker", "Undo took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public static void redo(List<Bitmap> layers) {
        long startTime = System.currentTimeMillis();

        // There must be an item to redo
        if (redoItems.size() <= 0) {
            return;
        }

        UndoItem redoItem = redoItems.pop();
        undoItems.push(redoItem);

        switch (redoItem.getType()) {
            case DRAW_OP:
                drawOpManager.redo(layers, redoItem);
                break;
            case LAYER:
                LayerUndoData layerUndoData = (LayerUndoData) redoItem.getData();
                switch (layerUndoData.getType()) {
                    case ADD:
                        if (undoLayerListener != null) {
                            Log.d("UndoManager", "Redoing addition of layer");
                            undoLayerListener.onAddLayerFromUndo(layerUndoData.getLayerIndex(), layerUndoData.getDeletedLayer());
                        }
                        break;
                    case DELETE:
                        if (undoLayerListener != null) {
                            Log.d("UndoManager", "Redoing deletion of layer");
                            undoLayerListener.onDeleteLayerFromUndo(layerUndoData.getLayerIndex());
                        }
                        break;
                }
                break;
        }

        Log.d("UndoRedoTracker", "Redo took " + (System.currentTimeMillis() - startTime) + "ms");
    }*/

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
            undoItems.removeFirst();
        }
    }

    /**
     * Pops the top of the undo stack and pushes it to the redo stack
     * @return The UndoItem on the top of the stack or null if there was no such element
     */
    public UndoItem popUndoItem() {
        UndoItem undoItem = undoItems.poll();

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
        UndoItem redoItem = redoItems.poll();

        // Pushes item onto undo stack
        if (redoItem != null) {
            undoItems.push(redoItem);
        }
        return redoItem;
    }

    /*public void layerModified(Bitmap layer, int layerIndex) {
        long startTime = System.currentTimeMillis();

        UndoItem undoItem = drawOpManager.add(layer, layerIndex);
        pushUndoItem(undoItem);

        Log.d("UndoRedoTracker", "Modification took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void switchLayer(Bitmap newLayer) {
        drawOpManager.setLayerBeforeModification(newLayer);
    }*/

    /*public void addLayer(int layerIndex) {
        LayerUndoData layerUndoData = new LayerUndoData(layerIndex);
        UndoItem undoItem = new UndoItem(UndoItem.Type.LAYER, 1, layerUndoData);
        pushUndoItem(undoItem);
    }

    public void deleteLayer(int layerIndex) {
        LayerUndoData layerUndoData = new LayerUndoData(layerIndex, layers);
        UndoItem undoItem = new UndoItem(UndoItem.Type.LAYER, 2, layerUndoData);
        pushUndoItem(undoItem);
    }

    public void setUndoLayerListener(UndoLayerListener undoLayerListener) {
        this.undoLayerListener = undoLayerListener;
    }

    public interface UndoLayerListener {
        public void onAddLayerFromUndo(int layerIndex, Bitmap layer);
        public void onDeleteLayerFromUndo(int layerIndex);
    }*/
}
