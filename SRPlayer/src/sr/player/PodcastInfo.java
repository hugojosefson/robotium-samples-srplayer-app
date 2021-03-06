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

import java.io.Serializable;

public class PodcastInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2009121400001L;
	
	private String Title;
	private String ID;
	private String PoddID;
	private String Link;
	private String LowLink; //Url for low quality
	private String HighLink; //Url for high quality
	private String Description;
	private int DBIndex;
	private String guid;
	private int type;
	private int filesize;
	private int bytesdownloaded;
	private String Tagline;
	
	
	/**
	 * @return the Title
	 */
	public String getTitle() {
		return Title;
	}
	/**
	 * @param Title the Title to set
	 */
	public void setTitle(String Title) {
		this.Title = Title;
	}
	
	/**
	 * @return the ID
	 */
	public String getID() {
		return ID;
	}
	/**
	 * @param ID the ID to set
	 */
	public void setID(String ID) {
		this.ID = ID;
	}
	
	/**
	 * @return the ID
	 */
	public String getPoddID() {
		return PoddID;
	}
	/**
	 * @param ID the ID to set
	 */
	public void setPoddID(String ID) {
		this.PoddID = ID;
	}

	/**
	 * @return the ID
	 */
	public String getLink() {
		return Link;
	}
	
	/**
	 * @return the ID
	 */
	public String getLowLink() {
		return LowLink;
	}
	
	/**
	 * @return the ID
	 */
	public String getHighLink() {
		return HighLink;
	}
	
	/**
	 * @param Link is the link to be set
	 */
	public void setLink(String Link) {
		this.Link = Link;
	}
	
	/**
	 * @param Link is the link to be set
	 */
	public void setLowLink(String Link) {
		this.LowLink = Link;
	}
	
	/**
	 * @param Link is the link to be set
	 */
	public void setHighLink(String Link) {
		this.HighLink = Link;
	}
	
	/**
	 * @return the ID
	 */
	public String getDescription() {
		return Description;
	}
	/**
	 * @param ID the ID to set
	 */
	public void setDescription(String Description) {
		this.Description = Description;
	}
	
	/**
	 * @return the ID
	 */
	public int getDBIndex() {
		return DBIndex;
	}
	/**
	 * @param ID the ID to set
	 */
	public void setDBIndex(int DBIndex) {
		this.DBIndex = DBIndex;
	}

	/**
	 * @return the guid
	 */
	public String getGuid() {
		return guid;
	}
	/**
	 * @param set the guid
	 */
	public void setGuid(String guid) {
		this.guid = guid;
	}
	
	/**
	 * @return the guid
	 */
	public int getType() {
		return type;
	}
	/**
	 * @param set the guid
	 */
	public void setType(int type) {
		this.type = type;
	}
	
	/**
	 * @return the filesize
	 */
	public int getFilesize() {
		return filesize;
	}
	/**
	 * @param set the filesize
	 */
	public void setFilesize(int filesize) {
		this.filesize = filesize;
	}
	
	/**
	 * @return the bytesdownloaded
	 */
	public int getBytesdownloaded() {
		return bytesdownloaded;
	}
	/**
	 * @param set the bytesdownloaded
	 */
	public void setBytesdownloaded(int bytesdownloaded) {
		this.bytesdownloaded = bytesdownloaded;
	}
	
	/**
	 * @return the Tagline
	 */
	public String getTagline() {
		return Tagline;
	}
	
	/**
	 * @param Tagline
	 */
	public void setTagline(String Tagline) {
		this.Tagline = Tagline;
	}
}
