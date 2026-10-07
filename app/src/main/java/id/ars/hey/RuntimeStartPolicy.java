package id.ars.hey;

/** Bounded visible-owner retries; explicit resume overrides intentional pause. */
final class RuntimeStartPolicy {
  private int attempts;
  private long lastAttempt=Long.MIN_VALUE;
  synchronized boolean begin(long now,boolean explicit,boolean paired,boolean paused,boolean running){
    if(!paired||!explicit&&(paused||running))return false;
    if(explicit){attempts=0;lastAttempt=Long.MIN_VALUE;}
    if(attempts>=3||lastAttempt!=Long.MIN_VALUE&&now-lastAttempt<10000)return false;
    attempts++;lastAttempt=now;return true;
  }
}
