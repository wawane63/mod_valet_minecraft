package com.wawane.valet.breeding;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum ValetAnimalType {
    CHICKEN("animal.valet.chicken", "Poules", Chicken.class, new Item[]{Items.WHEAT_SEEDS}),
    COW("animal.valet.cow", "Vaches", Cow.class, new Item[]{Items.WHEAT}),
    SHEEP("animal.valet.sheep", "Moutons", Sheep.class, new Item[]{Items.WHEAT}),
    PIG("animal.valet.pig", "Cochons", Pig.class, new Item[]{Items.CARROT});

    private final String translationKey;
    private final String defaultName;
    private final Class<? extends Animal> animalClass;
    private final Item[] feedItems;

    ValetAnimalType(String translationKey, String defaultName, Class<? extends Animal> animalClass, Item[] feedItems) {
        this.translationKey = translationKey;
        this.defaultName = defaultName;
        this.animalClass = animalClass;
        this.feedItems = feedItems;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String defaultAreaName(int index) {
        return "Enclos " + defaultName.toLowerCase() + " " + index;
    }

    public boolean matches(Animal animal) {
        return animalClass.isInstance(animal);
    }

    public Item[] feedItems() {
        return feedItems.clone();
    }

    public Item primaryFeedItem() {
        return feedItems[0];
    }

    public static ValetAnimalType fromIndex(int index) {
        ValetAnimalType[] values = values();
        return index < 0 || index >= values.length ? null : values[index];
    }

    public static ValetAnimalType fromAnimal(Animal animal) {
        for (ValetAnimalType type : values()) {
            if (type.matches(animal)) {
                return type;
            }
        }
        return null;
    }
}
