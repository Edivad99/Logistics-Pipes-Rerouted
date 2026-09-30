package logisticspipes.textures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The body texture of each pipe type.
 *
 * <p>A pipe names its texture through {@code getCenterTexture()}; the server sends the texture's
 * {@link TextureType#index() index} to the client, which looks the sprite up by it. The indices are
 * assigned here, in declaration order, so both sides agree on them without the texture atlas.
 */
public class Textures {

	private static final List<TextureType> PIPE_TEXTURES = new ArrayList<>();

	// Standalone pipes
	public static final TextureType LOGISTICSPIPE_TEXTURE = register("pipes/basic");
	public static final TextureType LOGISTICSPIPE_PROVIDER_TEXTURE = register("pipes/provider");
	public static final TextureType LOGISTICSPIPE_REQUESTER_TEXTURE = register("pipes/request");
	public static final TextureType LOGISTICSPIPE_CRAFTER_TEXTURE = register("pipes/crafting");
	public static final TextureType LOGISTICSPIPE_SATELLITE_TEXTURE = register("pipes/satellite");
	public static final TextureType LOGISTICSPIPE_SUPPLIER_TEXTURE = register("pipes/supplier");
	public static final TextureType LOGISTICSPIPE_LIQUIDSUPPLIER_TEXTURE = register("pipes/liquid_supplier");
	public static final TextureType LOGISTICSPIPE_LIQUIDSUPPLIER_MK2_TEXTURE = register("pipes/liquid_supplier_mk2");
	public static final TextureType LOGISTICSPIPE_CRAFTERMK2_TEXTURE = register("pipes/crafting_mk2");
	public static final TextureType LOGISTICSPIPE_REQUESTERMK2_TEXTURE = register("pipes/request_mk2");
	public static final TextureType LOGISTICSPIPE_PROVIDERMK2_TEXTURE = register("pipes/provider_mk2");
	public static final TextureType LOGISTICSPIPE_REMOTE_ORDERER_TEXTURE = register("pipes/remote_orderer");
	public static final TextureType LOGISTICSPIPE_INVSYSCON_CON_TEXTURE = register("pipes/invsyscon_con");
	public static final TextureType LOGISTICSPIPE_INVSYSCON_DIS_TEXTURE = register("pipes/invsyscon_dis");
	public static final TextureType LOGISTICSPIPE_INVSYSCON_MIS_TEXTURE = register("pipes/invsyscon_mis");
	public static final TextureType LOGISTICSPIPE_ENTRANCE_TEXTURE = register("pipes/entrance");
	public static final TextureType LOGISTICSPIPE_DESTINATION_TEXTURE = register("pipes/destination");
	public static final TextureType LOGISTICSPIPE_CRAFTERMK3_TEXTURE = register("pipes/crafting_mk3");
	public static final TextureType LOGISTICSPIPE_FIREWALL_TEXTURE = register("pipes/firewall");
	// Fluid pipes
	public static final TextureType LOGISTICSPIPE_LIQUID_BASIC = register("pipes/liquid_basic");
	public static final TextureType LOGISTICSPIPE_LIQUID_INSERTION = register("pipes/liquid_insertion");
	public static final TextureType LOGISTICSPIPE_LIQUID_PROVIDER = register("pipes/liquid_provider");
	public static final TextureType LOGISTICSPIPE_LIQUID_REQUEST = register("pipes/liquid_request");
	public static final TextureType LOGISTICSPIPE_LIQUID_EXTRACTOR = register("pipes/liquid_extractor");
	public static final TextureType LOGISTICSPIPE_LIQUID_SATELLITE = register("pipes/liquid_satellite");
	public static final TextureType LOGISTICSPIPE_LIQUID_TERMINUS = register("pipes/liquid_terminus");
	// Chassis pipes
	public static final TextureType LOGISTICSPIPE_CHASSI1_TEXTURE = register("pipes/chassi/chassi_mk1");
	public static final TextureType LOGISTICSPIPE_CHASSI2_TEXTURE = register("pipes/chassi/chassi_mk2");
	public static final TextureType LOGISTICSPIPE_CHASSI3_TEXTURE = register("pipes/chassi/chassi_mk3");
	public static final TextureType LOGISTICSPIPE_CHASSI4_TEXTURE = register("pipes/chassi/chassi_mk4");
	public static final TextureType LOGISTICSPIPE_CHASSI5_TEXTURE = register("pipes/chassi/chassi_mk5");
	// Transport
	public static final TextureType LOGISTICSPIPE_BASIC_TRANSPORT_TEXTURE = register("pipes/transport/basic");

	public static Object LOGISTICS_SIDE_SELECTION;

	private static TextureType register(String fileName) {
		TextureType texture = new TextureType(PIPE_TEXTURES.size(), fileName);
		PIPE_TEXTURES.add(texture);
		return texture;
	}

	/**
	 * Every pipe texture, at the position of its index.
	 */
	public static List<TextureType> pipeTextures() {
		return Collections.unmodifiableList(PIPE_TEXTURES);
	}

	/**
	 * @param index    what the client looks the sprite up by
	 * @param fileName the texture under {@code blocks/pipes/new_texture/}, with a leading {@code pipes/}
	 */
	public record TextureType(int index, String fileName) {
	}
}
