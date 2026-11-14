package dk.ultrabarx.screensync;
import android.app.*;
import android.content.*;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.view.Display;
import android.util.Log;
import org.json.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.*;

public class SyncService extends Service implements DisplayManager.DisplayListener {
 final Handler main=new Handler(Looper.getMainLooper());
 final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
 final WakeTiming timing=new WakeTiming();
 ScheduledFuture<?> wakeTask;
 volatile boolean enabled, destroyed;
 DisplayManager displays; NotificationManager notices;
 public IBinder onBind(Intent i){return null;}
 public void onCreate(){
  super.onCreate();
  notices=getSystemService(NotificationManager.class);
  notices.createNotificationChannel(new NotificationChannel("sync","Vivo screen sync",NotificationManager.IMPORTANCE_LOW));
  startForeground(1,notification("Starting"));
  displays=getSystemService(DisplayManager.class);displays.registerDisplayListener(this,main);
  enabled=getSharedPreferences("sync",0).getBoolean("enabled",false);
  worker.scheduleWithFixedDelay(()->sync(),2,15,TimeUnit.SECONDS);
  update();
 }
 Notification notification(String text){
  return new Notification.Builder(this,"sync").setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Vivo Screen Sync").setContentText(text).setOngoing(true)
   .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build();
 }
 public int onStartCommand(Intent i,int flags,int id){
  if(i!=null && "enable".equals(i.getAction()))enabled=true;
  if(i!=null && "disable".equals(i.getAction()))enabled=false;
  getSharedPreferences("sync",0).edit().putBoolean("enabled",enabled).apply();
  update();return START_STICKY;
 }
 void update(){
  Display d=displays.getDisplay(Display.DEFAULT_DISPLAY);
  int state=d==null?Display.STATE_UNKNOWN:d.getState();
  int next=!enabled?1:state==Display.STATE_OFF?0:state==Display.STATE_ON?1:-1;
  timing.update(next,enabled,SystemClock.elapsedRealtime(),getSharedPreferences("sync",0).getInt("wakeDelayMs",1000));
  if(wakeTask!=null)wakeTask.cancel(false);
  long wait=timing.remaining(SystemClock.elapsedRealtime());
  if(wait>0)wakeTask=worker.schedule(()->sync(),wait,TimeUnit.MILLISECONDS);
  Log.i("VivoSync","display="+state+" requested="+next+" wakeDelay="+wait+" enabled="+enabled);
  worker.execute(()->sync());
 }
 public void onDisplayChanged(int id){if(id==Display.DEFAULT_DISPLAY)update();}
 public void onDisplayAdded(int id){onDisplayChanged(id);}
 public void onDisplayRemoved(int id){}
 JSONObject config() throws Exception {
  try(InputStream in=openFileInput("config.json")){
   ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[2048];int n;
   while((n=in.read(buf))!=-1)b.write(buf,0,n);return new JSONObject(b.toString("UTF-8"));
  }
 }
 void sync(){
  int target=timing.target(SystemClock.elapsedRealtime());
  if(destroyed||target<0)return;
  PowerManager.WakeLock lock=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"VivoSync:command");
  lock.acquire(25000);
  try{
   JSONObject c=config();JSONArray serials=c.getJSONArray("serials");
   try(Link link=new Link(c.getString("token"))){
    List<JSONObject> changed=new ArrayList<>();
    for(int x=0;x<serials.length();x++){
     String serial=serials.getString(x);
     JSONObject address=new JSONObject().put("serialNo",serial).put("deviceId",256).put("propertyId",4);
     int before=link.ask(30200,address).getJSONObject("data").getInt("result");
     if(destroyed||target!=timing.target(SystemClock.elapsedRealtime()))return;
     if(before!=target)changed.add(address);
    }
    // Send all changes before waiting for acknowledgements/readback from either cube.
    List<String> replies=new ArrayList<>();
    for(JSONObject address:changed){
     if(destroyed||target!=timing.target(SystemClock.elapsedRealtime()))return;
     replies.add(link.send(30201,new JSONObject(address.toString()).put("value",target)));
    }
    for(String id:replies)link.receive(id);
    for(JSONObject address:changed){
      int actual=link.ask(30200,address).getJSONObject("data").getInt("result");
      if(actual!=target)throw new IOException("Readback mismatch");
      Log.i("VivoSync","Screen state verified: "+actual);
    }
   }
   if(destroyed||target!=timing.target(SystemClock.elapsedRealtime()))return;
   notices.notify(1,notification(enabled?"Active - Vivo follows Core":"Disabled - Vivo screens on"));
   if(!enabled && target==1)main.post(()->{if(!enabled){stopForeground(true);stopSelf();}});
  }catch(Exception e){Log.w("VivoSync","Sync failed: "+e.getClass().getSimpleName());notices.notify(1,notification("Retrying connection"));}
  finally{if(lock.isHeld())lock.release();}
 }
 class Link implements AutoCloseable {
  final Map<String,JSONObject> pending=new HashMap<>();
  final Set<String> outstanding=new HashSet<>();
  SSLSocket socket;BufferedReader reader;OutputStream out;
  Link(String token)throws Exception{
   byte[] pin;
   try(InputStream in=openFileInput("server.crt")){pin=CertificateFactory.getInstance("X.509").generateCertificate(in).getEncoded();}
   final byte[] expected=pin;
   TrustManager[] trust={new X509TrustManager(){
    public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
    public void checkClientTrusted(X509Certificate[] c,String a)throws CertificateException{throw new CertificateException();}
    public void checkServerTrusted(X509Certificate[] c,String a)throws CertificateException{if(c.length==0||!Arrays.equals(expected,c[0].getEncoded()))throw new CertificateException("Pin mismatch");}
   }};
   SSLContext tls=SSLContext.getInstance("TLS");tls.init(null,trust,null);
   socket=(SSLSocket)tls.getSocketFactory().createSocket();
   try{socket.connect(new InetSocketAddress("127.0.0.1",7431),3000);socket.setSoTimeout(4000);socket.startHandshake();
    reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));out=socket.getOutputStream();ask(100,token);
   }catch(Exception e){socket.close();throw e;}
  }
  JSONObject ask(int act,Object data)throws Exception{
   return receive(send(act,data));
  }
  String send(int act,Object data)throws Exception{
   String id=UUID.randomUUID().toString();outstanding.add(id);
   out.write((new JSONObject().put("id",id).put("act",act).put("data",data).toString()+"\n").getBytes("UTF-8"));out.flush();return id;
  }
  JSONObject checked(String id,JSONObject reply)throws Exception{
   outstanding.remove(id);
   if(reply.optInt("code")!=200)throw new IOException("Command rejected");return reply;
  }
  JSONObject receive(String id)throws Exception{
   JSONObject cached=pending.remove(id);if(cached!=null)return checked(id,cached);
   long deadline=SystemClock.elapsedRealtime()+5000;
   while(SystemClock.elapsedRealtime()<deadline){
    StringBuilder line=new StringBuilder();int ch;
    while((ch=reader.read())!=-1&&ch!='\n'){line.append((char)ch);if(line.length()>2097152||SystemClock.elapsedRealtime()>deadline)throw new IOException("Reply limit");}
    if(ch==-1)throw new EOFException();if(line.length()==0)continue;
    JSONObject reply=new JSONObject(line.toString());
    String replyId=reply.optString("id");
    if(id.equals(replyId))return checked(id,reply);
    if(outstanding.contains(replyId))pending.put(replyId,reply);
   }throw new IOException("Reply timeout");
  }
  public void close()throws IOException{socket.close();}
 }
 public void onDestroy(){destroyed=true;displays.unregisterDisplayListener(this);worker.shutdownNow();super.onDestroy();}
}
