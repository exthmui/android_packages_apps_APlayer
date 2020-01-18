package remix.myplayer.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.common.file.FileUtils;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.qos.logback.core.util.FileUtil;
import io.reactivex.annotations.NonNull;
import remix.myplayer.App;
import remix.myplayer.R;


/**
 * Created by Remix on 2016/3/26.
 * Rewrite by kiRa Rumia on 2020/1/18.
 */
public class AboutActivity extends ToolbarActivity {

  private MaterialDialog.Builder mBuilder;
  private MaterialDialog mMaterialDialog;
  @BindView(R.id.about_text)
  TextView mVersion;
  Button btn;
  ImageView img;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate (savedInstanceState);
    setContentView (R.layout.activity_about);
    btn = (Button) findViewById (R.id.check_update);
    btn.setOnClickListener (new update ());
    img = (ImageView) findViewById (R.id.about_img);
    img.setOnClickListener (new touhou ());
    ButterKnife.bind (this);
    try {
      PackageManager pm = App.getContext ().getPackageManager ();
      PackageInfo pi = pm
              .getPackageInfo (App.getContext ().getPackageName (), PackageManager.GET_ACTIVITIES);
      mVersion.setText ("v" + pi.versionName);
    } catch (Exception ignored) {

    }

    setUpToolbar (getString (R.string.about));
  }

  //检查更新
  class update implements View.OnClickListener {
    @Override
    public void onClick(View arg0) {
      Uri uri = Uri.parse ("https://www.coolapk.com/apk/remix.myplayer");
      Intent it = new Intent (Intent.ACTION_VIEW, uri);
      startActivity (it);
    }
  }

  //又到了我最爱的彩蛋环节～
  class touhou implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      img.setImageDrawable (getResources ().getDrawable ((R.drawable.touhou)));
      Toast.makeText (AboutActivity.this, R.string.touhou, Toast.LENGTH_LONG).show ();
      showdialog ();
    }
  }

  private void showdialog() {
    mBuilder = new MaterialDialog.Builder (AboutActivity.this);
    mBuilder.title ("你解锁了一首隐藏歌曲！");
    mBuilder.titleColor(Color.parseColor("#db4437"));
    mBuilder.content ("你解锁了一首MiraGevot - 緋色の丘を越えて，它是 济南THO-东方清泉飨 的主题曲。");
    mBuilder.positiveText ("好");
    mBuilder.negativeText("济南THO介绍");
    mBuilder.cancelable(false);
    mMaterialDialog = mBuilder.build();
    mMaterialDialog.show();
    mBuilder.onAny(new MaterialDialog.SingleButtonCallback() {
      @Override
      public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
        if (which == DialogAction.POSITIVE) {
          copyfile ();
          mMaterialDialog.dismiss();
        } else if (which == DialogAction.NEGATIVE) {
          Uri uri = Uri.parse ("https://thwiki.cc/%E4%B8%9C%E6%96%B9%E6%B8%85%E6%B3%89%E9%A3%A8");
          Intent it = new Intent (Intent.ACTION_VIEW, uri);
          startActivity (it);
          mMaterialDialog.dismiss();
        }
      }
    });
  }

//释放文件
    private void copyfile ()
    {
      try {
        InputStream in = AboutActivity.this.getClass ().getClassLoader ().getResourceAsStream ("assets/MiraGevot.mp3");
        File file = new File (this.getExternalFilesDir(null)+ "/MiraGevot.mp3");
        FileOutputStream fos = new FileOutputStream (file);
        int len = 0;
        byte[] buffer = new byte[1024];
        while ((len = in.read (buffer)) != -1) {
          fos.write (buffer, 0, len);
          fos.flush ();
        }
        in.close ();
        fos.close ();
        moveFile (this.getExternalFilesDir (null)+"/MiraGevot.mp3", "/storage/emulated/0/Music/MiraGevot.mp3");
      } catch (FileNotFoundException e) {
        e.printStackTrace ();
      } catch (IOException e) {
        e.printStackTrace ();
      }
    }


  public void moveFile(String oldPath, String newPath) {
    try {
//    int  bytesum  =  0;
      int byteread = 0;
      File oldfile = new File(oldPath);
      if (oldfile.exists()) {  //文件存在时
        InputStream inStream = new FileInputStream (oldPath);  //读入原文件
        FileOutputStream fs = new FileOutputStream(newPath);
        byte[] buffer = new byte[1444];
//               int  length;
        while ((byteread = inStream.read(buffer)) != -1) {
//                   bytesum  +=  byteread;  //字节数  文件大小
//                   System.out.println(bytesum);
          fs.write(buffer, 0, byteread);
        }
        inStream.close();
        File oldFile = new File(oldPath);
        oldFile.delete();
        Toast.makeText (AboutActivity.this, "文件复制完成，请去APlayer查看。", Toast.LENGTH_LONG).show ();
      }
    } catch (Exception e) {
      System.out.println("复制文件时出错");
      e.printStackTrace();

    }

  }
  }


