package com.giousa.bleterminaltest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements InputSystemManager.HeartBeatSystemEventListener {

    @InjectView(R.id.tv_achieve)
    TextView mTvAchieve;
    private InputSystemManager mInputSystemManager;
    private int count = 0;
    String devicesName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        devicesName =getIntent().getExtras().getString("name");
    }

    @OnClick({R.id.btn_start, R.id.btn_send, R.id.btn_stop})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                //初始化
                mInputSystemManager = InputSystemManager.getInstance();
                mInputSystemManager.initWithContext(UIUtils.getContext(),devicesName);

                //获取心率数据
                mInputSystemManager.setHeartBeatSystemEventListener(this);

                Toast.makeText(UIUtils.getContext(),"连接设备",Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_send:
                mInputSystemManager.sendData(count++);
                break;
            case R.id.btn_stop:
                Toast.makeText(UIUtils.getContext(),"断开连接",Toast.LENGTH_SHORT).show();
                mInputSystemManager.disconnectDevice();
                break;
        }
    }

    @Override
    public void onHeartBeatChanged(final int heartBeat) {
        Log.d("MainActivity", "heartBeat:" + heartBeat);
        UIUtils.runInMainThread(new Runnable() {
            @Override
            public void run() {
                mTvAchieve.setText("" + heartBeat);
            }
        });
    }


}
