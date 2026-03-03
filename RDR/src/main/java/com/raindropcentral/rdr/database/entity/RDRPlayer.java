package com.raindropcentral.rdr.database.entity;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "rdr_players")
@SuppressWarnings({
	"FieldCanBeLocal",
	"unused",
	"JpaDataSourceORMInspection"
})
public class RDRPlayer extends BaseEntity {
	
	@Column(name = "player_uuid", unique = true, nullable = false)
	@Convert(converter = UUIDConverter.class)
	private UUID player_uuid;
	
	public RDRPlayer(UUID player_uuid) {
		this.player_uuid = player_uuid;
	}
	
	public RDRPlayer() {}
	
	public UUID getIdentifier() {
		return this.player_uuid;
	}
}
