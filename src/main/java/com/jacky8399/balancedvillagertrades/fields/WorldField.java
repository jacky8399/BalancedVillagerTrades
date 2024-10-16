package com.jacky8399.balancedvillagertrades.fields;

import org.bukkit.World;
import org.bukkit.entity.AbstractVillager;

import java.util.Map;

public class WorldField extends SimpleContainerField<AbstractVillager, World> {


    private static final Map<String, Field<World, ?>> WORLD_FIELDS = Map.of(
            "name", Field.readOnlyField(String.class, World::getName),
            "environment", Field.readOnlyField(String.class, world -> world.getEnvironment().name()),
            "time", Field.readOnlyField(Integer.class, world -> (int) world.getTime()),
            "full-time", Field.readOnlyField(Integer.class, world -> (int) world.getFullTime()),
            "is-day-time", Field.readOnlyField(Boolean.class, world -> world.getTime() < 12000),
            "weather", Field.readOnlyField(String.class,
                    world -> world.isThundering() ? "thunder" : world.hasStorm() ? "rain" : "clear"),
            "seed", Field.readOnlyField(Integer.class, world -> (int) world.getSeed()),
            "seed-upper", Field.readOnlyField(Integer.class, world -> (int) (world.getSeed() >> 32))
    );

    WorldField() {
        super(World.class, AbstractVillager::getWorld, null, WORLD_FIELDS);
    }

}
