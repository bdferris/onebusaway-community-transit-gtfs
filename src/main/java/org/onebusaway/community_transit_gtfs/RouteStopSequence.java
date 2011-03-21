package org.onebusaway.community_transit_gtfs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RouteStopSequence implements Iterable<RouteStopSequenceItem>{

  private List<RouteStopSequenceItem> _items = new ArrayList<RouteStopSequenceItem>();

  public void add(RouteStopSequenceItem item) {
    _items.add(item);
  }
  
  public List<RouteStopSequenceItem> getItems() {
    return _items;
  }

  @Override
  public Iterator<RouteStopSequenceItem> iterator() {
    return _items.iterator();
  }
}
