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

document.getElementById('submit').addEventListener('submit', createMapWithWaypoint());

/**
 * Create a route and map from a waypoint entered by the user.
 */
async function createMapWithWaypoint() {
    let res = await getWaypoint();
    let waypoint = new google.maps.LatLng(res.y, res.x);
    var start = new google.maps.LatLng(41.850033, -87.6500523); // hardcoded start; will get from user later
    var end = new google.maps.LatLng(41.850033, -86.6500523); // hardcoded end; will get from user later
    createMapWithDirections(start, end, [waypoint]);

}

/**
 * Given a start coordinate, end coordinate, and a list of waypoint coordinates, 
 * create a route and a map and display the route on the map to the user.
 */
function createMapWithDirections(start, end, waypoints) {
    let directionsService = new google.maps.DirectionsService();
    let directionsRenderer = new google.maps.DirectionsRenderer();
    let map = initMap(start);
    directionsRenderer.setMap(map);
    calcRoute(directionsService, directionsRenderer, start, end, waypoints);
    generateURL (start, end, waypoints);
    generateURL(start, end, waypoints)
}

/**
 * Get a waypoint by querying the waypoint servlet.
 */
async function getWaypoint() {
    let res = await fetch('/query');
    let waypoint = await res.json();
    if (waypoint == null) {
        throw new Error("Waypoint is null");
    }
    return waypoint;
}

/**
 * Given a DirectionsService object, DirectionsRenderer object, and a center coordinate, create a Google Map.
 */
function initMap(center) {
    let mapOptions = {
    zoom: 4,
    center: center
    }
    return new google.maps.Map(document.getElementById('map'), mapOptions);
}

/**
 * Given a DirectionsService object, a DirectionsRenderer object, start/end coordinates and a list
 * of waypoint coordinates, generate a route using the Google Maps API.
 */
function calcRoute(directionsService, directionsRenderer, start, end, waypoints) {
    let waypointsData = [];
    waypoints.forEach(pt => waypointsData.push({ location: pt }));
    let request = {
        origin: start,
        destination: end,
        waypoints: waypointsData,
        travelMode: 'WALKING'
    };
    directionsService.route(request, function(result, status) {
        if (status == 'OK') {
            directionsRenderer.setDirections(result);
        }
    });
}

function generateURL(start, end, waypoints){
    let globalURL = 'https://www.google.com/maps/dir/?api=1';
    globalURL = globalURL + '&origin=' + start + '&destination=' + end;
    globalURL += '&waypoints='
    waypoints.forEach(pt => globalURL += pt + '|')
    globalURL = globalURL + '&travelmode=walking';
    const URLcontainer = document.getElementById('globalURL');
    URLcontainer.innerHTML = globalURL;
}

