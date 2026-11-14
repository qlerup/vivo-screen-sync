package dk.ultrabarx.screensync;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
 final int BG=Color.rgb(12,18,22), CARD=Color.rgb(24,34,40), MUTED=Color.rgb(153,172,181), INK=Color.rgb(240,247,249), ACCENT=Color.rgb(79,224,189);
 TextView status; LinearLayout enableCard, disableCard;
 final Handler handler=new Handler(Looper.getMainLooper());
 final Runnable refreshTask=new Runnable(){public void run(){refresh();handler.postDelayed(this,700);}};
 int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
 GradientDrawable box(int fill,int border){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(15));d.setStroke(dp(1),border);return d;}
 TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.HORIZONTAL);root.setGravity(Gravity.CENTER_VERTICAL);root.setPadding(dp(24),dp(18),dp(24),dp(18));root.setBackgroundColor(BG);
  LinearLayout info=new LinearLayout(this);info.setOrientation(1);info.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
  ImageView icon=new ImageView(this);icon.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));heading.addView(icon,new LinearLayout.LayoutParams(dp(44),dp(44)));
  TextView brand=text("  ULTRABAR X  /  VIVO",11,MUTED,true);brand.setLetterSpacing(0.12f);heading.addView(brand);info.addView(heading);
  TextView title=text("Screen sync",24,INK,true);title.setPadding(0,dp(10),0,dp(5));info.addView(title);
  status=text("",13,ACCENT,true);info.addView(status);
  TextView detail=text("Keep your Vivo screens in sync with Core.",12,MUTED,false);detail.setPadding(0,dp(7),0,0);info.addView(detail);
  TextView delayLabel=text("",12,MUTED,false);info.addView(delayLabel);
  SeekBar delay=new SeekBar(this);delay.setMax(12);
  delay.setProgress(getSharedPreferences("sync",0).getInt("wakeDelayMs",1000)/250);
  delayLabel.setText("Vivo wake delay: "+delay.getProgress()*250+" ms");
  delay.setContentDescription("Vivo wake delay, zero to three seconds");
  delay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar bar,int value,boolean fromUser){delayLabel.setText("Vivo wake delay: "+value*250+" ms");}
   public void onStartTrackingTouch(SeekBar bar){}
   public void onStopTrackingTouch(SeekBar bar){getSharedPreferences("sync",0).edit().putInt("wakeDelayMs",bar.getProgress()*250).apply();}
  });
  info.addView(delay,new LinearLayout.LayoutParams(-1,dp(40)));
  root.addView(info,new LinearLayout.LayoutParams(0,-1,1.15f));
  enableCard=card("01", "Follow Core", "Turn off and on with Core",true);
  disableCard=card("02", "Always on", "Turn off screen sync",false);
  LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,-1,1);a.setMargins(dp(20),0,0,0);root.addView(enableCard,a);
  LinearLayout.LayoutParams d=new LinearLayout.LayoutParams(0,-1,1);d.setMargins(dp(14),0,0,0);root.addView(disableCard,d);
  TextView close=text("Close",13,MUTED,true);close.setGravity(Gravity.CENTER);close.setBackground(box(CARD,CARD));close.setOnClickListener(v->finish());close.setContentDescription("Close settings");
  LinearLayout.LayoutParams c=new LinearLayout.LayoutParams(dp(62),dp(48));c.setMargins(dp(14),0,0,0);root.addView(close,c);
  setContentView(root);
  getFilesDir();
  if(getSharedPreferences("sync",0).getBoolean("enabled",false))startForegroundService(new Intent(this,SyncService.class).setAction("enable"));
 }
 LinearLayout card(String number,String title,String detail,boolean on){
  LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setGravity(Gravity.CENTER_VERTICAL);l.setPadding(dp(23),dp(16),dp(16),dp(16));
  TextView tag=text(number,11,ACCENT,true);tag.setLetterSpacing(0.15f);l.addView(tag);
  TextView name=text(title,23,INK,true);name.setPadding(0,dp(11),0,dp(7));l.addView(name);
  l.addView(text(detail,12,MUTED,false));l.setClickable(true);l.setFocusable(true);l.setContentDescription(title+". "+detail);
  l.setOnClickListener(v->{if(!new java.io.File(getFilesDir(),"config.json").isFile()){status.setText("Install or configure through Studio first");return;}startForegroundService(new Intent(this,SyncService.class).setAction(on?"enable":"disable"));status.setText(on?"Enabling screen sync\u2026":"Disabling sync and waking Vivo\u2026");handler.postDelayed(()->refresh(),300);});return l;
 }
 void refresh(){boolean on=getSharedPreferences("sync",0).getBoolean("enabled",false);status.setText(on?"\u25cf  Follow Core selected":"\u25cf  Always on selected");enableCard.setBackground(box(CARD,on?ACCENT:CARD));disableCard.setBackground(box(CARD,on?CARD:ACCENT));}
 public void onResume(){super.onResume();handler.post(refreshTask);}
 public void onPause(){handler.removeCallbacks(refreshTask);super.onPause();}
}
