package com.jacky8399.balancedvillagertrades.fields;

import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class WorldField extends SimpleContainerField<Villager, World> {


    private static final Map<String, Field<World, ?>> WORLD_FIELDS = Map.of(
            "name", Field.readOnlyField(String.class, World::getName),
            "environment", Field.readOnlyField(String.class, world -> world.getEnvironment().name()),
            "time", Field.readOnlyField(Integer.class, world -> (int) world.getTime()),
            "full-time", Field.readOnlyField(Integer.class, world -> (int) world.getFullTime()),
            "is-day-time", Field.readOnlyField(Boolean.class, World::isDayTime),
            "weather", Field.readOnlyField(String.class,
                    world -> world.isThundering() ? "thunder" : world.hasStorm() ? "rain" : "clear"),
            "seed", Field.readOnlyField(Integer.class, world -> (int) world.getSeed()),
            "seed-upper", Field.readOnlyField(Integer.class, world -> (int) (world.getSeed() >> 32))
    );

    WorldField() {
        super(World.class, Villager::getWorld, null, WORLD_FIELDS);
    }

}
