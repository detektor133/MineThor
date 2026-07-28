package org.angelauramc.methodsInjectorAgent.minethor;

final class Obfuscated1201MinecraftAdapter extends ReflectiveMinecraftAdapter {
    private static final String[] CLIENT_CLASSES = {"enn"};
    private static final String[] CLIENT_INSTANCE_METHODS = {"N"};
    private static final String[] PLAYER_FIELDS = {"t"};
    private static final String[] INVENTORY_METHODS = {};
    private static final String[] INVENTORY_FIELDS = {};
    private static final String[] INVENTORY_CLASS_NAMES = {"byn"};
    private static final String[] SELECTED_SLOT_FIELDS = {"l"};
    private static final String[] X_ACCESSORS = {"J"};
    private static final String[] Y_ACCESSORS = {"K"};
    private static final String[] Z_ACCESSORS = {"L"};
    private static final String[] YAW_ACCESSORS = {"Y"};
    private static final String[] HEALTH_ACCESSORS = {"m_21223_"};
    private static final String[] MAX_HEALTH_ACCESSORS = {"m_21233_"};
    private static final String[] FOOD_ACCESSORS = {};
    private static final String[] ARMOR_ACCESSORS = {"m_21230_"};
    private static final String[] FOOD_DATA_ACCESSORS = {};
    private static final String[] XP_LEVEL_FIELDS = {};

    @Override
    public String name() {
        return "obfuscated-1.20.1";
    }

    @Override
    protected String[] clientClasses() {
        return CLIENT_CLASSES;
    }

    @Override
    protected String[] clientInstanceMethods() {
        return CLIENT_INSTANCE_METHODS;
    }

    @Override
    protected String[] playerFields() {
        return PLAYER_FIELDS;
    }

    @Override
    protected String[] inventoryMethods() {
        return INVENTORY_METHODS;
    }

    @Override
    protected String[] inventoryFields() {
        return INVENTORY_FIELDS;
    }

    @Override
    protected String[] inventoryClassNames() {
        return INVENTORY_CLASS_NAMES;
    }

    @Override
    protected String[] selectedSlotFields() {
        return SELECTED_SLOT_FIELDS;
    }

    @Override
    protected String[] xAccessors() {
        return X_ACCESSORS;
    }

    @Override
    protected String[] yAccessors() {
        return Y_ACCESSORS;
    }

    @Override
    protected String[] zAccessors() {
        return Z_ACCESSORS;
    }

    @Override
    protected String[] yawAccessors() {
        return YAW_ACCESSORS;
    }

    @Override
    protected String[] healthAccessors() {
        return HEALTH_ACCESSORS;
    }

    @Override
    protected String[] maxHealthAccessors() {
        return MAX_HEALTH_ACCESSORS;
    }

    @Override
    protected String[] foodAccessors() {
        return FOOD_ACCESSORS;
    }

    @Override
    protected String[] armorAccessors() {
        return ARMOR_ACCESSORS;
    }

    @Override
    protected String[] foodDataAccessors() {
        return FOOD_DATA_ACCESSORS;
    }

    @Override
    protected String[] xpLevelFields() {
        return XP_LEVEL_FIELDS;
    }
}
