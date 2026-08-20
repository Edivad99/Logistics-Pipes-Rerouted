package logisticspipes.pipes;

import java.util.Locale;

public enum SatelliteNamingResult {
	SUCCESS, DUPLICATE_NAME, BLANK_NAME;

	@Override
	public String toString() {
		return super.toString().toLowerCase(Locale.ROOT);
	}
}
