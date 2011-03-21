package org.onebusaway.community_transit_gtfs;

public class RouteStopSequenceItem {

  private long sequenceArc;

  private long sequenceArcId;

  private long sequence;

  private double length;

  private String route;

  private String routeDirection;

  private String routeDirectionAlternate;

  private String schedule;

  private String routeVariation;

  private long stopId;

  private String timePoint;

  private Object geometry;

  public long getSequenceArc() {
    return sequenceArc;
  }

  public void setSequenceArc(long sequenceArc) {
    this.sequenceArc = sequenceArc;
  }

  public long getSequenceArcId() {
    return sequenceArcId;
  }

  public void setSequenceArcId(long sequenceArcId) {
    this.sequenceArcId = sequenceArcId;
  }

  public long getSequence() {
    return sequence;
  }

  public void setSequence(long sequence) {
    this.sequence = sequence;
  }

  public double getLength() {
    return length;
  }

  public void setLength(double length) {
    this.length = length;
  }

  public String getRoute() {
    return route;
  }

  public void setRoute(String route) {
    this.route = route;
  }

  public String getRouteDirection() {
    return routeDirection;
  }

  public void setRouteDirection(String routeDirection) {
    this.routeDirection = routeDirection;
  }

  public String getRouteDirectionAlternate() {
    return routeDirectionAlternate;
  }

  public void setRouteDirectionAlternate(String routeDirectionAlternate) {
    this.routeDirectionAlternate = routeDirectionAlternate;
  }

  public String getSchedule() {
    return schedule;
  }

  public void setSchedule(String schedule) {
    this.schedule = schedule;
  }

  public String getRouteVariation() {
    return routeVariation;
  }

  public void setRouteVariation(String routeVariation) {
    this.routeVariation = routeVariation;
  }

  public long getStopId() {
    return stopId;
  }

  public void setStopId(long stopId) {
    this.stopId = stopId;
  }

  public String getTimePoint() {
    return timePoint;
  }

  public void setTimePoint(String timePoint) {
    this.timePoint = timePoint;
  }

  public Object getGeometry() {
    return geometry;
  }

  public void setGeometry(Object geometry) {
    this.geometry = geometry;
  }

  @Override
  public String toString() {
    return length + "\t" + sequence + "\t" + stopId + "\t" + timePoint;
  }

}
