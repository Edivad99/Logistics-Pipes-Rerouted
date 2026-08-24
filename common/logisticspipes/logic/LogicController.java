package logisticspipes.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.common.util.ValueIOSerializable;

import lombok.Getter;

import logisticspipes.utils.item.SimpleStackInventory;

public class LogicController implements ValueIOSerializable {

	/**
	 * Registry of task type names to factory functions.
	 * Concrete BaseLogicTask subclasses register themselves here so that
	 * deserialize() can reconstruct the right class from a saved type name.
	 */
	private static final Map<String, Function<ValueInput, BaseLogicTask>> TASK_TYPES = new HashMap<>();

	public static void registerTaskType(String typeName, Function<ValueInput, BaseLogicTask> factory) {
		TASK_TYPES.put(typeName, factory);
	}

	public SimpleStackInventory diskInv = new SimpleStackInventory(1, "Disk Inv", 1);

	public List<BaseLogicConnection> connections = new ArrayList<>();
	public List<BaseLogicTask> tasks = new ArrayList<>();

	private Thread oldThread = null;
	@Getter
	private boolean unresolvedTasks = false;

	public void calculate(BlockEntity tile) {
		if (oldThread != null && oldThread.isAlive()) {
			return;
		}
		for (BaseLogicTask task : tasks) {
			task.syncTick(tile);
		}
		//oldThread = new Thread() { @Override public void run() {
		for (BaseLogicConnection connection : connections) {
			if (!connection.isInvalidConnection()) {
				if (connection.getSource().getAmountOfOutput() <= connection.getSourceIndex()) {
					connection.setInvalidConnection(true);
					continue;
				}
				if (connection.getSource().getOutputParameterType(connection.getSourceIndex()) != connection.getType()) {
					connection.setInvalidConnection(true);
					continue;
				}
				if (connection.getTarget().getAmountOfInput() <= connection.getTargetIndex()) {
					connection.setInvalidConnection(true);
					continue;
				}
				if (connection.getTarget().getInputParameterType(connection.getTargetIndex()) != connection.getType()) {
					connection.setInvalidConnection(true);
					continue;
				}

			}
		}
		List<BaseLogicTask> toDos = new ArrayList<>(tasks);
		while (!toDos.isEmpty()) {
			boolean nothingDone = true;
			Iterator<BaseLogicTask> iter = toDos.iterator();
			while (iter.hasNext()) {
				BaseLogicTask task = iter.next();
				if (task.isCalculated()) {
					iter.remove();
					nothingDone = false;
				}
			}
			for (BaseLogicConnection connection : connections) {
				if (!connection.isInvalidConnection() && connection.getSource().isCalculated()) {
					connection.getTarget().setInputParameter(connection.getTargetIndex(), connection.getSource().getResult(connection.getSourceIndex()));
					nothingDone = false;
				}
			}
			if (nothingDone) {
				unresolvedTasks = true;
				return;
			}
		}
		unresolvedTasks = false;
		/*}};
		oldThread.setDaemon(true);
		oldThread.start();
		//*/
	}

	public void serialize(ValueOutput output) {
		diskInv.serialize(output, "LogicDiskInv");
		writeTasks(output);
		writeConnections(output);
	}

	public void deserialize(ValueInput input) {
		diskInv.deserialize(input, "LogicDiskInv");
		readTasks(input);
		readConnections(input);
	}

	// ── Tasks ─────────────────────────────────────────────────────────────────

	private void writeTasks(ValueOutput output) {
		ValueOutput.ValueOutputList list = output.childrenList("LogicTasks");
		for (BaseLogicTask task : tasks) {
			ValueOutput entry = list.addChild();
			task.serialize(entry);
			entry.putString("taskType", task.getTypeName());
		}
	}

	private void readTasks(ValueInput input) {
		tasks.clear();
		for (ValueInput entry : input.childrenListOrEmpty("LogicTasks")) {
			String typeName = entry.getStringOr("taskType", "");
			Function<ValueInput, BaseLogicTask> factory = TASK_TYPES.get(typeName);
			if (factory != null) {
				tasks.add(factory.apply(entry));
			}
			// Unknown task types are silently skipped; no data loss for known types.
		}
	}

	// ── Connections ───────────────────────────────────────────────────────────

	private void writeConnections(ValueOutput output) {
		ValueOutput.ValueOutputList list = output.childrenList("LogicConnections");
		for (BaseLogicConnection conn : connections) {
			ValueOutput entry = list.addChild();
			entry.putString("sourceUUID",  conn.getSource().getUuid().toString());
			entry.putInt   ("sourceIndex", conn.getSourceIndex());
			entry.putString("targetUUID",  conn.getTarget().getUuid().toString());
			entry.putInt   ("targetIndex", conn.getTargetIndex());
			entry.putString("type",        conn.getType().name());
		}
	}

	private void readConnections(ValueInput input) {
		connections.clear();
		if (tasks.isEmpty()) return; // nothing to wire

		// Index tasks by UUID for fast lookup
		Map<UUID, BaseLogicTask> byUUID = new HashMap<>();
		for (BaseLogicTask task : tasks) {
			byUUID.put(task.getUuid(), task);
		}

		for (ValueInput entry : input.childrenListOrEmpty("LogicConnections")) {
			try {
				UUID sourceUUID = UUID.fromString(entry.getStringOr("sourceUUID", ""));
				UUID targetUUID = UUID.fromString(entry.getStringOr("targetUUID", ""));
				BaseLogicTask source = byUUID.get(sourceUUID);
				BaseLogicTask target = byUUID.get(targetUUID);
				if (source == null || target == null) continue; // dangling reference

				int sourceIndex = entry.getIntOr("sourceIndex", 0);
				int targetIndex = entry.getIntOr("targetIndex", 0);
				LogicParameterType type = LogicParameterType.valueOf(entry.getStringOr("type", ""));

				connections.add(new BaseLogicConnection(source, sourceIndex, target, targetIndex, type) {});
			} catch (IllegalArgumentException ignored) {
				// Malformed UUID or unknown LogicParameterType — skip this connection.
			}
		}
	}
}
