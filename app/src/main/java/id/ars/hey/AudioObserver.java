package id.ars.hey;
import android.content.Context;
import android.media.*;
import android.media.projection.MediaProjection;
import android.os.Process;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class AudioObserver implements AutoCloseable {
  private final MediaProjection projection;private final StateStore state;private AudioRecord recorder;private Thread thread;private volatile boolean running,closed,intentionalClose;private final ByteArrayOutputStream pending=new ByteArrayOutputStream();private long startedAt;private boolean overflow;private long sampleCursor;
  AudioObserver(Context context,MediaProjection projection,StateStore state)throws Exception {
    this.projection=projection;this.state=state;
    if(context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("AUDIO_PERMISSION_REQUIRED");
    AudioPlaybackCaptureConfiguration capture=new AudioPlaybackCaptureConfiguration.Builder(projection).addMatchingUid(Process.myUid()).addMatchingUsage(AudioAttributes.USAGE_MEDIA).addMatchingUsage(AudioAttributes.USAGE_GAME).addMatchingUsage(AudioAttributes.USAGE_UNKNOWN).build();
    AudioFormat format=new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(16000).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
    int buffer=Math.max(8192,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT));
    recorder=new AudioRecord.Builder().setAudioFormat(format).setBufferSizeInBytes(buffer*2).setAudioPlaybackCaptureConfig(capture).build();
    if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("AUDIO_INITIALIZATION_FAILED");
    projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){if(!intentionalClose){closeInternal();state.audio("CONSENT_ENDED");}}},new android.os.Handler(android.os.Looper.getMainLooper()));
    recorder.startRecording();running=true;startedAt=System.currentTimeMillis();state.audio("CAPTURING");
    thread=new Thread(()->{try{byte[] bytes=new byte[4096];while(running){int n=recorder.read(bytes,0,bytes.length,AudioRecord.READ_BLOCKING);if(n<=0){running=false;state.audio("CAPTURE_ERROR");break;}synchronized(pending){if(pending.size()+n>192000){overflow=true;pending.reset();}pending.write(bytes,0,n);}}}catch(Exception e){running=false;state.audio("CAPTURE_ERROR");}},"Hey audio");thread.start();
  }
  boolean active(){return running;}
  void reset(){synchronized(pending){pending.reset();overflow=false;startedAt=System.currentTimeMillis();sampleCursor=0;}}
  JSONObject drain()throws Exception {
    if(!active())return null;byte[] data;boolean gap;long start,end; synchronized(pending){if(!active())return null;data=pending.toByteArray();pending.reset();gap=overflow;overflow=false;start=startedAt+sampleCursor*1000/16000;sampleCursor+=data.length/2;end=startedAt+sampleCursor*1000/16000;}
    if(data.length==0)return null;long sum=0;for(int i=0;i+1<data.length;i+=2){short s=(short)((data[i]&255)|data[i+1]<<8);sum+=Math.abs((int)s);}
    ByteBuffer header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);header.put("RIFF".getBytes()).putInt(data.length+36).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(data.length);
    ByteArrayOutputStream wav=new ByteArrayOutputStream();wav.write(header.array());wav.write(data);
    return new JSONObject().put("mimeType","audio/wav").put("data",Base64.encodeToString(wav.toByteArray(),Base64.NO_WRAP)).put("startedAt",start).put("endedAt",end).put("samples",data.length/2).put("signal",sum>0?"PRESENT":"SILENT").put("gap",gap);
  }
  @Override public synchronized void close(){intentionalClose=true;closeInternal();}
  private synchronized void closeInternal(){if(closed)return;closed=true;running=false;synchronized(pending){pending.reset();}try{recorder.stop();}catch(Exception ignored){}try{if(thread!=null&&thread!=Thread.currentThread())thread.join(500);}catch(InterruptedException ignored){}recorder.release();state.audio("OFF");try{projection.stop();}catch(Exception ignored){}}
}

