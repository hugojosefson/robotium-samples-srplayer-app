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