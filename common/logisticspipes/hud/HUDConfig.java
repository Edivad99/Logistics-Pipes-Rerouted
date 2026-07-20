package logisticspipes.hud;

import logisticspipes.world.item.component.LPDataComponents;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.world.item.component.HUDComponent;
import net.minecraft.world.item.ItemStack;

public class HUDConfig implements IHUDConfig {

	private ItemStack itemStack;

	public HUDConfig(ItemStack stack) {
		stack.set(LPDataComponents.HUD, HUDComponent.DEFAULT);
		this.itemStack = stack;
	}

	private HUDComponent getComponent() {
		return this.itemStack.get(LPDataComponents.HUD);
	}

	@Override
	public boolean isChassisHUD() {
		return getComponent().HUDChassie();
	}

	@Override
	public boolean isHUDCrafting() {
		return getComponent().HUDCrafting();
	}

	@Override
	public boolean isHUDInvSysCon() {
		return getComponent().HUDInvSysCon();
	}

	@Override
	public boolean isHUDPowerLevel() {
		return getComponent().HUDPowerJunction();
	}

	@Override
	public boolean isHUDProvider() {
		return getComponent().HUDProvider();
	}

	@Override
	public boolean isHUDSatellite() {
		return getComponent().HUDSatellite();
	}

	@Override
	public void setChassisHUD(boolean flag) {
		HUDComponent currentHUDComponent = getComponent();
		this.itemStack.set(LPDataComponents.HUD, new HUDComponent(
				flag,
				currentHUDComponent.HUDCrafting(),
				currentHUDComponent.HUDInvSysCon(),
				currentHUDComponent.HUDPowerJunction(),
				currentHUDComponent.HUDProvider(),
				currentHUDComponent.HUDSatellite()
		));
	}

	@Override
	public void setHUDCrafting(boolean flag) {
		HUDComponent currentHUDComponent = getComponent();
		this.itemStack.set(LPDataComponents.HUD, new HUDComponent(
				currentHUDComponent.HUDChassie(),
				flag,
				currentHUDComponent.HUDInvSysCon(),
				currentHUDComponent.HUDPowerJunction(),
				currentHUDComponent.HUDProvider(),
				currentHUDComponent.HUDSatellite()
		));
	}

	@Override
	public void setHUDInvSysCon(boolean flag) {
		HUDComponent currentHUDComponent = getComponent();
		this.itemStack.set(LPDataComponents.HUD, new HUDComponent(
				currentHUDComponent.HUDChassie(),
				currentHUDComponent.HUDCrafting(),
				flag,
				currentHUDComponent.HUDPowerJunction(),
				currentHUDComponent.HUDProvider(),
				currentHUDComponent.HUDSatellite()
		));
	}

	@Override
	public void setHUDPowerJunction(boolean flag) {
		HUDComponent currentHUDComponent = getComponent();
		this.itemStack.set(LPDataComponents.HUD, new HUDComponent(
				currentHUDComponent.HUDChassie(),
				currentHUDComponent.HUDCrafting(),
				currentHUDComponent.HUDInvSysCon(),
				flag,
				currentHUDComponent.HUDProvider(),
				currentHUDComponent.HUDSatellite()
		));
	}

	@Override
	public void setHUDProvider(boolean flag) {
		HUDComponent currentHUDComponent = getComponent();
		this.itemStack.set(LPDataComponents.HUD, new HUDComponent(
				currentHUDComponent.HUDChassie(),
				currentHUDComponent.HUDCrafting(),
				currentHUDComponent.HUDInvSysCon(),
				currentHUDComponent.HUDPowerJunction(),
				flag,
				currentHUDComponent.HUDSatellite()
		));
	}

	@Override
	public void setHUDSatellite(boolean flag) {
		HUDComponent currentHUDComponent = getComponent();
		this.itemStack.set(LPDataComponents.HUD, new HUDComponent(
				currentHUDComponent.HUDChassie(),
				currentHUDComponent.HUDCrafting(),
				currentHUDComponent.HUDInvSysCon(),
				currentHUDComponent.HUDPowerJunction(),
				currentHUDComponent.HUDProvider(),
				flag
		));
	}
}
