package org.onebusaway.community_transit_gtfs.xml;

import java.util.ArrayList;
import java.util.List;

public class PublicTimeTable {

  private String bookingIdentifier;

  private String type;

  private String scenario;

  public List<PttRoute> routes = new ArrayList<PttRoute>();

  private List<PttPlaceInfo> placeInfos = new ArrayList<PttPlaceInfo>();

  public String getBookingIdentifier() {
    return bookingIdentifier;
  }

  public void setBookingIdentifier(String bookingIdentifier) {
    this.bookingIdentifier = bookingIdentifier;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getScenario() {
    return scenario;
  }

  public void setScenario(String scenario) {
    this.scenario = scenario;
  }

  public List<PttRoute> getRoutes() {
    return routes;
  }

  public void setRoutes(List<PttRoute> routes) {
    this.routes = routes;
  }

  public void addRoute(PttRoute route) {
    routes.add(route);
  }

  public List<PttPlaceInfo> getPlaceInfos() {
    return placeInfos;
  }

  public void setPlaceInfos(List<PttPlaceInfo> placeInfos) {
    this.placeInfos = placeInfos;
  }

  public void addPlaceInfo(PttPlaceInfo placeInfo) {
    placeInfos.add(placeInfo);
  }
}
