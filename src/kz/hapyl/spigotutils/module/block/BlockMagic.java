package kz.hapyl.spigotutils.module.block;

import io.netty.util.internal.ConcurrentSet;
import kz.hapyl.spigotutils.module.annotate.InsuredViewers;
import kz.hapyl.spigotutils.module.util.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Allows to manipulate with blocks such as changing types or sending packet changes.
 *
 * @author hapyl
 */
public class BlockMagic {

	protected static final Set<BlockMagic> allAffected = new ConcurrentSet<>();

	private final Set<Origin> blocks;

	public BlockMagic() {
		this.blocks = new ConcurrentSet<>();
		allAffected.add(this);
	}

	/**
	 * Adds a block to the hashset.
	 *
	 * @param block - Block to add.
	 * @return true if this set did not already contain the specified element.
	 */
	public boolean addBlock(Block block) {
		return this.blocks.add(new Origin(block));
	}

	/**
	 * Removes block from the hashset.
	 *
	 * @param block - Block to remove.
	 * @return true if blocks was removed.
	 */
	public boolean removeBlock(Block block) {
		for (final Origin origin : this.blocks) {
			if (origin.getBlock() == block) {
				this.blocks.remove(origin);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all the blocks in hashset.
	 *
	 * @return all the blocks in hashset.
	 */
	public Set<Block> getBlocks() {
		Set<Block> blocks = new HashSet<>();
		for (final Origin block : this.blocks) {
			blocks.add(block.getBlock());
		}
		return blocks;
	}

	/**
	 * Performs an action for each block. (Use 'sendChange' to send block change)
	 *
	 * @param consumer - Action to perform.
	 */
	public void forEach(Consumer<Block> consumer) {
		this.forEach0(consumer, true);
	}

	private void forEach0(Consumer<Block> consumer, boolean b) {
		for (final Origin block : this.blocks) {
			consumer.accept(block.getBlock());
			if (b) {
				block.markAffected();
			}
		}
	}

	/**
	 * Sends a visual change to all blocks.
	 *
	 * @param material - Material to change.
	 * @param viewers  - Viewers that will see the change, if empty or null everyone will see the change.
	 */
	@InsuredViewers
	public void sendChange(Material material, Player... viewers) {
		Validate.isTrue(material.isBlock(), "material must be block");
		final BlockData blockData = material.createBlockData();
		this.forEach(block -> {
			if (viewers == null || viewers.length == 0) {
				Bukkit.getOnlinePlayers().forEach(player -> player.sendBlockChange(block.getLocation(), blockData));
			}
			else {
				for (final Player viewer : viewers) {
					viewer.sendBlockChange(block.getLocation(), blockData);
				}
			}
		});
	}

	/**
	 * Updates the state of the blocks.
	 *
	 * @param force     - Force.
	 * @param applyPhys - Apply Physics.
	 */
	public void update(boolean force, boolean applyPhys) {
		this.forEach(block -> block.getState().update(false, applyPhys));
	}

	/**
	 * Sets the new type of the block. Keep in mind this will actually sets the type of the block, not visually for player.
	 *
	 * @param material  - Material to change to.
	 * @param applyPhys - Apply Physics.
	 */
	public void setType(Material material, boolean applyPhys) {
		this.forEach(block -> block.setType(material, applyPhys));
	}

	/**
	 * Fully restores blocks to their initial state and clears them from the hashset.
	 */
	public void reset() {
		for (final Origin block : this.blocks) {
			block.restore();
		}
		this.blocks.clear();
		allAffected.remove(this);
	}

	// This class is used to keep track of a block original states.
	private static class Origin {

		private final Block block;
		private final BlockData blockData;
		private final Material type;
		private boolean packetAffected;

		private Origin(Block block) {
			this.block = block;
			this.blockData = block.getBlockData();
			this.type = block.getType();
			this.packetAffected = false;
		}

		private void markAffected() {
			this.packetAffected = true;
		}

		private Block getBlock() {
			return block;
		}

		private void restore() {
			this.block.setType(type, false);
			this.block.setBlockData(blockData, false);
			// Update state if was affected
			if (this.packetAffected) {
				this.block.getState().update(false, false);
			}
		}

	}

	/**
	 * Resets all blocks that were affected
	 */
	public static void resetAll() {
		for (final BlockMagic blockMagic : allAffected) {
			blockMagic.reset();
		}
	}

}
