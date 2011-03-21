package org.onebusaway.community_transit_gtfs.xml;

public class PttRoute {
  private String id;

  private String description;

  private String publicId;

  private String firstDirectionName;

  private String secondDirectionName;

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

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public String getFirstDirectionName() {
    return firstDirectionName;
  }

  public void setFirstDirectionName(String firstDirectionName) {
    this.firstDirectionName = firstDirectionName;
  }

  public String getSecondDirectionName() {
    return secondDirectionName;
  }

  public void setSecondDirectionName(String secondDirectionName) {
    this.secondDirectionName = secondDirectionName;
  }
}
