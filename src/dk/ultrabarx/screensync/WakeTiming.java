package dk.ultrabarx.screensync;

// Monotonic time supplied by the caller; repeated display events must not restart the delay.
final class WakeTiming {
 private int requested=-1;
 private long wakeAt;
 synchronized void update(int next, boolean follow, long now, long delay){
  if(next==1 && requested!=1)wakeAt=now+Math.max(0,delay);
  if(!follow)wakeAt=now;
  requested=next;
 }
 synchronized int target(long now){return requested==1 && now<wakeAt?0:requested;}
 synchronized long remaining(long now){return requested==1?Math.max(0,wakeAt-now):0;}
}
