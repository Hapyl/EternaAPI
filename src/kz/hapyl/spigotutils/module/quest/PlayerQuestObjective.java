package kz.hapyl.spigotutils.module.quest;

import kz.hapyl.spigotutils.module.chat.Chat;
import kz.hapyl.spigotutils.module.util.BukkitUtils;
import org.bukkit.entity.Player;

/**
 * This used to store player progress to certain objective
 */
public abstract class PlayerQuestObjective extends QuestObjective {

	private final QuestProgress progress;
	private final Player        player;

	private double goalCurrent;

	protected PlayerQuestObjective(QuestProgress progress, QuestObjective objective) {
		super(objective.getQuestType(), objective.getCompletionGoal(), objective.getObjectiveName(), objective.getObjectiveShortInfo());
		this.progress = progress;
		this.player = progress.getPlayer();
		this.goalCurrent = 0;
	}

	public Player getPlayer() {
		return this.player;
	}

	public double getGoalCurrent() {
		return this.goalCurrent;
	}

	public void incrementGoal() {
		this.incrementGoal(true);
	}

	public void incrementGoal(boolean checkCompletion) {
		this.incrementGoal(1.0d, checkCompletion);
	}

	public void incrementGoal(double amount, boolean checkCompletion) {
		this.goalCurrent += amount;
		this.afterObjectiveIncrement(this.getPlayer(), amount);
		if (this.isFinished()) {
			this.afterObjectiveCompletion(this.getPlayer(), amount);
		}
		if (checkCompletion) {
			if (this.isFinished()) {
				this.progress.nextObjective();
			}
		}
	}

	public boolean isFinished() {
		return this.goalCurrent >= this.getCompletionGoal();
	}

	public String getPercentComplete() {
		// Don't show percent completion if one time action
		if (this.getCompletionGoal() <= 1.0f) {
			return "";
		}
		return Chat.format(" &7(%s%%)", BukkitUtils.decimalFormat(this.goalCurrent * 100 / this.getCompletionGoal(), "0.0"));
	}

	public void sendMessage(Type type) {

		Chat.sendMessage(player, "");

		switch (type) {

			case COMPLETE: {
				Chat.sendCenterMessage(player, "&2&lOBJECTIVE COMPLETE!");
				Chat.sendCenterMessage(player, "&a✔ " + this.getObjectiveName());
				break;
			}

			case STARTED: {
				Chat.sendCenterMessage(player, "&e&lNEW OBJECTIVE!");
				Chat.sendCenterMessage(player, "&6" + this.getObjectiveName());
				Chat.sendCenterMessage(player, "&7" + this.getObjectiveShortInfo());
				break;
			}

			// TODO : add impl for failing such as timed quests etc
			case FAILED: {
				Chat.sendCenterMessage(player, "&c&lOBJECTIVE FAILED!");
				Chat.sendCenterMessage(player, "&7It's ok! Try again.");
				break;
			}
		}

		Chat.sendMessage(player, "");


	}

	@Override
	public String toString() {
		return "PlayerQuestObjective{" +
				", type=" + this.getQuestType() +
				", goal=" + this.getCompletionGoal() +
				", done=" + this.getGoalCurrent() +
				'}';
	}

	public enum Type {

		STARTED(true),
		COMPLETE(false),
		FAILED(false);

		private final boolean b;

		Type(boolean b) {
			this.b = b;
		}

		public boolean sendAnotherLine() {
			return this.b;
		}

	}

}
