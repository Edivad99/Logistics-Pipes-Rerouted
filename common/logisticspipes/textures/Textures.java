package logisticspipes.textures;

import org.jspecify.annotations.Nullable;


import logisticspipes.LogisticsPipes;

public class Textures {

	public static TextureType empty = new TextureType();
	public static TextureType empty_1 = new TextureType();
	public static TextureType empty_2 = new TextureType();
	public static SmallTextureType smallEmpty = new SmallTextureType();
	public static TextureType LOGISTICSPIPE_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_PROVIDER_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_REQUESTER_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CRAFTER_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_SATELLITE_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_SUPPLIER_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUIDSUPPLIER_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUIDSUPPLIER_MK2_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_ROUTED_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_NOTROUTED_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUID_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_ROUTED_POWERED_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_NOTROUTED_POWERED_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUID_POWERED_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_DIRECTION_POWERED_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_SUBPOWER_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_POWERED_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_POWERED_POWERED_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_SECURITY_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CHASSI_ROUTED_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CHASSI_NOTROUTED_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CHASSI_DIRECTION_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CHASSI1_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CHASSI2_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CHASSI3_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CHASSI4_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CHASSI5_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CRAFTERMK2_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_REQUESTERMK2_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_PROVIDERMK2_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_REMOTE_ORDERER_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_INVSYSCON_CON_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_INVSYSCON_DIS_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_INVSYSCON_MIS_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_ENTRANCE_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_DESTINATION_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_CRAFTERMK3_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_FIREWALL_TEXTURE = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUID_BASIC = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUID_INSERTION = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUID_PROVIDER = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUID_REQUEST = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUID_EXTRACTOR = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUID_SATELLITE = Textures.empty;
	public static TextureType LOGISTICSPIPE_LIQUID_TERMINUS = Textures.empty;
	public static TextureType LOGISTICSPIPE_OPAQUE_TEXTURE = Textures.empty;
	public static SmallTextureType LOGISTICSPIPE_BASIC_TRANSPORT_TEXTURE = Textures.smallEmpty;

	public static Object[] LOGISTICS_REQUEST_TABLE = new Object[0];
	public static @Nullable Object LOGISTICS_REQUEST_TABLE_NEW = null;
	public static @Nullable Object LOGISTICS_REQUEST_TABLE_NEW_ROUTED = null;
	public static @Nullable Object LOGISTICS_REQUEST_TABLE_NEW_UNROUTED = null;
	public static @Nullable Object LOGISTICS_REQUEST_TABLE_NEW_EMPTY = null;

	public static int LOGISTICSPIPE_LIQUID_CONNECTOR = 0;
	/*	public static TextureAtlasSprite LOGISTICSACTIONTRIGGERS_DISABLED;
		public static TextureAtlasSprite LOGISTICSACTIONTRIGGERS_CRAFTING_ICON;
		public static TextureAtlasSprite LOGISTICSACTIONTRIGGERS_TEXTURE_FILE;
		public static TextureAtlasSprite LOGISTICSACTIONTRIGGERS_NEEDS_POWER_ICON;
		public static TextureAtlasSprite LOGISTICSACTIONTRIGGERS_SUPPLIER_FAILED_ICON;
		public static TextureAtlasSprite[] LOGISTICS_UPGRADES_DISCONECT_ICONINDEX;
		public static TextureAtlasSprite[] LOGISTICS_UPGRADES_SNEAKY_ICONINDEX;
		public static TextureAtlasSprite[] LOGISTICS_UPGRADES_ICONINDEX;
		public static TextureAtlasSprite LOGISTICSITEMS_ITEMHUD_ICON;
		public static TextureAtlasSprite LOGISTICSITEMTEXTURE_FOR_DISK;*/
	public static Object LOGISTICS_SIDE_SELECTION;

	// Standalone pipes
	public static String LOGISTICSPIPE_TEXTURE_FILE = "pipes/basic";
	public static String LOGISTICSPIPE_PROVIDER_TEXTURE_FILE = "pipes/provider";
	public static String LOGISTICSPIPE_PROVIDERMK2_TEXTURE_FILE = "pipes/provider_mk2";
	public static String LOGISTICSPIPE_REQUESTER_TEXTURE_FILE = "pipes/request";
	public static String LOGISTICSPIPE_REQUESTERMK2_TEXTURE_FILE = "pipes/request_mk2";
	public static String LOGISTICSPIPE_CRAFTER_TEXTURE_FILE = "pipes/crafting";
	public static String LOGISTICSPIPE_CRAFTERMK2_TEXTURE_FILE = "pipes/crafting_mk2";
	public static String LOGISTICSPIPE_SATELLITE_TEXTURE_FILE = "pipes/satellite";
	public static String LOGISTICSPIPE_SUPPLIER_TEXTURE_FILE = "pipes/supplier";
	public static String LOGISTICSPIPE_LIQUIDSUPPLIER_TEXTURE_FILE = "pipes/liquid_supplier";
	public static String LOGISTICSPIPE_LIQUIDSUPPLIER_MK2_TEXTURE_FILE = "pipes/liquid_supplier_mk2";
	public static String LOGISTICSPIPE_REMOTE_ORDERER_TEXTURE_FILE = "pipes/remote_orderer";
	public static String LOGISTICSPIPE_INVSYSCON_CON_TEXTURE_FILE = "pipes/invsyscon_con";
	public static String LOGISTICSPIPE_INVSYSCON_DIS_TEXTURE_FILE = "pipes/invsyscon_dis";
	public static String LOGISTICSPIPE_INVSYSCON_MIS_TEXTURE_FILE = "pipes/invsyscon_mis";
	public static String LOGISTICSPIPE_ENTRANCE_TEXTURE_FILE = "pipes/entrance";
	public static String LOGISTICSPIPE_DESTINATION_TEXTURE_FILE = "pipes/destination";
	public static String LOGISTICSPIPE_CRAFTERMK3_TEXTURE_FILE = "pipes/crafting_mk3";
	public static String LOGISTICSPIPE_FIREWALL_TEXTURE_FILE = "pipes/firewall";
	// Fluid Pipes
	public static String LOGISTICSPIPE_LIQUID_CONNECTOR_TEXTURE_FILE = "pipes/liquid_connector";
	public static String LOGISTICSPIPE_LIQUID_BASIC_FILE = "pipes/liquid_basic";
	public static String LOGISTICSPIPE_LIQUID_INSERTION_FILE = "pipes/liquid_insertion";
	public static String LOGISTICSPIPE_LIQUID_PROVIDER_FILE = "pipes/liquid_provider";
	public static String LOGISTICSPIPE_LIQUID_REQUEST_FILE = "pipes/liquid_request";
	public static String LOGISTICSPIPE_LIQUID_EXTRACTOR_FILE = "pipes/liquid_extractor";
	public static String LOGISTICSPIPE_LIQUID_SATELLITE_FILE = "pipes/liquid_satellite";
	public static String LOGISTICSPIPE_LIQUID_TERMINUS_FILE = "pipes/liquid_terminus";
	// Status overlay
	public static String LOGISTICSPIPE_ROUTED_TEXTURE_FILE = "pipes/status_overlay/routed";
	public static String LOGISTICSPIPE_NOTROUTED_TEXTURE_FILE = "pipes/status_overlay/not_routed";
	public static String LOGISTICSPIPE_LIQUID_TEXTURE_FILE = "pipes/status_overlay/liquid_connection";
	public static String LOGISTICSPIPE_ROUTED_POWERED_TEXTURE_FILE = "pipes/status_overlay/routed_powered";
	public static String LOGISTICSPIPE_NOTROUTED_POWERED_TEXTURE_FILE = "pipes/status_overlay/not_routed_powered";
	public static String LOGISTICSPIPE_LIQUID_POWERED_TEXTURE_FILE = "pipes/status_overlay/liquid_connection_powered";
	public static String LOGISTICSPIPE_POWERED_TEXTURE_FILE = "pipes/status_overlay/powered";
	public static String LOGISTICSPIPE_POWERED_POWERED_TEXTURE_FILE = "pipes/status_overlay/powered_powered";
	public static String LOGISTICSPIPE_DIRECTION_POWERED_TEXTURE_FILE = "pipes/status_overlay/direction_powered";
	public static String LOGISTICSPIPE_SECURITY_TEXTURE_FILE = "pipes/status_overlay/security";
	public static String LOGISTICSPIPE_SUBPOWER_TEXTURE_FILE = "pipes/status_overlay/subpower";
	public static String LOGISTICSPIPE_OPAQUE_TEXTURE_FILE = "pipes/status_overlay/opaque";
	// Chassi pipes
	public static String LOGISTICSPIPE_CHASSI1_TEXTURE_FILE = "pipes/chassi/chassi_mk1";
	public static String LOGISTICSPIPE_CHASSI2_TEXTURE_FILE = "pipes/chassi/chassi_mk2";
	public static String LOGISTICSPIPE_CHASSI3_TEXTURE_FILE = "pipes/chassi/chassi_mk3";
	public static String LOGISTICSPIPE_CHASSI4_TEXTURE_FILE = "pipes/chassi/chassi_mk4";
	public static String LOGISTICSPIPE_CHASSI5_TEXTURE_FILE = "pipes/chassi/chassi_mk5";
	// Chassi status overlay
	public static String LOGISTICSPIPE_CHASSI_ROUTED_TEXTURE_FILE = "pipes/chassi/status_overlay/routed";
	public static String LOGISTICSPIPE_CHASSI_NOTROUTED_TEXTURE_FILE = "pipes/chassi/status_overlay/not_routed";
	public static String LOGISTICSPIPE_CHASSI_DIRECTION_TEXTURE_FILE = "pipes/chassi/status_overlay/direction";
	// Pipe Power Overlays
	public static String LOGISTICSPIPE_OVERLAY_POWERED_TEXTURE_FILE = "pipes/status_overlay/powered-pipe";
	public static String LOGISTICSPIPE_OVERLAY_UNPOWERED_TEXTURE_FILE = "pipes/status_overlay/un-powered-pipe";
	public static String LOGISTICSPIPE_UN_OVERLAY_TEXTURE_FILE = "pipes/status_overlay/un-overlayed";
	public static String LOGISTICSPIPE_BASIC_TRANSPORT_TEXTURE_FILE = "pipes/transport/basic";
	public static String LOGISTICS_SOLID_BLOCK = Textures.LOGISTICSPIPE_TEXTURE_FILE;

	static {
		Textures.empty.normal = 0;
		Textures.empty.powered = 0;
		Textures.empty.unpowered = 0;
	}

	static {
		Textures.empty_1.normal = 1;
		Textures.empty_1.powered = 1;
		Textures.empty_1.unpowered = 1;
	}

	static {
		Textures.empty_2.normal = 2;
		Textures.empty_2.powered = 2;
		Textures.empty_2.unpowered = 2;
	}

	static {
		Textures.smallEmpty.normal = 0;
		Textures.smallEmpty.newTexture = 0;
	}

	private int index = 0;
	private int newTextureIndex = 0;

	/**
	 * Walks the texture table, assigning every index and handing each entry to
	 * {@link TextureRegistrar}.
	 *
	 * <p>Client only, by its single caller: {@link TextureRegistrar} is registered on the client
	 * mod bus and nothing else calls this. It used to be safe on either side because the server got
	 * a no-op proxy implementation; now the calls land in a client class, so calling this from the
	 * server would fail to resolve it.
	 */
	public void registerBlockIcons() {
		//Register Empty Texture for slot 0
		TextureRegistrar.record(0, "empty");
		TextureRegistrar.record(1, "empty");
		TextureRegistrar.record(2, "empty");

		index = 3;
		newTextureIndex = 0;

		// Standalone pipes
		Textures.LOGISTICSPIPE_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_PROVIDER_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_PROVIDER_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_POWERED_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_POWERED_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_POWERED_POWERED_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_POWERED_POWERED_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_DIRECTION_POWERED_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_DIRECTION_POWERED_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_SECURITY_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_SECURITY_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_ROUTED_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_ROUTED_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_NOTROUTED_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_NOTROUTED_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_ROUTED_POWERED_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_ROUTED_POWERED_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_NOTROUTED_POWERED_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_NOTROUTED_POWERED_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_SUBPOWER_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_SUBPOWER_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_OPAQUE_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_OPAQUE_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_REQUESTER_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_REQUESTER_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_CRAFTER_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CRAFTER_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_SATELLITE_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_SATELLITE_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_SUPPLIER_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_SUPPLIER_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_LIQUIDSUPPLIER_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_LIQUIDSUPPLIER_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_LIQUIDSUPPLIER_MK2_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_LIQUIDSUPPLIER_MK2_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_CRAFTERMK2_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CRAFTERMK2_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_REQUESTERMK2_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_REQUESTERMK2_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_PROVIDERMK2_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_PROVIDERMK2_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_REMOTE_ORDERER_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_REMOTE_ORDERER_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_INVSYSCON_CON_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_INVSYSCON_CON_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_INVSYSCON_DIS_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_INVSYSCON_DIS_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_INVSYSCON_MIS_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_INVSYSCON_MIS_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_ENTRANCE_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_ENTRANCE_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_DESTINATION_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_DESTINATION_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_CRAFTERMK3_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CRAFTERMK3_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_FIREWALL_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_FIREWALL_TEXTURE_FILE);
		//Fluid
		Textures.LOGISTICSPIPE_LIQUID_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_LIQUID_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_LIQUID_POWERED_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_LIQUID_POWERED_TEXTURE_FILE, 2);
		Textures.LOGISTICSPIPE_LIQUID_CONNECTOR = registerSingleTexture(Textures.LOGISTICSPIPE_LIQUID_CONNECTOR_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_LIQUID_BASIC = registerTexture(Textures.LOGISTICSPIPE_LIQUID_BASIC_FILE);
		Textures.LOGISTICSPIPE_LIQUID_INSERTION = registerTexture(Textures.LOGISTICSPIPE_LIQUID_INSERTION_FILE);
		Textures.LOGISTICSPIPE_LIQUID_PROVIDER = registerTexture(Textures.LOGISTICSPIPE_LIQUID_PROVIDER_FILE);
		Textures.LOGISTICSPIPE_LIQUID_REQUEST = registerTexture(Textures.LOGISTICSPIPE_LIQUID_REQUEST_FILE);
		Textures.LOGISTICSPIPE_LIQUID_EXTRACTOR = registerTexture(Textures.LOGISTICSPIPE_LIQUID_EXTRACTOR_FILE);
		Textures.LOGISTICSPIPE_LIQUID_SATELLITE = registerTexture(Textures.LOGISTICSPIPE_LIQUID_SATELLITE_FILE);
		Textures.LOGISTICSPIPE_LIQUID_TERMINUS = registerTexture(Textures.LOGISTICSPIPE_LIQUID_TERMINUS_FILE);

		//Chassi
		Textures.LOGISTICSPIPE_CHASSI_ROUTED_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CHASSI_ROUTED_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_CHASSI_NOTROUTED_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CHASSI_NOTROUTED_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_CHASSI_DIRECTION_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CHASSI_DIRECTION_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_CHASSI1_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CHASSI1_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_CHASSI2_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CHASSI2_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_CHASSI3_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CHASSI3_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_CHASSI4_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CHASSI4_TEXTURE_FILE);
		Textures.LOGISTICSPIPE_CHASSI5_TEXTURE = registerTexture(Textures.LOGISTICSPIPE_CHASSI5_TEXTURE_FILE);

		//Transport
		Textures.LOGISTICSPIPE_BASIC_TRANSPORT_TEXTURE = registerSmallTexture(Textures.LOGISTICSPIPE_BASIC_TRANSPORT_TEXTURE_FILE);

		// TODO: rendering deferred — TextureAtlas.registerSprite() removed in 1.20.1;
		// sprite registration must use TextureStitchEvent.Pre or RegisterSpriteSheetIconEvent

		if (LogisticsPipes.isDEBUG()) {
			LogisticsPipes.LOG.debug("LP: pipetextures {}", index);
		}
	}

	private TextureType registerTexture(String fileName) {
		return registerTexture(fileName, 1);
	}

	/**
	 * @param fileName - name of texture
	 * @param flag     - 2 - register single texture without overlay, 1/0 register with overlay
	 */
	private TextureType registerTexture(String fileName, int flag) {
		TextureType texture = new TextureType();
		texture.normal = index++;
		texture.powered = texture.normal;
		texture.unpowered = texture.normal;
		texture.fileName = fileName;
		if (flag == 2) {
			TextureRegistrar.record(texture.normal, fileName);
		} else {
			TextureRegistrar.recordOverlay(texture.normal, fileName, Textures.LOGISTICSPIPE_UN_OVERLAY_TEXTURE_FILE);
		}
		if (flag == 1) {
			texture.powered = index++;
			texture.unpowered = index++;
			TextureRegistrar.recordOverlay(texture.powered, fileName, Textures.LOGISTICSPIPE_OVERLAY_POWERED_TEXTURE_FILE);
			TextureRegistrar.recordOverlay(texture.unpowered, fileName, Textures.LOGISTICSPIPE_OVERLAY_UNPOWERED_TEXTURE_FILE);
			if (!fileName.contains("status_overlay")) {
				texture.newTexture = newTextureIndex++;
				TextureRegistrar.recordNew(texture.newTexture, fileName);
			}
		}
		return texture;
	}

	private SmallTextureType registerSmallTexture(String fileName) {
		SmallTextureType texture = new SmallTextureType();
		texture.normal = index++;
		texture.fileName = fileName;
		TextureRegistrar.record(texture.normal, fileName);
		texture.newTexture = newTextureIndex++;
		TextureRegistrar.recordNew(texture.newTexture, fileName);
		return texture;
	}

	private int registerSingleTexture(String fileName) {
		int texture = index++;
		// flag was true, which took priority over the overlay name in the old proxy dispatch
		TextureRegistrar.record(texture, fileName);
		return texture;
	}

	public static class TextureType {

		public int normal;
		public int powered;
		public int unpowered;
		public int newTexture;
		public String fileName = "";
	}

	public static class SmallTextureType {

		public int normal;
		public int newTexture;
		public String fileName = "";
	}
}
