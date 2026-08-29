package com.eyad.planner;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout tasksContainer; TextView timerText, lessonsText, statusText; ImageView scheduleImage;
    SharedPreferences prefs; Handler handler = new Handler(Looper.getMainLooper());
    long remaining = 25*60; boolean running = false;
    Runnable timerRunnable = new Runnable(){ public void run(){ if(!running)return; remaining--; updateTimer(); if(remaining<=0){ running=false; notifyUser("انتهى وقت الـ Pomodoro", "خد راحة 5 دقائق ☕"); Toast.makeText(MainActivity.this,"انتهى الـ Pomodoro 🎉",Toast.LENGTH_LONG).show(); remaining=25*60; updateTimer(); } else handler.postDelayed(this,1000); }};

    @Override public void onCreate(Bundle b){ super.onCreate(b); setContentView(R.layout.activity_main);
        prefs=getSharedPreferences("planner",MODE_PRIVATE); bind(); loadLessons(); loadTasks(); loadImage(); askNotifications();
    }
    void bind(){
        tasksContainer=findViewById(R.id.tasksContainer); timerText=findViewById(R.id.timerText); lessonsText=findViewById(R.id.lessonsText); scheduleImage=findViewById(R.id.scheduleImage); statusText=findViewById(R.id.statusText);
        findViewById(R.id.addTaskButton).setOnClickListener(v->showAddTask()); findViewById(R.id.startButton).setOnClickListener(v->startPomodoro()); findViewById(R.id.resetButton).setOnClickListener(v->resetPomodoro()); findViewById(R.id.pickScheduleButton).setOnClickListener(v->pickImage()); updateTimer();
    }
    void showAddTask(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(30,10,30,0);
        EditText name=new EditText(this); name.setHint("مثال: مذاكرة رياضيات"); box.addView(name);
        EditText time=new EditText(this); time.setHint("الوقت (مثال 7:00 م)"); box.addView(time);
        new AlertDialog.Builder(this).setTitle("إضافة مهمة").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->addTask(name.getText().toString(),time.getText().toString())).show();
    }
    void addTask(String n,String t){ if(n.trim().isEmpty())return; String old=prefs.getString("tasks",""); String line=n.replace("|","/")+"|"+t.replace("|","/")+"|0"; String all=old.isEmpty()?line:old+"\n"+line; prefs.edit().putString("tasks",all).apply(); loadTasks(); }
    void loadTasks(){ tasksContainer.removeAllViews(); String raw=prefs.getString("tasks",""); if(raw.isEmpty()){ addTaskView("أضف أول مهمة ليومك","مثال: مذاكرة عربي • 7:00 م",false,""); return; } for(String line:raw.split("\\n")){ String[] p=line.split("\\|",-1); if(p.length<3)continue; addTaskView(p[0],p[1],p[2].equals("1"),line); }}
    void addTaskView(String n,String t,boolean checked,String full){
        LinearLayout row=new LinearLayout(this); row.setPadding(12,10,12,10); row.setGravity(Gravity.CENTER_VERTICAL);
        CheckBox cb=new CheckBox(this); cb.setChecked(checked); cb.setText(n+"\n"+t); cb.setTextColor(Color.WHITE); cb.setTextSize(15); cb.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1)); row.addView(cb);
        if(!full.isEmpty()){ TextView del=new TextView(this); del.setText("🗑"); del.setTextSize(22); del.setPadding(12,0,0,0); row.addView(del); del.setOnClickListener(v->removeTask(full)); cb.setOnCheckedChangeListener((b,c)->toggleTask(full,c)); }
        tasksContainer.addView(row);
    }
    void toggleTask(String full,boolean checked){ String[] lines=prefs.getString("tasks","").split("\\n"); StringBuilder s=new StringBuilder(); for(String l:lines){ if(l.equals(full)){ String[] p=l.split("\\|",-1); l=p[0]+"|"+p[1]+"|"+(checked?"1":"0"); } if(s.length()>0)s.append('\n'); s.append(l);} prefs.edit().putString("tasks",s.toString()).apply(); }
    void removeTask(String full){ ArrayList<String> a=new ArrayList<>(Arrays.asList(prefs.getString("tasks","").split("\\n"))); a.remove(full); prefs.edit().putString("tasks",String.join("\n",a)).apply(); loadTasks(); }
    void startPomodoro(){ if(running)return; running=true; handler.post(timerRunnable); statusText.setText("Pomodoro شغال — ركّز في مهمة واحدة فقط 🎯"); }
    void resetPomodoro(){ running=false; handler.removeCallbacks(timerRunnable); remaining=25*60; updateTimer(); statusText.setText("جاهز لجلسة تركيز جديدة."); }
    void updateTimer(){ long m=remaining/60,s=remaining%60; timerText.setText(String.format(Locale.getDefault(),"%02d:%02d",m,s)); }
    void loadLessons(){ lessonsText.setText("السبت  • العربي  • 6:00 مساءً\n"+"الإثنين • الرياضيات • مستر إسماعيل • 5:30 مساءً\n"+"الأربعاء • العربي • 6:00 مساءً\n"+"الخميس • الرياضيات • مستر إسماعيل • 5:30 مساءً"); }
    void pickImage(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,20); }
    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(r==20&&c==RESULT_OK&&d!=null&&d.getData()!=null){ Uri u=d.getData(); scheduleImage.setImageURI(u); prefs.edit().putString("schedule",u.toString()).apply(); try{ getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION); }catch(Exception ignored){} }}
    void loadImage(){ String u=prefs.getString("schedule",""); if(!u.isEmpty()) try{ scheduleImage.setImageURI(Uri.parse(u)); }catch(Exception ignored){} }
    void askNotifications(){ if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9); }
    void notifyUser(String title,String text){ NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE); if(Build.VERSION.SDK_INT>=26){ NotificationChannel ch=new NotificationChannel("planner","Eyad Planner",NotificationManager.IMPORTANCE_HIGH); nm.createNotificationChannel(ch);} Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"planner"):new Notification.Builder(this); b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setAutoCancel(true); nm.notify((int)System.currentTimeMillis(),b.build()); }
}
