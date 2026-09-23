package com.novastore.example

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val label = TextView(this).apply {
            text = "APP ABIERTA (licencia válida)"
            textSize = 20f
            gravity = Gravity.CENTER
        }
        val container = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(label)
        }
        setContentView(container)
    }
}