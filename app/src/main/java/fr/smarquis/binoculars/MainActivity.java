package fr.smarquis.binoculars;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;

import com.google.vrtoolkit.cardboard.CardboardActivity;

public class MainActivity extends CardboardActivity {

    private CustomCardboardView cardboardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cardboardView = new CustomCardboardView(this);
        setContentView(cardboardView);
        cardboardView.setOnCardboardBackButtonListener(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    @Override
    public void onCardboardTrigger() {
        super.onCardboardTrigger();
        cardboardView.onCardboardTrigger();
        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
    }

}
