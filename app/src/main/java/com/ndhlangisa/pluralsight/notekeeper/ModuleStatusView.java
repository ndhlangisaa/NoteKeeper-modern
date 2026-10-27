package com.ndhlangisa.pluralsight.notekeeper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import java.util.List;

/**
 * A custom view that draws one circle (or square) per course module.
 * Filled = module complete. Tap a shape to toggle it. Works with TalkBack too.
 */
public class ModuleStatusView extends View {
    private static final int EDIT_MODE_MODULE_COUNT = 7;
    private static final int INVALID_INDEX = -1;
    private static final int SHAPE_CIRCLE = 0;
    private static final float DEFAULT_OUTLINE_WIDTH_DP = 2f;
    private static final float SHAPE_SIZE_DP = 40f;
    private static final float SPACING_DP = 10f;

    private boolean[] mModuleStatus = new boolean[0];
    private Rect[] mModuleRectangles = new Rect[0];
    private float mShapeSize;
    private float mSpacing;
    private float mOutlineWidth;
    private float mRadius;
    private int mShape;
    private Paint mPaintOutline;
    private Paint mPaintFill;
    private ModuleStatusAccessibilityHelper mAccessibilityHelper;

    // Android calls one of these constructors, depending on whether the view comes from code or XML
    public ModuleStatusView(Context context) {
        super(context);
        init(null, 0);
    }

    public ModuleStatusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ModuleStatusView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(@Nullable AttributeSet attrs, int defStyle) {
        if (isInEditMode())
            setupEditModeValues(); // gives the Layout Editor something to draw

        setFocusable(true);
        mAccessibilityHelper = new ModuleStatusAccessibilityHelper(this);
        ViewCompat.setAccessibilityDelegate(this, mAccessibilityHelper);

        // dp -> pixels, so the view is the same physical size on every screen density
        float density = getResources().getDisplayMetrics().density;
        float defaultOutlineWidthPixels = DEFAULT_OUTLINE_WIDTH_DP * density;
        mShapeSize = SHAPE_SIZE_DP * density;
        mSpacing = SPACING_DP * density;

        // Read our custom XML attributes (declared in res/values/attrs_module_status_view.xml)
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ModuleStatusView, defStyle, 0);
        int outlineColor = a.getColor(R.styleable.ModuleStatusView_moduleOutlineColor, Color.BLACK);
        mShape = a.getInt(R.styleable.ModuleStatusView_moduleShape, SHAPE_CIRCLE);
        mOutlineWidth = a.getDimension(R.styleable.ModuleStatusView_moduleOutlineWidth, defaultOutlineWidthPixels);
        a.recycle(); // TypedArrays are pooled - always give them back

        mRadius = (mShapeSize - mOutlineWidth) / 2;

        mPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintOutline.setStyle(Paint.Style.STROKE);
        mPaintOutline.setStrokeWidth(mOutlineWidth);
        mPaintOutline.setColor(outlineColor);

        mPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintFill.setStyle(Paint.Style.FILL);
        mPaintFill.setColor(ContextCompat.getColor(getContext(), R.color.notekeeper_orange));
    }

    public boolean[] getModuleStatus() {
        return mModuleStatus;
    }

    public void setModuleStatus(boolean[] moduleStatus) {
        mModuleStatus = moduleStatus;
        setupModuleRectangles(getWidth());
        requestLayout(); // our size may change
        invalidate();    // and we certainly need to redraw
    }

    private void setupEditModeValues() {
        boolean[] exampleModuleValues = new boolean[EDIT_MODE_MODULE_COUNT];
        int middle = EDIT_MODE_MODULE_COUNT / 2;
        for (int i = 0; i < middle; i++)
            exampleModuleValues[i] = true;

        mModuleStatus = exampleModuleValues;
    }

    /** How many shapes fit on one row for a given width (always at least 1). */
    private int columnsFor(int width) {
        int availableWidth = width - getPaddingLeft() - getPaddingRight();
        int horizontalModulesThatCanFit = (int) ((availableWidth + mSpacing) / (mShapeSize + mSpacing));
        return Math.max(1, Math.min(horizontalModulesThatCanFit, mModuleStatus.length));
    }

    /** Works out where each shape goes, as a grid of rectangles. */
    private void setupModuleRectangles(int width) {
        int columns = columnsFor(width);
        mModuleRectangles = new Rect[mModuleStatus.length];
        for (int moduleIndex = 0; moduleIndex < mModuleRectangles.length; moduleIndex++) {
            int column = moduleIndex % columns;
            int row = moduleIndex / columns;
            int x = getPaddingLeft() + (int) (column * (mShapeSize + mSpacing));
            int y = getPaddingTop() + (int) (row * (mShapeSize + mSpacing));
            mModuleRectangles[moduleIndex] = new Rect(x, y, x + (int) mShapeSize, y + (int) mShapeSize);
        }
    }

    /** Tells our parent how big we'd like to be. */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int columns = columnsFor(MeasureSpec.getSize(widthMeasureSpec));
        int rows = mModuleStatus.length == 0 ? 0 : ((mModuleStatus.length - 1) / columns) + 1;

        int desiredWidth = (int) (columns * (mShapeSize + mSpacing) - mSpacing)
                + getPaddingLeft() + getPaddingRight();
        int desiredHeight = rows == 0 ? 0 : (int) (rows * (mShapeSize + mSpacing) - mSpacing);
        desiredHeight += getPaddingTop() + getPaddingBottom();

        int width = resolveSizeAndState(desiredWidth, widthMeasureSpec, 0);
        int height = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        setupModuleRectangles(w);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        for (int moduleIndex = 0; moduleIndex < mModuleRectangles.length; moduleIndex++) {
            if (mShape == SHAPE_CIRCLE) {
                float x = mModuleRectangles[moduleIndex].centerX();
                float y = mModuleRectangles[moduleIndex].centerY();
                if (mModuleStatus[moduleIndex])
                    canvas.drawCircle(x, y, mRadius, mPaintFill);
                canvas.drawCircle(x, y, mRadius, mPaintOutline);
            } else {
                drawSquare(canvas, moduleIndex);
            }
        }
    }

    private void drawSquare(Canvas canvas, int moduleIndex) {
        Rect moduleRectangle = mModuleRectangles[moduleIndex];

        if (mModuleStatus[moduleIndex])
            canvas.drawRect(moduleRectangle, mPaintFill);

        canvas.drawRect(moduleRectangle.left + (mOutlineWidth / 2),
                moduleRectangle.top + (mOutlineWidth / 2),
                moduleRectangle.right - (mOutlineWidth / 2),
                moduleRectangle.bottom - (mOutlineWidth / 2),
                mPaintOutline);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true; // "yes, I want this touch"
            case MotionEvent.ACTION_UP:
                int moduleIndex = findItemAtPoint(event.getX(), event.getY());
                onModuleSelected(moduleIndex);
                performClick();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick(); // keeps accessibility tools informed of clicks
    }

    private void onModuleSelected(int moduleIndex) {
        if (moduleIndex == INVALID_INDEX)
            return;

        mModuleStatus[moduleIndex] = !mModuleStatus[moduleIndex];
        invalidate(); // redraw

        mAccessibilityHelper.invalidateVirtualView(moduleIndex);
        mAccessibilityHelper.sendEventForVirtualView(moduleIndex, AccessibilityEvent.TYPE_VIEW_CLICKED);
    }

    private int findItemAtPoint(float x, float y) {
        for (int i = 0; i < mModuleRectangles.length; i++) {
            if (mModuleRectangles[i].contains((int) x, (int) y))
                return i;
        }
        return INVALID_INDEX;
    }

    // ---------- Accessibility: let TalkBack users focus and toggle each shape ----------

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        mAccessibilityHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mAccessibilityHelper.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        return mAccessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }

    /** Presents each shape to accessibility services as if it were its own little view. */
    private class ModuleStatusAccessibilityHelper extends ExploreByTouchHelper {

        ModuleStatusAccessibilityHelper(@NonNull View host) {
            super(host);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            int moduleIndex = findItemAtPoint(x, y);
            return moduleIndex == INVALID_INDEX ? ExploreByTouchHelper.INVALID_ID : moduleIndex;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (int moduleIndex = 0; moduleIndex < mModuleRectangles.length; moduleIndex++)
                virtualViewIds.add(moduleIndex);
        }

        @Override
        @SuppressWarnings("deprecation") // ExploreByTouchHelper still requires bounds-in-parent
        protected void onPopulateNodeForVirtualView(int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
            node.setFocusable(true);
            node.setBoundsInParent(mModuleRectangles[virtualViewId]);
            node.setContentDescription(getContext().getString(R.string.module_status_description, virtualViewId + 1));
            node.setCheckable(true);
            node.setChecked(mModuleStatus[virtualViewId]);
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, @Nullable Bundle arguments) {
            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
                onModuleSelected(virtualViewId);
                return true;
            }
            return false;
        }
    }
}