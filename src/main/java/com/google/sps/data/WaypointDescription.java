// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;
import java.util.LinkedHashSet;
import java.util.Iterator;

/** Description of a user's waypoint query: how many the user wants, the label, and the features in the waypoint. */
public final class WaypointDescription {
  // The maximum number of coordinates will be an optional input by the user, with 5 as the default
  private static final int DEFAULT_AMOUNT = 5;
  private static final String DEFAULT_LABEL = "UNLABELED";
  private int amount;
  private boolean hasSetAmount;
  private String label;
  private final LinkedHashSet<String> features;

  /** Creates a new waypoint description with all needed information
    */
  public WaypointDescription(int amount, LinkedHashSet<String> features) {
    this.amount = amount;
    this.label = DEFAULT_LABEL;
    this.features = features;
    hasSetAmount = true;
  }

  /** Creates a new waypoint description with an amount
    */
  public WaypointDescription(int amount) {
    this.amount = amount;
    this.label = DEFAULT_LABEL;
    this.features = new LinkedHashSet<String>();
    hasSetAmount = true;
  }

  /** Creates a new waypoint description with some features
    */
  public WaypointDescription(LinkedHashSet<String> features) {
    this.amount = DEFAULT_AMOUNT;
    this.label = DEFAULT_LABEL;
    this.features = features;
    hasSetAmount = false;
  }

  /** Creates a new waypoint description with only default values
    */
  public WaypointDescription() {
    this.amount = DEFAULT_AMOUNT;
    this.label = DEFAULT_LABEL;
    this.features = new LinkedHashSet<String>();
    hasSetAmount = false;
  }

  /** Returns the amount of waypoints the user wants.
    */
  public int getMaxAmount() {
    return amount;
  }

  /** Returns whether or not the user has requested a max number of waypoints
    */
  public boolean maxAmountWasSet() {
    return hasSetAmount;
  }

  /** Updates amount of waypoints -- in our code, the user should only update if
    * there are no features for this waypoint yet
    */
  public void setMaxAmount(int newAmount) {
    amount = newAmount;
  }

  /** Returns the label of this waypoint description.
    */
  public String getLabel() {
    return label;
  }

  /** Checks whether this waypoint description has a label
    */
  public boolean hasLabel() {
    return (!label.equals(DEFAULT_LABEL));
  }
  
  /** Returns the y coordinate of this waypoint description.
    */
  public LinkedHashSet<String> getFeatures() {
    return features;
  }

  /** Checks whether this waypoint description has any features
    */
  public boolean hasFeatures() {
    return (!features.isEmpty());
  }

  /** Add a feature to the waypoint
    */
  public void addFeature(String feature) {
    features.add(feature);
  }

  /** Waypoint description is done being made, so a label can be made
    * Returns whether or not the description was successfully finalized
    */
  public boolean createLabel() {
    if (hasFeatures()) {
      Iterator<String> iterator = features.iterator();
      label = iterator.next();
      while( iterator.hasNext() ){
        label += ", " + iterator.next();
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object other) {
    // If the object is compared with itself then return true   
    if (other == this) { 
        return true; 
    } 

    /* Check if other is an instance of WaypointDescription or not 
      "null instanceof [type]" also returns false */
    if (!(other instanceof WaypointDescription)) { 
        return false; 
    } 
      
    // typecast other to WaypointDescription so that we can compare data members  
    WaypointDescription otherWaypoint = (WaypointDescription) other; 
      
    // Compare the data members and return accordingly 
    return (amount == otherWaypoint.getMaxAmount() && label.equals(otherWaypoint.getLabel()));
  }
}