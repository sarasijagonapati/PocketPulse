package com.example.pocketpulse;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.annotation.Nullable;

public class PrivacyActivity extends AppCompatActivity {

    private CardView cardFront;
    private CardView cardBack;
    private boolean isShowingFront = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        cardFront = findViewById(R.id.card_front);
        cardBack = findViewById(R.id.card_back);
        View cardContainer = findViewById(R.id.card_container);
        Button btnBack = findViewById(R.id.btn_back_privacy);

        // Adjusts camera distance so the 3D card doesn't distort or pop outward while spinning
        float scale = getResources().getDisplayMetrics().density;
        int cameraDistanceValue = (int) (8000 * scale);
        cardFront.setCameraDistance(cameraDistanceValue);
        cardBack.setCameraDistance(cameraDistanceValue);

        // Handles the card tap flip event
        cardContainer.setOnClickListener(v -> executeCardFlipAnimation());

        // Closes the screen and goes back
        btnBack.setOnClickListener(v -> finish());
    }

    private void executeCardFlipAnimation() {
        if (isShowingFront) {
            cardFront.animate()
                    .rotationY(90)
                    .setDuration(180)
                    .withEndAction(() -> {
                        cardFront.setVisibility(View.INVISIBLE);
                        cardBack.setVisibility(View.VISIBLE);
                        cardBack.setRotationY(-90);

                        cardBack.animate()
                                .rotationY(0)
                                .setDuration(180)
                                .start();
                    }).start();
        } else {
            cardBack.animate()
                    .rotationY(-90)
                    .setDuration(180)
                    .withEndAction(() -> {
                        cardBack.setVisibility(View.INVISIBLE);
                        cardFront.setVisibility(View.VISIBLE);
                        cardFront.setRotationY(90);

                        cardFront.animate()
                                .rotationY(0)
                                .setDuration(180)
                                .start();
                    }).start();
        }
        isShowingFront = !isShowingFront;
    }
}