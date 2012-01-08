package org.onebusaway.community_transit_gtfs;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.digester.Digester;
import org.geotools.feature.FeatureCollection;
import org.onebusaway.community_transit_gtfs.xml.PttPlaceInfo;
import org.onebusaway.community_transit_gtfs.xml.PttPlaceInfoPlace;
import org.onebusaway.community_transit_gtfs.xml.PttRoute;
import org.onebusaway.community_transit_gtfs.xml.PttTimingPoint;
import org.onebusaway.community_transit_gtfs.xml.PttTrip;
import org.onebusaway.community_transit_gtfs.xml.PublicTimeTable;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.serialization.GtfsWriter;
import org.onebusaway.gtfs_transformer.GtfsTransformer;
import org.onebusaway.gtfs_transformer.factory.TransformFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

public class CommunityTransitGtfsFactory {

  private static Logger _log = LoggerFactory.getLogger(CommunityTransitGtfsFactory.class);

  private static Pattern _routeVariationA = Pattern.compile("\\b([a-z]{2})\\b");

  private static Pattern _routeVariationB = Pattern.compile("\\b([a-z]{1})/([a-z]{1})\\b");

  private static Pattern _routeVariationC = Pattern.compile("\\b\\d+([a-z]{2})\\b");

  private static DateFormat _dateParse = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSS");

  private File _gisInputPath;

  private File _scheduleInputPath;

  private File _gtfsOutputPath;

  private String _modificationsPath;

  /**
   * From APTA's set of agency ids
   */
  private String _agencyId = "29";

  private GtfsDaoImpl _dao = new GtfsDaoImpl();

  private Agency _agency;

  private Map<String, RouteStopSequence> _stopSequences = new HashMap<String, RouteStopSequence>();

  private Map<AgencyAndId, String> _serviceIdAndScheduleType = new HashMap<AgencyAndId, String>();

  private Date _midnight;

  public void setGisInputPath(File gisInputPath) {
    _gisInputPath = gisInputPath;
  }

  public void setScheduleInputPath(File scheduleInputPath) {
    _scheduleInputPath = scheduleInputPath;
  }

  public void setGtfsOutputPath(File gtfsOutputPath) {
    _gtfsOutputPath = gtfsOutputPath;
  }

  public void setModificationsPath(String modificationsPath) {
    _modificationsPath = modificationsPath;
  }

  public void run() throws Exception {

    processAgency();
    processStops();
    processRoutesStopSequences();
    processShapes();
    processSchedules();
    processCalendars();

    GtfsWriter writer = new GtfsWriter();
    writer.setOutputLocation(_gtfsOutputPath);
    writer.run(_dao);
    writer.close();

    // Release the dao
    _dao = null;

    applyModifications();
  }

  private void processAgency() {
    _agency = new Agency();
    _agency.setId(_agencyId);
    _agency.setLang("en");
    _agency.setName("Community Transit");
    _agency.setPhone("(800) 562-1375");
    _agency.setTimezone("America/Los_Angeles");
    _agency.setUrl("http://www.communitytransit.org/");
    _dao.saveEntity(_agency);
  }

  private void processStops() throws Exception {

    File stopsShapeFile = new File(_gisInputPath, "ctstops.shp");

    FeatureCollection<SimpleFeatureType, SimpleFeature> features = ShapefileLibrary.loadShapeFile(stopsShapeFile);

    Iterator<SimpleFeature> it = features.iterator();

    while (it.hasNext()) {

      SimpleFeature feature = it.next();
      Long stopId = (Long) feature.getProperty("STOP_ID").getValue();
      String primaryName = (String) feature.getProperty("PRIMARY").getValue();
      String crossName = (String) feature.getProperty("CROSS").getValue();
      Point point = (Point) feature.getDefaultGeometry();

      String stopName = computeStopName(primaryName, crossName);

      Stop stop = new Stop();
      stop.setId(id(stopId.toString()));
      stop.setName(stopName);
      stop.setLat(point.getY());
      stop.setLon(point.getX());
      _dao.saveEntity(stop);
    }

    features.close(it);
  }

  private String computeStopName(String primaryName, String crossName) {
    String stopName = primaryName + " & " + crossName;
    stopName = stopName.replaceAll(" & Bay", " - Bay");
    return stopName;
  }

  private void processRoutesStopSequences() throws Exception {

    // File routesShapeFile = new File(_gisInputPath, "ctroutes.shp");
    File routesShapeFile = new File(_gisInputPath, "ctrteseq.shp");

    FeatureCollection<SimpleFeatureType, SimpleFeature> features = ShapefileLibrary.loadShapeFile(routesShapeFile);

    Iterator<SimpleFeature> it = features.iterator();

    while (it.hasNext()) {
      SimpleFeature feature = it.next();

      String route = (String) feature.getProperty("ROUTE").getValue();
      String routeVariation = (String) feature.getProperty("RT_VAR").getValue();
      String scheduleType = (String) feature.getProperty("SCHEDULE").getValue();

      String id = constructSequenceId(route, routeVariation, scheduleType);

      RouteStopSequence sequence = _stopSequences.get(id);
      if (sequence == null) {
        sequence = new RouteStopSequence();
        _stopSequences.put(id, sequence);
      }

      RouteStopSequenceItem item = new RouteStopSequenceItem();
      item.setSequenceArc((Long) feature.getProperty("SEQARC_").getValue());
      item.setSequenceArcId((Long) feature.getProperty("SEQARC_ID").getValue());
      item.setSequence((Long) feature.getProperty("SEQARC_ID").getValue());
      item.setLength((Double) feature.getProperty("LENGTH").getValue());
      item.setRoute(route);
      item.setRouteDirection((String) feature.getProperty("RT_DIR").getValue());
      item.setRouteDirectionAlternate((String) feature.getProperty("ROUTE_DIR").getValue());
      item.setSchedule((String) feature.getProperty("SCHEDULE").getValue());
      item.setStopId((Long) feature.getProperty("STOP_ID").getValue());
      item.setTimePoint((String) feature.getProperty("TIME_PT").getValue());
      item.setGeometry(feature.getDefaultGeometry());
      sequence.add(item);
    }
    features.close(it);
  }

  private void processShapes() {

    for (Map.Entry<String, RouteStopSequence> entry : _stopSequences.entrySet()) {

      String rawId = entry.getKey();
      RouteStopSequence stopSequence = entry.getValue();

      AgencyAndId shapeId = id(rawId);
      int sequence = 0;

      for (RouteStopSequenceItem item : stopSequence) {
        MultiLineString mls = (MultiLineString) item.getGeometry();
        for (int i = 0; i < mls.getNumGeometries(); i++) {
          LineString ls = (LineString) mls.getGeometryN(i);
          for (int j = 0; j < ls.getNumPoints(); j++) {
            Coordinate c = ls.getCoordinateN(j);
            ShapePoint shapePoint = new ShapePoint();
            shapePoint.setShapeId(shapeId);
            shapePoint.setLat(c.y);
            shapePoint.setLon(c.x);
            shapePoint.setSequence(sequence);
            _dao.saveEntity(shapePoint);
            sequence++;
          }
        }
      }
    }
  }

  private void processSchedules() throws IOException, SAXException,
      ParseException {

    _midnight = _dateParse.parse("2000-01-01T00:00:00.000");

    List<PublicTimeTable> timetables = processScheduleDirectory(
        _scheduleInputPath, new ArrayList<PublicTimeTable>());

    for (PublicTimeTable timetable : timetables) {
      int directionIndex = 0;
      for (PttPlaceInfo placeInfo : timetable.getPlaceInfos()) {

        Map<String, Integer> timepointPositions = getTimepointPositions(placeInfo);

        for (PttTrip pttTrip : placeInfo.getTrips()) {

          String tripIdRaw = timetable.getBookingIdentifier() + "-"
              + placeInfo.getScheduleType() + "-" + placeInfo.getId() + "-"
              + pttTrip.getSequence();

          AgencyAndId tripId = id(tripIdRaw);

          Route route = getRoute(timetable, placeInfo, pttTrip);
          AgencyAndId serviceId = getServiceId(timetable, placeInfo);

          String routeVariation = getRouteVariationForPlaceInfo(placeInfo);

          String stopSequenceId = constructSequenceId(pttTrip.getRouteId(),
              routeVariation, placeInfo.getScheduleType());
          AgencyAndId shapeId = id(stopSequenceId);
          RouteStopSequence stopSequence = _stopSequences.get(stopSequenceId);

          if (stopSequence == null) {
            _log.info("unknown stop sequence: " + stopSequenceId);
            continue;
          }

          Trip trip = new Trip();
          trip.setId(tripId);
          trip.setDirectionId(Integer.toString(directionIndex));
          trip.setRoute(route);
          trip.setServiceId(serviceId);
          trip.setShapeId(shapeId);
          trip.setTripHeadsign(placeInfo.getDirectionName());

          _dao.saveEntity(trip);

          processStopTimesForTrip(timepointPositions, pttTrip, tripIdRaw,
              stopSequence, trip);

        }
      }
      directionIndex++;
    }
  }

  private void processStopTimesForTrip(Map<String, Integer> timepointPositions,
      PttTrip pttTrip, String tripIdRaw, RouteStopSequence stopSequence,
      Trip trip) throws ParseException {

    SortedMap<Integer, Integer> arrivalTimesByTimepointPosition = computeTimepointPositionToScheduleTimep(pttTrip);

    if (arrivalTimesByTimepointPosition.size() < 2) {
      _log.warn("less than two timepoints specified for trip: id="
          + trip.getId());
      return;
    }

    int firstTimepointPosition = arrivalTimesByTimepointPosition.firstKey();
    int lastTimepointPosition = arrivalTimesByTimepointPosition.lastKey();

    int firstStopIndex = Integer.MAX_VALUE;
    int lastStopIndex = Integer.MIN_VALUE;

    /**
     * Find the bounds on the set of stops that have stop times defined
     */
    List<RouteStopSequenceItem> items = stopSequence.getItems();

    for (int index = 0; index < items.size(); index++) {
      RouteStopSequenceItem item = items.get(index);
      Integer time = getScheduledTimeForTimepoint(item, timepointPositions,
          arrivalTimesByTimepointPosition);

      if (time != null) {
        firstStopIndex = Math.min(firstStopIndex, index);
        lastStopIndex = Math.max(lastStopIndex, index);
      }
    }

    StopTime first = null;
    StopTime last = null;

    for (int index = firstStopIndex; index < lastStopIndex + 1; index++) {
      RouteStopSequenceItem item = items.get(index);

      Integer time = getScheduledTimeForTimepoint(item, timepointPositions,
          arrivalTimesByTimepointPosition);
      Stop stop = _dao.getStopForId(id(Long.toString(item.getStopId())));

      StopTime stopTime = new StopTime();
      stopTime.setStop(stop);
      stopTime.setStopSequence(index - firstStopIndex);
      stopTime.setTrip(trip);

      if (time != null) {
        stopTime.setArrivalTime(time);
        stopTime.setDepartureTime(time);
      }

      _dao.saveEntity(stopTime);

      if (first == null)
        first = stopTime;
      last = stopTime;
    }

    if (!first.isDepartureTimeSet()) {
      _log.warn("departure time for first StopTime is not set: stop="
          + first.getStop().getId() + " trip=" + tripIdRaw + " firstPosition="
          + firstTimepointPosition + " lastPosition=" + lastTimepointPosition);
      for (RouteStopSequenceItem item : stopSequence)
        _log.warn("  stop=" + item.getStopId() + " timepoint="
            + item.getTimePoint() + " pos="
            + timepointPositions.get(item.getTimePoint()));
    }

    if (!last.isArrivalTimeSet()) {
      _log.warn("arrival time for last StopTime is not set: stop="
          + last.getStop().getId() + " trip=" + tripIdRaw + " firstPosition="
          + firstTimepointPosition + " lastPosition=" + lastTimepointPosition);
      for (RouteStopSequenceItem item : stopSequence)
        _log.warn("  stop=" + item.getStopId() + " timepoint="
            + item.getTimePoint() + " pos="
            + timepointPositions.get(item.getTimePoint()));
    }
  }

  private Integer getScheduledTimeForTimepoint(RouteStopSequenceItem item,
      Map<String, Integer> timepointPositions,
      SortedMap<Integer, Integer> arrivalTimesByTimepointPosition) {

    String timepoint = item.getTimePoint();

    if (timepoint == null || timepoint.length() == 0)
      return null;

    /**
     * There seem to be plenty of timepoint ids mentioned in the GIS route shape
     * data that aren't in the schedule files. Just silently ignore.
     */
    Integer position = timepointPositions.get(timepoint);
    if (position == null)
      return null;

    return arrivalTimesByTimepointPosition.get(position);
  }

  private List<PublicTimeTable> processScheduleDirectory(File path,
      List<PublicTimeTable> results) throws IOException, SAXException {

    if (path.isFile() && path.getName().endsWith(".xml")) {
      PublicTimeTable timetable = processSchedule(path);
      results.add(timetable);
    } else if (path.isDirectory()) {
      for (File file : path.listFiles())
        processScheduleDirectory(file, results);
    }
    return results;
  }

  private PublicTimeTable processSchedule(File path) throws IOException,
      SAXException {

    Digester digester = new Digester();

    digester.addObjectCreate("PublicTimeTable", PublicTimeTable.class);
    digester.addBeanPropertySetter(
        "PublicTimeTable/VehicleSchedule/BookingIdentifier",
        "bookingIdentifier");
    digester.addBeanPropertySetter("PublicTimeTable/VehicleSchedule/Type",
        "type");
    digester.addBeanPropertySetter("PublicTimeTable/VehicleSchedule/Scenario",
        "scenario");

    digester.addObjectCreate("PublicTimeTable/Route", PttRoute.class);
    digester.addBeanPropertySetter("PublicTimeTable/Route/Identifier", "id");
    digester.addBeanPropertySetter("PublicTimeTable/Route/Description",
        "description");
    digester.addBeanPropertySetter("PublicTimeTable/Route/PublicIdentifier",
        "publicId");
    digester.addBeanPropertySetter("PublicTimeTable/Route/FirstDirectionName",
        "firstDirectionName");
    digester.addBeanPropertySetter("PublicTimeTable/Route/SecondDirectionName",
        "secondDirectionName");
    digester.addSetNext("PublicTimeTable/Route", "addRoute");

    digester.addObjectCreate("PublicTimeTable/PttPlaceInfo", PttPlaceInfo.class);
    digester.addBeanPropertySetter("PublicTimeTable/PttPlaceInfo/Identifier",
        "id");
    digester.addBeanPropertySetter("PublicTimeTable/PttPlaceInfo/Description",
        "description");
    digester.addBeanPropertySetter("PublicTimeTable/PttPlaceInfo/ScheduleType",
        "scheduleType");
    digester.addBeanPropertySetter(
        "PublicTimeTable/PttPlaceInfo/DirectionName", "directionName");
    digester.addSetNext("PublicTimeTable/PttPlaceInfo", "addPlaceInfo");

    digester.addObjectCreate("PublicTimeTable/PttPlaceInfo/Trip", PttTrip.class);
    digester.addBeanPropertySetter(
        "PublicTimeTable/PttPlaceInfo/Trip/RouteIdentifier", "routeId");
    digester.addBeanPropertySetter(
        "PublicTimeTable/PttPlaceInfo/Trip/SequenceNo", "sequence");
    digester.addBeanPropertySetter(
        "PublicTimeTable/PttPlaceInfo/Trip/trp_route_public", "routePublicId");
    digester.addSetNext("PublicTimeTable/PttPlaceInfo/Trip", "addTrip");

    digester.addObjectCreate("PublicTimeTable/PttPlaceInfo/Trip/TimingPoint",
        PttTimingPoint.class);
    digester.addBeanPropertySetter(
        "PublicTimeTable/PttPlaceInfo/Trip/TimingPoint/PositionInPattern",
        "positionInPattern");
    digester.addBeanPropertySetter(
        "PublicTimeTable/PttPlaceInfo/Trip/TimingPoint/PassingTime",
        "passingTime");
    digester.addSetNext("PublicTimeTable/PttPlaceInfo/Trip/TimingPoint",
        "addTimingPoint");

    digester.addObjectCreate("PublicTimeTable/PttPlaceInfo/PttPlaceInfoPlace",
        PttPlaceInfoPlace.class);
    digester.addBeanPropertySetter(
        "PublicTimeTable/PttPlaceInfo/PttPlaceInfoPlace/PositionInPattern",
        "positionInPattern");
    digester.addBeanPropertySetter(
        "PublicTimeTable/PttPlaceInfo/PttPlaceInfoPlace/PlaceIdentifier", "id");
    digester.addSetNext("PublicTimeTable/PttPlaceInfo/PttPlaceInfoPlace",
        "addPlace");

    return (PublicTimeTable) digester.parse(path);
  }

  private void processCalendars() {

    for (Map.Entry<AgencyAndId, String> entry : _serviceIdAndScheduleType.entrySet()) {
      AgencyAndId id = entry.getKey();
      String scheduleType = entry.getValue();

      ServiceCalendar c = new ServiceCalendar();
      c.setServiceId(id);
      if (scheduleType.equals("Weekday")) {
        c.setMonday(1);
        c.setTuesday(1);
        c.setWednesday(1);
        c.setThursday(1);
        c.setFriday(1);
      } else if (scheduleType.equals("Saturday")) {
        c.setSaturday(1);
      } else if (scheduleType.equals("Sunday")) {
        c.setSunday(1);
      } else {
        throw new IllegalStateException("unknown schedule type: "
            + scheduleType);
      }
      c.setStartDate(new ServiceDate(2010, 06, 12));
      c.setEndDate(new ServiceDate(2010, 10, 01));

      _dao.saveEntity(c);
    }
  }

  private void applyModifications() throws IOException, MalformedURLException,
      Exception {

    if (_modificationsPath == null)
      return;

    GtfsTransformer transformer = new GtfsTransformer();
    transformer.setGtfsInputDirectory(_gtfsOutputPath);
    transformer.setOutputDirectory(_gtfsOutputPath);

    TransformFactory modificationFactory = new TransformFactory();
    if (_modificationsPath.startsWith("http")) {
      modificationFactory.addModificationsFromUrl(transformer, new URL(
          _modificationsPath));
    } else {
      modificationFactory.addModificationsFromFile(transformer, new File(
          _modificationsPath));
    }

    transformer.run();
  }

  private Route getRoute(PublicTimeTable timetable, PttPlaceInfo placeInfo,
      PttTrip pttTrip) {

    AgencyAndId routeId = id(pttTrip.getRouteId());

    Route route = _dao.getRouteForId(routeId);

    if (route == null) {

      PttRoute pttRoute = getRouteForId(timetable, pttTrip.getRouteId());

      route = new Route();
      route.setAgency(_agency);
      route.setId(routeId);
      route.setShortName(pttRoute.getId());
      route.setLongName(pttRoute.getDescription());
      route.setType(3);
      _dao.saveEntity(route);
    }

    return route;
  }

  private PttRoute getRouteForId(PublicTimeTable timetable, String routeId) {
    for (PttRoute route : timetable.getRoutes()) {
      if (route.getId().equals(routeId))
        return route;
    }
    return null;
  }

  private Map<String, Integer> getTimepointPositions(PttPlaceInfo placeInfo) {
    Map<String, Integer> positions = new HashMap<String, Integer>();
    for (PttPlaceInfoPlace place : placeInfo.getPlaces()) {
      String id = place.getId();
      Integer position = place.getPositionInPattern();
      positions.put(id, position);
    }
    return positions;
  }

  private SortedMap<Integer, Integer> computeTimepointPositionToScheduleTimep(
      PttTrip pttTrip) throws ParseException {
    List<PttTimingPoint> timepoints = pttTrip.getTimingPoints();
    SortedMap<Integer, Integer> times = new TreeMap<Integer, Integer>();
    for (PttTimingPoint timepoint : timepoints) {
      Date date = _dateParse.parse(timepoint.getPassingTime());
      int time = (int) ((date.getTime() - _midnight.getTime()) / 1000);
      times.put(timepoint.getPositionInPattern(), time);
    }
    return times;
  }

  private AgencyAndId getServiceId(PublicTimeTable timeTable,
      PttPlaceInfo placeInfo) {

    String bookingIdentifier = timeTable.getBookingIdentifier();
    String scheduleType = placeInfo.getScheduleType();

    AgencyAndId id = id(bookingIdentifier + "-" + scheduleType);
    if (!_serviceIdAndScheduleType.containsKey(id))
      _serviceIdAndScheduleType.put(id, scheduleType);
    return id;
  }

  /**
   * We really want the route variation code that matches the GIS feed, but
   * we'll have to settle for the place info description field with some cleanup
   */
  private String getRouteVariationForPlaceInfo(PttPlaceInfo placeInfo) {

    String desc = placeInfo.getDescription();
    String routeVariation = null;

    if (routeVariation == null) {
      Matcher m = _routeVariationA.matcher(desc);
      if (m.find())
        routeVariation = m.group(1);
    }

    if (routeVariation == null) {
      Matcher m = _routeVariationB.matcher(desc);
      if (m.find())
        routeVariation = m.group(1) + m.group(2);
    }

    if (routeVariation == null) {
      Matcher m = _routeVariationC.matcher(desc);
      if (m.find())
        routeVariation = m.group(1);
    }

    if (routeVariation == null)
      _log.info("unknown routeVariation: " + placeInfo.getDescription());
    return routeVariation;
  }

  private String constructSequenceId(String route, String routeVariation,
      String scheduleType) {

    // TODO: Hack!
    if (route.equals("414") && routeVariation.equals("xn"))
      routeVariation = "nb";

    if (route.equals("201") && routeVariation.equals("s1")
        && scheduleType.equals("Saturday"))
      routeVariation = "sb";

    return route + "-" + routeVariation + "-" + scheduleType;
  }

  private AgencyAndId id(String id) {
    return new AgencyAndId(_agencyId, id);
  }

}
