package sr.player;

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
import java.io.IOException;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateHandler extends PhoneStateListener {
	private PlayerService service;
	private boolean WaitingForEndOfCall;
	
	
	
	
	public PhoneStateHandler(PlayerService playerService) {
		this.service = playerService;
	}




	public void onCallStateChanged(int state, String incomingNumber) {
		switch (state) {
		case TelephonyManager.CALL_STATE_OFFHOOK:
			Log.d(SRPlayer.TAG, "Offhook state detected");
			break;
		case TelephonyManager.CALL_STATE_RINGING:
			Log.d(SRPlayer.TAG, "Ringing detected");
			WaitingForEndOfCall = true;
			service.stopPlay();
			break;
		case TelephonyManager.CALL_STATE_IDLE:
			Log.d(SRPlayer.TAG, "Idle state detected");
			if (WaitingForEndOfCall) {
				try {
					service.startPlay(service.getCurrentStation());
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				WaitingForEndOfCall = false;
			}
			break;
		default:
			Log.d(getClass().getSimpleName(), "Unknown phone state=" + state);
		}
	}
}
