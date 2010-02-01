package sr.player;

import java.util.TimerTask;

public class downloadTimerTask extends TimerTask {
		
		private SRPlayer callingactivity;

		public downloadTimerTask(SRPlayer act) {
			this.callingactivity = act;
		}
	
		@Override
		public void run() {				
			this.callingactivity.onDownloadTimerElapse();
		}
	
}