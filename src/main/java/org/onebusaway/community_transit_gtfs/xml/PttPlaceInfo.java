package org.onebusaway.community_transit_gtfs.xml;

import java.util.ArrayList;
import java.util.List;

public class PttPlaceInfo {
  private String id;
  private String description;
  private String scheduleType;
  private String directionName;
  private List<PttTrip> trips = new ArrayList<PttTrip>();
  private List<PttPlaceInfoPlace> places = new ArrayList<PttPlaceInfoPlace>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getScheduleType() {
    return scheduleType;
  }

  public void setScheduleType(String scheduleType) {
    this.scheduleType = scheduleType;
  }

  public String getDirectionName() {
    return directionName;
  }

  public void setDirectionName(String directionName) {
    this.directionName = directionName;
  }

  public List<PttTrip> getTrips() {
    return trips;
  }

  public void setTrips(List<PttTrip> trips) {
    this.trips = trips;
  }

  public void addTrip(PttTrip trip) {
    trips.add(trip);
  }

  public List<PttPlaceInfoPlace> getPlaces() {
    return places;
  }

  public void setPlaces(List<PttPlaceInfoPlace> places) {
    this.places = places;
  }

  public void addPlace(PttPlaceInfoPlace place) {
    places.add(place);
  }
}
