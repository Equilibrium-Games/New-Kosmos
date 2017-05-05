/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved
 *
 * This source file is part of New Kosmos
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package kosmos;

import flounder.devices.*;
import flounder.framework.*;
import flounder.framework.updater.*;
import flounder.logger.*;
import flounder.maths.*;
import flounder.networking.*;
import flounder.standards.*;
import kosmos.network.packets.*;

public class KosmosServer extends Framework {
	public static void main(String[] args) {
		new KosmosServer().run();
		System.exit(0);
	}

	public KosmosServer() {
		super("kosmos", new UpdaterDefault(null), 30, new Extension[]{new ServerInterface()}, new Module[]{});
		//	FlounderDisplay.setup(256, 128, "New Kosmos Server", new MyFile[]{}, false, false, 0, false, true);
	}

	public static class ServerInterface extends Standard {
		public static int serverPort;
		public static int serverSeed;

		private Timer timerWorld;

		public ServerInterface() {
			super(FlounderNetwork.class, FlounderDisplay.class);
		}

		@Override
		public void init() {
			ServerInterface.serverPort = KosmosConfigs.HOST_PORT.setReference(() -> serverPort).getInteger();
			ServerInterface.serverSeed = KosmosConfigs.HOST_SEED.setReference(() -> serverSeed).getInteger();

			this.timerWorld = new Timer(45.0f);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				FlounderLogger.get().log(e);
			}

			FlounderLogger.get().log("Server seed: " + serverSeed);
			FlounderNetwork.get().startServer(serverPort);
		}

		@Override
		public void update() {
			// Remind the clients the time, acts as a "are your there" ping as well.
			if (timerWorld.isPassedTime()) {
				new PacketWorld(serverSeed, Framework.getTimeSec()).writeData(FlounderNetwork.get().getSocketServer());
				timerWorld.resetStartTime();
			}
		}

		@Override
		public void profile() {

		}

		@Override
		public void dispose() {
			new PacketDisconnect("server").writeData(FlounderNetwork.get().getSocketServer());
			KosmosConfigs.saveAllConfigs();
		}

		@Override
		public boolean isActive() {
			return true;
		}
	}
}
