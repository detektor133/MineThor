package org.angelauramc.methodsInjectorAgent.minethor;

final class DescriptorMinecraftAdapter extends ReflectiveMinecraftAdapter {
    @Override
    public String name() {
        return value("name");
    }

    @Override
    public boolean isAvailable() {
        return DescriptorProperties.load() && super.isAvailable();
    }

    @Override
    protected String[] clientClasses() {
        return values("client.classes");
    }

    @Override
    protected String[] clientInstanceMethods() {
        return values("client.instanceMethods");
    }

    @Override
    protected String[] playerFields() {
        return values("player.fields");
    }

    @Override
    protected String[] inventoryMethods() {
        return values("inventory.methods");
    }

    @Override
    protected String[] inventoryFields() {
        return values("inventory.fields");
    }

    @Override
    protected String[] inventoryClassNames() {
        return values("inventory.classNames");
    }

    @Override
    protected String[] selectedSlotFields() {
        return values("selectedSlot.fields");
    }

    @Override
    protected String[] mainInventoryFields() {
        return values("mainInventory.fields");
    }

    @Override
    protected String[] armorInventoryFields() {
        return values("armorInventory.fields");
    }

    @Override
    protected String[] offhandInventoryFields() {
        return values("offhandInventory.fields");
    }

    @Override
    protected String[] itemStackIsEmptyMethods() {
        return values("itemStack.isEmptyMethods");
    }

    @Override
    protected String[] itemStackCountMethods() {
        return values("itemStack.countMethods");
    }

    @Override
    protected String[] itemStackDescriptionIdMethods() {
        return values("itemStack.descriptionIdMethods");
    }

    @Override
    protected String[] itemStackHoverNameMethods() {
        return values("itemStack.hoverNameMethods");
    }

    @Override
    protected String[] itemStackDamageMethods() {
        return values("itemStack.damageMethods");
    }

    @Override
    protected String[] itemStackMaxDamageMethods() {
        return values("itemStack.maxDamageMethods");
    }

    @Override
    protected String[] itemStackCopyMethods() {
        return values("itemStack.copyMethods");
    }

    @Override
    protected String[] itemStackTagMethods() {
        return values("itemStack.tagMethods");
    }

    @Override
    protected String[] itemStackFoilMethods() {
        return values("itemStack.foilMethods");
    }

    @Override
    protected String[] componentStringMethods() {
        return values("component.stringMethods");
    }

    @Override
    protected String[] xAccessors() {
        return values("x.accessors");
    }

    @Override
    protected String[] yAccessors() {
        return values("y.accessors");
    }

    @Override
    protected String[] zAccessors() {
        return values("z.accessors");
    }

    @Override
    protected String[] yawAccessors() {
        return values("yaw.accessors");
    }

    @Override
    protected String[] healthAccessors() {
        return values("health.accessors");
    }

    @Override
    protected String[] maxHealthAccessors() {
        return values("maxHealth.accessors");
    }

    @Override
    protected String[] foodAccessors() {
        return values("food.accessors");
    }

    @Override
    protected String[] armorAccessors() {
        return values("armor.accessors");
    }

    @Override
    protected String[] foodDataAccessors() {
        return values("foodData.accessors");
    }

    @Override
    protected String[] xpLevelFields() {
        return values("xpLevel.fields");
    }

    private String value(String key) {
        return DescriptorProperties.value(key);
    }

    private String[] values(String key) {
        return DescriptorProperties.values(key);
    }
}
