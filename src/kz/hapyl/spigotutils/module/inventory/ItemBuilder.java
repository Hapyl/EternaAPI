package kz.hapyl.spigotutils.module.inventory;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import kz.hapyl.spigotutils.SpigotUtilsPlugin;
import kz.hapyl.spigotutils.module.chat.Chat;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.PatternSyntaxException;

public final class ItemBuilder {

	private static final transient String PLUGIN_ID_PATH = "ItemBuilderId";
	protected static Map<String, ItemBuilder> itemsWithEvents = new HashMap<>();

	private final ItemStack item;
	private final String id;
	private final Set<ItemAction> functions;

	private ItemMeta meta;
	private int cd;
	private Predicate<Player> predicate;
	private String error;


	public ItemBuilder(Material material) {
		this(new ItemStack(material));
	}

	public ItemBuilder(ItemStack stack) {
		this(stack.clone(), null);
	}

	public ItemBuilder(Material material, String id) {
		this(new ItemStack(material), id);
	}

	public ItemBuilder(ItemStack stack, String id) {
		this.item = stack;
		this.meta = stack.getItemMeta();
		this.id = id;
		this.functions = new HashSet<>();
	}

	public ItemBuilder predicate(boolean predicate, Consumer<ItemBuilder> action) {
		if (predicate) {
			action.accept(this);
		}
		return this;
	}

	// Static Members
	public static ItemBuilder playerHead(String texture) {
		return new ItemBuilder(Material.PLAYER_HEAD).setHeadTexture(texture);
	}

	public static ItemBuilder leatherHat(Color color) {
		return new ItemBuilder(Material.LEATHER_HELMET).setLeatherArmorColor(color);
	}

	public static ItemBuilder leatherTunic(Color color) {
		return new ItemBuilder(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(color);
	}

	public static ItemBuilder leatherPants(Color color) {
		return new ItemBuilder(Material.LEATHER_LEGGINGS).setLeatherArmorColor(color);
	}

	public static ItemBuilder leatherBoots(Color color) {
		return new ItemBuilder(Material.LEATHER_BOOTS).setLeatherArmorColor(color);
	}

	public static void clear() {
		itemsWithEvents.clear();
	}

	private static boolean isIdRegistered(String id) {
		return id != null && itemsWithEvents.containsKey(id);
	}

	@Nullable
	public static ItemStack getItemByID(String id) {
		if (itemsWithEvents.containsKey(id)) {
			return itemsWithEvents.get(id).item;
		}
		return null;
	}

	public static void broadcastRegisteredIDs() {
		Bukkit.getLogger().info("[ItemBuilder] Registered Custom Items:");
		System.out.println(itemsWithEvents.keySet());
	}

	public static Set<String> getRegisteredIDs() {
		return itemsWithEvents.keySet();
	}

	@Nullable
	public static String getItemID(ItemStack item) {
		final ItemMeta iMeta = item.getItemMeta();
		if (iMeta == null) {
			return "null";
		}
		return iMeta.getPersistentDataContainer().get(new NamespacedKey(SpigotUtilsPlugin.getPlugin(), PLUGIN_ID_PATH), PersistentDataType.STRING);
	}

	public static boolean itemHasID(ItemStack item, String id) {
		return itemHasID(item) && getItemID(item).equalsIgnoreCase(id.toLowerCase());
	}

	public static boolean itemContainsId(ItemStack item, String id) {
		return itemHasID(item) && getItemID(item).contains(id.toLowerCase());
	}

	private static List<String> splitAfter(String linePrefix, String text, int maxChars) {
		List<String> list = new ArrayList<>();
		String line = "";
		int counter = 0;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			final boolean checkLast = c == text.charAt(text.length() - 1);
			line = line.concat(c + "");
			counter++;

			if (c == '_' && text.charAt(i + 1) == '_') {
				list.add(colorize(linePrefix + line.substring(0, line.length() - 1).trim()));
				line = "";
				counter = 0;
				i++;
				continue;
			}
			if (counter >= maxChars || i == text.length() - 1) {
				if (c == ' ' || checkLast) {
					list.add(colorize(linePrefix + line.trim()));
					line = "";
					counter = 0;
				}
			}
		}

		return list;
	}

	public static List<String> splitAfter(String text, int max) {
		return splitAfter("&7", text, max);
	}

	public static List<String> splitAfter(String text, int max, String prefix) {
		return splitAfter(prefix, text, max);
	}

	private static String colorize(String s) {
		return ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', s);
	}

	// Item Value Setters
	public static void setName(ItemStack item, String name) {
		final ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(colorize(name));
		item.setItemMeta(meta);
	}

	public static void setLore(ItemStack item, String lore) {
		final ItemMeta meta = item.getItemMeta();
		meta.setLore(Collections.singletonList(lore));
		item.setItemMeta(meta);
	}

	public ItemBuilder setItemMeta(ItemMeta meta) {
		this.meta = meta;
		return this;
	}

	public ItemBuilder clone() {
		if (!this.id.isEmpty()) {
			throw new UnsupportedOperationException("Clone does not support ID's!");
		}
		return new ItemBuilder(this.item).setItemMeta(this.meta);
	}

	public ItemBuilder setMapView(MapView view) {
		if (this.item.getType() != Material.FILLED_MAP) {
			throw new IllegalArgumentException("Material must be FILLED_MAP to set Map View.");
		}
		MapMeta meta = (MapMeta)this.meta;
		meta.setMapView(view);
		return this;
	}

	public ItemBuilder setBookName(String name) {
		this.validateBookMeta();
		final BookMeta bookMeta = (BookMeta)this.meta;
		bookMeta.setDisplayName(colorize(name));
		this.item.setItemMeta(bookMeta);
		return this;
	}

	public ItemBuilder setBookAuthor(String author) {
		this.validateBookMeta();
		final BookMeta bookMeta = (BookMeta)this.meta;
		bookMeta.setAuthor(colorize(author));
		this.item.setItemMeta(bookMeta);
		return this;
	}

	public ItemBuilder setBookTitle(String title) {
		this.validateBookMeta();
		final BookMeta bookMeta = (BookMeta)this.meta;
		bookMeta.setTitle(colorize(title));
		this.item.setItemMeta(bookMeta);
		return this;
	}

	public ItemBuilder setBookPages(List<String> pages) {
		this.validateBookMeta();
		final BookMeta bookMeta = (BookMeta)this.meta;
		bookMeta.setPages(pages);
		this.item.setItemMeta(bookMeta);
		return this;
	}

	public ItemBuilder setBookPages(BaseComponent[]... base) {
		this.validateBookMeta();
		final BookMeta meta = (BookMeta)this.meta;
		meta.spigot().setPages(base);
		this.item.setItemMeta(meta);
		return this;
	}

	public ItemBuilder setBookPage(int page, BaseComponent[] base) {
		this.validateBookMeta();
		final BookMeta meta = (BookMeta)this.meta;
		meta.spigot().setPage(page, base);
		this.item.setItemMeta(meta);
		return this;
	}

	private void validateBookMeta() {
		final Material type = this.getItem().getType();
		if (type != Material.WRITTEN_BOOK) {
			throw new ItemBuilderException("Material must be WRITTEN_BOOK, not " + type);
		}
	}

	// Item Click Features
	public ItemBuilder withCooldown(int ticks) {
		withCooldown(ticks, null);
		return this;
	}

	public ItemBuilder withCooldown(int ticks, Predicate<Player> predicate) {
		withCooldown(ticks, predicate, "&cCannot use that!");
		return this;
	}

	public ItemBuilder withCooldown(int ticks, Predicate<Player> predicate, String errorMessage) {
		this.predicate = predicate;
		this.cd = ticks;
		this.error = errorMessage;
		return this;
	}

	public ItemBuilder removeClickEvent() {
		this.functions.clear();
		return this;
	}

	public ItemBuilder addClickEvent(Consumer<Player> consumer, Action... act) {
		if (act.length < 1)
			throw new IndexOutOfBoundsException("This requires at least 1 action.");
		this.functions.add(new ItemAction(consumer, act));
		return this;
	}

	public ItemBuilder addClickEvent(Consumer<Player> consumer) {
		this.addClickEvent(consumer, Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR);
		return this;
	}

	public static boolean itemHasID(ItemStack item) {
		return getItemID(item) != null;
	}

	public ItemBuilder addNbt(String path, Object value) {
		if (value instanceof String) {
			this.setPersistentData(path, PersistentDataType.STRING, (String)value);
		}
		if (value instanceof Byte) {
			this.setPersistentData(path, PersistentDataType.BYTE, (byte)value);
		}
		if (value instanceof Short) {
			this.setPersistentData(path, PersistentDataType.SHORT, (short)value);
		}
		if (value instanceof Integer) {
			this.setPersistentData(path, PersistentDataType.INTEGER, (int)value);
		}
		if (value instanceof Long) {
			this.setPersistentData(path, PersistentDataType.LONG, (long)value);
		}
		if (value instanceof Float) {
			this.setPersistentData(path, PersistentDataType.FLOAT, (float)value);
		}
		if (value instanceof Double) {
			this.setPersistentData(path, PersistentDataType.DOUBLE, (double)value);
		}
		return this;
	}

	public <T> T getNbt(String path, PersistentDataType<T, T> value) {
		return this.getPersistentData(path, value);
	}

	public ItemBuilder setAmount(int amount) {
		this.item.setAmount(Math.min(Math.max(0, amount), Byte.MAX_VALUE));
		return this;
	}

	public ItemBuilder setSmartLore(String lore, final int separator) {
		this.meta.setLore(splitAfter(lore, separator));
		return this;
	}

	public ItemBuilder setLore(List<String> lore) {
		this.meta.setLore(lore);
		return this;
	}

	public ItemBuilder setSmartLore(String lore) {
		this.meta.setLore(splitAfter(lore, 30));
		return this;
	}

	public ItemBuilder addSmartLore(String lore, final int splitAfter) {
		this.addSmartLore(lore, "&7", splitAfter);
		return this;
	}

	public ItemBuilder addSmartLore(String lore, String prefixText) {
		this.addSmartLore(lore, prefixText, 30);
		return this;
	}

	public ItemBuilder addSmartLore(String lore) {
		addSmartLore(lore, 30);
		return this;
	}

	public ItemBuilder setSmartLore(String lore, String prefixColor) {
		this.setSmartLore(lore, prefixColor, 30);
		return this;
	}

	public ItemBuilder setSmartLore(String lore, String prefixColor, int splitAfter) {
		this.meta.setLore(splitAfter(prefixColor, lore, splitAfter));
		return this;
	}

	public ItemBuilder addSmartLore(String lore, String prefixText, int splitAfter) {
		List<String> metaLore = this.meta.getLore() != null ? this.meta.getLore() : Lists.newArrayList();
		metaLore.addAll(splitAfter(prefixText, lore, splitAfter));
		this.meta.setLore(metaLore);
		return this;
	}

	public ItemBuilder setLore(int line, String lore) {
		List<String> oldLore = this.meta.getLore() == null ? Lists.newArrayList() : this.meta.getLore();
		oldLore.set(line, colorize(lore));
		this.meta.setLore(oldLore);
		return this;
	}


	public ItemBuilder setLore(String lore) {
		this.setLore(lore, "__");
		return this;
	}


	public ItemBuilder addLore(final String lore, ChatColor afterSplitColor) {
		List<String> metaLore = this.meta.getLore() != null ? this.meta.getLore() : Lists.newArrayList();
		for (String value : lore.split("__")) {
			metaLore.add(afterSplitColor + colorize(value));
		}
		this.meta.setLore(metaLore);
		return this;
	}


	public ItemBuilder addLore(final String lore) {
		return this.addLore(lore, ChatColor.GRAY);
	}

	public ItemBuilder addLore(final String lore, final Object... replacements) {
		this.addLore(Chat.format(lore, replacements));
		return this;
	}

	public ItemBuilder addLoreIf(final String lore, final boolean b) {
		this.addLoreIf(lore, b, "");
		return this;
	}

	public ItemBuilder addLoreIf(final String lore, final boolean b, final Object... replacements) {
		if (b) {
			this.addLore(lore, replacements);
		}
		return this;
	}

	public ItemBuilder addLore() {
		return this.addLore("");
	}

	public ItemBuilder setLore(final String lore, final String separator) {
		try {
			this.meta.setLore(Arrays.asList(colorize(lore).split(separator)));
		}
		catch (PatternSyntaxException ex) {
			Bukkit.getConsoleSender().sendMessage(colorize("&4[ERROR] &cChar &e" + separator + " &cused as separator for lore!"));
		}
		return this;
	}

	public ItemBuilder removeLore() {
		if (this.meta.getLore() != null)
			this.meta.setLore(null);
		return this;
	}

	public ItemBuilder removeLoreLine(int line) {
		if (this.meta.getLore() == null) {
			throw new NullPointerException("ItemMeta doesn't have any lore!");
		}
		if (line > this.meta.getLore().size()) {
			throw new IndexOutOfBoundsException("ItemMeta has only " + this.meta.getLore().size() + " lines! Given " + line);
		}
		List<String> old = this.meta.getLore();
		old.remove(line);
		this.meta.setLore(old);
		return this;

	}

	public ItemBuilder applyDefaultSettings() {
		return applyDefaultSettings(true);
	}

	public ItemBuilder applyDefaultSettings(boolean applyCurse) {
		if (applyCurse) {
			this.meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
		}
		this.meta.setUnbreakable(true);
		this.meta.addItemFlags(ItemFlag.values());
		return this;
	}

	public ItemBuilder setName(String name) {
		this.meta.setDisplayName(colorize(name));
		return this;
	}

	public ItemBuilder setName(String name, Object... repl) {
		this.setName(Chat.format(name, repl));
		return this;
	}

	public ItemBuilder addEnchant(Enchantment ench, int lvl) {
		this.meta.addEnchant(ench, lvl, true);
		return this;
	}

	public ItemBuilder setUnbreakable() {
		this.meta.setUnbreakable(true);
		return this;
	}

	public ItemBuilder setUnbreakable(boolean v) {
		this.meta.setUnbreakable(v);
		return this;
	}

	public ItemBuilder setRepairCost(int valueInLevels) {
		Repairable r = (Repairable)this.meta;
		r.setRepairCost(valueInLevels);
		return this;
	}

	public ItemBuilder setPotionMeta(PotionEffectType type, int lvl, int duration, Color color) {
		Material m = this.item.getType();
		if (m == Material.POTION || m == Material.SPLASH_POTION || m == Material.LINGERING_POTION) {
			PotionMeta meta = (PotionMeta)this.meta;
			meta.addCustomEffect(new PotionEffect(type, duration, lvl), false);
			meta.setColor(color);
			return this;
		}
		return this;
	}

	public ItemBuilder setPotionColor(Color color) {
		this.validatePotionMeta();
		final PotionMeta meta = (PotionMeta)this.meta;
		meta.setColor(color);
		this.item.setItemMeta(meta);
		return this;
	}

	private void validatePotionMeta() {
		final Material type = this.item.getType();
		switch (type) {
			case LINGERING_POTION:
			case POTION:
			case SPLASH_POTION: {
				return;
			}
			default: {
				throw new IllegalArgumentException("Material must be POTION, SPLASH_POTION or LINGERING_POTION to use this!");
			}
		}
	}

	public ItemBuilder setLeatherArmorColor(Color color) {
		final Material m = this.item.getType();
		if (m == Material.LEATHER_BOOTS || m == Material.LEATHER_CHESTPLATE || m == Material.LEATHER_LEGGINGS || m == Material.LEATHER_HELMET) {
			LeatherArmorMeta meta = (LeatherArmorMeta)this.meta;
			meta.setColor(color);
			this.item.setItemMeta(meta);
			return this;
		}
		return this;
	}

	public ItemBuilder setHeadTexture(String base64) {

		final GameProfile profile = new GameProfile(UUID.randomUUID(), "");
		profile.getProperties().put("textures", new Property("textures", base64));

		try {
			Field f = this.meta.getClass().getDeclaredField("profile");
			f.setAccessible(true);
			f.set(this.meta, profile);
			f.setAccessible(false);
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return this;
	}


	public ItemBuilder setSkullOwner(String owner) {
		if (this.item.getType() == Material.PLAYER_HEAD) {
			SkullMeta meta = (SkullMeta)this.meta;
			meta.setOwner(owner);
			return this;
		}
		return this;
	}

	public ItemBuilder setPureDamage(double damage) {
		this.addAttribute(Attribute.GENERIC_ATTACK_DAMAGE, damage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
		return this;
	}

	public ItemBuilder addAttribute(Attribute a, double amount, AttributeModifier.Operation operation, EquipmentSlot slot) {
		this.meta.addAttributeModifier(a, new AttributeModifier(UUID.randomUUID(), a.toString(), amount, operation, slot));
		return this;
	}

	public ItemBuilder hideFlag(ItemFlag... flag) {
		this.meta.addItemFlags(flag);
		return this;
	}

	public ItemBuilder showFlag(ItemFlag... flag) {
		this.meta.removeItemFlags(flag);
		return this;
	}

	public ItemBuilder showFlags() {
		this.meta.removeItemFlags(ItemFlag.values());
		return this;
	}

	public ItemBuilder hideFlags() {
		this.meta.addItemFlags(ItemFlag.values());
		return this;
	}

	public ItemBuilder clearName() {
		this.meta.setDisplayName("");
		return this;
	}

	public ItemBuilder setDurability(int dura) {
		Damageable meta = (Damageable)this.meta;
		meta.setDamage(dura);
		return this;
	}

	public ItemBuilder setType(Material icon) {
		this.item.setType(icon);
		return this;
	}

	public ItemStack toItemStack() {
		this.item.setItemMeta(this.meta);
		return this.item;
	}

	public ItemStack cleanToItemSack() {
		this.hideFlags();
		this.setName("&0");
		this.removeLore();
		this.setUnbreakable(true);
		return this.toItemStack();
	}


	public ItemStack build(boolean overrideIfExists) {

		if (this.id != null) {
			if (isIdRegistered(this.id) && !overrideIfExists) {
				sendErrorMessage("Could not build ItemBuilder! ID \"%s\" is already registered. Use \"toItemStack\" if you wish to clone it or \"build(true)\" to override existing item!", this
						.getItem()
						.getType());
				return item;
			}
			setPersistentData(PLUGIN_ID_PATH, PersistentDataType.STRING, this.id);
			itemsWithEvents.put(this.id, this);
		}


		else if (!this.functions.isEmpty()) {
			sendErrorMessage("Could not build ItemBuilder! ID is required to add click events. \"new ItemBuilder(%s, ID)\"", this.getItem()
					.getType());
			return item;
		}

		this.item.setItemMeta(this.meta);
		return item;

	}

	private void sendErrorMessage(String msg, Object... dot) {
		final String message = Chat.format(msg, dot);
		Bukkit.getLogger().log(Level.SEVERE, message);
		new ItemBuilderException(message).printStackTrace();
	}

	public ItemStack build() {
		return this.build(false);
	}


	public String getName() {
		return this.meta.getDisplayName();
	}


	@Nullable
	public List<String> getLore() {
		return this.meta.getLore();
	}


	@Nullable
	public List<String> getLore(int start, int end) {
		final List<String> hash = new ArrayList<>();
		final List<String> lore = this.getLore();
		if (lore == null || end > lore.size()) {
			Bukkit.getLogger().warning("There is either no lore or given more that there is lines.");
			return null;
		}
		for (int i = start; i < end; i++) {
			hash.add(lore.get(i));
		}
		return hash;
	}


	public int getAmount() {
		return this.item.getAmount();
	}


	public Map<Enchantment, Integer> getEnchants() {
		return this.meta.getEnchants();
	}


	public boolean isUnbreakable() {
		return this.meta.isUnbreakable();
	}

	public String getError() {
		return error;
	}

	public ItemStack getItem() {
		return item;
	}


	public int getRepairCost() {
		return ((Repairable)this.meta).getRepairCost();
	}


	public Color getLeatherColor() {
		return ((LeatherArmorMeta)this.meta).getColor();
	}


	@Nullable
	public String getHeadTexture() {
		try {
			return (String)this.meta.getClass().getDeclaredField("profile").get(this.meta);
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}


	public Set<ItemFlag> getFlags() {
		return this.meta.getItemFlags();
	}


	public Multimap<Attribute, AttributeModifier> getAttributes() {
		return this.meta.getAttributeModifiers();
	}


	public double getPureDamage() {
		double most = 0;
		for (AttributeModifier t : getAttributes().get(Attribute.GENERIC_ATTACK_DAMAGE)) {
			final double current = t.getAmount();
			most = Math.max(current, most);
		}
		return most;
	}

	public String getId() {
		return id;
	}

	public <T> ItemBuilder setPersistentData(String path, PersistentDataType<T, T> type, T value) {
		try {
			this.meta.getPersistentDataContainer().set(new NamespacedKey(SpigotUtilsPlugin.getPlugin(), path), type, value);
		}
		catch (IllegalArgumentException er) {
			Bukkit.broadcastMessage(ChatColor.RED + "An error occurred whilst trying to perform this action. Check the console!");
			throw new ItemBuilderException
					("Plugin call before plugin initiated. Make sure to register ItemBuilder BEFORE you register commands, events etc!");
		}
		return this;
	}

	public <T> boolean hasPersistentData(String path, PersistentDataType<T, T> type) {
		return this.meta.getPersistentDataContainer().has(new NamespacedKey(SpigotUtilsPlugin.getPlugin(), path), type);
	}

	public <T> T getPersistentData(String path, PersistentDataType<T, T> type) {
		return this.meta.getPersistentDataContainer().get(new NamespacedKey(SpigotUtilsPlugin.getPlugin(), path), type);
	}

	public ItemBuilder glow() {
		this.addEnchant(Enchantment.LUCK, 1);
		this.hideFlag(ItemFlag.HIDE_ENCHANTS);
		return this;
	}

	public static ItemBuilder fromItemStack(ItemStack stack) {
		return new ItemBuilder(stack);
	}

	public Set<ItemAction> getFunctions() {
		return this.functions;
	}

	public int getCd() {
		return this.cd;
	}

	public Predicate<Player> getPredicate() {
		return this.predicate;
	}

	private static class ItemBuilderException extends RuntimeException {
		private ItemBuilderException() {
			super();
		}

		private ItemBuilderException(String args) {
			super(args);
		}
	}

}