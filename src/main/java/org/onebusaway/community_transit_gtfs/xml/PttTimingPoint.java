package org.onebusaway.community_transit_gtfs.xml;

public class PttTimingPoint {

  private int positionInPattern;

  private String passingTime;

  public int getPositionInPattern() {
    return positionInPattern;
  }

  public void setPositionInPattern(int positionInPattern) {
    this.positionInPattern = positionInPattern;
  }

  public String getPassingTime() {
    return passingTime;
  }

  public void setPassingTime(String passingTime) {
    this.passingTime = passingTime;
  }
}
