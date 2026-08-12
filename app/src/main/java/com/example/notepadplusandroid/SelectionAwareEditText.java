package com.example.notepadplusandroid;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class SelectionAwareEditText extends EditText {
    public interface SelectionListener {
        void onSelectionChanged(int start, int end);
    }

    private SelectionListener listener;

    public SelectionAwareEditText(Context context) {
        super(context);
    }

    public SelectionAwareEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectionAwareEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnSelectionChangedListener(SelectionListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSelectionChanged(int start, int end) {
        super.onSelectionChanged(start, end);
        if (listener != null) {
            listener.onSelectionChanged(start, end);
        }
    }
}
