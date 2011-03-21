package org.onebusaway.community_transit_gtfs;

import java.io.File;

public class CommunityTransitGtfsMain {

  public static void main(String[] args) throws Exception {

    if (args.length != 3 && args.length != 4) {
      usage();
      System.exit(-1);
    }

    CommunityTransitGtfsFactory factory = new CommunityTransitGtfsFactory();
    factory.setScheduleInputPath(new File(args[0]));
    factory.setGisInputPath(new File(args[1]));
    factory.setGtfsOutputPath(new File(args[2]));
    
    if( args.length == 4)
      factory.setModificationsPath(args[3]);

    factory.run();
  }

  private static void usage() {
    System.err.println("usage: schedule_path gis_path gtfs_path [modifications]");

  }
}
