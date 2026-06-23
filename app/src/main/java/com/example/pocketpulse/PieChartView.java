package com.example.pocketpulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();
    private List<Float> degreesList = new ArrayList<>();
    private List<Integer> colorsList = new ArrayList<>();

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // Default placeholder state if database has no entries yet
        degreesList.add(360f);
        colorsList.add(Color.parseColor("#E2E8F0")); // Light Slate Gray
    }

    public void setChartData(float food, float travel, float shopping, float utilities, float others) {
        float total = food + travel + shopping + utilities + others;
        degreesList.clear();
        colorsList.clear();

        if (total == 0) {
            degreesList.add(360f);
            colorsList.add(Color.parseColor("#E2E8F0"));
        } else {
            if (food > 0) { degreesList.add((food / total) * 360f); colorsList.add(Color.parseColor("#F97316")); } // Orange
            if (travel > 0) { degreesList.add((travel / total) * 360f); colorsList.add(Color.parseColor("#3B82F6")); } // Blue
            if (shopping > 0) { degreesList.add((shopping / total) * 360f); colorsList.add(Color.parseColor("#EC4899")); } // Pink
            if (utilities > 0) { degreesList.add((utilities / total) * 360f); colorsList.add(Color.parseColor("#A855F7")); } // Purple
            if (others > 0) { degreesList.add((others / total) * 360f); colorsList.add(Color.parseColor("#94A3B8")); } // Gray
        }
        invalidate(); // Force redraw view layout
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float size = Math.min(getWidth(), getHeight());
        float padding = 10f;
        rectF.set(padding, padding, size - padding, size - padding);

        float startAngle = 0f;
        for (int i = 0; i < degreesList.size(); i++) {
            paint.setColor(colorsList.get(i));
            canvas.drawArc(rectF, startAngle, degreesList.get(i), true, paint);
            startAngle += degreesList.get(i);
        }
    }
}
