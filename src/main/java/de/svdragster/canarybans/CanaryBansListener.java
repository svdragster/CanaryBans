package de.svdragster.canarybans;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import net.canarymod.Canary;
import net.canarymod.ToolBox;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.chat.ChatFormat;
import net.canarymod.hook.HookHandler;
import net.canarymod.hook.command.PlayerCommandHook;
import net.canarymod.hook.player.ConnectionHook;
import net.canarymod.hook.player.DisconnectionHook;
import net.canarymod.hook.player.SlotClickHook;
import net.canarymod.plugin.PluginListener;
import net.visualillusionsent.utils.PropertiesFile;

public class CanaryBansListener implements PluginListener {

	private static String uuid;
	private static String password;
	private int reputationBan = -300; //when the player should be banned
	
	private ArrayList<Player> banned = new ArrayList<Player>();
	
	public void setUuid(String uuid) {
		CanaryBansListener.uuid = uuid;
	}
	
	public String getUuid() {
		return CanaryBansListener.uuid;
	}
	
	public void setPassword(String password) {
		CanaryBansListener.password = password;
	}
	
	public String getPassword() {
		return CanaryBansListener.password;
	}
	
	public void setReputationBan(int argNumber) {
		this.reputationBan = argNumber;
	}
	
	public int getReputationBan() {
		return this.reputationBan;
	}
	
	public boolean isSet(String string) {
		if (string == null || string.isEmpty() || string.equalsIgnoreCase("null")) {
			return false;
		}
		return true;
	}
	
	public void broadcast(String string) {
		Canary.getServer().broadcastMessageToAdmins(ChatFormat.GOLD + "[CanaryBans] " + string);
	}
	
	public String sha256(String string) {
        MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
	        md.update(string.getBytes());
	 
	        byte byteData[] = md.digest();
	 
	        //convert the byte to hex format method 2
	        StringBuffer hexString = new StringBuffer();
	    	for (int i=0;i<byteData.length;i++) {
	    		String hex=Integer.toHexString(0xff & byteData[i]);
	   	     	if(hex.length()==1) hexString.append('0');
	   	     	hexString.append(hex);
	    	}
	    	return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void saveProperties() {
		PropertiesFile props = net.canarymod.config.Configuration.getPluginConfig(new CanaryBans());
		if (props.containsKey("uuid")) {
			props.setString("uuid", getUuid());
		}
		if (props.containsKey("password")) {
			props.setString("password", getPassword());
		}
		if (props.containsKey("reputation-ban")) {
			props.setInt("reputation-ban", getReputationBan());
		}
		props.save();
	}
	
	public void loadProperties() {
		/*File dir = new File("config/canarybans/");
		if (!dir.exists()) {
			dir.mkdir();
		}
		File file = new File("config/canarybans/canarybans.properties");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				broadcast(ChatFormat.YELLOW + "Could not create properties: " + e.getMessage());
				return;
			}
		}*/
		
		//PropertiesFile props = new PropertiesFile(file);
		PropertiesFile props = net.canarymod.config.Configuration.getPluginConfig(new CanaryBans());
		if (!props.containsKey("uuid")) {
			props.setString("uuid", "null");
			props.addComment("reputation-ban", "PLEASE don't change uuid or password here, use /cb login or /cb register, or your password won't be hashed");
		}
		if (!props.containsKey("password")) {
			props.setString("password", "null");
			broadcast(ChatFormat.YELLOW + "Username and/or password not set. For help use " + ChatFormat.RED + "/canarybans help");
		}
		if (!props.containsKey("reputation-ban")) {
			props.setInt("reputation-ban", getReputationBan());
			props.addComment("reputation-ban", "reputation-ban: When a players reputation is lower than this he will be banned from your server as long as his reputation stays below. Default: -300");
		}
		props.save();
		
		setUuid(props.getString("uuid"));
		setPassword(props.getString("password"));
		setReputationBan(props.getInt("reputation-ban"));
	}
	
	@HookHandler
	public void onLogin(ConnectionHook hook) {
		Player player = hook.getPlayer();
		int rep = totalReputation(player.getUUIDString());
		if (rep < getReputationBan() && !player.hasPermission("cbans.exception")) {
			player.kickNoHook(ChatFormat.GOLD + "Automatic Ban:\n" + ChatFormat.RED + "Your CanaryBans reputation: " + ChatFormat.YELLOW + rep + ChatFormat.RED + "\nReputation needed to join: " + ChatFormat.GREEN + getReputationBan() + "\n\n" + ChatFormat.BLUE + "If you think you have been banned wrongly,\n contact " + ChatFormat.DARK_AQUA + "canarybans@go4more.de");
			banned.add(player);
			hook.setHidden(true);
		} else {
			player.message(ChatFormat.GOLD + "[CanaryBans] " + ChatFormat.BLUE + "Your Reputation: " + ChatFormat.DARK_PURPLE + rep + ChatFormat.YELLOW + "/" + ChatFormat.GREEN + getReputationBan());
		}
		
		if (hook.getPlayer().hasPermission("cbans.checkforupdates")) {
			String version = new CanaryBans().getVersion();
			try {
				String params = "checkupdate.php?version=" + version + "&plugin=canarybans&canary=" + Canary.getServer().getCanaryModVersion() + "&player=" + player.getName();
				String result = sendGet(params);
				if ((result != null) && (!result.isEmpty())) {
					hook.getPlayer().message(result);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@HookHandler
	public void onLogout(DisconnectionHook hook) {
		Player player = hook.getPlayer();
		if (banned.contains(player)) {
			hook.setHidden(true);
			banned.remove(player);
		}
	}
	
	/*@HookHandler
	public void onBan(BanHook hook) { // NOT CALLING
		Canary.getServer().message("BAN");
		PlayerReference player = hook.getBannedPlayer();
		Canary.getServer().broadcastMessage(hook.getBantime() + ", " + player.getUUIDString());
		String params = "cbans.php?serverid=" + getUuid() + "&pass=" + getPassword() + "&action=setrep&rep=-100" + "&playerid=" + player.getUUIDString() + "&reason=ban";
		try {
			sendGet(params);
		} catch (Exception e) {
			Canary.getServer().broadcastMessageToAdmins(ChatFormat.GOLD + "[CanaryBans] " + ChatFormat.RED + "Could not set ban reputation of player " + player.getName());
			e.printStackTrace();
		}
	}*/
	
	
	
	@HookHandler
	public void onCommand(PlayerCommandHook hook) {
		String[] command = hook.getCommand();
		if (command.length >= 2) {
			if (command[0].equalsIgnoreCase("/ban")) {
				if (hook.getPlayer().hasPermission("cbans.setrep")) {
					String playerUuid = ToolBox.uuidFromUsername(command[1]).toString();
					String params = "cbans.php?serverid=" + getUuid() + "&pass=" + getPassword() + "&action=setrep&rep=-100" + "&playerid=" + playerUuid + "&reason=ban";
					try {
						sendGet(params);
					} catch (Exception e) {
						Canary.getServer().broadcastMessageToAdmins(ChatFormat.GOLD + "[CanaryBans] " + ChatFormat.RED + "Could not set ban reputation of player " + command[1]);
						e.printStackTrace();
					}
				}
			} else if (command[0].equalsIgnoreCase("/unban")) {
				if (hook.getPlayer().hasPermission("cbans.setrep")) {
					String playerUuid = ToolBox.uuidFromUsername(command[1]).toString();
					String params = "cbans.php?serverid=" + getUuid() + "&pass=" + getPassword() + "&action=setrep&rep=0" + "&playerid=" + playerUuid + "&reason=unban";
					try {
						sendGet(params);
					} catch (Exception e) {
						Canary.getServer().broadcastMessageToAdmins(ChatFormat.GOLD + "[CanaryBans] " + ChatFormat.RED + "Could not set unban reputation of player " + command[1]);
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	@HookHandler
	public void onSlotClick(SlotClickHook hook) {
		if (hook.getInventory().getInventoryName().equalsIgnoreCase("CanaryBans")) {
			hook.setCanceled();
		}
	}
	
	public int totalReputation(String playerid) {
		String params = "cbans.php?serverid=" + getUuid() + "&pass=" + getPassword() + "&action=info&playerid=" + playerid;
		String result = null;
		try {
			result = sendGet(params);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (result != null && !result.isEmpty() && result.contains("\t")) {
			String[] reps = result.split("\t");
			int totalRep = 0;
			for (int i=0; i<reps.length; i++) {
				String rep = reps[i];
				if (rep != null && !rep.isEmpty()) {
					int intRep = 0;
					if (rep.contains(";")) {
						intRep = Integer.parseInt(rep.split(";")[0]);
					} else {
						intRep = Integer.parseInt(rep);
					}
					totalRep = totalRep + intRep;
				}
			}
			return totalRep;
		}
		return 0;
	}
	
	public String sendGet(String parameters) throws Exception {
		String MYIDSTART = "svdragster>";
		String MYIDEND = "<svdragster";
		String url = "http://svdragster.dtdns.net/" + parameters;

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("GET");

		con.setRequestProperty("User-Agent", "canary_minecraft");

		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));

		StringBuffer response = new StringBuffer();
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		String result = response.toString();
		if ((result.contains(MYIDSTART)) && (result.contains(MYIDEND))) {
			int endPos = result.indexOf(MYIDEND);
			result = result.substring(MYIDSTART.length(), endPos);
		}
		return result;
	}
}
