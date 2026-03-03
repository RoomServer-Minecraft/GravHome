package com.gravhome.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeState extends PersistentState {
    private final Map<UUID, Map<String, HomeData>> homes = new HashMap<>();

    public Map<String, HomeData> getPlayerHomes(UUID uuid) {
        return homes.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public void addHome(UUID uuid, String name, HomeData data) {
        getPlayerHomes(uuid).put(name, data);
        markDirty();
    }

    public boolean removeHome(UUID uuid, String name) {
        Map<String, HomeData> playerHomes = homes.get(uuid);
        if (playerHomes != null && playerHomes.remove(name) != null) {
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList playersList = new NbtList();
        for (Map.Entry<UUID, Map<String, HomeData>> entry : homes.entrySet()) {
            NbtCompound playerCompound = new NbtCompound();
            playerCompound.putUuid("uuid", entry.getKey());

            NbtCompound homesCompound = new NbtCompound();
            for (Map.Entry<String, HomeData> homeEntry : entry.getValue().entrySet()) {
                homesCompound.put(homeEntry.getKey(), homeEntry.getValue().toNbt());
            }
            playerCompound.put("homes", homesCompound);
            playersList.add(playerCompound);
        }
        nbt.put("Players", playersList);
        return nbt;
    }

    public static HomeState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        HomeState state = new HomeState();
        if (nbt.contains("Players", NbtElement.LIST_TYPE)) {
            NbtList playersList = nbt.getList("Players", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < playersList.size(); i++) {
                NbtCompound playerCompound = playersList.getCompound(i);
                if (playerCompound.contains("uuid")) {
                    UUID uuid = playerCompound.getUuid("uuid");
                    Map<String, HomeData> playerHomes = new HashMap<>();

                    if (playerCompound.contains("homes", NbtElement.COMPOUND_TYPE)) {
                        NbtCompound homesCompound = playerCompound.getCompound("homes");
                        for (String key : homesCompound.getKeys()) {
                            playerHomes.put(key, HomeData.fromNbt(homesCompound.getCompound(key)));
                        }
                    }
                    state.homes.put(uuid, playerHomes);
                }
            }
        }
        return state;
    }

    public static HomeState getServerState(MinecraftServer server) {
        PersistentState.Type<HomeState> type = new PersistentState.Type<>(
                HomeState::new,
                HomeState::fromNbt,
                null);
        HomeState state = server.getOverworld().getPersistentStateManager().getOrCreate(type, "gravhome_data");
        state.markDirty();
        return state;
    }
}
