package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure;
import de.jexcellence.oneblock.manager.infrastructure.InfrastructureManager;
import de.jexcellence.oneblock.manager.infrastructure.InfrastructureTickProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IInfrastructureService {
    
    @Nullable
    IslandInfrastructure getInfrastructure(@NotNull Long islandId, @NotNull UUID playerId);
    
    @NotNull
    CompletableFuture<Optional<IslandInfrastructure>> getInfrastructureAsync(@NotNull Long islandId);
    
    @NotNull
    InfrastructureManager getManager();
    
    @NotNull
    InfrastructureTickProcessor getTickProcessor();
}
