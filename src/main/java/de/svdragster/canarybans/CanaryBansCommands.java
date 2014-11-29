package de.svdragster.canarybans;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import net.canarymod.Canary;
import net.canarymod.api.OfflinePlayer;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.factory.ItemFactory;
import net.canarymod.api.factory.ObjectFactory;
import net.canarymod.api.inventory.Inventory;
import net.canarymod.api.inventory.Item;
import net.canarymod.api.inventory.ItemType;
import net.canarymod.chat.ChatFormat;
import net.canarymod.chat.MessageReceiver;
import net.canarymod.commandsys.Command;
import net.canarymod.commandsys.CommandListener;

public class CanaryBansCommands implements CommandListener {

	CanaryBansListener listener = new CanaryBansListener();
	
	public static boolean isNumeric(String str) {
		try {
			@SuppressWarnings("unused")
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
	public static String getNameFromUuid(String UUID) {
		try {
			URL url = new URL("https://api.mojang.com/user/profiles/"+ UUID.replaceAll("-", "") + "/names");
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = in.readLine();
			line = line.replace("[\"", "");
			line = line.replace("\"]", "");
			return line;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	@Command(aliases = { "canarybans", "cb", "cbans" }, description = "Canarybans command", permissions = { "cbans.command" }, toolTip = "/canarybans")
	public void CanaryBansCommand(MessageReceiver caller, String[] parameters) {
		Player player = (Player) caller;
		//Canary.getServer().broadcastMessage("UUID: " + new MojangUuid(player.getName()).getUniqueId());
		//caller.message(ChatFormat.GREEN + player.getUUIDString());
		int l = parameters.length;
		if (l >= 2) {
			String p1 = parameters[1];
			if (p1.equalsIgnoreCase("register")) {
				if (l >= 4) {
					String password = parameters[2];
					String repeat = parameters[3];
					if (password.equals(repeat)) {
						String hpass = listener.sha256(password);
						String params = "cbans.php?serverid=" + player.getUUIDString() + "&pass=" + hpass + "&action=register";
						String result = null;
						try {
							result = listener.sendGet(params);
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (result != null && !result.isEmpty() && result.equalsIgnoreCase("okay")) {
							caller.message(ChatFormat.GREEN + "Successfully registered! CanaryBans should now function properly.");
							listener.setUuid(player.getUUIDString());
							listener.setPassword(hpass);
						} else {
							caller.notice("Could not register. Are you registered already, or is svdragster.dtdns.net down?");
						}
					} else {
						caller.notice("Repeat password does not match password");
					}
				} else {
					caller.notice("Usage: /cbans register <password> <repeat password>");
				}
				return;
			} else if (p1.equalsIgnoreCase("changepassword")) {
				if (l >= 4) {
					String password = parameters[2];
					String newpass = parameters[3];
					String hpass = listener.sha256(password);
					String hnewpass = listener.sha256(newpass);
					String params = "cbans.php?serverid=" + listener.getUuid() + "&pass=" + hpass + "&action=changepass&newpass=" + hnewpass;
					String result = null;
					try {
						result = listener.sendGet(params);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (result != null && !result.isEmpty() && result.equalsIgnoreCase("okay")) {
						caller.message(ChatFormat.GREEN + "Successfully changed password!");
						listener.setPassword(hpass);
					} else {
						caller.notice("Could not change password. Does your account exist, or is svdragster.dtdns.net down?");
					}
				} else {
					caller.notice("Usage: /cbans changepass <old password> <new password>");
				}
				return;
			} else if (p1.equalsIgnoreCase("reputation")) {
				if (player.hasPermission("cbans.setrep")) {
					if (l >= 5) {
						String playername = parameters[2];
						OfflinePlayer offlinePlayer = Canary.getServer().getOfflinePlayer(playername);
						//if (offlinePlayer != null) {
							String uuid;
							if (offlinePlayer == null) {
								uuid = new MojangUuid(playername).getUniqueId();
							} else {
								uuid = offlinePlayer.getUUIDString();
							}
							String strRep = parameters[3];
							if (isNumeric(strRep)) {
								StringBuilder reason = new StringBuilder();
								for (int i=4; i<l; i++) {
									reason.append(parameters[i] + "_");
								}
								int rep = Integer.parseInt(strRep);
								if (rep >= -100 && rep <= 100) {
									String params = "cbans.php?serverid=" + listener.getUuid() + "&pass=" + listener.getPassword() + "&action=setrep&rep=" + rep + "&playerid=" + uuid + "&reason=" + reason.toString();
									String result = null;
									try {
										result = listener.sendGet(params);
									} catch (Exception e) {
										e.printStackTrace();
									}
									if (result != null && !result.isEmpty() && result.equalsIgnoreCase("okay")) {
										caller.message(ChatFormat.GREEN + "Successfully set reputation!");
									} else {
										caller.notice("Could not set reputation. Does your account exist, is " + playername + " in the system, or is svdragster.dtdns.net down?");
									}
								} else {
									caller.notice("Reputation can maximally be 100 or minimally -100.");
								}
							} else {
								caller.notice("Reputation must be a number.");
							}
						/*} else {
							caller.notice(playername + " has not joined your server yet.");
						}*/
					} else {
						caller.notice("Usage: /cbans reputation <playername> <reputation> <reason> -- Max: 100, Min: -100");
					}
				} else {
					caller.notice("You don't have permission to set the reputation of a player.");
				}
				return;
			} else if (p1.equalsIgnoreCase("info")) {
				if (l >= 3) {
					String playername = parameters[2];
					OfflinePlayer offlinePlayer = Canary.getServer().getOfflinePlayer(playername);
					//if (offlinePlayer != null) {
						String uuid;
						if (offlinePlayer == null) {
							uuid = new MojangUuid(playername).getUniqueId();
						} else {
							uuid = offlinePlayer.getUUIDString();
						}
						String params = "cbans.php?serverid=" + listener.getUuid() + "&pass=" + listener.getPassword() + "&action=info&playerid=" + uuid;
						String result = null;
						try {
							result = listener.sendGet(params);
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (result != null && !result.isEmpty() && result.contains("\t")) {
							ItemFactory iFactory = Canary.factory().getItemFactory();
							ObjectFactory oFactory = Canary.factory().getObjectFactory();
							Inventory inv = oFactory.newCustomStorageInventory("CanaryBans", 4);
							Item head = iFactory.newItem(ItemType.SkeletonHead);
							head.setAmount(1);
							head.setDamage(3);
							//SkullHelper.setOwner(head, offlinePlayer.getName());
							//SkullHelper.setOwner(head, new GameProfile(offlinePlayer.getUUID(), offlinePlayer.getName())); // DOES NOT WORK
							head.setSlot(0);
							String[] reps = result.split("\t");
							//StringBuilder builder = new StringBuilder();
							int totalRep = 0;
							for (int i=reps.length-1; i>=0; i--) {
								String rep = reps[i];
								if (rep != null && !rep.isEmpty()) {
									int intRep = Integer.parseInt(rep.split(";")[0]);
									ChatFormat color = ChatFormat.GRAY;
									if (intRep > 0) {
										color = ChatFormat.GREEN;
									} else if (intRep < 0) {
										color = ChatFormat.RED;
									}
									//builder.append(color.toString() + rep + "  ");
									totalRep = totalRep + intRep;
									
									Item book = iFactory.newItem(ItemType.Book);
									book.setAmount(1);
									book.setDisplayName(ChatFormat.DARK_AQUA.toString() + "Server " + i + ": " + color.toString() + intRep);
									book.setLore("Reason: " + ChatFormat.YELLOW + rep.split(";")[1]);
									inv.addItem(book);
								}
							}
							//caller.message(ChatFormat.GOLD + playername + "s Reputations:");
							//caller.message(builder.toString());
							ChatFormat color = ChatFormat.GRAY;
							if (totalRep > 0) {
								color = ChatFormat.GREEN;
							} else if (totalRep < 0) {
								color = ChatFormat.RED;
							}
							head.setDisplayName(playername + color + "(" + totalRep + ")");
							head.setSlot(inv.getSize() - 1);
							inv.setSlot(head);
							player.openInventory(inv);
							//caller.message(ChatFormat.GOLD + "Total: " + color + totalRep);
						} else {
							caller.notice("Could not get reputation. Does your account exist, is " + playername + " in the system, or is svdragster.dtdns.net down?");
						}
					/*} else {
						caller.notice(playername + " has not joined your server yet.");
					}*/
				} else {
					caller.notice("Usage: /cbans info <playername>");
				}
				return;
			} else if (p1.equalsIgnoreCase("load")) {
				listener.loadProperties();
				caller.message(ChatFormat.GREEN + "Loaded properties.");
				return;
			} else if (p1.equalsIgnoreCase("save")) {
				listener.saveProperties();
				caller.message(ChatFormat.GREEN + "Saved properties.");
				return;
			} else if (p1.equalsIgnoreCase("login")) {
				if (l >= 3) {
					listener.setUuid(player.getUUIDString());
					String password = parameters[2];
					String hpass = listener.sha256(password);
					listener.setPassword(hpass);
					caller.message(ChatFormat.GREEN + "Set password.");
				}
				return;
			}
		}
		caller.message(ChatFormat.GRAY + "/cbans register <password> <repeat password>");
		caller.message(ChatFormat.WHITE + "/cbans changepassword <old password> <new password>");
		caller.message(ChatFormat.GRAY + "/cbans reputation <playername> <reputation> -- Max: 100, Min: -100");
		caller.message(ChatFormat.WHITE + "/cbans info <playername> -- lists all repuation of this player");
		caller.message(ChatFormat.GRAY + "/cbans load -- loads the canarybans.properties");
		caller.message(ChatFormat.WHITE + "/cbans save -- saves the canarybans.properties");
		caller.message(ChatFormat.GRAY + "/cbans login <password> -- sets password locally");
		caller.message(ChatFormat.WHITE + "Email support: canarybans@go4more.de");
	}
}
