package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.Type;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;

/**
 * Persistent representation of a claimed chunk belonging to a {@link RTown}.
 * <p>
 * Each chunk stores its X/Z coordinates and a {@link Type} describing its role
 * inside the town (e.g., default). A chunk belongs to a single town.
 */
@Entity
@Table(name = "r_chunks")
@SuppressWarnings({
        "unused",
        "DefaultAnnotationParam",
        "JpaDataSourceORMInspection"
})
public class RChunk extends BaseEntity {

    /** Owning {@link RTown}. Matches {@code mappedBy = "town"} in {@link RTown}. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "town_id", nullable = false)
    private RTown town;

    /** Chunk X coordinate. */
    @Column(name = "x_loc", unique = false, nullable = false)
    private int x_loc;

    /** Chunk Z coordinate. */
    @Column(name = "z_loc", unique = false, nullable = false)
    private int z_loc;

    /** Semantic chunk type for town systems. */
    @Column(name = "type", unique = false, nullable = false)
    private Type type;

    /** Required by Hibernate. */
    protected RChunk() {}

    /**
     * Construct a new town chunk.
     *
     * @param town   owning town
     * @param x_loc  chunk X coordinate
     * @param z_loc  chunk Z coordinate
     * @param type   type marker
     */
    public RChunk(RTown town, int x_loc, int z_loc, Type type) {
        this.town = town;
        this.x_loc = x_loc;
        this.z_loc = z_loc;
        this.type = type;
    }

    public int getX_loc() { return this.x_loc; }

    public int getZ_loc() { return this.z_loc; }

    public Type getType() { return this.type; }

}