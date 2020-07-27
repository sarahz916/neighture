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

window.onload = function setup() {
    // Inialize and create a map with no directions on it when the page is reloaded.
    var chicago = new google.maps.LatLng(41.850033, -87.6500523); // hardcoded start; will get from user later
    initMap(chicago);
}

document.getElementById('form').addEventListener('submit', createMapWithWaypoints());

/**
 * Create a route and map from a waypoint entered by the user.
 */
async function createMapWithWaypoints() {
    var res = await getWaypoints();
    let waypoints = convertWaypointstoLatLng(res);
    let start = new google.maps.LatLng(41.850033, -87.6500523); // hardcoded start; will get from user later
    let end = new google.maps.LatLng(41.850033, -87.5500523); // hardcoded end; will get from user later
    let map = initMap(start);
    let directionsService = new google.maps.DirectionsService();
    let directionsRenderer = new google.maps.DirectionsRenderer({
        map: map
    });
    calcRoute(directionsService, directionsRenderer, start, end, waypoints);
    generateURL (start, end, waypoints);
    writeToAssociatedText();
}

/**
 * Given a DirectionsService object, a DirectionsRenderer object, start/end coordinates and a list
 * of waypoint coordinates, generate a route using the Google Maps API.
 */
function calcRoute(directionsService, directionsRenderer, start, end, waypoints) {
    var waypointsWithLabels = waypoints;
    let waypointsData = [];
    Object.entries(waypoints).forEach(pt => waypointsData.push({ location: pt[1] }));
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
            createWaypointLegend(result.routes[0], waypointsWithLabels);
        } else {
            alert(`Could not display directions: ${status}`);
        }
    });
}

function addNewLegendElem(parent, text) {
    let newElem = document.createElement('p');
    newElem.textContent = text;
    parent.appendChild(newElem);
}

/**
 * Given a generated route and a JSON object containing waypoint locations with their labels as inputted
 * by the user, create a legend that maps a marker on the map to corresponding user input.
 */
async function createWaypointLegend(route, waypointsWithLabels) {
    let legend = document.getElementById('legend');
    let marker = 'A';
    addNewLegendElem(legend, `${marker}: start`);
    let i;
    for (i = 0; i < route.legs.length - 1; i++) {
        let pt = route.legs[i].end_location;
        let label = getLabelFromLatLng(pt, waypointsWithLabels);
        marker = String.fromCharCode(marker.charCodeAt(0) + 1);
        addNewLegendElem(legend, `${marker}: ${label}`);
    }
    let end = route.legs[route.legs.length - 1].end_location;
    marker = String.fromCharCode(marker.charCodeAt(0) + 1);
    addNewLegendElem(legend, `${marker}: end`);
}

/**
 * Given a Google Maps LatLng object and JSON containing waypoint coords with labels, 
 * return the label matching the given LatLng object.
 */
function getLabelFromLatLng(pt, waypointsWithLabels) {
    for (let [label, waypoint] of Object.entries(waypointsWithLabels)) {
        // Calculate the difference between the lat/long of the points and 
        // check if its within a certain range.
        let latDiff = Math.abs(waypoint.lat() - pt.lat());
        let lngDiff = Math.abs(waypoint.lng() - pt.lng());
        const range = 0.001;
        if (latDiff < range && lngDiff < range) {
            return label;
        }
    }
    return '';
}

/**
 * Convert waypoints in JSON form returned by servlet to Google Maps LatLng objects.
 */
function convertWaypointstoLatLng(waypoints) {
     let latlngWaypoints = {};
     for (let pt of waypoints) {
        let waypoint = new google.maps.LatLng(pt.y, pt.x);
        latlngWaypoints[pt.label] = waypoint;
    }
    return latlngWaypoints;
}

/**
 * Get a list of waypoints by querying the waypoint servlet.
 */
async function getWaypoints() {
    let res = await fetch('/query');
    let waypoints = await res.json();
    return waypoints;
}

/**
 * Given a center coordinate, create a Google Map.
 */
function initMap(center) {
    let mapOptions = {
    zoom: 4,
    center: center
    }
    return new google.maps.Map(document.getElementById('route-map'), mapOptions);
}

/**
 * Creates a URL based on Maps URLs that will open the generated route on Google Maps on any device.
 */
function generateURL(start, end, waypoints){
    let globalURL = 'https://www.google.com/maps/dir/?api=1';
    globalURL = globalURL + '&origin=' + start + '&destination=' + end;
    globalURL += '&waypoints='
    Object.entries(waypoints).forEach(pt => globalURL += pt + '|')
    globalURL = globalURL + '&travelmode=walking';
    const URLcontainer = document.getElementById('globalURL');
    globalURL = globalURL.split(" ").join("") //need to get rid of white space for link to work
    URLcontainer.innerHTML = '<a href ='+ globalURL  + '>' + globalURL + '</a>';
}

/**
 * Fetches the last text-input from /route-store to display with the map.
 */
async function writeToAssociatedText(){
    const response = await fetch("/text-store");
    const storedtext = await response.json();
    // /text-store has all the input text sorted by most recent first.
    const associatedTextEl = document.getElementById('associated-text');
    // To get the most recent entered term, get first element of array
    // of all input text. 
    associatedTextEl.innerText = "You entered: " + storedtext[0];
}
