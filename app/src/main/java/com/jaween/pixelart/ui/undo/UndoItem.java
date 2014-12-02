package com.jaween.pixelart.ui.undo;

/**
 * Created by ween on 11/29/14.
 */
public class UndoItem {
    public static enum Type {
        DRAW_OP, LAYER
    }

    private Type type;
    private int titleId;
    private Object data;

    public UndoItem(Type type, int titleId, Object data) {
        this.type = type;
        this.titleId = titleId;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public int getTitleId() {
        return titleId;
    }

    public Object getData() {
        return data;
    }
}
