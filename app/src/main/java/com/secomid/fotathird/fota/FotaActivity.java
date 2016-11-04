package com.secomid.fotathird.fota;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.secomid.fotathird.FotaApplication;
import com.secomid.fotathird.R;
import com.secomid.fotathird.fota.policy.PolicyManager;
import com.secomid.fotathird.fota.view.CenterCircleView;
import com.fota.iport.MobAgentPolicy;
import com.fota.iport.info.MobileParamInfo;

import java.io.File;

public class FotaActivity extends Activity implements FotaContract.View<FotaPresenter> {

    private static final String TAG = "FotaActivity";
    private FotaPresenter m_presenter;

    private CenterCircleView m_center_circle;

    private TextView m_title;
    private TextView m_error_tips;
    private TextView m_version_detail;
    private TextView m_current_version;
    private TextView m_release_note;
    private Dialog m_transfer_dialog;
    private Button m_control_btn;

    private LocalReceiver m_receiver;//本地广播
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        m_receiver = new LocalReceiver(this);// 本地广播持有当前activity对象，会在接收到检测，下载，升级等广播时，修改UI
        registerLocalBroadcast();
        new FotaPresenter(this, this);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_presenter.start();
    }

    private void initView() {
        m_title = (TextView) findViewById(R.id.iot_main_title);
        m_center_circle = (CenterCircleView) findViewById(R.id.view_center_circle);
        m_error_tips = (TextView) findViewById(R.id.iot_error_tips);
        m_current_version = (TextView) findViewById(R.id.cur_version);
        m_version_detail = (TextView) findViewById(R.id.iot_version_detail);
        m_release_note = (TextView) findViewById(R.id.iot_release_note_content);
        m_control_btn = (Button) findViewById(R.id.button_control);
        m_control_btn.setVisibility(View.GONE);
        // 圆形UI的按键监听
        m_center_circle.setListener(new CenterCircleView.OnClickListener() {

            @Override
            public void on_check_version() {
                m_presenter.check_version();
            }

            @Override
            public void on_start_download() {
                m_presenter.download();
            }

            @Override
            public void on_reboot_upgrade() {
                m_presenter.reboot_upgrade();
            }
        });
        // 方便查看手机参数->长按标题会弹出手机参数框
        m_title.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(FotaActivity.this)
                        .setTitle(R.string.mobile_params)
                        .setMessage(MobileParamInfo.getInstance().displayString() + "\n" + new PolicyManager().displayPolicy())
                        .setPositiveButton(R.string.delete_update_file, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new File(FotaApplication.s_update_package_absolute_path).delete();
                                m_center_circle.resetUI_check_version();
                            }
                        })
                        .show();
                return false;
            }
        });

        m_control_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_presenter.download_cancel();
                m_control_btn.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void set_presenter(FotaPresenter presenter) {
        m_presenter = presenter;
    }

    @Override
    public void show_default_ui() {
        Log.e("zengny", "show_default_ui()");
        m_current_version.setText(MobileParamInfo.getInstance().version);
//        m_version_detail.setText(String.format(getString(R.string.iot_current_version), MobileParamInfo.getInstance().version));
        m_center_circle.resetUI_check_version();
        m_release_note.setText(R.string.def_releas_note_text);
        m_control_btn.setVisibility(View.GONE);
        m_error_tips.setText("");
    }

    @Override
    public void show_checking() {
        Log.e("zengny", "show_checking()");
        m_center_circle.resetUI_checking_version();
        m_release_note.setText(R.string.def_releas_note_text);
        m_control_btn.setVisibility(View.GONE);
        m_error_tips.setText("");
    }

    @Override
    public void show_can_download() {
        Log.e("zengny", "show_can_download()");
        m_center_circle.resetUI_can_download();
        m_release_note.setText(MobAgentPolicy.getRelNotesInfo().content);
        m_control_btn.setVisibility(View.GONE);
        m_version_detail.setText(String.format(getString(R.string.iot_found_new_version), MobAgentPolicy.getVersionInfo().versionName));
        m_error_tips.setText("");
    }

    @Override
    public void show_downloading(int progress) {
        Log.e("zengny", "show_downloading()");
        m_center_circle.resetUI_downloading(progress);
        m_release_note.setText(MobAgentPolicy.getRelNotesInfo().content);
        m_control_btn.setVisibility(View.VISIBLE);
        m_version_detail.setText(String.format(getString(R.string.iot_found_new_version), MobAgentPolicy.getVersionInfo().versionName));
        m_error_tips.setText("");
    }

    @Override
    public void show_can_upgrade() {
        Log.e("zengny", "show_can_upgrade()");
        m_center_circle.resetUI_can_upgrade();
        m_release_note.setText(MobAgentPolicy.getRelNotesInfo().content);
        m_control_btn.setVisibility(View.GONE);
        m_error_tips.setText("");
    }

    @Override
    public void show_upgrading() {
        Log.e("zengny", "show_upgrading()");
        m_center_circle.resetUI_upgrading();
        m_release_note.setText(MobAgentPolicy.getRelNotesInfo().content);
        m_control_btn.setVisibility(View.GONE);
        m_error_tips.setText("");
    }

    @Override
    public void show_error(String e) {
        Log.e("zengny", "show_error()>> e = " + e);
        m_center_circle.resetUI_check_version();
       // m_error_tips.setText(e);
        m_release_note.setText("");
    }

    /**
     * 注册本地广播
     */
    private void registerLocalBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocalReceiver.ACTION_CHECK_SUCCESS);
        filter.addAction(LocalReceiver.ACTION_CHECK_ERROR);
        filter.addAction(LocalReceiver.ACTION_CHECK_START);
        filter.addAction(LocalReceiver.ACTION_DOWNLOAD_PROGRESS);
        filter.addAction(LocalReceiver.ACTION_DOWNLOAD_FINISHED);
        filter.addAction(LocalReceiver.ACTION_DOWNLOAD_ERROR);
        filter.addAction(LocalReceiver.ACTION_UPGRADE_ERROR);
        filter.addAction(LocalReceiver.ACTION_UPGRADE_START);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_receiver, filter);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_receiver);
        super.onDestroy();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Fota Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
