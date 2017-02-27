package com.faris.kingkits.listener;

import com.faris.kingkits.KingKits;
import com.faris.kingkits.Kit;
import com.faris.kingkits.Messages;
import com.faris.kingkits.Permissions;
import com.faris.kingkits.controller.*;
import com.faris.kingkits.helper.util.*;
import com.faris.kingkits.player.KitPlayer;
import com.faris.kingkits.storage.DataStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class EventListener implements Listener {

	private final KingKits plugin;

	private Map<UUID, Integer> joinTasks = new HashMap<>();
	private Map<UUID, Integer> updateInventoryTasks = new HashMap<>();
	private World kitpvpworld;

	public EventListener(KingKits pluginInstance) {
		this.plugin = pluginInstance;
		kitpvpworld = pluginInstance.getServer().getWorld("dogepvp");
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getPlayer().getWorld() != kitpvpworld) //Pls do not execute this cancer on other worlds.
			return;
		try {
			final Player player = event.getPlayer();
			final KitPlayer kitPlayer = PlayerController.getInstance().getPlayer(player);

			try {
				if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					if (event.getItem() != null) {
						if (event.getItem().getType() == ConfigController.getInstance().getGuiItemType() && (ConfigController.getInstance().getGuiItemData() == -1 || event.getItem().getDurability() == ConfigController.getInstance().getGuiItemData())) {
							if (Utilities.isPvPWorld(player.getWorld())) {
								GuiController.getInstance().openKitsMenu(player);
								event.setCancelled(true);
								return;
							}
						} else if (event.getItem().getType() == Material.MUSHROOM_SOUP) {
							if (ConfigController.getInstance().canQuickSoup(player.getWorld())) {
								if (player.hasPermission(Permissions.SOUP_QUICKSOUP)) {
									if (Utilities.isPvPWorld(player.getWorld())) {
										int soupAmount = event.getItem().getAmount();
										if (soupAmount > 0) {
											boolean valid = true;
											if (player.getHealth() < player.getMaxHealth()) {
												player.setHealth(Math.min(player.getHealth() + ConfigController.getInstance().getQuickSoupHeal(), player.getMaxHealth()));
											} else if (player.getFoodLevel() < 20) {
												player.setFoodLevel(Math.min(player.getFoodLevel() + 6, 20));
											} else {
												valid = false;
											}
											if (valid) {
												event.setCancelled(true);
												if (soupAmount == 1) {
													if (event.getHand() == EquipmentSlot.HAND)
														player.getInventory().setItemInMainHand(new ItemStack(Material.BOWL));
													else if (event.getHand() == EquipmentSlot.OFF_HAND)
														player.getInventory().setItemInOffHand(new ItemStack(Material.BOWL));
												} else {
													ItemStack newItem = event.getItem();
													newItem.setAmount(soupAmount - 1);
													if (event.getHand() == EquipmentSlot.HAND)
														player.getInventory().setItemInMainHand(newItem);
													else if (event.getHand() == EquipmentSlot.OFF_HAND)
														player.getInventory().setItemInOffHand(newItem);
													player.getInventory().addItem(new ItemStack(Material.BOWL));
												}
												return;
											}
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception ex) {
				Bukkit.getServer().getLogger().log(Level.SEVERE, "Failed to execute the item detection code.", ex);
			}

			try {
				if (event.getItem() != null) {
					if (Utilities.isPvPWorld(player.getWorld())) {
						if (kitPlayer != null && kitPlayer.hasKit() && !kitPlayer.getKit().canItemsBreak()) {
							if (ItemUtilities.getDamageableMaterials().contains(event.getItem().getType())) {
								event.getItem().setDurability((short) 0);
							}
						}
					}
				}
			} catch (Exception ex) {
				Bukkit.getServer().getLogger().log(Level.SEVERE, "Failed to repair an unbreakable item.", ex);
			}

			try {
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					if (Utilities.isPvPWorld(player.getWorld())) {
						if (event.getClickedBlock().getState() instanceof Sign) {
							Sign clickedSign = (Sign) event.getClickedBlock().getState();
							String firstLine = clickedSign.getLine(0);

							if (firstLine.equals(ConfigController.getInstance().getSignsKit(player.getWorld())[1])) {
								event.setCancelled(true);
								if (player.hasPermission(Permissions.SIGN_KIT_USE)) {
									String strKit = ChatUtilities.stripColour(clickedSign.getLine(1));

									Kit kit = null;
									KitUtilities.KitSearchResult kitResult = clickedSign.getLine(3).equalsIgnoreCase("User") ? KitUtilities.getKits(strKit, kitPlayer) : KitUtilities.getKits(strKit);
									if (kitResult.hasKit()) {
										kit = kitResult.getKit();
									} else if (kitResult.hasOtherKits()) {
										if (kitResult.getOtherKits().size() == 1) {
											kit = kitResult.getOtherKits().get(0);
										} else {
											Messages.sendMessage(player, Messages.KIT_MULTIPLE_FOUND, strKit);
										}
									} else {
										Messages.sendMessage(player, Messages.KIT_NOT_FOUND, strKit);
									}
									if (kit != null) KitUtilities.setKit(player, kit);
								} else {
									Messages.sendMessage(player, Messages.SIGN_USE_NO_PERMISSION, "kit");
								}
								return;
							} else if (firstLine.equals(ConfigController.getInstance().getSignsKitList(player.getWorld())[1])) {
								event.setCancelled(true);
								if (player.hasPermission(Permissions.SIGN_KIT_LIST_USE)) {
									KitUtilities.listKits(player);
								} else {
									Messages.sendMessage(player, Messages.SIGN_USE_NO_PERMISSION, "kit list");
								}
								return;
							} else if (firstLine.equals(ConfigController.getInstance().getSignsRefill(player.getWorld())[1])) {
								event.setCancelled(true);
								if (player.hasPermission(Permissions.SIGN_REFILL_USE)) {
									String strType = clickedSign.getLine(1);
									if (strType != null && strType.equalsIgnoreCase("All")) {
										player.performCommand("refill all");
									} else {
										player.performCommand("refill");
									}
								} else {
									Messages.sendMessage(player, Messages.SIGN_USE_NO_PERMISSION, "refill");
								}
								return;
							}
						}
					}
				}
			} catch (Exception ex) {
				Bukkit.getServer().getLogger().log(Level.SEVERE, "Failed to check block for any valid signs.", ex);
			}

			try {
				if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					if (event.getItem() != null) {
						if (event.getItem().getType() == Material.COMPASS) {
							if (ConfigController.getInstance().shouldSetCompassToNearestPlayer(player.getWorld())) {
								if (player.hasPermission(Permissions.COMPASS)) {
									if (Utilities.isPvPWorld(player.getWorld())) {
										Player nearestPlayer = null;
										double distance = -1D;
										for (Player target : player.getWorld().getPlayers()) {
											if (!target.getUniqueId().equals(player.getUniqueId())) {
												if (distance == -1D) {
													distance = player.getLocation().distanceSquared(target.getLocation());
													nearestPlayer = target;
												} else {
													double distanceSquared = player.getLocation().distanceSquared(target.getLocation());
													if (distanceSquared <= distance) {
														distance = distanceSquared;
														nearestPlayer = target;
													}
												}
											}
										}
										if (nearestPlayer != null) {
											player.setCompassTarget(nearestPlayer.getLocation());
											Messages.sendMessage(player, Messages.COMPASS_POINTING_PLAYER, nearestPlayer.getName());
											CompassController.getInstance().setTarget(player.getUniqueId(), nearestPlayer.getUniqueId());
										} else {
											player.setCompassTarget(player.getWorld().getSpawnLocation());
											Messages.sendMessage(player, Messages.COMPASS_POINTING_SPAWN);
											CompassController.getInstance().removeTarget(player.getUniqueId());
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception ex) {
				Bukkit.getServer().getLogger().log(Level.SEVERE, "Failed to execute the compass code.", ex);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerChangeSign(SignChangeEvent event) {
		if (event.getPlayer().getWorld() != kitpvpworld) //Pls do not execute this cancer on other worlds.
			return;
		try {
			final Player player = event.getPlayer();
			if (player == null) return;
			if (Utilities.isPvPWorld(player.getWorld())) {
				final KitPlayer kitPlayer = PlayerController.getInstance().getPlayer(player);
				if (!PlayerUtilities.checkPlayer(player, kitPlayer)) return;

				String firstLine = event.getLine(0);
				if (firstLine.equals(ConfigController.getInstance().getSignsKit(player.getWorld())[0])) {
					if (player.hasPermission(Permissions.SIGN_KIT_CREATE)) {
						String strKit = event.getLine(1);

						Kit kit = null;
						KitUtilities.KitSearchResult kitResult = event.getLine(3).equalsIgnoreCase("User") ? KitUtilities.getKits(strKit, kitPlayer) : KitUtilities.getKits(strKit);
						if (kitResult.hasKit()) {
							kit = kitResult.getKit();
						} else if (kitResult.hasOtherKits()) {
							if (kitResult.getOtherKits().size() == 1) {
								kit = kitResult.getOtherKits().get(0);
							} else {
								Messages.sendMessage(player, Messages.KIT_MULTIPLE_FOUND, strKit);
								event.setLine(0, ConfigController.getInstance().getSignsKit(player.getWorld())[2]);
							}
						} else {
							Messages.sendMessage(player, Messages.KIT_NOT_FOUND, strKit);
							event.setLine(0, ConfigController.getInstance().getSignsKit(player.getWorld())[2]);
						}
						if (kit != null)
							event.setLine(0, ConfigController.getInstance().getSignsKit(player.getWorld())[1]);
					} else {
						Messages.sendMessage(player, Messages.SIGN_CREATE_NO_PERMISSION, "kit");
						event.setCancelled(true);
					}
				} else if (firstLine.equals(ConfigController.getInstance().getSignsKitList(player.getWorld())[0])) {
					if (player.hasPermission(Permissions.SIGN_KIT_LIST_CREATE)) {
						event.setLine(0, ConfigController.getInstance().getSignsKitList(player.getWorld())[1]);
					} else {
						Messages.sendMessage(player, Messages.SIGN_CREATE_NO_PERMISSION, "kit list");
						event.setCancelled(true);
					}
				} else if (firstLine.equals(ConfigController.getInstance().getSignsRefill(player.getWorld())[0])) {
					if (player.hasPermission(Permissions.SIGN_REFILL_CREATE)) {
						event.setLine(0, ConfigController.getInstance().getSignsRefill(player.getWorld())[1]);
					} else {
						Messages.sendMessage(player, Messages.SIGN_CREATE_NO_PERMISSION, "refill");
						event.setCancelled(true);
					}
				}
			}
		} catch (Exception ex) {
			Bukkit.getServer().getLogger().log(Level.SEVERE, "Failed to handle a sign change.", ex);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		if (event.getPlayer().getWorld() != kitpvpworld) //Pls do not execute this cancer on other worlds.
			return;
		try {
			final Player player = event.getPlayer();
			if (Utilities.isPvPWorld(player.getWorld())) {
				String strCommand = event.getMessage().contains(" ") ? event.getMessage().split(" ")[0].substring(1) : event.getMessage().substring(1);
				KitUtilities.KitSearchResult searchResult = KitUtilities.getKits(strCommand);
				Kit targetKit = null;
				if (searchResult.hasKit()) {
					targetKit = searchResult.getKit();
				} else if (searchResult.hasOtherKits()) {
					if (searchResult.getOtherKits().size() == 1) {
						targetKit = searchResult.getOtherKits().get(0);
					}
				}
				if (targetKit != null && targetKit.hasCommandAlias()) {
					event.setCancelled(true);
					player.performCommand("pvpkit " + targetKit.getName());
				}
			}
		} catch (Exception ex) {
			Bukkit.getServer().getLogger().log(Level.SEVERE, "Failed to handle processing a command.", ex);
		}
	}


	@EventHandler(ignoreCancelled = true)
	public void onPlayerFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.getEntity().getWorld() != kitpvpworld) //Pls do not execute this cancer on other worlds.
			return;
		try {
			if (ConfigController.getInstance().shouldLockFoodLevel(event.getEntity().getWorld())) {
				if (Utilities.isPvPWorld(event.getEntity().getWorld())) {
					event.setFoodLevel(Math.min(Math.abs(ConfigController.getInstance().getFoodLevelLock()), 20));
				}
			}
		} catch (Exception ex) {
			Bukkit.getServer().getLogger().log(Level.SEVERE, "Failed to handle a change in food level.", ex);
		}
	}

	@EventHandler
	public void onPlayerShootBow(EntityShootBowEvent event) {
		if (event.getEntity().getWorld() != kitpvpworld) //Pls do not execute this cancer on other worlds.
			return;
		try {
			if (event.getEntity() instanceof Player && event.getBow() != null) {
				Player player = (Player) event.getEntity();
				if (player.getGameMode() != GameMode.CREATIVE && Utilities.isPvPWorld(player.getWorld())) {
					KitPlayer kitPlayer = PlayerController.getInstance().getPlayer(player);
					if (kitPlayer != null && kitPlayer.hasKit() && !kitPlayer.getKit().canItemsBreak()) {
						event.getBow().setDurability((short) 0);
					}
				}
			}
		} catch (Exception ex) {
			Bukkit.getServer().getLogger().log(Level.SEVERE, "Failed to repair an unbreakable item.", ex);
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity().getWorld() != kitpvpworld) //Pls do not execute this cancer on other worlds.
			return;
		try {
			if (!(event instanceof EntityDamageByEntityEvent) && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
				if (event.getEntity() instanceof Player) {
					final Player damaged = (Player) event.getEntity();
					if (damaged.getGameMode() == GameMode.SURVIVAL || damaged.getGameMode() == GameMode.ADVENTURE) {
						if (Utilities.isPvPWorld(damaged.getWorld())) {
							final KitPlayer kitPlayer = PlayerController.getInstance().getPlayer(damaged);
							if (kitPlayer != null && kitPlayer.hasKit() && !kitPlayer.getKit().canItemsBreak()) {
								boolean updateHelmet = false, updateChestplate = false, updateLeggings = false, updateBoots = false, updateOffhand = false;
								final ItemStack helmet = damaged.getInventory().getHelmet(), chestplate = damaged.getInventory().getChestplate(), leggings = damaged.getInventory().getLeggings(), boots = damaged.getInventory().getBoots(), offhand = damaged.getInventory().getItemInOffHand();
								if (helmet != null && ItemUtilities.getDamageableMaterials().contains(helmet.getType())) {
									helmet.setDurability((short) 0);
									updateHelmet = true;
								}
								if (chestplate != null && ItemUtilities.getDamageableMaterials().contains(chestplate.getType())) {
									chestplate.setDurability((short) 0);
									updateChestplate = true;
								}
								if (leggings != null && ItemUtilities.getDamageableMaterials().contains(leggings.getType())) {
									leggings.setDurability((short) 0);
									updateLeggings = true;
								}
								if (boots != null && ItemUtilities.getDamageableMaterials().contains(boots.getType())) {
									boots.setDurability((short) 0);
									updateBoots = true;
								}
								if (offhand != null && ItemUtilities.getDamageableMaterials().contains(offhand.getType())) {
									offhand.setDurability((short) 0);
									updateOffhand = true;
								}
								if (updateHelmet || updateChestplate || updateLeggings || updateBoots || updateOffhand) {
									final boolean finalUpdateHelmet = updateHelmet, finalUpdateChestplate = updateChestplate, finalUpdateLeggings = updateLeggings, finalUpdateBoots = updateBoots, finalUpdateOffhand = updateOffhand;
									damaged.getServer().getScheduler().runTask(this.plugin, new Runnable() {
										@Override
										public void run() {
											if (damaged.isOnline() && kitPlayer.hasKit() && !kitPlayer.getKit().canItemsBreak()) {
												if (finalUpdateHelmet) damaged.getInventory().setHelmet(helmet);
												if (finalUpdateChestplate)
													damaged.getInventory().setChestplate(chestplate);
												if (finalUpdateLeggings) damaged.getInventory().setLeggings(leggings);
												if (finalUpdateBoots) damaged.getInventory().setBoots(boots);
												if (finalUpdateOffhand)
													damaged.getInventory().setItemInOffHand(offhand);
											}
										}
									});
								}
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity().getWorld() != kitpvpworld) //Pls do not execute this cancer on other worlds.
			return;
		try {
			if (event.getDamager() instanceof Player) {
				final Player damager = (Player) event.getDamager();
				if (damager.getGameMode() == GameMode.SURVIVAL || damager.getGameMode() == GameMode.ADVENTURE) {
					if (Utilities.isPvPWorld(damager.getWorld())) {
						final KitPlayer damagerKitPlayer = PlayerController.getInstance().getPlayer(damager);
						if (damagerKitPlayer != null && damagerKitPlayer.hasKit() && !damagerKitPlayer.getKit().canItemsBreak()) {
							if (damager.getInventory().getItemInMainHand() != null) {
								if (ItemUtilities.getDamageableMaterials().contains(damager.getInventory().getItemInMainHand().getType())) {
									damager.getServer().getScheduler().runTask(this.plugin, new Runnable() {
										@Override
										public void run() {
											if (damager.isOnline() && damagerKitPlayer.hasKit() && damager.getInventory().getItemInMainHand() != null && ItemUtilities.getDamageableMaterials().contains(damager.getInventory().getItemInMainHand().getType())) {
												ItemStack itemInHand = damager.getInventory().getItemInMainHand();
												itemInHand.setDurability((short) 0);
												damager.getInventory().setItemInMainHand(itemInHand);
												damager.updateInventory();
											}
										}
									});
								}
							}
							if (damager.getInventory().getItemInOffHand() != null) {
								if (ItemUtilities.getDamageableMaterials().contains(damager.getInventory().getItemInOffHand().getType())) {
									damager.getServer().getScheduler().runTask(this.plugin, new Runnable() {
										@Override
										public void run() {
											if (damager.isOnline() && damagerKitPlayer.hasKit() && damager.getInventory().getItemInOffHand() != null && ItemUtilities.getDamageableMaterials().contains(damager.getInventory().getItemInOffHand().getType())) {
												ItemStack itemInHand = damager.getInventory().getItemInOffHand();
												itemInHand.setDurability((short) 0);
												damager.getInventory().setItemInOffHand(itemInHand);
												damager.updateInventory();
											}
										}
									});
								}
							}
						}
					}
				}
			}
			if (event.getEntity() instanceof Player) {
				final Player damaged = (Player) event.getEntity();
				if (damaged.getGameMode() == GameMode.SURVIVAL || damaged.getGameMode() == GameMode.ADVENTURE) {
					if (Utilities.isPvPWorld(damaged.getWorld())) {
						final KitPlayer kitPlayer = PlayerController.getInstance().getPlayer(damaged);
						if (kitPlayer != null && kitPlayer.hasKit() && !kitPlayer.getKit().canItemsBreak()) {
							boolean updateHelmet = false, updateChestplate = false, updateLeggings = false, updateBoots = false, updateOffhand = false;
							final ItemStack helmet = damaged.getInventory().getHelmet(), chestplate = damaged.getInventory().getChestplate(), leggings = damaged.getInventory().getLeggings(), boots = damaged.getInventory().getBoots(), offhand = damaged.getInventory().getItemInOffHand();
							if (helmet != null && ItemUtilities.getDamageableMaterials().contains(helmet.getType())) {
								helmet.setDurability((short) 0);
								updateHelmet = true;
							}
							if (chestplate != null && ItemUtilities.getDamageableMaterials().contains(chestplate.getType())) {
								chestplate.setDurability((short) 0);
								updateChestplate = true;
							}
							if (leggings != null && ItemUtilities.getDamageableMaterials().contains(leggings.getType())) {
								leggings.setDurability((short) 0);
								updateLeggings = true;
							}
							if (boots != null && ItemUtilities.getDamageableMaterials().contains(boots.getType())) {
								boots.setDurability((short) 0);
								updateBoots = true;
							}
							if (offhand != null && ItemUtilities.getDamageableMaterials().contains(offhand.getType())) {
								offhand.setDurability((short) 0);
								updateOffhand = true;
							}
							if (updateHelmet || updateChestplate || updateLeggings || updateBoots || updateOffhand) {
								final boolean finalUpdateHelmet = updateHelmet, finalUpdateChestplate = updateChestplate, finalUpdateLeggings = updateLeggings, finalUpdateBoots = updateBoots, finalUpdateOffhand = updateOffhand;
								damaged.getServer().getScheduler().runTask(this.plugin, new Runnable() {
									@Override
									public void run() {
										if (damaged.isOnline() && kitPlayer.hasKit() && !kitPlayer.getKit().canItemsBreak()) {
											if (finalUpdateHelmet) damaged.getInventory().setHelmet(helmet);
											if (finalUpdateChestplate) damaged.getInventory().setChestplate(chestplate);
											if (finalUpdateLeggings) damaged.getInventory().setLeggings(leggings);
											if (finalUpdateBoots) damaged.getInventory().setBoots(boots);
											if (finalUpdateOffhand) damaged.getInventory().setItemInOffHand(offhand);
										}
									}
								});
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
