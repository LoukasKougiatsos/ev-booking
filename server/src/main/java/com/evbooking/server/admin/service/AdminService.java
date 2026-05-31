package com.evbooking.server.admin.service;

import com.evbooking.server.admin.dto.ConnectorAdminRequest;
import com.evbooking.server.admin.dto.ConnectorAdminResponse;
import com.evbooking.server.admin.dto.StationAdminRequest;
import com.evbooking.server.admin.dto.StationAdminResponse;
import com.evbooking.server.booking.dto.BookingResponse;
import com.evbooking.server.entity.Booking;
import com.evbooking.server.entity.Connector;
import com.evbooking.server.entity.ConnectorType;
import com.evbooking.server.entity.Station;
import com.evbooking.server.repository.BookingRepository;
import com.evbooking.server.repository.ConnectorRepository;
import com.evbooking.server.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;
    private final ConnectorRepository connectorRepository;

    public AdminService(BookingRepository bookingRepository,
                        StationRepository stationRepository,
                        ConnectorRepository connectorRepository) {
        this.bookingRepository = bookingRepository;
        this.stationRepository = stationRepository;
        this.connectorRepository = connectorRepository;
    }

    public List<BookingResponse> listAllBookings() {
        return bookingRepository.findAll().stream().map(this::toBookingResponse).toList();
    }

    public List<StationAdminResponse> listStations() {
        return stationRepository.findAll().stream().map(this::toStationResponse).toList();
    }

    public StationAdminResponse createStation(StationAdminRequest request) {
        Station station = new Station();
        station.setName(request.name());
        station.setAddress(request.address());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        return toStationResponse(stationRepository.save(station));
    }

    public StationAdminResponse updateStation(Long id, StationAdminRequest request) {
        Station station = stationRepository.findById(id).orElseThrow();
        station.setName(request.name());
        station.setAddress(request.address());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        return toStationResponse(stationRepository.save(station));
    }

    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
    }

    public List<ConnectorAdminResponse> listConnectors() {
        return connectorRepository.findAllWithStation().stream().map(this::toConnectorResponse).toList();
    }

    public ConnectorAdminResponse createConnector(ConnectorAdminRequest request) {
        Station station = stationRepository.findById(request.stationId()).orElseThrow();
        Connector connector = new Connector();
        connector.setStation(station);
        connector.setConnectorType(ConnectorType.valueOf(request.connectorType().toUpperCase()));
        connector.setMaxKw(request.maxKw());
        return toConnectorResponse(connectorRepository.save(connector));
    }

    public ConnectorAdminResponse updateConnector(Long id, ConnectorAdminRequest request) {
        Connector connector = connectorRepository.findById(id).orElseThrow();
        Station station = stationRepository.findById(request.stationId()).orElseThrow();
        connector.setStation(station);
        connector.setConnectorType(ConnectorType.valueOf(request.connectorType().toUpperCase()));
        connector.setMaxKw(request.maxKw());
        return toConnectorResponse(connectorRepository.save(connector));
    }

    public void deleteConnector(Long id) {
        connectorRepository.deleteById(id);
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getConnector().getId(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus().name(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    private StationAdminResponse toStationResponse(Station station) {
        return new StationAdminResponse(
                station.getId(),
                station.getName(),
                station.getAddress(),
                station.getLatitude(),
                station.getLongitude(),
                station.getCreatedAt()
        );
    }

    private ConnectorAdminResponse toConnectorResponse(Connector connector) {
        return new ConnectorAdminResponse(
                connector.getId(),
                connector.getStation().getId(),
                connector.getStation().getName(),
                connector.getConnectorType().name(),
                connector.getMaxKw(),
                connector.getCreatedAt()
        );
    }
}
