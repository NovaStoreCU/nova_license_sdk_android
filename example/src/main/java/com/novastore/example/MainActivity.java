package com.novastore.example;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Actividad protegida: solo se llega aquí si la licencia es válida. */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView label = new TextView(this);
        label.setText("APP ABIERTA (licencia válida)");
        label.setTextSize(20f);
        label.setGravity(Gravity.CENTER);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setBackgroundColor(Color.WHITE);
        container.addView(label);

        setContentView(container);
    }
}
