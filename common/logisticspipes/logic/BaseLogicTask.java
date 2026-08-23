package logisticspipes.logic;

import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import java.util.UUID;
import lombok.Getter;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class BaseLogicTask implements ValueIOSerializable {

	//Graphical Interface
	@Getter
	protected int posX;
	@Getter
	protected int posY;
	@Getter
	protected String name = getTypeName();
	@Getter
	protected String comment = "";

	//Saving and Server/Client sync
	@Getter
	protected UUID uuid;

	public BaseLogicTask(ValueInput input) {
		posX = input.getIntOr("posX", 0);
		posY = input.getIntOr("posY", 0);
		name = input.getStringOr("name", "");
		comment = input.getStringOr("comment", "");
		uuid = UUID.fromString(input.getStringOr("uuid", ""));
	}

	public BaseLogicTask(int posX, int posY) {
		this.posX = posX;
		this.posY = posY;
		uuid = UUID.randomUUID();
	}

	@Override
	public void serialize(ValueOutput output) {
		output.putInt("posX", posX);
		output.putInt("posY", posY);
		output.putString("name", name);
		output.putString("comment", comment);
		output.putString("uuid", uuid.toString());
	}

	@Override
	public void deserialize(ValueInput input) {
	}

	public abstract int getAmountOfInput();

	public abstract int getAmountOfOutput();

	public abstract LogicParameterType getInputParameterType(int i);

	public abstract LogicParameterType getOutputParameterType(int i);

	public abstract void setInputParameter(int i, Object value);

	public abstract boolean isCalculated();

	public abstract Object getResult(int i);

	public abstract void resetState();

	public abstract String getTypeName();

	public abstract void syncTick(BlockEntity tile);
}
