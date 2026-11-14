package dk.ultrabarx.screensync;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()) && c.getSharedPreferences("sync",0).getBoolean("enabled",false))
   c.startForegroundService(new Intent(c,SyncService.class));
 }
}
