package org.angelauramc.methodsInjectorAgent.minethor;

final class MappedMinecraftAdapter extends ReflectiveMinecraftAdapter {
    private static final String[] CLIENT_CLASSES = {
            "net.minecraft.client.Minecraft",
            "net.minecraft.client.MinecraftClient"
    };
    private static final String[] CLIENT_INSTANCE_METHODS = {"getInstance", "getMinecraft"};
    private static final String[] PLAYER_FIELDS = {"player", "thePlayer", "field_1724", "f_91074_"};
    private static final String[] INVENTORY_METHODS = {"getInventory", "method_31548", "m_150109_"};
    private static final String[] INVENTORY_FIELDS = {"inventory", "field_71071_by", "f_36095_"};
    private static final String[] INVENTORY_CLASS_NAMES = {};
    private static final String[] SELECTED_SLOT_FIELDS = {"selected", "selectedSlot", "currentItem", "field_7545", "f_35977_"};
    private static final String[] X_ACCESSORS = {"getX", "method_23317", "m_20185_"};
    private static final String[] Y_ACCESSORS = {"getY", "method_23318", "m_20186_"};
    private static final String[] Z_ACCESSORS = {"getZ", "method_23321", "m_20189_"};
    private static final String[] YAW_ACCESSORS = {"getYRot", "getYaw", "method_36454", "m_146908_"};
    private static final String[] HEALTH_ACCESSORS = {"getHealth", "method_6032", "m_21223_"};
    private static final String[] MAX_HEALTH_ACCESSORS = {"getMaxHealth", "method_6063", "m_21233_"};
    private static final String[] FOOD_ACCESSORS = {"getFoodLevel", "method_7586", "m_38702_"};
    private static final String[] ARMOR_ACCESSORS = {"getArmorValue", "getArmor", "method_6096", "m_21230_"};
    private static final String[] FOOD_DATA_ACCESSORS = {"getFoodData", "getHungerManager", "method_7344", "m_36324_"};
    private static final String[] XP_LEVEL_FIELDS = {"experienceLevel", "field_7520", "f_108650_"};

    @Override
    public String name() {
        return "mapped";
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
