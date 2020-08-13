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

package com.google.sps.data;
import com.google.sps.Coordinate;
import java.util.*;

/** A object to sort Stored Routes in a Priority Queue by order away from a point.*/
public class SortedStoredRoute implements Comparable<SortedStoredRoute> {

  private final float distance;
  private final StoredRoute route;

  public SortedStoredRoute(float distance, StoredRoute route) {
    this.distance = distance;
    this.route = route;
  }

  public StoredRoute getRoute(){
      return this.route;
  }

  public Float getDistance(){
      return this.distance;
  }

  @Override
  public int compareTo(SortedStoredRoute other) {
      return this.getDistance().compareTo(other.getDistance());
  }

}
