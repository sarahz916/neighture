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

export default class RoutePage {

    constructor() {
        this.startCoord = null;
        this.endCoord = null;
        this.startAddr = null;
        this.endAddr = null;
        this.waypoints = null; 
        //this.addNewLegendElem = this.addNewLegendElem.bind(this);
    }

    /**
     * Given a center coordinate, create a Google Map.
     */
    initMap(center, id) {
        let mapOptions = {
            zoom: 4,
            center: center
        }
        return new google.maps.Map(document.getElementById(id), mapOptions);
    }

    /**
    * Create a route and map from a waypoint entered by the user.
    */
    async createMapWithWaypoints(route,  mapID, legendID, urlID) {
        let waypointjson = JSON.parse(route.waypoints);
        let waypoints = this.convertWaypointstoLatLng(waypointjson);
        let start = await this.getStartCoord();
        let end = await this.getEndCoord();
        let map = this.initMap(start, mapID);
        let directionsService = new google.maps.DirectionsService();
        let directionsRenderer = new google.maps.DirectionsRenderer({
            map: map
        });
        this.calcRoute(directionsService, directionsRenderer, start, end, waypoints, legendID);
        this.generateURL (start, end, waypoints, urlID);
    }

    /**
    * Convert waypoints in JSON form returned by servlet to Google Maps LatLng objects.
    */
    convertWaypointstoLatLng(waypoints) {
        let latLngWaypoints = [];
        for (let pt of waypoints) {
            let waypoint = new google.maps.LatLng(pt.y, pt.x);
            latLngWaypoints.push({ latlng: waypoint, label: pt.label, species: pt.species });
        }
        return latLngWaypoints;
    }

    /**
    * Given a generated route and a JSON object containing waypoint locations with their labels as inputted
    * by the user, create a legend that maps a marker on the map to corresponding user input.
    */
    createWaypointLegend(route, waypointsWithLabels, legendID) {
        let legend = document.getElementById(legendID);
        let marker = 'A';
        this.addNewLegendElem(legend, `${marker}: start`);

        const waypointOrder = route.waypoint_order;

        // For each waypoint, in the order given by the route, add a new legend elem with label/species
        for (let idx of waypointOrder) {
            let waypoint = waypointsWithLabels[idx];
            marker = String.fromCharCode(marker.charCodeAt(0) + 1);
            this.addNewLegendElem(legend, `${marker}: ${waypoint.label} (${waypoint.species})`);
        }

        marker = String.fromCharCode(marker.charCodeAt(0) + 1);
        this.addNewLegendElem(legend, `${marker}: end`);

        // Add route distance to legend
        let totalDistance = this.getRouteDistance(route);
        this.addNewLegendElem(legend, `Total Route Distance: ${totalDistance} miles`);

        // Add route duration to legend
        let totalDuration = this.getRouteDuration(route);
        let durationMetric = 'hours';
        if (totalDuration < 1) {
            totalDuration = Math.round(convertHoursToMinutes(totalDuration) * 10) / 10;
            durationMetric = 'minutes'
        }
        this.addNewLegendElem(legend, `Total Route Duration: ${totalDuration} ${durationMetric}`);
    }

    /**
    * Given a DirectionsService object, a DirectionsRenderer object, start/end coordinates and a list
    * of waypoint coordinates, generate a route using the Google Maps API.
    */
    calcRoute(directionsService, directionsRenderer, start, end, waypoints, legendID) {
        var waypointsWithLabels = waypoints;
        let waypointsData = [];
        waypoints.forEach(pt => waypointsData.push({ location: pt.latlng }));
        let request = {
            origin: start,
            destination: end,
            waypoints: waypointsData,
            optimizeWaypoints: true,
            travelMode: 'WALKING'
        };
        directionsService.route(request, function(result, status) {
            if (status == 'OK') {
                directionsRenderer.setDirections(result);
                this.createWaypointLegend(result.routes[0], waypointsWithLabels, legendID);
            } else {
                alert(`Could not display directions: ${status}`);
            }
        }.bind(this));
    }

    /**
     * Add a new p element to the given parent with the given text.
     */
    addNewLegendElem(parent, text) {
        let newElem = document.createElement('p');
        newElem.textContent = text;
        parent.appendChild(newElem);
    }

    /**
    * Calculate and return the total distance of a route in miles.
    */
    getRouteDistance(route) {
        let totalDistance = 0;
        for (let i = 0; i < route.legs.length - 1; i++) {
            let pt = route.legs[i].end_location;
            totalDistance += route.legs[i].distance.value;
        }
        let end = route.legs[route.legs.length - 1].end_location;
        totalDistance += route.legs[route.legs.length - 1].distance.value
        /* Convert distance to a more helpful metric. */
        return Math.round(this.convertMetersToMiles(totalDistance) * 10) / 10;
    }

    /**
    * Calculate and return the total duration of a route in hours.
    */
    getRouteDuration(route) {
        let totalDuration = 0;
        // For each leg of the route, find the label of the end point
        // and add it to the page.
        for (let i = 0; i < route.legs.length - 1; i++) {
            let pt = route.legs[i].end_location;
            totalDuration += route.legs[i].duration.value;
        }
        let end = route.legs[route.legs.length - 1].end_location;
        totalDuration += route.legs[route.legs.length - 1].duration.value;
        /* Convert time to a more helpful metric. */
        return Math.round(this.convertSecondsToHours(totalDuration) * 10) / 10;
    }

    /**
    * Convert a distance in meters to miles.
    */
    convertMetersToMiles(distance) {
        const CONVERSION = 0.000621371;
        return distance * CONVERSION;
    }
    /**
    * Convert a time in seconds to hours.
    */
    convertSecondsToHours(time) {
        const CONVERSION = 3600;
        return time / CONVERSION;
    }

    /**
    * Convert a time in hours to minutes.
    */
    convertHoursToMinutes(time) {
        const CONVERSION = 60;
        return time / CONVERSION;
    }

    /**
    * Creates a URL based on Maps URLs that will open the generated route on Google Maps on any device.
    */
    generateURL(start, end, waypoints, urlID){
        let globalURL = 'https://www.google.com/maps/dir/?api=1';
        globalURL = globalURL + '&origin=' + start + '&destination=' + end;
        globalURL += '&waypoints=';
        waypoints.forEach(pt => globalURL += pt.latlng + '|');
        globalURL = globalURL + '&travelmode=walking';
        const URLcontainer = document.getElementById(urlID);
        globalURL = globalURL.split(" ").join("") //need to get rid of white space for link to work
        URLcontainer.innerHTML = '<a href ='+ globalURL  + '>' + globalURL + '</a>';
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
        let startEnd = await this.getStartEnd();
        this.startCoord = new google.maps.LatLng(startEnd.start.y, startEnd.start.x);
        return this.startCoord;
    }

    /**
    * Gets the end location coordinate in LatLng entered by the user.
    */
    async getEndCoord() {
        let startEnd = await this.getStartEnd();
        this.endCoord = new google.maps.LatLng(startEnd.end.y, startEnd.end.x);
        return this.endCoord;
    }
    /**
    * Get the start location address entered by the user.
    */
    async getStartAddr() {
        let startEnd = await this.getStartEnd();
        this.startAddr = startEnd.start.label;
        return this.startAddr;
    }

    /**
    * Gets the end location address entered by the user.
    */
    async getEndAddr() {
        let startEnd = await this.getStartEnd();
        this.endAddr = startEnd.end.label;
        return this.endAddr;
    }
}