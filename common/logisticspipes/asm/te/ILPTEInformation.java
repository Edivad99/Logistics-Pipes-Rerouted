package logisticspipes.asm.te;

import org.jspecify.annotations.Nullable;

public interface ILPTEInformation {

	@Nullable LPTileEntityObject getLPTileEntityObject();

	void setLPTileEntityObject(LPTileEntityObject object);
}
