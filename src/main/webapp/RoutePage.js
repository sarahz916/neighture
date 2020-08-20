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

class RoutePage {

    constructor() {
        this.startCoord = null;
        this.endCoord = null;
        this.startAddr = null;
        this.endAddr = null;
        this.waypoints = null; 
    }

    /**
    * Create a route and map from a waypoint entered by the user.
    */
    async createMapWithWaypoints(route,  mapID, legendID, urlID) {
        let waypointjson = JSON.parse(route.waypoints);
        let waypoints = convertWaypointstoLatLng(waypointjson);
        let start = await getStartCoord();
        let end = await getEndCoord();
        let map = initMap(start, mapID);
        let directionsService = new google.maps.DirectionsService();
        let directionsRenderer = new google.maps.DirectionsRenderer({
            map: map
        });
        calcRoute(directionsService, directionsRenderer, start, end, waypoints, legendID);
        generateURL (start, end, waypoints, urlID);
    }

    /**
    * Fetches the start and end location addresses from the StartEndServlet.
    */
    async getStartEnd() {
        let res = await fetch('/start-end');
        let startEnd = await res.json();
        return startEnd;
    }

    /**
    * Get the start location coordinate in LatLng entered by the user.
    */
    async getStartCoord() {
        let startEnd = await getStartEnd();
        this.startCoord = new google.maps.LatLng(startEnd.start.y, startEnd.start.x);
        return this.startCoord;
    }

    /**
    * Gets the end location coordinate in LatLng entered by the user.
    */
    async getEndCoord() {
        let startEnd = await getStartEnd();
        this.endCoord = new google.maps.LatLng(startEnd.end.y, startEnd.end.x);
        return this.endCoord;
    }
    /**
    * Get the start location address entered by the user.
    */
    async getStartAddr() {
        let startEnd = await getStartEnd();
        this.startAddr = startEnd.start.label;
        return this.startAddr;
    }

    /**
    * Gets the end location address entered by the user.
    */
    async getEndAddr() {
        let startEnd = await getStartEnd();
        this.endAddr = startEnd.end.label;
        return this.endAddr;
    }
}