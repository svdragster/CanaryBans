package de.svdragster.canarybans;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.Callable;

public class MojangUuid implements Callable<String> {
	private String name;
	private String uuid;

	public MojangUuid(String name) {
		this.name = name;
		try {
			this.uuid = call();
		} catch (Exception e) {
			System.out.print("no connection with mojang site");
		}
	}

	public String getUniqueId() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public String call() throws Exception {
		try {
			URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + this.name);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			String Line;
			while ((Line = in.readLine()) != null) {
				String uuid = Line.substring(7, 39);
				return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-"
					+ uuid.substring(12, 16) + "-" + uuid.substring(16, 20)
					+ "-" + uuid.substring(20, 32);
			}
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
