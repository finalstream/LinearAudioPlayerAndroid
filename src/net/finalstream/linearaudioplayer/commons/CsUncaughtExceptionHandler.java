package net.finalstream.linearaudioplayer.commons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import net.finalstream.linearaudioplayer.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;

public class CsUncaughtExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {
    private static Context sContext = null;
    private static final String BUG_FILE = "BugReport";
    private static final UncaughtExceptionHandler sDefaultHandler 
             = Thread.getDefaultUncaughtExceptionHandler();
    
    /**
     * �R���X�g���N�^
     * @param context
     */
    public CsUncaughtExceptionHandler(Context context){
        sContext = context;
    }

    /**
     * �L���b�`����Ȃ���O�ɂ���Ďw�肳�ꂽ�X���b�h���I�������Ƃ��ɌĂяo����܂�
     * ��O�X�^�b�N�g���[�X�̓��e���t�@�C���ɏo�͂��܂�
     */
    public void uncaughtException(Thread thread, Throwable ex) { 
        PrintWriter pw = null;   
        try {
            pw = new PrintWriter(sContext.openFileOutput(BUG_FILE, Context.MODE_WORLD_READABLE));
            ex.printStackTrace(pw); 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (pw != null) pw.close();      
        }
        sDefaultHandler.uncaughtException(thread, ex);
    }
    
    /**
     * �o�O���|�[�g�̓��e�����[���ő��M���܂��B
     * @param activity
     */
    public static void SendBugReport(final Activity activity) {
        //�o�O���|�[�g���Ȃ���Έȍ~�̏������s���܂���B
        final File bugfile = activity.getFileStreamPath(BUG_FILE);
        if (!bugfile.exists()) {
            return;
        }    
        //AlertDialog��\�����܂��B
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setTitle("ERROR");
        alert.setMessage("�\�����Ȃ��G���[���������܂����B�J�����ɃG���[�𑗐M���Ă��������B");
        alert.setPositiveButton("���M", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SendMail(activity,bugfile);
            }});
        alert.setNegativeButton("�L�����Z��", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bugfile.delete();
            }});
        alert.show();
    }
    
    /**
     * �o�O���|�[�g�̓��e�����[���ő��M���܂��B
     * @param activity
     * @param bugfile
     */
    private static void SendMail(final Activity activity,File bugfile){
        //�o�O���|�[�g�̓��e��ǂݍ��݂܂��B
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(bugfile));
            String str;      
            while((str = br.readLine()) != null){      
                sb.append(str +"\n");     
            }       
        } catch (Exception e) {
            e.printStackTrace();
        }
        //���[���ő��M���܂��B
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:info@finalstream.net"));
        Resources res = activity.getResources();
        intent.putExtra(Intent.EXTRA_SUBJECT, "�yAndroid BugReport�z" + res.getString(R.string.app_name) );
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        activity.startActivity(intent);
        //�o�O���|�[�g���폜���܂��B
        bugfile.delete();
    }
    
}
