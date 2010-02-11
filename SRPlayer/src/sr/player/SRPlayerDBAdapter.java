/**
  * This file is part of SR Player for Android
  *
  * SR Player for Android is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License version 2 as published by
  * the Free Software Foundation.
  *
  * SR Player for Android is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with SR Player for Android.  If not, see <http://www.gnu.org/licenses/>.
  */

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
    public static final String KEY_GUID = "guid";
    public static final String KEY_FILESIZE = "filesize";
    public static final String KEY_BYTESDOWNLOADED = "bytesdownloaded";
    
    public static final int INDEX_ROWID = 0;
    public static final int INDEX_TYPE = 1;    
    public static final int INDEX_ID = 2;
    public static final int INDEX_LABEL = 3;
    public static final int INDEX_LINK = 4;
    public static final int INDEX_DESC = 5;
    public static final int INDEX_NAME = 6;
    public static final int INDEX_GUID = 7;
    public static final int INDEX_FILESIZE = 8;
    public static final int INDEX_BYTESDOWNLOADED = 9;
        
    public static final int CHANNEL = 0;
    public static final int PROGRAM = 1;
    public static final int EPISODE = 2;
    public static final int CATEGORY = 3;
    public static final int EPISODE_TO_DOWNLOAD = 10;
    public static final int EPISODE_OFFLINE = 4;
    public static final int DOWNLOAD_QUEUE = 5;
    
    public static final int QUEUE_FOR_DOWNLOAD = 0;
    public static final int ACTIVE_DOWNLOAD = 1;
    public static final int ACTIVE_DOWNLOAD_PAUSED = 2;
    
    
    private static final String TAG = "SRPlayerDBAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "create table favorites (_id integer primary key autoincrement, "
                    + "type integer not null, id integer, label string not null, " 
                    + "link string, desc string, name string, guid string, "
                    + "filesize int, bytesdownloaded int);";

    private static final String DATABASE_NAME = "user_data.db";
    private static final String DATABASE_TABLE = "favorites";
    private static final int DATABASE_VERSION = 4;

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
                    + newVersion);
            if (oldVersion == 3)
            {
            	//Add three columns
            	db.execSQL("ALTER TABLE favorites ADD COLUMN filesize INT");
            	db.execSQL("ALTER TABLE favorites ADD COLUMN bytesdownloaded INT");
            }
            else
            {
            	db.execSQL("DROP TABLE IF EXISTS favorites");
            	onCreate(db);
            }
            
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
    public long createFavorite(int Type, int ID, String Label, String Link, String Desc, String Name, String guid) {
    	Log.d(TAG,"New row in datase");
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TYPE, Type);
        initialValues.put(KEY_ID, ID);
        initialValues.put(KEY_LABEL, Label);
        initialValues.put(KEY_LINK, Link);
        initialValues.put(KEY_DESC, Desc);
        initialValues.put(KEY_NAME, Name);
        initialValues.put(KEY_GUID, guid);
        initialValues.put(KEY_FILESIZE, -1);
        initialValues.put(KEY_BYTESDOWNLOADED, 0);
        
        
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
                KEY_ID, KEY_LABEL, KEY_LINK, KEY_DESC, KEY_NAME, KEY_GUID, KEY_FILESIZE, KEY_BYTESDOWNLOADED}, null, null, null, null, KEY_LABEL);
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor
     */
    public Cursor fetchPodcastsToDownload() {

    	return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TYPE,
                KEY_ID, KEY_LABEL, KEY_LINK, KEY_DESC, KEY_NAME, KEY_GUID, KEY_FILESIZE, KEY_BYTESDOWNLOADED}, "type =" + String.valueOf(EPISODE_TO_DOWNLOAD), null, null, null, KEY_ROWID);
    }
    
    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor
     */
    public Cursor fetchFavoritesByType(String Type) {

    	return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TYPE,
                KEY_ID, KEY_LABEL, KEY_LINK, KEY_DESC, KEY_NAME, KEY_GUID, KEY_FILESIZE, KEY_BYTESDOWNLOADED}, "type =" + Type, null, null, null, KEY_LABEL);
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
    		int Count = count_res.getCount();
    		count_res.close();
    		return Count;
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
                        KEY_TYPE, KEY_ID, KEY_LABEL, KEY_LINK, KEY_DESC, KEY_NAME, KEY_GUID, KEY_FILESIZE, KEY_BYTESDOWNLOADED}, KEY_ROWID + "=" + rowId, null,
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the favorite
     */
    public boolean updateFavorite(long rowId, int Type, int ID, String Label, String Link, String Desc, String Name, String guid) {
        ContentValues args = new ContentValues();
        args.put(KEY_TYPE, Type);
        args.put(KEY_ID, ID);        
        args.put(KEY_LABEL, Label);
        args.put(KEY_LINK, Link);
        args.put(KEY_DESC, Desc);
        args.put(KEY_NAME, Name);
        args.put(KEY_GUID, guid);        
        
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     *  Move favorite to OFFLINE_AVSNITT
     *  When a podcast has been downloaded the database is so that the podcast can be found in the gui
     *  This is done by chaning the type to AVSNITT_OFFLINE and changing the link to the filepath
     */
    public boolean podcastDownloadCompleteUpdate(long rowId, String FilePath) {
        ContentValues args = new ContentValues();
        args.put(KEY_TYPE, EPISODE_OFFLINE);
        args.put(KEY_LINK, FilePath);
        
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     *  Move favorite to OFFLINE_AVSNITT
     *  When a podcast has been downloaded the database is so that the podcast can be found in the gui
     *  This is done by chaning the type to AVSNITT_OFFLINE and changing the link to the filepath
     */
    public boolean SetFileSize(long rowId, int Size) {
        ContentValues args = new ContentValues();
        args.put(KEY_FILESIZE, Size);        
        
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     *  Move favorite to OFFLINE_AVSNITT
     *  When a podcast has been downloaded the database is so that the podcast can be found in the gui
     *  This is done by chaning the type to AVSNITT_OFFLINE and changing the link to the filepath
     */
    public boolean SetBytesDownloaded(long rowId, int BytesDownloaded) {
        ContentValues args = new ContentValues();
        args.put(KEY_BYTESDOWNLOADED, BytesDownloaded);        
        
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     *  Change a favorite as the active downloading podcast
     */
    public boolean podcastSetAsCurrentDownloading(long rowId) {
        ContentValues args = new ContentValues();
        args.put(KEY_ID, ACTIVE_DOWNLOAD);        
        
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     *  Change a favorite as the active downloading podcast
     */
    public boolean podcastSetAsPaused(long rowId) {
        ContentValues args = new ContentValues();
        args.put(KEY_ID, ACTIVE_DOWNLOAD_PAUSED);        
        
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
