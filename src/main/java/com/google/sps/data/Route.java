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

import java.awt.Point;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;


public final class Route {
    public Point start = new Point();
    public Point end = new Point();
    public final ArrayList<Point> waypoints = new ArrayList<Point>();

   /**
    * Creates a new Route.
    *
    * @param start The coordinate point of the start location. Must be non-null.
    * @param end The coordinate point of the end location. Must be non-null.
    * @param waypoints The collection of waypoints that must be included along the route. Must be non-null.
    */
    public Route(Point start, Point end, Collection<Point> waypoints) {
        if (start == null) {
            throw new IllegalArgumentException("title cannot be null");
        }

        if (end == null) {
            throw new IllegalArgumentException("when cannot be null");
        }

        if (waypoints == null) {
            throw new IllegalArgumentException("attendees cannot be null. Use empty array instead.");
        }

        this.start.setLocation(start);
        this.end.setLocation(end);
        this.waypoints.addAll(waypoints);
    }

   /**
    * Returns the start coordinate of this route.
    */
    public Point getStart() {
        return this.start;
    }

    /**
    * Returns the end coordinate of this route.
    */
    public Point getEnd() {
        return this.end;
    }

    /**
    * Returns the list of waypoint coordinates of this route.
    */
    public ArrayList<Point> getWaypoints() {
        return this.waypoints;
    }
}