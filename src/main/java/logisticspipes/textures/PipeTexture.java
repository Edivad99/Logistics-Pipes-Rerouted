package logisticspipes.textures;

/**
 * The body texture of each pipe type.
 *
 * <p>A pipe names its texture through {@code getCenterTexture()}; the server sends the texture's
 * {@link #ordinal() ordinal} to the client, which looks the sprite up by it. Both sides share this
 * declaration order, so they agree on the numbers without the texture atlas.
 */
public enum PipeTexture {
	// Standalone pipes
	BASIC("basic"),
	PROVIDER("provider"),
	REQUEST("request"),
	CRAFTING("crafting"),
	SATELLITE("satellite"),
	SUPPLIER("supplier"),
	LIQUID_SUPPLIER("liquid_supplier"),
	LIQUID_SUPPLIER_MK2("liquid_supplier_mk2"),
	REQUEST_MK2("request_mk2"),
	REMOTE_ORDERER("remote_orderer"),
	INVSYSCON_CON("invsyscon_con"),
	INVSYSCON_DIS("invsyscon_dis"),
	INVSYSCON_MIS("invsyscon_mis"),
	ENTRANCE("entrance"),
	DESTINATION("destination"),
	FIREWALL("firewall"),
	// Fluid pipes
	LIQUID_BASIC("liquid_basic"),
	LIQUID_INSERTION("liquid_insertion"),
	LIQUID_PROVIDER("liquid_provider"),
	LIQUID_REQUEST("liquid_request"),
	LIQUID_EXTRACTOR("liquid_extractor"),
	LIQUID_SATELLITE("liquid_satellite"),
	LIQUID_TERMINUS("liquid_terminus"),
	// Chassis pipes
	CHASSIS_MK1("chassi/chassi_mk1"),
	CHASSIS_MK2("chassi/chassi_mk2"),
	CHASSIS_MK3("chassi/chassi_mk3"),
	CHASSIS_MK4("chassi/chassi_mk4"),
	CHASSIS_MK5("chassi/chassi_mk5"),
	// Transport
	BASIC_TRANSPORT("transport/basic");

	/** The texture under {@code blocks/pipes/new_texture/}, without the extension. */
	public final String path;

	PipeTexture(String path) {
		this.path = path;
	}
}
