package id.ars.hey;

/** Delayed pause completion may stop only the pause that scheduled it. */
final class ServiceLifecycle {
  private long generation;
  private boolean paused, destroyed;
  synchronized void resume(){generation++;paused=false;destroyed=false;}
  synchronized long pause(){paused=true;return ++generation;}
  synchronized boolean shouldStop(long ticket){return !destroyed&&paused&&generation==ticket;}
  synchronized void destroy(){destroyed=true;generation++;}
}
