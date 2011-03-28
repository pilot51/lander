package com.pilot51.lander;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Main extends Activity implements OnClickListener {
	private Button btnClassic, btnEnhanced;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btnClassic = (Button)findViewById(R.id.btnClassic);
        btnEnhanced = (Button)findViewById(R.id.btnEnhanced);
        btnClassic.setOnClickListener(this);
        btnEnhanced.setOnClickListener(this);
        btnEnhanced.setVisibility(Button.GONE);
    }
    
    @Override
	public void onClick(View src) {
		Intent intent = new Intent(this, Lander.class);
		switch (src.getId()) {
		case R.id.btnClassic:
			startActivity(intent);
			break;
		}
	}
}