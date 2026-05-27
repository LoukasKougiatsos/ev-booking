package com.evbooking.server.repository;

import com.evbooking.server.entity.Connector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectorRepository extends JpaRepository<Connector, Long> {
    @Query("""
            SELECT c
            FROM Connector c
            JOIN FETCH c.station
            ORDER BY c.station.id, c.id
            """)
    List<Connector> findAllWithStation();
}
