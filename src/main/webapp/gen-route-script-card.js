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

//TODOs: Want page to work without start and end added 

import RoutePage from './RoutePage.js'

window.onload = async function setup() {
    let genRoutePage = new RoutePage();
    await genRoutePage.init();
    let startAddr = await genRoutePage.getStartAddr();
    let endAddr = await genRoutePage.getEndAddr();
    let startCoord = await genRoutePage.getStartCoord();
    setupGenRouteCards(genRoutePage);
}
/** Feteches routes from routestore and creates cards to displayed on the page. */
async function setupGenRouteCards(genRoutePage) {
    let routes = await getRouteStore();
    createCards(routes, genRoutePage);
}

/** Given stored text create cards with maps
    Create card with maps in card decks of two. */
function createCards(routes, genRoutePage) {
  const cardDivEl = document.getElementById('cards');
  cardDivEl.innerHTML = "";
  var CardDeck = newCardDeck();
  cardDivEl.append(CardDeck);
  routes.forEach((route, index) => {
    CardDeck.appendChild(createCardEl(route, genRoutePage));
    if ((index % 2) === 1){
        CardDeck = newCardDeck();
        cardDivEl.append(CardDeck);
    }
  });
}

/**Create new card deck */
function newCardDeck(){
    const CardDeck = document.createElement('div');
    CardDeck.setAttribute('class', 'card-deck');
    return CardDeck;
}

/** Creates an card map element */
function createCardEl(route, genRoutePage){
    const cardEl = document.createElement('div');
    cardEl.setAttribute('class', 'card');
    cardEl.appendChild(createCardBody(route));
    cardEl.appendChild(createURLEl(route));
    const mapID = route.id.toString(16) + 'map';
    const legendID = route.id.toString(16) + 'legend';
    const urlID = route.id.toString(16) + 'url';
    let waypointjson = JSON.parse(route.waypoints);
    genRoutePage.createMapWithWaypoints(waypointjson, mapID, legendID, urlID, false);
    return cardEl;
}

/** Creates an card body map element */
function createCardBody(route){
    const cardBody = document.createElement('div');
    cardBody.setAttribute('class', 'card-body');
    cardBody.appendChild(createTitleEl(route.text));
    cardBody.appendChild(createMapEl(route));
    cardBody.appendChild(createLegendEl(route));
    return cardBody;
}

/** Creates an card title */
function createTitleEl(text){
    const cardTitle = document.createElement('h5');
    cardTitle.setAttribute('class', 'card-title');
    cardTitle.innerText = text;
    return cardTitle;
}

/** Creates an map element with id route.id.toString(16) + 'map' */
function createMapEl(route){
    const mapEl = document.createElement('div');
    const mapID = route.id.toString(16) + 'map';
    mapEl.setAttribute('id', mapID);
    mapEl.setAttribute('class', 'map small-map');
    return mapEl;
}
/** Creates an url element as card-footer with id route.id.toString(16) + 'url' */
function createURLEl(route){
    const URLEl = document.createElement('div');
    const urlID = route.id.toString(16) + 'url';
    URLEl.setAttribute('id', urlID);
    URLEl.setAttribute('class', 'card-footer');
    return URLEl;
}
/** Creates an legend element with id route.id.toString(16) + 'legend' */
function createLegendEl(route){
    const legendEl = document.createElement('div');
    const legendID = route.id.toString(16) + 'legend';
    legendEl.setAttribute('id', legendID);
    legendEl.setAttribute('class', 'legend');
    return legendEl;
}

/**
<<<<<<< HEAD
=======
 * Create a route and map from a waypoint entered by the user.
 */
async function createMapWithWaypoints(route,  mapID, legendID, urlID) {
    let waypointjson = JSON.parse(route.waypoints);
    let waypoints = convertWaypointstoLatLng(waypointjson);
    let start = await getStartCoord();
    let end = await getEndCoord();
    let map = initMap(start, mapID);
    let directionsService = new google.maps.DirectionsService();
    let directionsRenderer = new google.maps.DirectionsRenderer({
        map: map
    });
    calcRoute(directionsService, directionsRenderer, start, end, waypoints, legendID, urlID);
}

/**
 * Given a DirectionsService object, a DirectionsRenderer object, start/end coordinates and a list
 * of waypoint coordinates, generate a route using the Google Maps API.
 */
function calcRoute(directionsService, directionsRenderer, start, end, waypoints, legendID, urlID) {
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
            createWaypointLegend(result.routes[0], waypointsWithLabels, legendID);
            generateURL (start, end, waypoints, result.routes[0].waypoint_order, urlID);
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
async function createWaypointLegend(route, waypointsWithLabels, legendID) {
    let legend = document.getElementById(legendID);
    let marker = 'A';
    addNewLegendElem(legend, `${marker}: start`);

    const waypointOrder = route.waypoint_order;

    // For each waypoint, in the order given by the route, add a new legend elem with label/species
    for (let idx of waypointOrder) {
        let waypoint = waypointsWithLabels[idx];
        marker = String.fromCharCode(marker.charCodeAt(0) + 1);
        addNewLegendElem(legend, `${marker}: ${waypoint.label} (${waypoint.species})`);
    }

    marker = String.fromCharCode(marker.charCodeAt(0) + 1);
    addNewLegendElem(legend, `${marker}: end`);

    // Add route distance to legend
    let totalDistance = getRouteDistance(route);
    addNewLegendElem(legend, `Total Route Distance: ${totalDistance} miles`);

    // Add route duration to legend
    let totalDuration = getRouteDuration(route);
    let durationMetric = 'hours';
    if (totalDuration < 1) {
        totalDuration = Math.round(convertHoursToMinutes(totalDuration) * 10) / 10;
        durationMetric = 'minutes'
    }
    addNewLegendElem(legend, `Total Route Duration: ${totalDuration} ${durationMetric}`);
}

/**
 * Given the total distance and time of a route, convert the numbers to more useful metrics
 * and add them to the legend to display to the user.
 */
function addDistanceTimeToLegend(legend, totalDistance, totalDuration) {
    // Convert totalDistance and totalDuration to more helpful metrics.
    totalDistance = Math.round(convertMetersToMiles(totalDistance) * 10) / 10;
    totalDuration = Math.round(convertSecondsToHours(totalDuration) * 10) / 10;
    let durationMetric = 'hours';
    if (totalDuration < 1) {
        totalDuration = Math.round(convertHoursToMinutes(totalDuration) * 10) / 10;
        durationMetric = 'minutes'
    }
    addNewLegendElem(legend, `Total Route Distance: ${totalDistance} miles`);
    addNewLegendElem(legend, `Total Route Duration: ${totalDuration} ${durationMetric}`);
}
/**
 * Calculate and return the total distance of a route in miles.
 */
function getRouteDistance(route) {
    let totalDistance = 0;
     for (i = 0; i < route.legs.length - 1; i++) {
        let pt = route.legs[i].end_location;
        totalDistance += route.legs[i].distance.value;
    }
    let end = route.legs[route.legs.length - 1].end_location;
    totalDistance += route.legs[route.legs.length - 1].distance.value
    /* Convert distance to a more helpful metric. */
    return Math.round(convertMetersToMiles(totalDistance) * 10) / 10;
}

/**
 * Calculate and return the total duration of a route in hours.
 */
function getRouteDuration(route) {
    let totalDuration = 0;
    // For each leg of the route, find the label of the end point
    // and add it to the page.
    for (i = 0; i < route.legs.length - 1; i++) {
        let pt = route.legs[i].end_location;
        totalDuration += route.legs[i].duration.value;
    }
    let end = route.legs[route.legs.length - 1].end_location;
    totalDuration += route.legs[route.legs.length - 1].duration.value;
    /* Convert time to a more helpful metric. */
    return Math.round(convertSecondsToHours(totalDuration) * 10) / 10;
}
/**
 * Convert a distance in meters to miles.
 */
function convertMetersToMiles(distance) {
    const CONVERSION = 0.000621371;
    return distance * CONVERSION;
}
/**
 * Convert a time in seconds to hours.
 */
function convertSecondsToHours(time) {
    const CONVERSION = 3600;
    return time / CONVERSION;
}

/**
 * Convert a time in hours to minutes.
 */
function convertHoursToMinutes(time) {
    const CONVERSION = 60;
    return time / CONVERSION;
}

/**
 * Given a Google Maps LatLng object and JSON containing waypoint coords with labels, 
 * return the label matching the given LatLng object.
 */
function getInfoFromLatLng(pt, waypointsWithLabels, infoRequested) {
    //for (let [label, waypoints] of waypointsWithLabels.entries()) {
    for (let waypoint of waypointsWithLabels) {
        // Calculate the difference between the lat/long of the points and 
        // check if its within a certain range.
        let latDiff = Math.abs(waypoint.latlng.lat() - pt.lat());
        let lngDiff = Math.abs(waypoint.latlng.lng() - pt.lng());
        const range = 0.001;
        if (latDiff < DIFF && lngDiff < DIFF) {
            return waypoint[`${infoRequested}`];
        }
    }
    return '';
}


/**
>>>>>>> a1ec45fd8a9cfbb7b30c731966007055bef4fa5c
 * Get a list of StoredRouts from fetching from /route-store
 */
async function getRouteStore() {
    let res = await fetch('/route-store');
    let routestore = await res.json();
    return routestore;
<<<<<<< HEAD
}
=======
}

/**
 * Given a center coordinate, create a Google Map.
 */
function initMap(center, id) {
    let mapOptions = {
    zoom: 4,
    center: center
    }
    return new google.maps.Map(document.getElementById(id), mapOptions);
}

/**
 * Creates a URL based on Maps URLs that will open the generated route on Google Maps on any device.
 */
function generateURL(start, end, waypoints, waypointOrder, urlID){
    let globalURL = 'https://www.google.com/maps/dir/?api=1';
    globalURL = globalURL + '&origin=' + start + '&destination=' + end;
    globalURL += '&waypoints=';
    for (let idx of waypointOrder) {
        let pt = waypoints[idx];
        globalURL += pt.latlng + '|';
    }
    globalURL = globalURL + '&travelmode=walking';
    const URLcontainer = document.getElementById(urlID);
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
/**
 * Convert waypoints in JSON form returned by servlet to Google Maps LatLng objects.
 */
function convertWaypointstoLatLng(waypoints) {
    let latLngWaypoints = [];
     for (let pt of waypoints) {
        let waypoint = new google.maps.LatLng(pt.y, pt.x);
        latLngWaypoints.push({ latlng: waypoint, label: pt.label, species: pt.species });
    }
    return latLngWaypoints;
}

/**
 * Fetches the start and end location addresses from the StartEndServlet.
 */
async function getStartEnd() {
    let res = await fetch('/start-end');
    let startEnd = await res.json();
    return startEnd;
}

/**
 * Get the start location coordinate in LatLng entered by the user.
 */
async function getStartCoord() {
    let startEnd = await getStartEnd();
    return new google.maps.LatLng(startEnd.start.y, startEnd.start.x);
}

/**
 * Gets the end location coordinate in LatLng entered by the user.
 */
async function getEndCoord() {
    let startEnd = await getStartEnd();
    return new google.maps.LatLng(startEnd.end.y, startEnd.end.x);
}
/**
 * Get the start location address entered by the user.
 */
async function getStartAddr() {
    let startEnd = await getStartEnd();
    return startEnd.start.label;
}

/**
 * Gets the end location address entered by the user.
 */
async function getEndAddr() {
    let startEnd = await getStartEnd();
    return startEnd.end.label;
}
>>>>>>> a1ec45fd8a9cfbb7b30c731966007055bef4fa5c
