package id.ars.hey;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServiceLifecycleTest {
  @Test public void checkingAnAlreadyActiveServicePreservesCurrentPoll(){
    ServiceLifecycle l=new ServiceLifecycle();l.resume();long request=l.ticket();l.resume();assertTrue(l.active(request));
  }
  @Test public void stalePollResponseCannotDispatchAfterPauseOrResume(){
    ServiceLifecycle l=new ServiceLifecycle();l.resume();long old=l.ticket();assertTrue(l.active(old));l.pause();assertFalse(l.active(old));l.resume();assertFalse(l.active(old));assertTrue(l.active(l.ticket()));
  }
  @Test public void delayedPauseCannotKillResumedService(){
    ServiceLifecycle l=new ServiceLifecycle();l.resume();long old=l.pause();assertTrue(l.shouldStop(old));l.resume();assertFalse(l.shouldStop(old));
  }
  @Test public void earlierPauseCannotKillALaterPause(){
    ServiceLifecycle l=new ServiceLifecycle();long old=l.pause();l.resume();long latest=l.pause();assertFalse(l.shouldStop(old));assertTrue(l.shouldStop(latest));
  }
  @Test public void destroyedInstanceCannotCompleteOldPause(){
    ServiceLifecycle l=new ServiceLifecycle();long ticket=l.pause();l.destroy();assertFalse(l.shouldStop(ticket));
  }
  @Test public void automaticStartupHonorsPairingAndOwnerPause(){
    RuntimeStartPolicy p=new RuntimeStartPolicy();assertFalse(p.begin(0,false,false,false,false));assertFalse(p.begin(0,false,true,true,false));assertFalse(p.begin(0,false,true,false,true));assertTrue(p.begin(0,false,true,false,false));
  }
  @Test public void automaticRetriesAreSpacedAndBounded(){
    RuntimeStartPolicy p=new RuntimeStartPolicy();assertTrue(p.begin(0,false,true,false,false));assertFalse(p.begin(1000,false,true,false,false));assertTrue(p.begin(10000,false,true,false,false));assertTrue(p.begin(20000,false,true,false,false));assertFalse(p.begin(30000,false,true,false,false));
  }
  @Test public void explicitResumeRecoversAfterRetriesAndIntentionalPause(){
    RuntimeStartPolicy p=new RuntimeStartPolicy();for(int i=0;i<3;i++)assertTrue(p.begin(i*10000,false,true,false,false));assertTrue(p.begin(21000,true,true,true,false));assertFalse(p.begin(21001,false,true,false,false));
  }
}
