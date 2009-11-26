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

/** <code>Station</code> is a value object used to pass station infromation between different
 * parts of the application.
 *
 */
public class Station implements Cloneable {
	private String streamUrl;
	private String stationName;
	private String rightNowUrl;
	private int channelId;
	
	public Station(String stationName, String streamUrl,  String rightNowUrl,
			int channelId) {
		super();
		this.streamUrl = streamUrl;
		this.stationName = stationName;
		this.rightNowUrl = rightNowUrl;
		this.channelId = channelId;
	}

	/**
	 * @return the streamUrl
	 */
	public String getStreamUrl() {
		return streamUrl;
	}
	/**
	 * @param streamUrl the streamUrl to set
	 */
	public void setStreamUrl(String streamUrl) {
		this.streamUrl = streamUrl;
	}
	/**
	 * @return the stationName
	 */
	public String getStationName() {
		return stationName;
	}
	/**
	 * @param stationName the stationName to set
	 */
	public void setStationName(String stationName) {
		this.stationName = stationName;
	}
	/**
	 * @return the rightNowUrl
	 */
	public String getRightNowUrl() {
		return rightNowUrl;
	}
	/**
	 * @param rightNowUrl the rightNowUrl to set
	 */
	public void setRightNowUrl(String rightNowUrl) {
		this.rightNowUrl = rightNowUrl;
	}
	/**
	 * @return the channelId
	 */
	public int getChannelId() {
		return channelId;
	}
	/**
	 * @param channelId the channelId to set
	 */
	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + channelId;
		result = prime * result
				+ ((rightNowUrl == null) ? 0 : rightNowUrl.hashCode());
		result = prime * result
				+ ((stationName == null) ? 0 : stationName.hashCode());
		result = prime * result
				+ ((streamUrl == null) ? 0 : streamUrl.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Station other = (Station) obj;
		if (channelId != other.channelId)
			return false;
		if (rightNowUrl == null) {
			if (other.rightNowUrl != null)
				return false;
		} else if (!rightNowUrl.equals(other.rightNowUrl))
			return false;
		if (stationName == null) {
			if (other.stationName != null)
				return false;
		} else if (!stationName.equals(other.stationName))
			return false;
		if (streamUrl == null) {
			if (other.streamUrl != null)
				return false;
		} else if (!streamUrl.equals(other.streamUrl))
			return false;
		return true;
	}
	
	@Override
	public Station clone()  {
		Station newStation = new Station(this.stationName, this.streamUrl, 
				this.rightNowUrl, this.channelId);
		return newStation;
	}
}
