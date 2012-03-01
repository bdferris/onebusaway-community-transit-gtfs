/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
