package studio.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.AtomicFile;
import java.io.*;
import java.net.URI;
import java.util.*;
import org.json.*;

/** Settings API 1. App subclasses provide read/apply hooks; no install scripts. */
public abstract class StudioSettingsBridge extends BroadcastReceiver {
 protected abstract JSONObject readValues(Context context, JSONObject schema) throws Exception;
 protected abstract void applyValues(Context context, JSONObject values) throws Exception;

 public final void onReceive(Context context, Intent intent) {
  if (!((context.getPackageName()+".STUDIO_SETTINGS").equals(intent.getAction()))) return;
  File directory=new File(context.getFilesDir(),"ultrabar-studio");
  String revision=null;
  try {
   JSONObject request=new JSONObject(read(new FileInputStream(new File(directory,"request.json"))));
   revision=request.getString("revision");
   if(!revision.matches("[a-f0-9]{32}") || !(request.get("api") instanceof Integer) || request.getInt("api")!=1) return;
   JSONObject schema=new JSONObject(read(context.getAssets().open("studio-settings.json")));
   String operation=request.getString("operation");
   if(operation.equals("apply")) {
    if(request.length()!=4) throw new Exception("fields");
    JSONObject values=request.getJSONObject("values");validate(schema,values);applyValues(context,values);
   } else if(!operation.equals("read") || request.length()!=3) throw new Exception("operation");
   JSONObject actual=readValues(context,schema);validate(schema,actual);
   write(directory,new JSONObject().put("api",1).put("revision",revision).put("status","ok").put("values",actual));
  } catch(Exception failure) {
   if(revision!=null && revision.matches("[a-f0-9]{32}")) try {write(directory,new JSONObject().put("api",1).put("revision",revision).put("status","error"));}catch(Exception ignored){}
  }
 }

 static String read(InputStream input) throws Exception {
  try(InputStream source=input;ByteArrayOutputStream output=new ByteArrayOutputStream()) {
   byte[] buffer=new byte[2048];int count;
   while((count=source.read(buffer))!=-1){if(output.size()+count>32768)throw new IOException("size");output.write(buffer,0,count);}
   return output.toString("UTF-8");
  }
 }
 static void write(File directory,JSONObject response) throws Exception {
  if(!directory.isDirectory() && !directory.mkdirs())throw new IOException("directory");
  AtomicFile file=new AtomicFile(new File(directory,"response.json"));FileOutputStream output=null;
  try {output=file.startWrite();output.write(response.toString().getBytes("UTF-8"));file.finishWrite(output);}
  catch(Exception failure){if(output!=null)file.failWrite(output);throw failure;}
 }
 static void validate(JSONObject schema,JSONObject values) throws Exception {
  JSONArray fields=schema.getJSONArray("fields");if(values.length()!=fields.length())throw new Exception("keys");
  for(int index=0;index<fields.length();index++) {
   JSONObject field=fields.getJSONObject(index);String key=field.getString("key"),type=field.getString("type");Object value=values.get(key);boolean valid=false;
   if(type.equals("boolean"))valid=value instanceof Boolean;
   else if(type.equals("integer") || type.equals("number")) {
    if(value instanceof Number) {
     double number=((Number)value).doubleValue(),min=field.getDouble("min"),step=field.getDouble("step"),ticks=(number-min)/step;
     valid=!Double.isNaN(number) && !Double.isInfinite(number) && number>=min && number<=field.getDouble("max") && Math.abs(ticks-Math.rint(ticks))<=1e-7;
     if(type.equals("integer"))valid=valid && !(value instanceof Float) && !(value instanceof Double);
    }
   } else if(type.equals("choice")) {
    JSONArray options=field.getJSONArray("options");for(int j=0;j<options.length();j++)if(value instanceof String && value.equals(options.getJSONObject(j).getString("value")))valid=true;
   } else if(type.equals("string") || type.equals("url")) {
    if(value instanceof String) {
     String text=(String)value;valid=text.codePointCount(0,text.length())<=field.getInt("maxLength");
     for(int j=0;j<text.length();j++)if(text.charAt(j)<32)valid=false;
     if(valid && type.equals("url")){URI uri=new URI(text);valid=("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) && uri.getHost()!=null && uri.getUserInfo()==null;}
    }
   }
   if(!valid)throw new Exception("invalid field");
  }
 }
}
