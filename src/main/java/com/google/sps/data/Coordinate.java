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

import com.google.appengine.api.datastore.GeoPt;

/** A coordinate. */
public final class Coordinate {

  private final Double x;
  private final Double y;
  private final String label;
  private final String species;
  private final String url;

  /** Creates a new coordinate with a label, species, and url
    */
  public Coordinate(Double x, Double y, String label, String species, String url) {
    this.x = x;
    this.y = y;
    this.label = label;
    this.species = species;
    this.url = url;
  }

  /** Creates a new coordinate with just a label
    */
  public Coordinate(Double x, Double y, String label) {
    this.x = x;
    this.y = y;
    this.label = label;
    this.species = "";
    this.url = "";
  }

  /** Returns the x coordinate of this point.
    */
  public Double getX() {
    return x;
  }
  
  /** Returns the y coordinate of this point.
    */
  public Double getY() {
    return y;
  }

  /** Returns the label of this point.
    */
  public String getLabel() {
    return label;
  }

  /** Returns the species of this point.
    */
  public String getSpecies() {
    return species;
  }

  /** Returns the url of this point.
    */
  public String getUrl() {
    return url;
  }

  @Override
  public boolean equals(Object other) {
    // If the object is compared with itself then return true   
    if (other == this) { 
        return true; 
    } 

    /* Check if other is an instance of Coordinate or not 
      "null instanceof [type]" also returns false */
    if (!(other instanceof Coordinate)) { 
        return false; 
    } 
      
    // typecast other to Coordinate so that we can compare data members  
    Coordinate otherCoordinate = (Coordinate) other; 
      
    // Compare the data members and return accordingly 
    return (x.equals(otherCoordinate.getX()) && y.equals(otherCoordinate.getY()));
  }

  /** Returns the Coordinate as a GeoPt.
    */
  public GeoPt toGeoPt(){
      return new GeoPt(y.floatValue(), x.floatValue());
  }
}