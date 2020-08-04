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

// 25 fill colors for markers (max number of waypoints is 25)
const FILL_COLORS = ["#FF0000", '#F1C40F', '#3498DB', '#154360', '#D1F2EB', '#D7BDE2', '#DC7633', 
                    '#145A32', '#641E16', '#5B2C6F', '#F1948A', '#FF00FF', '#C0C0C0', '#808080',
                    '#000000', '#33FF39', '#F5D3ED', '#D3F5F4', '#7371DE', '#110EEC', '#FFAA72',
                    '#F8F000', '#F8006D', '#AB0500', '#2DC4BB'
                    ];
const MAX_WAYPOINTS = 25;

window.onload = function setup() {
    // Inialize and create a map with no directions on it when the page is reloaded.
    var chicago = new google.maps.LatLng(41.850033, -87.6500523); // hardcoded start; will get from user later
    initMap(chicago, 'route-map');
    initMap(chicago, 'point-map');
}

document.getElementById('text-git statform').addEventListener('submit', setupUserChoices());
document.getElementById('select-points').addEventListener('submit', createMapWithWaypoints());

/**
 * Create a route and map from a waypoint entered by the user.
 */
async function createMapWithWaypoints() {
    var res = await getChosenPoints();
    let waypoints = convertWaypointstoLatLng(res);
    let start = new google.maps.LatLng(41.850033, -87.6500523); // hardcoded start; will get from user later
    let end = new google.maps.LatLng(41.866940, -87.607105); // hardcoded end; will get from user later
    let map = initMap(start, 'route-map');
    let directionsService = new google.maps.DirectionsService();
    let directionsRenderer = new google.maps.DirectionsRenderer({
        map: map
    });
    calcRoute(directionsService, directionsRenderer, start, end, waypoints);
    generateURL (start, end, waypoints);
    writeToAssociatedText();
}

/**
 * Fetch the checked waypoints from the chosen-waypoints servlet.
 */
async function getChosenPoints() {
    let res = await fetch('/chosen-waypoints');
    let waypoints = await res.json();
    return waypoints;
}

/**
 * Set up the two left-hand panels of the page that allow the user to choose what they want in their route.
 */
async function setupUserChoices() {
    let res = await getWaypoints();
    let waypoints = convertWaypointClusterstoLatLng(res);
    createPointInfoMap(waypoints);
    createCheckBoxes(res);
}

/**
 * Get waypoints from the servlet and map each cluster of waypoints on the map in a different marker color.
 */
function createPointInfoMap(waypoints) {
    let start = new google.maps.LatLng(41.850033, -87.6500523); // hardcoded start; will get from user later
    let end = new google.maps.LatLng(41.866940, -87.607105); // hardcoded end; will get from user later
    let map = initMap(start, 'point-map');
    map.setZoom(10);

    createMarker(map, start, 'start', google.maps.SymbolPath.CIRCLE, 'black');
    createMarker(map, end, 'end', google.maps.SymbolPath.CIRCLE, 'black');


    // Make one marker for each waypoint, in a different color.
    for (let i = 0; i < Object.entries(waypoints).length; i++) {
        let [label, cluster] = Object.entries(waypoints)[i];
        let letter = 'A';
        for (let pt of cluster) {
            createMarker(map, pt, letter, google.maps.SymbolPath.BACKWARD_CLOSED_ARROW, FILL_COLORS[i % MAX_WAYPOINTS]);
            letter = String.fromCharCode(letter.charCodeAt(0) + 1); // update the marker letter label to the next letter
        }
    }
}

/**
 * Given options for a custom Google Maps marker, create the marker on the map.
 */
function createMarker(map, pos, label, shape, color) {
    let markerOpts = {
        animation: google.maps.Animation.DROP,
        map: map,
        position: pos,
        label: label,
        icon: {
            path: shape,
            fillColor: color,
            fillOpacity: 100,
            scale: 5,
            strokeWeight: 2,
        }
    };
    let marker = new google.maps.Marker(markerOpts);
}

//TODO: create already checked boxes for labels with only one choice.
/** Takes ArrayList<ArrayList<Coordinates>> and creates checkboxes grouped by labels */
function createCheckBoxes(waypointChoices) {
  submitEl = document.createElement("input");
  submitEl.setAttribute("type", "submit");
  submitEl.setAttribute('id', 'submit-checkbox');

  const waypointChoiceEl = document.getElementById('select-points');
  for (let i = 0; i < waypointChoices.length; i++) {
      waypointChoiceEl.appendChild(createCheckBoxSet(waypointChoices[i], FILL_COLORS[i % MAX_WAYPOINTS]));
  }
  waypointChoiceEl.appendChild(submitEl);
}

/** Creates an element that has Name of set and checkpoints of coordinates */
function createCheckBoxSet(set, color) {
  const setName = set[0].label;
  const returnDiv = document.createElement('div');
  const CheckBoxTitle = document.createElement('h4');
  CheckBoxTitle.innerText = setName;
  const colorbox = createColorBoxElem(color);
  returnDiv.appendChild(CheckBoxTitle);
  returnDiv.appendChild(colorbox);

  let letter = 'A';
  set.forEach((choice)=>{
      returnDiv.appendChild(createCheckBoxEl(choice, letter))
      letter = String.fromCharCode(letter.charCodeAt(0) + 1); // update the marker letter label to the next letter
  })
  return returnDiv;
}

/**
 * Create an box-shaped element filled with the given color.
 */
function createColorBoxElem(color) {
  const colorbox = document.createElement('div');
  colorbox.style.height = '20px';
  colorbox.style.width = '20px';
  colorbox.style.backgroundColor = color;
  return colorbox;
}

/** Creates an checkbox element with label */
function createCheckBoxEl(choice, label){
    const checkBoxEl = document.createElement('input');
    checkBoxEl.setAttribute("type", "checkbox");
    const checkBoxValue = JSON.stringify(choice);
    checkBoxEl.setAttribute("value", checkBoxValue);
    checkBoxEl.setAttribute("name", checkBoxValue);
    const checkBoxLabel = document.createElement('label');
    checkBoxLabel.innerText = label;
    const labelAndBox = document.createElement('div');
    labelAndBox.appendChild(checkBoxEl);
    labelAndBox.appendChild(checkBoxLabel);
    return labelAndBox;
}

/**
 * Given a DirectionsService object, a DirectionsRenderer object, start/end coordinates and a list
 * of waypoint coordinates, generate a route using the Google Maps API.
 */
function calcRoute(directionsService, directionsRenderer, start, end, waypoints) {
    var waypointsWithLabels = waypoints;
    let waypointsData = [];
    waypoints.forEach((pts, label) => pts.forEach(pt => waypointsData.push({ location: pt })));
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
    let totalDistance = 0;
    let totalDuration = 0;
    // For each leg of the route, find the label of the end point
    // and add it to the page.
    for (i = 0; i < route.legs.length - 1; i++) {
        let pt = route.legs[i].end_location;
        totalDistance += route.legs[i].distance.value;
        totalDuration += route.legs[i].duration.value;
        let label = getLabelFromLatLng(pt, waypointsWithLabels);
        marker = String.fromCharCode(marker.charCodeAt(0) + 1);
        addNewLegendElem(legend, `${marker}: ${label}`);
    }
    let end = route.legs[route.legs.length - 1].end_location;
    totalDistance += route.legs[route.legs.length - 1].distance.value;
    totalDuration += route.legs[route.legs.length - 1].duration.value;
    marker = String.fromCharCode(marker.charCodeAt(0) + 1);
    addNewLegendElem(legend, `${marker}: end`);
    addDistanceTimeToLegend(legend, totalDistance, totalDuration);
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
function getLabelFromLatLng(pt, waypointsWithLabels) {
    for (let [label, waypoints] of waypointsWithLabels.entries()) {
        // Calculate the difference between the lat/long of the points and 
        // check if its within a certain range.
        for (let waypoint of waypoints) {
            let latDiff = Math.abs(waypoint.lat() - pt.lat());
            let lngDiff = Math.abs(waypoint.lng() - pt.lng());
            const range = 0.001;
            if (latDiff < range && lngDiff < range) {
                return label;
            }
        }
    }
    return '';
}

/**
 * Convert waypoint clusters in JSON form returned by servlet to Google Maps LatLng objects.
 */
function convertWaypointClusterstoLatLng(waypoints) {
     let latlngWaypoints = {};
     for (let cluster of waypoints) {
         pts = [];
         for (let pt of cluster) {
            let waypoint = new google.maps.LatLng(pt.y, pt.x);
            pts.push(waypoint);
         }
         latlngWaypoints[cluster[0].label] = pts;
    }
    return latlngWaypoints;
}

/**
 * Convert waypoints in JSON form returned by servlet to Google Maps LatLng objects.
 */
function convertWaypointstoLatLng(waypoints) {
     let latlngWaypoints = new Map();
     for (let pt of waypoints) {
        let waypoint = new google.maps.LatLng(pt.y, pt.x);
        // If the given label doesn't exist in the map, add it.
        if (!latlngWaypoints.has(pt.label)) {
            latlngWaypoints.set(pt.label, [waypoint]);
        } else {
            latlngWaypoints.get(pt.label).push(waypoint);
        }
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
function generateURL(start, end, waypoints){
    let globalURL = 'https://www.google.com/maps/dir/?api=1';
    globalURL = globalURL + '&origin=' + start + '&destination=' + end;
    globalURL += '&waypoints='
    waypoints.forEach((pts, label) => pts.forEach(pt => globalURL += pt + '|'));
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
    const storedtext = await response.text();
    // /text-store has all the input text sorted by most recent first.
    const associatedTextEl = document.getElementById('associated-text');
    // To get the most recent entered term, get first element of array
    // of all input text. 
    associatedTextEl.innerText = "You entered: " + storedtext;
}
