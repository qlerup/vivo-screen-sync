package dk.ultrabarx.screensync;

public class WakeTimingTest {
 static void equal(long expected,long actual){if(expected!=actual)throw new AssertionError(expected+" != "+actual);}
 public static void main(String[] args){
  WakeTiming t=new WakeTiming();
  equal(-1,t.target(0));
  t.update(0,true,0,1000);equal(0,t.target(0));
  t.update(1,true,100,1000);equal(0,t.target(1099));equal(1,t.target(1100));
  t.update(1,true,500,1000);equal(600,t.remaining(500));
  t.update(0,true,600,1000);equal(0,t.target(1200));equal(0,t.remaining(600));
  t.update(1,true,700,1000);equal(0,t.target(1699));equal(1,t.target(1700));
  t.update(1,false,800,1000);equal(1,t.target(800));equal(0,t.remaining(800));
  t.update(-1,true,900,1000);equal(-1,t.target(900));
  t.update(1,true,1000,0);equal(1,t.target(1000));
  System.out.println("Wake timing checks passed");
 }
}
