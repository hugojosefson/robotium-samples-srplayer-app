package sr.player;

import java.util.TimerTask;

public class seekTimerTask extends TimerTask {
		
		private SRPlayer callingactivity;

		public seekTimerTask(SRPlayer act) {
			this.callingactivity = act;
		}
	
		@Override
		public void run() {				
			this.callingactivity.onSeekReqUpdate();
		}
	
}