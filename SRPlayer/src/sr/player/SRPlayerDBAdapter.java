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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
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
    
    public long createFavorite(int Type, int ID, String Label, String Link, String Desc, String Name, String guid, int FileSize, int BytesDownloaded) {
    	Log.d(TAG,"New row in datase");
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TYPE, Type);
        initialValues.put(KEY_ID, ID);
        initialValues.put(KEY_LABEL, Label);
        initialValues.put(KEY_LINK, Link);
        initialValues.put(KEY_DESC, Desc);
        initialValues.put(KEY_NAME, Name);
        initialValues.put(KEY_GUID, guid);
        initialValues.put(KEY_FILESIZE, FileSize);
        initialValues.put(KEY_BYTESDOWNLOADED, BytesDownloaded);
        
        
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
    
    public int ImportFromXML(String XMLFilename) {
    	int FavId,FavFilesize,FavbytesDownloaded,FavType;
    	String FavLabel,FavLink,FavDesc,FavGuid,FavName,CurrentTag;
    	
    	InputStream XmlStream = null;
    	
    	File root,subdirectory;
        
		root = Environment.getExternalStorageDirectory();					
		subdirectory = new File(root, "SRPlayer");
		if (subdirectory.exists() == false)
		{
			return 0;
		}
		
		String XMLFullFilepath = subdirectory.getAbsolutePath() + "/" + XMLFilename;		
		
		FileInputStream fis = null;
		try {
		
		fis = new FileInputStream(XMLFullFilepath);
				
		XmlPullParserFactory factory;
		
		factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(fis, null);
        int eventType = xpp.getEventType();
        while( eventType != XmlPullParser.END_DOCUMENT) {
       	 if (eventType == XmlPullParser.START_TAG)
       	 {
       		String CurrName = xpp.getName();
       		if ( CurrName.equals("favorite")) 
       		{
       			CurrentTag = "none";
       			FavId = 0;
       			FavFilesize = 0;
       			FavbytesDownloaded = 0;
       			FavType = 0;
       	    	FavLabel = null;
       	    	FavLink = null;
       	    	FavDesc = null;
       	    	FavGuid = null;
       	    	FavName = null;
       	    	
       			//found a favorite. Try to get all the fields
       			while (eventType != XmlPullParser.END_DOCUMENT) {          			
       				if(eventType == XmlPullParser.START_TAG)
       				{				          
       					CurrentTag = xpp.getName();				       									       					
       				}
       				else if(eventType == XmlPullParser.END_TAG) {              				         
       					if (xpp.getName().equals("favorite")) {
       						//Insert the favorite in the DB
       						createFavorite(FavType, 
       								FavId, 
       								FavLabel, 
       								FavLink, 
       								FavDesc, 
       								FavName, 
       								FavGuid,
       								FavFilesize,FavbytesDownloaded
       								);
       	        			break;
       	        		}
       					else CurrentTag = "none";
       				} 
       				else if(eventType == XmlPullParser.TEXT) {              				
       					String CurrentText = xpp.getText();       					
       					
       					if (CurrentTag.equals(KEY_TYPE)) {
       						FavType = Integer.valueOf(CurrentText);
       					}
       					else if (CurrentTag.equals(KEY_ID))
       					{
       						FavId = Integer.valueOf(CurrentText);
       					}
       					else if (CurrentTag.equals(KEY_LABEL))
       					{
       						FavLabel = CurrentText;
       					}
       					else if (CurrentTag.equals(KEY_LINK))
       					{
       						FavLink = CurrentText;
       					}
       					else if (CurrentTag.equals(KEY_DESC))
       					{
       						FavDesc = CurrentText;
       					}
       					else if (CurrentTag.equals(KEY_NAME))
       					{
       						FavName = CurrentText;
       					}
       					else if (CurrentTag.equals(KEY_GUID))
       					{
       						FavGuid = CurrentText;
       					}
       					else if (CurrentTag.equals(KEY_FILESIZE))
       					{
       						FavFilesize = Integer.valueOf(CurrentText);
       					}
       					else if (CurrentTag.equals(KEY_BYTESDOWNLOADED))
       					{
       						FavbytesDownloaded = Integer.valueOf(CurrentText);
       					}
       					
       				}			
       				eventType = xpp.next();         
       			}       			
       		}
       	 }       	 
       	 eventType = xpp.next();		
        }
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		} catch (XmlPullParserException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		
    	
		
    	return 0;
    }
    
    public int ExportToXML(String XMLFilename) {
    	int FavId,FavFilesize,FavbytesDownloaded,FavType;
    	String FavLabel,FavLink,FavDesc,FavGuid,FavName;
		
    	File root,subdirectory;
        
		root = Environment.getExternalStorageDirectory();					
		subdirectory = new File(root, "SRPlayer");
		if (subdirectory.exists() == false)
		{
			Log.d(SRPlayer.TAG, "Directory does not exist. Creating it");
			subdirectory.mkdir();
		}		
		String XMLFullFilepath = subdirectory.getAbsolutePath() + "/" + XMLFilename;
		File XMLExportFile = new File(XMLFullFilepath); //Create the file
		
		FileOutputStream fos = null;
		OutputStreamWriter out = null;
		try {
			fos = new FileOutputStream(XMLFullFilepath);
			out = new OutputStreamWriter(fos, "UTF-8");
			out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		} catch (FileNotFoundException e) { 
			e.printStackTrace();
			return 0;
	    } catch (UnsupportedEncodingException e) {			
			e.printStackTrace();
			return 0;
		} catch (IOException e) {					
			e.printStackTrace();
		}
		
	    Cursor FavCursor = fetchAllFavorites();
	    
	    if (FavCursor.moveToFirst())
	    {
	    	do {
	    		if (FavCursor.isNull(INDEX_ID))
	    			FavId = 0;
	    		else
	    			FavId = FavCursor.getInt(SRPlayerDBAdapter.INDEX_ID);
	    		
	    		if (FavCursor.isNull(INDEX_FILESIZE))
	    			FavFilesize = 0;
	    		else
		    		FavFilesize = FavCursor.getInt(SRPlayerDBAdapter.INDEX_FILESIZE);

	    		
	    		if (FavCursor.isNull(INDEX_TYPE))
	    			FavType = 0;
	    		else
	    			FavType = FavCursor.getInt(SRPlayerDBAdapter.INDEX_TYPE);

	    		
	    		if (FavCursor.isNull(INDEX_LABEL))
	    			FavLabel = "";
	    		else
	    			FavLabel = FavCursor.getString(SRPlayerDBAdapter.INDEX_LABEL);

	    		
	    		if (FavCursor.isNull(INDEX_LINK))
	    			FavLink = "";
	    		else
	    			FavLink = FavCursor.getString(SRPlayerDBAdapter.INDEX_LINK);

	    		
	    		if (FavCursor.isNull(INDEX_GUID))
	    			FavGuid = "";
	    		else
	    			FavGuid = FavCursor.getString(SRPlayerDBAdapter.INDEX_GUID);

	    		
	    		if (FavCursor.isNull(INDEX_NAME))
	    			FavName = "";
	    		else
	    			FavName = FavCursor.getString(SRPlayerDBAdapter.INDEX_NAME);
	    		
	    		if (FavCursor.isNull(INDEX_DESC))
	    			FavDesc = "";
	    		else
	    			FavDesc = FavCursor.getString(SRPlayerDBAdapter.INDEX_DESC);
	    		
	    		if (FavCursor.isNull(INDEX_BYTESDOWNLOADED))
	    			FavbytesDownloaded = 0;
	    		else
	    			FavbytesDownloaded = FavCursor.getInt(SRPlayerDBAdapter.INDEX_BYTESDOWNLOADED);

	    		
	    		
	    		//Write the xml file with the keys as tagnames	    		
	    		try {	    			
		    		out.write("<favorite>\n");
		    		out.write("<"+KEY_ID+">"+FavId+"</"+KEY_ID+">\n");
		    		out.write("<"+KEY_LABEL+">"+FavLabel+"</"+KEY_LABEL+">\n");
		    		out.write("<"+KEY_LINK+">"+FavLink+"</"+KEY_LINK+">\n");
		    		out.write("<"+KEY_DESC+">"+FavDesc+"</"+KEY_DESC+">\n");
		    		out.write("<"+KEY_GUID+">"+FavGuid+"</"+KEY_GUID+">\n");
		    		out.write("<"+KEY_TYPE+">"+FavType+"</"+KEY_TYPE+">\n");
		    		out.write("<"+KEY_NAME+">"+FavName+"</"+KEY_NAME+">\n");
		    		out.write("<"+KEY_FILESIZE+">"+FavFilesize+"</"+KEY_FILESIZE+">\n");
		    		out.write("<"+KEY_BYTESDOWNLOADED+">"+FavbytesDownloaded+"</"+KEY_BYTESDOWNLOADED+">\n");
					out.write("</favorite>\n");
				} catch (IOException e) {					
					XMLExportFile.delete();
					e.printStackTrace();
				}
	    			    		
	    	} while (FavCursor.moveToNext());
	    }
	    FavCursor.close();
	    try {
			out.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
			   
        return FavCursor.getCount();
    }
}
