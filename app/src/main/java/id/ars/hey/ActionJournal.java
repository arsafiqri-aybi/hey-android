package id.ars.hey;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.JSONObject;

/** Append-only encrypted receipts. No arbitrary lifetime action ceiling or replay GC. */
final class ActionJournal extends SQLiteOpenHelper {
  private final SecureStore secure;
  ActionJournal(Context context,SecureStore secure){super(context,"hey-actions.db",null,1);this.secure=secure;setWriteAheadLoggingEnabled(true);}
  @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE receipts (action TEXT PRIMARY KEY, sealed TEXT NOT NULL)");}
  @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("JOURNAL_MIGRATION_REQUIRED");}
  synchronized void migrate()throws Exception{
    String old=secure.get("receipts","");if(old.isEmpty())return;JSONObject receipts=new JSONObject(old);SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
    try{java.util.Iterator<String> keys=receipts.keys();while(keys.hasNext()){String key=keys.next();insert(db,key.replaceFirst("^receipt:",""),receipts.getJSONObject(key));}db.setTransactionSuccessful();}finally{db.endTransaction();}
    secure.remove("receipts");
  }
  private void insert(SQLiteDatabase db,String action,JSONObject receipt)throws Exception{ContentValues value=new ContentValues();value.put("action",action);value.put("sealed",secure.encrypt(receipt.toString()));db.insertWithOnConflict("receipts",null,value,SQLiteDatabase.CONFLICT_IGNORE);}
  synchronized boolean dispatch(String action,JSONObject receipt)throws Exception{
    SQLiteDatabase db=getWritableDatabase();try(Cursor c=db.rawQuery("SELECT sealed FROM receipts WHERE action=?",new String[]{action})){if(c.moveToFirst()){secure.decrypt(c.getString(0));return false;}}
    ContentValues value=new ContentValues();value.put("action",action);value.put("sealed",secure.encrypt(receipt.toString()));db.insertOrThrow("receipts",null,value);return true;
  }
}
