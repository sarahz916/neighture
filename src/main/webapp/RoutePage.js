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

    /**
     * Construct a new RoutePage object.
     */
    constructor() {
        this.startCoord = null;
        this.endCoord = null;
        this.startAddr = null;
        this.endAddr = null;
        this.SC_REQUEST_ENTITY_TOO_LARGE = 413;
        this.SC_BAD_REQUEST = 400;
    }

    /**
     * Initialize the instance variables of this class that require data from the servlet.
     */
    async init() {
        let res = await fetch('/start-end');
        let startEnd = await res.json();
        this.startCoord = new google.maps.LatLng(startEnd.start.y, startEnd.start.x);
        this.endCoord = new google.maps.LatLng(startEnd.end.y, startEnd.end.x);
        this.startAddr = startEnd.start.label;
        this.endAddr = startEnd.end.label
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
    * Create a route and map from a waypoint entered by the user. Writes to associated text 
    * if bool writeToText is true.
    */
    async createMapWithWaypoints(waypointjson, mapID, legendID, urlID, writeToText) {
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
        if (writeToText) {
            this.writeToAssociatedText();
        }

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
        legend.style.padding = '3px';
        legend.style.border = 'thin solid black';
    
        let title = this.addNewTypeElem(legend, 'Route Information', 'h2');
        title.style.textAlign = 'center';

       // Add start information
        let marker = 'A';
        let infoDiv = this.addNewTypeElem(legend, '', 'div');
        this.addNewTypeElem(infoDiv, `${marker}`, 'b');
        this.addNewTypeElem(infoDiv, 'start', 'p');

        const waypointOrder = route.waypoint_order;

        // For each waypoint, in the order given by the route, add a new legend elem with label/species
        for (let idx of waypointOrder) {
            let waypoint = waypointsWithLabels[idx];
            marker = String.fromCharCode(marker.charCodeAt(0) + 1);
            infoDiv = this.addNewTypeElem(legend, '', 'div');

            this.addNewTypeElem(infoDiv, `${marker}: ${waypoint.label}`, 'b');
            this.addNewTypeElem(infoDiv, `${waypoint.species}`, 'p');
            let link = this.addNewTypeElem(infoDiv, 'More Information', 'a');
            link.setAttribute('href', waypoint.url);
        }

        marker = String.fromCharCode(marker.charCodeAt(0) + 1);
        infoDiv = this.addNewTypeElem(legend, '', 'div');
        this.addNewTypeElem(infoDiv, `${marker}`, 'b');
        this.addNewTypeElem(infoDiv, 'end', 'p');

        this.addNewTypeElem(legend, '', 'br');

        // Add route distance to legend
        let totalDistance = this.getRouteDistance(route);
        this.addNewTypeElem(legend, `Total Route Distance: ${totalDistance} miles`, 'p');

        // Add route duration to legend
        let totalDuration = this.getRouteDuration(route);
        let durationMetric = 'hours';
        if (totalDuration < 1) {
            totalDuration = Math.round(this.convertHoursToMinutes(totalDuration) * 10) / 10;
            durationMetric = 'minutes'
        }
       this.addNewTypeElem(legend, `Total Route Duration: ${totalDuration} ${durationMetric}`, 'p');
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

    /** Create a new element with the given tag name with the given text and 
     *  append it to the given parent.
     */
    addNewTypeElem(parent, text, tag) {
        let newElem = document.createElement(tag);
        newElem.textContent = text;
        newElem.style.marginBottom = 0;
        parent.appendChild(newElem);
        return newElem;
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
    * Create a URL based on Maps URLs that will open the generated route on Google Maps on any device.
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
    * Fetch the last text-input from /route-store to display with the map.
    */
    async writeToAssociatedText(){
        const response = await fetch("/text-store");
        const storedtext = await response.text();
        // /text-store has all the input text sorted by most recent first.
        const associatedTextEl = document.getElementById('associated-text');
        // To get the most recent entered term, get first element of array
        // of all input text. 
        associatedTextEl.innerText = "You entered: " + storedtext;
    }

    /**
    * Get a list of waypoints by querying the waypoint servlet.
    */
    async getWaypoints() {
        let res = await fetch('/query');
        // Catch HTTP status error codes
        if (res.status === this.SC_REQUEST_ENTITY_TOO_LARGE) {
            alert('Too many waypoints in text input. Please try again.');
        } else if (res.status === this.SC_BAD_REQUEST) {
            alert('Malformed text input. Please try again.');
        }

        let waypoints = await res.json();
        return waypoints;
    }

    /**
    * Fetch the checked waypoints from the chosen-waypoints servlet.
    */
    async getChosenPoints() {
        let res = await fetch('/chosen-waypoints');
        let waypoints = await res.json();
        return waypoints;
    }


    /**
    * Get the start location coordinate in LatLng entered by the user.
    */
    getStartCoord() {
        return this.startCoord;
    }

    /**
    * Gets the end location coordinate in LatLng entered by the user.
    */
    getEndCoord() {
        return this.endCoord;
    }
    /**
    * Get the start location address entered by the user.
    */
    getStartAddr() {
        return this.startAddr;
    }

    /**
    * Gets the end location address entered by the user.
    */
    getEndAddr() {
        return this.endAddr;
    }
}