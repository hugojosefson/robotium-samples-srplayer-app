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

import java.util.TimerTask;

public class sleepTimertask extends TimerTask {
		
		private PlayerService boundservice;

		public sleepTimertask(PlayerService service) {
			this.boundservice = service;
		}
	
		@Override
		public void run() {				
			this.boundservice.stopPlay();
		}
	
}