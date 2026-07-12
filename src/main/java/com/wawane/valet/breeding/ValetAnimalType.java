package com.wawane.valet.breeding;

import java.util.List;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum ValetAnimalType {
    CHICKEN("animal.valet.chicken", "Poules", Chicken.class, List.of(
            Items.WHEAT_SEEDS,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.TORCHFLOWER_SEEDS,
            Items.PITCHER_POD
    )),
    COW("animal.valet.cow", "Vaches", Cow.class, List.of(Items.WHEAT)),
    SHEEP("animal.valet.sheep", "Moutons", Sheep.class, List.of(Items.WHEAT)),
    PIG("animal.valet.pig", "Cochons", Pig.class, List.of(Items.CARROT, Items.POTATO, Items.BEETROOT));

    private static final ValetAnimalType[] VALUES = values();

    private final String translationKey;
    private final String defaultName;
    private final Class<? extends Animal> animalClass;
    private final List<Item> feedItems;

    ValetAnimalType(String translationKey, String defaultName, Class<? extends Animal> animalClass, List<Item> feedItems) {
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

    public List<Item> feedItems() {
        return feedItems;
    }

    public Item primaryFeedItem() {
        return feedItems.get(0);
    }

    public static ValetAnimalType fromIndex(int index) {
        return index < 0 || index >= VALUES.length ? null : VALUES[index];
    }

    public static ValetAnimalType fromAnimal(Animal animal) {
        for (ValetAnimalType type : VALUES) {
            if (type.matches(animal)) {
                return type;
            }
        }
        return null;
    }
}
