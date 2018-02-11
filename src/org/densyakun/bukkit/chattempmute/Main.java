package org.densyakun.bukkit.chattempmute;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.densyakun.csvm.CSVFile;


public class Main extends JavaPlugin implements Listener {
	
	public File dir;
	public CSVFile mutelistcsv;
	public Map<UUID, Long> mutelist;
	
	@Override
	public void onEnable() {
		load();
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	public void load() {
		mutelist = new HashMap<UUID, Long>();
		(dir = getDataFolder()).mkdirs();
		if ((mutelistcsv = new CSVFile(new File(dir, "mutelist.csv"))) != null && mutelistcsv.getFile().exists()) {
			try {
				List<List<String>> a = mutelistcsv.AllRead();
				for (int b = 0; b < a.size(); b++) {
					if (2 <= a.get(b).size()) {
						mutelist.put(UUID.fromString(a.get(b).get(0)), Long.valueOf(a.get(b).get(1)));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void save() {
		if (!mutelistcsv.getFile().exists()) {
			try {
				mutelistcsv.getFile().createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		List<List<String>> data = new ArrayList<List<String>>();
		for (Iterator<UUID> keys = mutelist.keySet().iterator(); keys.hasNext();) {
			List<String> line = new ArrayList<String>();
			UUID key = keys.next();
			line.add(key.toString());
			line.add(mutelist.get(key).toString());
			data.add(line);
		}
		try {
			mutelistcsv.AllWrite(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender.isOp() || sender.hasPermission("chattempmute.admin")) {
			if (label.equalsIgnoreCase("mute")) {
				if (args.length < 1)
					sender.sendMessage(ChatColor.GREEN + "[" + getName() + "] " + ChatColor.RED + "パラメーターが足りません");
				else {
					Player player = getServer().getPlayer(args[0]);
					if (player == null)
						sender.sendMessage(ChatColor.GREEN + "[" + getName() + "] " + ChatColor.RED + "プレイヤーが見つかりません");
					else {
						if (args.length < 2) {
							mutelist.put(player.getUniqueId(), -1l);
							sender.sendMessage(ChatColor.GREEN + "[" + getName() + "] " + ChatColor.AQUA + "プレイヤー\"" + ChatColor.WHITE + player.getDisplayName() + ChatColor.AQUA + "\"のチャットを無期限で禁止しました");
							save();
						} else {
							int time = 0;
							String a = "";
							for (int b = 0; b < args[1].length(); b++) {
								char c = args[1].charAt(b);
								if (c == 'd' || c == 'D') {
									time += Integer.valueOf(a) * 24 * 60 * 60;
									a = "";
								} else if (c == 'h' || c == 'H') {
									time += Integer.valueOf(a) * 60 * 60;
									a = "";
								} else if (c == 'm' || c == 'M') {
									time += Integer.valueOf(a) * 60;
									a = "";
								} else if (c == 's' || c == 'S') {
									time += Integer.valueOf(a);
									a = "";
								} else
									a += c;
							}
							
							if (a.length() > 0)
								time += Integer.valueOf(a);
							
							if (time == 0)
								sender.sendMessage(ChatColor.GREEN + "[" + getName() + "] " + ChatColor.RED + "正しく数字を入力して下さい");
							else {
								mutelist.put(player.getUniqueId(), new Date().getTime() + time * 1000);
								sender.sendMessage(ChatColor.GREEN + "[" + getName() + "] " + ChatColor.AQUA + "プレイヤー\"" + ChatColor.WHITE + player.getDisplayName() + ChatColor.AQUA + "\"のチャットを" + a(time) + "禁止しました");
								save();
							}
						}
					}
				}
			} else if (label.equalsIgnoreCase("unmute")) {
				if (args.length < 1)
					sender.sendMessage(ChatColor.GREEN + "[" + getName() + "] " + ChatColor.RED + "パラメーターが足りません");
				else {
					Player player = getServer().getPlayer(args[0]);
					if (player == null)
						sender.sendMessage(ChatColor.GREEN + "[" + getName() + "] " + ChatColor.RED + "プレイヤーが見つかりません");
					else {
						mutelist.remove(player.getUniqueId());
						sender.sendMessage(ChatColor.GREEN + "[" + getName() + "] " + ChatColor.AQUA + "プレイヤー\"" + ChatColor.WHITE + player.getDisplayName() + ChatColor.AQUA + "\"のチャットを許可しました");
						save();
					}
				}
			} else if (label.equalsIgnoreCase("mutelist")) {
				int size = 5;
				int page = 0;
				if (1 <= args.length) {
					try {
						page = Integer.valueOf(args[0]);
						if (page < 0)
							page = 0;
					} catch (NumberFormatException e) {
					}
				}
				int maxpage = 0;
				for (int a = 0; a + size < mutelist.size(); a += size)
					maxpage++;
				if (maxpage < page)
					page = maxpage;
				
				String str = ChatColor.GREEN + "[" + getName() + "] " + ChatColor.AQUA + "ミュートリスト(" + (page + 1) + "/" + (maxpage + 1) + ")\n";
				if (mutelist.size() == 0)
					str += ChatColor.GRAY + "(なし)";
				else {
					Iterator<UUID> keys = mutelist.keySet().iterator();
					for (int a = 0; keys.hasNext() && a < page * size + size && a < mutelist.size(); a++) {
						UUID key = keys.next();
						if (page * size <= a) {
							Long value = mutelist.get(key);
							if (value == -1)
								str += ChatColor.AQUA + "UUID:" + ChatColor.GOLD + key + ChatColor.AQUA + " 期限: " + ChatColor.RED + "無期限\n";
							else
								str += ChatColor.AQUA + "UUID:" + ChatColor.GOLD + key + ChatColor.AQUA + " 期限: " + ChatColor.GOLD + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(value)) + "(あと" + a((value - new Date().getTime()) / 1000) + ")\n";
						}
					}
				}
				sender.sendMessage(str);
			}
		}
		return true;
	}
	
	@EventHandler
	public void AsyncPlayerChat(AsyncPlayerChatEvent e) {
		Long unmute = mutelist.get(e.getPlayer().getUniqueId());
		if (unmute != null) {
			if (unmute == -1) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatColor.GREEN + "[" + getName() + "] " + ChatColor.RED + "チャットが禁止されています。(無期限)");
			} else {
				long currenttime = new Date().getTime();
				if (currenttime >= unmute) {
					mutelist.remove(e.getPlayer().getUniqueId());
					save();
				} else {
					e.setCancelled(true);
					e.getPlayer().sendMessage(ChatColor.GREEN + "[" + getName() + "] " + ChatColor.RED + "現在チャットが禁止されています。期限: " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(unmute)) + "(あと" + a((unmute - currenttime) / 1000) + ")");
				}
			}
		}
	}
	
	public String a(long time) {
		String str = "";
		if (time < 24 * 60 * 60) {
			if (time < 60 * 60) {
				if (time < 60)
					str += time + "秒";
				else
					str += time / 60 + "分";
			} else
				str += time / 60 / 60 + "時間";
		} else
			str += time / 24 / 60 / 60 + "日";
		
		return str;
	}
}
