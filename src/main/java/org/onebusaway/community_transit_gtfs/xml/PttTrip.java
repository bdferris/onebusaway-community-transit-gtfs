package org.onebusaway.community_transit_gtfs.xml;

import java.util.ArrayList;
import java.util.List;

public class PttTrip {

  private String routeId;

  private String routePublicId;

  private int sequence;

  private List<PttTimingPoint> timingPoints = new ArrayList<PttTimingPoint>();

  public String getRouteId() {
    return routeId;
  }

  public void setRouteId(String routeId) {
    this.routeId = routeId;
  }

  public String getRoutePublicId() {
    return routePublicId;
  }

  public void setRoutePublicId(String routePublicId) {
    this.routePublicId = routePublicId;
  }

  public int getSequence() {
    return sequence;
  }

  public void setSequence(int sequence) {
    this.sequence = sequence;
  }

  public List<PttTimingPoint> getTimingPoints() {
    return timingPoints;
  }

  public void setTimingPoints(List<PttTimingPoint> timingPoints) {
    this.timingPoints = timingPoints;
  }

  public void addTimingPoint(PttTimingPoint timingPoint) {
    timingPoints.add(timingPoint);
  }
}
