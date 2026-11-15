package dk.ultrabarx.screensync;
import android.content.*;
import org.json.*;
import studio.settings.StudioSettingsBridge;

public class StudioSettingsReceiver extends StudioSettingsBridge {
 protected JSONObject readValues(Context context, JSONObject schema) throws Exception {
  SharedPreferences preferences=context.getSharedPreferences("sync",0);
  return new JSONObject().put("enabled",preferences.getBoolean("enabled",false)).put("wakeDelayMs",preferences.getInt("wakeDelayMs",1000));
 }
 protected void applyValues(Context context, JSONObject values) throws Exception {
  if(!context.getSharedPreferences("sync",0).edit().putBoolean("enabled",values.getBoolean("enabled")).putInt("wakeDelayMs",values.getInt("wakeDelayMs")).commit())throw new Exception("Preferences not saved");
  context.startForegroundService(new Intent(context,SyncService.class).setAction(values.getBoolean("enabled")?"enable":"disable"));
 }
}
