package net.kdt.pojavlaunch.minethor;

public class PlayerSnapshot {
    public final int x;
    public final int y;
    public final int z;
    public final int yaw;
    public final int health;
    public final int maxHealth;
    public final int food;
    public final int maxFood;
    public final int armor;
    public final int xpLevel;

    public PlayerSnapshot(
            int x,
            int y,
            int z,
            int yaw,
            int health,
            int maxHealth,
            int food,
            int maxFood,
            int armor,
            int xpLevel
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.health = health;
        this.maxHealth = maxHealth;
        this.food = food;
        this.maxFood = maxFood;
        this.armor = armor;
        this.xpLevel = xpLevel;
    }
}
