package com.mredrock.cyxbs.component.widget.card;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.mredrock.cyxbs.R;

import org.jetbrains.anko.DimensionsKt;

/**
 * 替代CardView，对CardView的阴影及圆角进行了改进。具体功能：
 * 1. 可设置阴影颜色及大小
 * 2. 可隐藏部分边阴影
 * 3. 可设置内凹圆角
 * <p>
 * Created By jay68 on 2018/05/30.
 */
public class JCardView extends FrameLayout {

    public JCardView(@NonNull Context context) {
        this(context, null);
    }

    public JCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.JCardView);
        final int dp9 = DimensionsKt.dip(this, 9);
        Rect radius = new Rect();
        radius.left = typedArray.getDimensionPixelSize(R.styleable.JCardView_leftRadius, dp9);
        radius.top = typedArray.getDimensionPixelSize(R.styleable.JCardView_topRadius, dp9);
        radius.right = typedArray.getDimensionPixelSize(R.styleable.JCardView_rightRadius, dp9);
        radius.bottom = typedArray.getDimensionPixelSize(R.styleable.JCardView_bottomRadius, dp9);
        boolean[] hideShadow = new boolean[4];
        hideShadow[0] = typedArray.getBoolean(R.styleable.JCardView_hideLeftShadow, false);
        hideShadow[1] = typedArray.getBoolean(R.styleable.JCardView_hideTopShadow, false);
        hideShadow[2] = typedArray.getBoolean(R.styleable.JCardView_hideRightShadow, false);
        hideShadow[3] = typedArray.getBoolean(R.styleable.JCardView_hideBottomShadow, false);
        int backgroundColor = typedArray.getColor(R.styleable.JCardView_backgroundColor, Color.parseColor("#fefefe"));
        int shadowColor = typedArray.getColor(R.styleable.JCardView_shadowColor, Color.parseColor("#0c000000"));
        int shadowRadius = typedArray.getDimensionPixelSize(R.styleable.JCardView_shadowRadius, DimensionsKt.dip(this, 10));
        typedArray.recycle();

        int left = shadowRadius;
        int top = shadowRadius;
        int right = shadowRadius;
        int bottom = shadowRadius;
        if (hideShadow[0]) left = 0;
        if (hideShadow[1]) top = 0;
        if (hideShadow[2]) right = 0;
        if (hideShadow[3]) bottom = 0;
        setPadding(left, top, right, bottom);   //给阴影留出空间
        RectShadowDrawable backgroundDrawable = new RectShadowDrawable(radius, backgroundColor, shadowColor,
                shadowRadius, 0, 0, hideShadow, this);
        ViewCompat.setBackground(this, backgroundDrawable);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}