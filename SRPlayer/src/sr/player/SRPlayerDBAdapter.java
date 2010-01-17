package sr.player;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class SRPlayerDBAdapter {

    public static final String KEY_TYPE = "type";
    public static final String KEY_ID = "id";
    public static final String KEY_LABEL = "label";    
    public static final String KEY_ROWID = "_id";
    public static final String KEY_LINK = "link";
    public static final String KEY_DESC = "desc";
    public static final String KEY_NAME = "name";
    
    
    public static final int INDEX_ROWID = 0;
    public static final int INDEX_TYPE = 1;    
    public static final int INDEX_ID = 2;
    public static final int INDEX_LABEL = 3;
    public static final int INDEX_LINK = 4;
    public static final int INDEX_DESC = 5;
    public static final int INDEX_NAME = 6;
        
    public static final int KANAL = 0;
    public static final int PROGRAM = 1;
    public static final int AVSNITT = 2;
    public static final int KATEGORI = 3;
    public static final int AVSNITT_OFFLINE = 4;
    
    
    private static final String TAG = "SRPlayerDBAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "create table favorites (_id integer primary key autoincrement, "
                    + "type integer not null, id integer, label string not null, link string, desc string, name string);";

    private static final String DATABASE_NAME = "user_data";
    private static final String DATABASE_TABLE = "favorites";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
            Log.d(TAG,"Database created"); 
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS favorites");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public SRPlayerDBAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the SRPlayer database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public SRPlayerDBAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new favorite
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createFavorite(int Type, int ID, String Label, String Link, String Desc, String Name) {
    	Log.d(TAG,"New row in datase");
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TYPE, Type);
        initialValues.put(KEY_ID, ID);
        initialValues.put(KEY_LABEL, Label);
        initialValues.put(KEY_LINK, Link);
        initialValues.put(KEY_DESC, Desc);
        initialValues.put(KEY_NAME, Name);
        
        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete the favorite with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteFavorite(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllFavorites() {

    	return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TYPE,
                KEY_ID, KEY_LABEL, KEY_LINK, KEY_DESC, KEY_NAME}, null, null, null, null, KEY_LABEL);
    }

    
    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor
     */
    public Cursor fetchFavoritesByType(String Type) {

    	return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TYPE,
                KEY_ID, KEY_LABEL, KEY_LINK, KEY_DESC, KEY_NAME}, "type =" + Type, null, null, null, KEY_LABEL);
    }

    /**
     * Return the number of rows of a specific type 
     * 
     * @return Cursor
     */
    public int fetchCountByType(String Type) {    
    	Cursor count_res = fetchFavoritesByType(Type);
    	if (count_res != null)
    	{
    		return count_res.getCount();
    	}
    	else
    		return 0;
    }

    
    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of favorite to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchFavorite(long rowId) throws SQLException {

        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                        KEY_TYPE, KEY_ID, KEY_LABEL, KEY_LINK, KEY_DESC, KEY_NAME}, KEY_ROWID + "=" + rowId, null,
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @param title value to set note title to
     * @param body value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateFavorite(long rowId, int Type, int ID, String Label, String Link, String Desc, String Name) {
        ContentValues args = new ContentValues();
        args.put(KEY_TYPE, Type);
        args.put(KEY_ID, ID);        
        args.put(KEY_LABEL, Label);
        args.put(KEY_LINK, Link);
        args.put(KEY_DESC, Desc);
        args.put(KEY_NAME, Name);
        
        
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
