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

/** Description of a user's waypoint query: how many the user wants, the feature, and the query equivalent. */
public final class WaypointDescription {
  // The maximum number of coordinates will be an optional input by the user, with 5 as the default
  private static final int DEFAULT_AMOUNT = 5;
  private static final String DEFAULT_TEXT = "";
  private int amount;
  private boolean hasSetAmount;
  private String query;
  private String feature; // Also doubles as the label

  /** Creates a new waypoint description with all needed information
    */
  public WaypointDescription(int amount, String feature) {
    this.amount = amount;
    this.feature = feature;
    createQuery();
    hasSetAmount = true;
  }

  /** Creates a new waypoint description with an amount
    */
  public WaypointDescription(int amount) {
    this.amount = amount;
    this.query = DEFAULT_TEXT;
    this.feature = DEFAULT_TEXT;
    hasSetAmount = true;
  }

  /** Creates a new waypoint description with a feature
    */
  public WaypointDescription(String feature) {
    this.amount = DEFAULT_AMOUNT;
    this.feature = feature;
    createQuery();
    hasSetAmount = false;
  }

  /** Creates a new waypoint description with only default values
    */
  public WaypointDescription() {
    this.amount = DEFAULT_AMOUNT;
    this.query = DEFAULT_TEXT;
    this.feature = DEFAULT_TEXT;
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

  /** Returns the query string of this waypoint description.
    */
  public String getQuery() {
    return query;
  }
  
  /** Returns the feature of this waypoint description.
    */
  public String getFeature() {
    return feature;
  }

  /** Checks whether this waypoint description has any features
    */
  public boolean hasFeature() {
    return (!feature.equals(DEFAULT_TEXT));
  }

  /** Add a feature to the waypoint
    */
  public void addFeature(String feature) {
    if (hasFeature()) {
      this.feature += " " + feature;
    } else {
      this.feature = feature;
    }
    createQuery();
  }

  /** Make the query to go along with the feature/label
    */
  private void createQuery() {
    query = feature.replace(" ", "%20");
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
    return (amount == otherWaypoint.getMaxAmount() && feature.equals(otherWaypoint.getFeature()));
  }
}