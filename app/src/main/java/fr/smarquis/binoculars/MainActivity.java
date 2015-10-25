package fr.smarquis.binoculars;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardDeviceParams;

public class MainActivity extends CardboardActivity {

    private CustomCardboardView cardboardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cardboardView = new CustomCardboardView(this);
        setContentView(cardboardView);
    }

    @Override
    public void onCardboardTrigger() {
        super.onCardboardTrigger();
        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
    }

    @Override
    public void onInsertedIntoCardboard(CardboardDeviceParams cardboardDeviceParams) {
        super.onInsertedIntoCardboard(cardboardDeviceParams);
    }

    @Override
    public void onRemovedFromCardboard() {
        super.onRemovedFromCardboard();
    }
}
