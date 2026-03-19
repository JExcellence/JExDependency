package de.jexcellence.oneblock.database.repository;

import de.jexcellence.oneblock.database.entity.evolution.*;
import de.jexcellence.oneblock.database.entity.oneblock.*;
import de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Central manager for all JExOneblock repositories.
 * Provides factory methods and centralized access to repository instances.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
public class RepositoryManager {
    
    private final ExecutorService executorService;
    private final EntityManagerFactory entityManagerFactory;
    
    // Repository instances
    private OneblockPlayerRepository playerRepository;
    private OneblockIslandRepository islandRepository;
    private OneblockEvolutionRepository evolutionRepository;
    private EvolutionBlockRepository evolutionBlockRepository;
    private EvolutionEntityRepository evolutionEntityRepository;
    private EvolutionItemRepository evolutionItemRepository;
    private OneblockIslandMemberRepository islandMemberRepository;
    private OneblockIslandBanRepository islandBanRepository;
    private OneblockVisitorSettingsRepository visitorSettingsRepository;
    private OneblockRegionRepository regionRepository;
    private IslandInfrastructureRepository infrastructureRepository;
    
    /**
     * Creates a new repository manager with the provided dependencies.
     *
     * @param executorService      the executor service for async operations
     * @param entityManagerFactory the JPA entity manager factory
     */
    public RepositoryManager(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory) {
        this.executorService = executorService;
        this.entityManagerFactory = entityManagerFactory;
        initializeRepositories();
    }
    
    /**
     * Initializes all repository instances with proper key extractors.
     */
    private void initializeRepositories() {
        // Core entity repositories
        this.playerRepository = new OneblockPlayerRepository(
            executorService, entityManagerFactory, OneblockPlayer.class, OneblockPlayer::getUniqueId
        );
        
        this.islandRepository = new OneblockIslandRepository(
            executorService, entityManagerFactory, OneblockIsland.class, OneblockIsland::getIdentifier
        );
        
        this.evolutionRepository = new OneblockEvolutionRepository(
            executorService, entityManagerFactory, OneblockEvolution.class, OneblockEvolution::getEvolutionName
        );
        
        // Evolution content repositories
        this.evolutionBlockRepository = new EvolutionBlockRepository(
            executorService, entityManagerFactory, EvolutionBlock.class, EvolutionBlock::getId
        );
        
        this.evolutionEntityRepository = new EvolutionEntityRepository(
            executorService, entityManagerFactory, EvolutionEntity.class, EvolutionEntity::getId
        );
        
        this.evolutionItemRepository = new EvolutionItemRepository(
            executorService, entityManagerFactory, EvolutionItem.class, EvolutionItem::getId
        );
        
        // Island management repositories
        this.islandMemberRepository = new OneblockIslandMemberRepository(
            executorService, entityManagerFactory, OneblockIslandMember.class, OneblockIslandMember::getId
        );
        
        this.islandBanRepository = new OneblockIslandBanRepository(
            executorService, entityManagerFactory, OneblockIslandBan.class, OneblockIslandBan::getId
        );
        
        this.visitorSettingsRepository = new OneblockVisitorSettingsRepository(
            executorService, entityManagerFactory, OneblockVisitorSettings.class, OneblockVisitorSettings::getId
        );
        
        this.regionRepository = new OneblockRegionRepository(
            executorService, entityManagerFactory, OneblockRegion.class, OneblockRegion::getId
        );
        
        // Infrastructure repository
        this.infrastructureRepository = new IslandInfrastructureRepository(
            executorService, entityManagerFactory, IslandInfrastructure.class, IslandInfrastructure::getIslandId
        );
    }
    
    // ========== Repository Getters ==========
    
    /**
     * Gets the player repository.
     * @return the OneblockPlayerRepository instance
     */
    public @NotNull OneblockPlayerRepository getPlayerRepository() {
        return playerRepository;
    }
    
    /**
     * Gets the island repository.
     * @return the OneblockIslandRepository instance
     */
    public @NotNull OneblockIslandRepository getIslandRepository() {
        return islandRepository;
    }
    
    /**
     * Gets the evolution repository.
     * @return the OneblockEvolutionRepository instance
     */
    public @NotNull OneblockEvolutionRepository getEvolutionRepository() {
        return evolutionRepository;
    }
    
    /**
     * Gets the evolution block repository.
     * @return the EvolutionBlockRepository instance
     */
    public @NotNull EvolutionBlockRepository getEvolutionBlockRepository() {
        return evolutionBlockRepository;
    }
    
    /**
     * Gets the evolution entity repository.
     * @return the EvolutionEntityRepository instance
     */
    public @NotNull EvolutionEntityRepository getEvolutionEntityRepository() {
        return evolutionEntityRepository;
    }
    
    /**
     * Gets the evolution item repository.
     * @return the EvolutionItemRepository instance
     */
    public @NotNull EvolutionItemRepository getEvolutionItemRepository() {
        return evolutionItemRepository;
    }
    
    /**
     * Gets the island member repository.
     * @return the OneblockIslandMemberRepository instance
     */
    public @NotNull OneblockIslandMemberRepository getIslandMemberRepository() {
        return islandMemberRepository;
    }
    
    /**
     * Gets the island ban repository.
     * @return the OneblockIslandBanRepository instance
     */
    public @NotNull OneblockIslandBanRepository getIslandBanRepository() {
        return islandBanRepository;
    }
    
    /**
     * Gets the visitor settings repository.
     * @return the OneblockVisitorSettingsRepository instance
     */
    public @NotNull OneblockVisitorSettingsRepository getVisitorSettingsRepository() {
        return visitorSettingsRepository;
    }
    
    /**
     * Gets the region repository.
     * @return the OneblockRegionRepository instance
     */
    public @NotNull OneblockRegionRepository getRegionRepository() {
        return regionRepository;
    }
    
    /**
     * Gets the infrastructure repository.
     * @return the IslandInfrastructureRepository instance
     */
    public @NotNull IslandInfrastructureRepository getInfrastructureRepository() {
        return infrastructureRepository;
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Shuts down all repositories and releases resources.
     * This should be called during plugin shutdown.
     */
    public void shutdown() {
        // Repositories will be cleaned up when the executor service shuts down
        // Individual repositories don't need explicit shutdown as they delegate to the shared executor
    }
    
    /**
     * Gets statistics about all repositories.
     * @return a formatted string with repository statistics
     */
    public @NotNull String getStatistics() {
        return String.format(
            "RepositoryManager Statistics:\n" +
            "- Player Repository: %s\n" +
            "- Island Repository: %s\n" +
            "- Evolution Repository: %s\n" +
            "- Evolution Block Repository: %s\n" +
            "- Evolution Entity Repository: %s\n" +
            "- Evolution Item Repository: %s\n" +
            "- Island Member Repository: %s\n" +
            "- Island Ban Repository: %s\n" +
            "- Visitor Settings Repository: %s\n" +
            "- Region Repository: %s\n" +
            "- Infrastructure Repository: %s",
            playerRepository.getClass().getSimpleName(),
            islandRepository.getClass().getSimpleName(),
            evolutionRepository.getClass().getSimpleName(),
            evolutionBlockRepository.getClass().getSimpleName(),
            evolutionEntityRepository.getClass().getSimpleName(),
            evolutionItemRepository.getClass().getSimpleName(),
            islandMemberRepository.getClass().getSimpleName(),
            islandBanRepository.getClass().getSimpleName(),
            visitorSettingsRepository.getClass().getSimpleName(),
            regionRepository.getClass().getSimpleName(),
            infrastructureRepository.getClass().getSimpleName()
        );
    }
}