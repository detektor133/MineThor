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
    private static final String[] MAIN_INVENTORY_FIELDS = {"items", "main", "field_7547", "f_35974_"};
    private static final String[] ARMOR_INVENTORY_FIELDS = {"armor", "armorItems", "field_7548", "f_35975_"};
    private static final String[] OFFHAND_INVENTORY_FIELDS = {"offhand", "offhandItems", "field_7544", "f_35976_"};
    private static final String[] ITEM_STACK_IS_EMPTY_METHODS = {"isEmpty", "method_7960", "m_41619_"};
    private static final String[] ITEM_STACK_COUNT_METHODS = {"getCount", "method_7947", "m_41613_"};
    private static final String[] ITEM_STACK_DESCRIPTION_ID_METHODS = {"getDescriptionId", "method_7866", "m_41786_"};
    private static final String[] ITEM_STACK_HOVER_NAME_METHODS = {"getHoverName", "getName", "method_7964", "m_41786_"};
    private static final String[] ITEM_STACK_DAMAGE_METHODS = {"getDamageValue", "getDamage", "method_7919", "m_41773_"};
    private static final String[] ITEM_STACK_MAX_DAMAGE_METHODS = {"getMaxDamage", "method_7936", "m_41776_"};
    private static final String[] ITEM_STACK_COPY_METHODS = {"copy", "method_7972", "m_41777_"};
    private static final String[] ITEM_STACK_TAG_METHODS = {"getTag", "method_7969", "m_41783_"};
    private static final String[] ITEM_STACK_FOIL_METHODS = {"hasFoil", "hasGlint", "method_7958", "m_41790_"};
    private static final String[] COMPONENT_STRING_METHODS = {"getString", "method_10851"};
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
    protected String[] mainInventoryFields() {
        return MAIN_INVENTORY_FIELDS;
    }

    @Override
    protected String[] armorInventoryFields() {
        return ARMOR_INVENTORY_FIELDS;
    }

    @Override
    protected String[] offhandInventoryFields() {
        return OFFHAND_INVENTORY_FIELDS;
    }

    @Override
    protected String[] itemStackIsEmptyMethods() {
        return ITEM_STACK_IS_EMPTY_METHODS;
    }

    @Override
    protected String[] itemStackCountMethods() {
        return ITEM_STACK_COUNT_METHODS;
    }

    @Override
    protected String[] itemStackDescriptionIdMethods() {
        return ITEM_STACK_DESCRIPTION_ID_METHODS;
    }

    @Override
    protected String[] itemStackHoverNameMethods() {
        return ITEM_STACK_HOVER_NAME_METHODS;
    }

    @Override
    protected String[] itemStackDamageMethods() {
        return ITEM_STACK_DAMAGE_METHODS;
    }

    @Override
    protected String[] itemStackMaxDamageMethods() {
        return ITEM_STACK_MAX_DAMAGE_METHODS;
    }

    @Override
    protected String[] itemStackCopyMethods() {
        return ITEM_STACK_COPY_METHODS;
    }

    @Override
    protected String[] itemStackTagMethods() {
        return ITEM_STACK_TAG_METHODS;
    }

    @Override
    protected String[] itemStackFoilMethods() {
        return ITEM_STACK_FOIL_METHODS;
    }

    @Override
    protected String[] componentStringMethods() {
        return COMPONENT_STRING_METHODS;
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
