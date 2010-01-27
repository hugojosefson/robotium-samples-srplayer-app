 /* This file is part of SR Player for Android
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

import android.util.Log;

public class History implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2010012700001L;
	private int Action;
	private String ID;
	private String Label;
	private Object StreamData;
	
	public History(int Action, String ID, String Label, Object StreamData) {
		super();
		this.Action = Action;
		this.ID = ID;
		this.Label = Label;
		this.StreamData = StreamData;		
	}

	public int ReadAction()
	{
		return this.Action;
	}
	
	public String ReadID()
	{
		return this.ID;
	}
	
	public String ReadLabel()
	{
		return this.Label;
	}
	
	public void SetStreamdata(Object Streamdata)
	{
		this.StreamData = Streamdata;
	}
	
	public Object ReadStreamdata()
	{
		return this.StreamData;
	}
	 
}