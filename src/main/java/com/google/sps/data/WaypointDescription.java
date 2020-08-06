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
import java.util.ArrayList;

/** Description of a user's waypoint query: how many the user wants, the label, and the features in the waypoint. */
public final class WaypointDescription {

  private final int amount;
  private String label;
  private final ArrayList<String> features;
  private static final int DEFAULT_AMOUNT = 0;
  private static final String DEFAULT_LABEL = "UNLABELED";

  /** Creates a new waypoint description with all needed information
    */
  public WaypointDescription(int amount, ArrayList<String> features) {
    this.amount = amount;
    this.label = DEFAULT_LABEL;
    this.features = features;
  }

  /** Creates a new waypoint description with an amount
    */
  public WaypointDescription(int amount) {
    this.amount = amount;
    this.label = DEFAULT_LABEL;
    this.features = new ArrayList<String>();
  }

  /** Creates a new waypoint description with some features
    */
  public WaypointDescription(ArrayList<String> features) {
    this.amount = DEFAULT_AMOUNT;
    this.label = DEFAULT_LABEL;
    this.features = features;
  }

  /** Returns the x coordinate of this waypoint description.
    */
  public int getAmount() {
    return amount;
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
  public ArrayList<String> getFeatures() {
    return features;
  }

  /** Checks whether this waypoint description has any features
    */
  public boolean hasFeatures() {
    return (!features.isEmpty());
  }

  /** Waypoint description is done being made, so a label can be made
    */
  public void finalize() {
    label = Integer.toString(amount);
    for (String feature : features) {
      label += ", " + feature;
    }
  }
}