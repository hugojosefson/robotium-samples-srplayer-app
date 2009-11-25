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
import java.util.Date;

public class RightNowChannelInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2009112300001L;
	
	private String programTitle;
	private String programInfo;
	private String programURL;
	private String iSidorTitle;
	private String iSidorInfo;
	private String iSidorUrl;
	private String song;
	private String nextSong;
	private String nextProgramTitle;
	private String nextProgramDescription;
	private String nextProgramURL;
	private Date nextProgramStartTime;
	/**
	 * @return the programTitle
	 */
	public String getProgramTitle() {
		return programTitle;
	}
	/**
	 * @param programTitle the programTitle to set
	 */
	public void setProgramTitle(String programTitle) {
		this.programTitle = programTitle;
	}
	/**
	 * @return the programInfo
	 */
	public String getProgramInfo() {
		return programInfo;
	}
	/**
	 * @param programInfo the programInfo to set
	 */
	public void setProgramInfo(String programInfo) {
		this.programInfo = programInfo;
	}
	/**
	 * @return the programURL
	 */
	public String getProgramURL() {
		return programURL;
	}
	/**
	 * @param programURL the programURL to set
	 */
	public void setProgramURL(String programURL) {
		this.programURL = programURL;
	}
	/**
	 * @return the iSidorTitle
	 */
	public String getiSidorTitle() {
		return iSidorTitle;
	}
	/**
	 * @param iSidorTitle the iSidorTitle to set
	 */
	public void setiSidorTitle(String iSidorTitle) {
		this.iSidorTitle = iSidorTitle;
	}
	/**
	 * @return the iSidorInfo
	 */
	public String getiSidorInfo() {
		return iSidorInfo;
	}
	/**
	 * @param iSidorInfo the iSidorInfo to set
	 */
	public void setiSidorInfo(String iSidorInfo) {
		this.iSidorInfo = iSidorInfo;
	}
	/**
	 * @return the iSidorUrl
	 */
	public String getiSidorUrl() {
		return iSidorUrl;
	}
	/**
	 * @param iSidorUrl the iSidorUrl to set
	 */
	public void setiSidorUrl(String iSidorUrl) {
		this.iSidorUrl = iSidorUrl;
	}
	/**
	 * @return the song
	 */
	public String getSong() {
		return song;
	}
	/**
	 * @param song the song to set
	 */
	public void setSong(String song) {
		this.song = song;
	}
	/**
	 * @return the nextSong
	 */
	public String getNextSong() {
		return nextSong;
	}
	/**
	 * @param nextSong the nextSong to set
	 */
	public void setNextSong(String nextSong) {
		this.nextSong = nextSong;
	}
	/**
	 * @return the nextProgramTitle
	 */
	public String getNextProgramTitle() {
		return nextProgramTitle;
	}
	/**
	 * @param nextProgramTitle the nextProgramTitle to set
	 */
	public void setNextProgramTitle(String nextProgramTitle) {
		this.nextProgramTitle = nextProgramTitle;
	}
	/**
	 * @return the nextProgramDescription
	 */
	public String getNextProgramDescription() {
		return nextProgramDescription;
	}
	/**
	 * @param nextProgramDescription the nextProgramDescription to set
	 */
	public void setNextProgramDescription(String nextProgramDescription) {
		this.nextProgramDescription = nextProgramDescription;
	}
	/**
	 * @return the nextProgramURL
	 */
	public String getNextProgramURL() {
		return nextProgramURL;
	}
	/**
	 * @param nextProgramURL the nextProgramURL to set
	 */
	public void setNextProgramURL(String nextProgramURL) {
		this.nextProgramURL = nextProgramURL;
	}
	/**
	 * @return the nextProgramStartTime
	 */
	public Date getNextProgramStartTime() {
		return nextProgramStartTime;
	}
	/**
	 * @param nextProgramStartTime the nextProgramStartTime to set
	 */
	public void setNextProgramStartTime(Date nextProgramStartTime) {
		this.nextProgramStartTime = nextProgramStartTime;
	}

}
