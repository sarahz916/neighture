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

// 25 fill colors for markers (max number of stops on route is 25)
const FILL_COLORS = ['#F1C40F', '#3498DB', '#154360', '#D1F2EB', '#D7BDE2', '#DC7633', 
                    '#145A32', '#641E16', '#5B2C6F', '#F1948A', '#FF00FF', '#C0C0C0', '#808080',
                    '#000000', '#33FF39', '#F5D3ED', '#D3F5F4', '#7371DE', '#110EEC', '#FFAA72',
                    '#F8F000', '#F8006D', '#AB0500', '#2DC4BB'
                    ];
const CHECKED_COLOR = "#FF0000";
const MAX_WAYPOINTS = 23;
const DIFF = 0.002;
const CHOICE_AT_ONCE = 3; 
const SC_REQUEST_ENTITY_TOO_LARGE = 413;
const SC_BAD_REQUEST = 400;
const SC_INTERNAL_SERVER_ERROR= 500;

window.onload = async function setup() {
    let startAddr = await getStartAddr();
    let endAddr = await getEndAddr();
    let startCoord = await getStartCoord;
    await initStartEndDisplay(startAddr, endAddr);

    // Either load the checkbox map or the directions map.
    let enteredText = await getWaypoints();
    if (JSON.stringify(enteredText) === '[]') {
        let chosenPoints = await getChosenPoints();
        await createMapWithWaypoints(chosenPoints);
    } else {
        await setupUserChoices(enteredText);
    }
}

/**
 * Get and displays the inputted start and end location addresses to the user.
 */
async function initStartEndDisplay(start, end) {
    document.getElementById('start-display').textContent = `Start location: ${start}`;
    document.getElementById('end-display').textContent = `End location: ${end}`;
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

/**
 * Fetches the start and end location addresses from the StartEndServlet.
 */
async function getStartEnd() {
    let res = await fetch('/start-end');
    let startEnd = await res.json();
    return startEnd;
}

/**
 * Create a route and map from a waypoint entered by the user.
 */
async function createMapWithWaypoints(res) {
    let waypoints = convertWaypointstoLatLng(res);
    let start = await getStartCoord();
    let end = await getEndCoord();

    let map = initMap(start, 'point-map');
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
async function setupUserChoices(res) {
    let waypoints = convertWaypointClusterstoLatLng(res);
    let startCoord = await getStartCoord();
    let endCoord = await getEndCoord();
    let startName = await getStartAddr();
    let endName = await getEndAddr();
    createCheckBoxes(res);
    createPointInfoMap(startCoord, endCoord, startName, endName, waypoints);
}

/**
 * Get waypoints from the servlet and map each cluster of waypoints on the map in a different marker color.
 */
function createPointInfoMap(start, end, startName, endName, waypoints) {
    let map = initMap(start, 'point-map');
    let bounds = createBounds(start, end, waypoints);
    map.fitBounds(bounds);

    if (startName === endName) {
        createMarker(map, start, 'start/end', google.maps.SymbolPath.CIRCLE, 'black');
    } else {
        createMarker(map, start, 'start', google.maps.SymbolPath.CIRCLE, 'black');
        createMarker(map, end, 'end', google.maps.SymbolPath.CIRCLE, 'black');
    }

    // Make one marker for each waypoint, in a different color.
    for (let i = 0; i < waypoints.length; i++) {
        let cluster = waypoints[i];
        let letter = 'A';
        for (let pt of cluster) {
            createCheckableMarker(map, pt, letter, google.maps.SymbolPath.BACKWARD_CLOSED_ARROW, FILL_COLORS[i % MAX_WAYPOINTS]);
            letter = String.fromCharCode(letter.charCodeAt(0) + 1); // update the marker letter label to the next letter
        }
    }
}


/**
 * Create a dynamic marker with an InfoWindow that appears with the label upon clicking.
 */
function createCheckableMarker(map, waypoint, letter, icon, color) {
    let marker = createMarker(map, waypoint.latlng, letter, icon, color);
    let infowindow = createInfoWindow(waypoint, marker);
    marker.addListener('click', function() {
        infowindow.open(map, marker);
    });
}

/**
 * Toggle the given checkbox to the opposite setting.
 */
function toggleCheckbox(box) {
    if (box.checked) {
        box.checked = false;
    } else {
        box.checked = true;
    }
}

/**
 * Creates a Google Maps InfoWindow object with the given text.
 */
function createInfoWindow(waypoint, marker) {
    const uncheckedIcon = marker.getIcon();
    const CHECKED_ICON = {
                        path: google.maps.SymbolPath.BACKWARD_CLOSED_ARROW,
                        fillColor: CHECKED_COLOR,
                        fillOpacity: 100,
                        scale: 5,
                        strokeWeight: 4,
                        labelOrigin: { x: 0, y: 2}
                    };
    const html = createInfoWindowHTML(waypoint);
    const infowindow = new google.maps.InfoWindow({
        content: html
    });
    let realCheckbox = getCheckboxFromMarker(waypoint.latlng);
    let markerCheckbox = html.getElementsByTagName('input')[0];
    markerCheckbox.addEventListener('click', function() {
        if (markerCheckbox.checked) {
            marker.setIcon(CHECKED_ICON);
        } else {
            marker.setIcon(uncheckedIcon);
        }
        toggleCheckbox(realCheckbox);
    });
    return infowindow;
}

/**
 * Create the HTML that goes inside an InfoWindow with the given text.
 */
function createInfoWindowHTML(waypoint) {
    console.log(waypoint);
    let html = document.createElement('div');
    addNewTypeElem(html, waypoint.label, 'b');
    addNewTypeElem(html, `species: ${waypoint.species}`, 'p');
    let link = addNewTypeElem(html, 'More Information', 'a');
    link.setAttribute('href', waypoint.url);
    link.setAttribute('target', '_blank');

    html.appendChild(document.createElement('br'));

    const markerCheckbox = document.createElement('input');
    markerCheckbox.setAttribute('type', 'checkbox');
    html.appendChild(markerCheckbox);
    return html;
}

/**
 * Given a start and end coordinate and a map of waypoint labels to a list of LatLng waypoints, 
 * create a Google Maps LatLngBounds object around which a map can be fit.
 */
function createBounds(start, end, waypoints) {
    let bounds = new google.maps.LatLngBounds();
    bounds.extend(start);
    bounds.extend(end);
    for (let cluster of waypoints) {
        for (let pt of cluster) {
            bounds.extend(pt.latlng);
        }
    }
    return bounds;
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
            strokeWeight: 1,
            labelOrigin: { x: 0, y: 2}
        }
    };
    let marker = new google.maps.Marker(markerOpts);
    return marker;
}

/**
 * Given a Google Maps LatLng location (from a marker), return the corresponding checkbox.
 */
function getCheckboxFromMarker(location) {
    let checkboxes = document.getElementsByClassName('checkbox');
    let boxFound = null;
    Array.from(checkboxes).forEach(box => {
        const pt = JSON.parse(box.value);
        if(pt.y === location.lat() && pt.x === location.lng()) {
            boxFound = box;
        }
    });
    return boxFound;
}

/** Takes ArrayList<ArrayList<Coordinates>> and creates checkboxes grouped by labels */
function createCheckBoxes(waypointChoices) {
  submitEl = document.createElement("input");
  submitEl.setAttribute("type", "submit");
  submitEl.setAttribute('id', 'submit-checkbox');
  submitEl.setAttribute('class', 'button');

  const waypointChoiceEl = document.getElementById('select-points');
  for (let i = 0; i < waypointChoices.length; i++) {
      for (let observation of waypointChoices[i]) {
          waypointChoiceEl.appendChild(createCheckBoxEl(observation));
      }
  }
  waypointChoiceEl.appendChild(submitEl);
}

/** Creates an checkbox element with label */
function createCheckBoxEl(choice){
    const checkBoxEl = document.createElement('input');
    checkBoxEl.setAttribute("type", "checkbox");
    const checkBoxValue = JSON.stringify(choice);
    checkBoxEl.setAttribute("value", checkBoxValue);
    checkBoxEl.setAttribute("name", checkBoxValue);
    checkBoxEl.setAttribute("class", "checkbox");
    return checkBoxEl;
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

/**
 * Catch errors when the user selects too many checkboxes.
 */
function catchCheckboxErrors(event) {
    const checkbox = event.target;
    let numChecked = getNumChecked();
    if (numChecked > MAX_WAYPOINTS) {
        alert('You have selected too many waypoints. Please select up to 23 waypoints.');
        checkbox.checked = false; // uncheck the checkbox
    }
}

/**
 * Get the number of waypoint checkboxes checked.
 */
function getNumChecked() {
    let numChecked = 0;
    let checkboxes = document.getElementsByClassName('checkbox');
    Array.from(checkboxes).forEach(box => {
        if (box.checked) {
            numChecked++;
        }
    });
    return numChecked;
}

/**
 * Given a DirectionsService object, a DirectionsRenderer object, start/end coordinates and a list
 * of waypoint coordinates, generate a route using the Google Maps API.
 */
function calcRoute(directionsService, directionsRenderer, start, end, waypoints) {
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
            createWaypointLegend(result.routes[0], waypointsWithLabels);
        } else {
            alert(`Could not display directions: ${status}`);
        }
    });
}

/** Create a new element iwth the given tag name with the given text and append it to the given parent. */
function addNewTypeElem(parent, text, tag) {
    let newElem = document.createElement(tag);
    newElem.textContent = text;
    parent.appendChild(newElem);
    return newElem;
}

/**
 * Given a generated route and a JSON object containing waypoint locations with their labels as inputted
 * by the user, create a legend that maps a marker on the map to corresponding user input.
 */
async function createWaypointLegend(route, waypointsWithLabels) {
    let legend = document.getElementById('legend');
    let marker = 'A';
    addNewTypeElem(legend, `${marker}: start`, 'p');

    const waypointOrder = route.waypoint_order;

    // For each waypoint, in the order given by the route, add a new legend elem with label/species
    for (let idx of waypointOrder) {
        let waypoint = waypointsWithLabels[idx];
        marker = String.fromCharCode(marker.charCodeAt(0) + 1);
        addNewTypeElem(legend, `${marker}: ${waypoint.label}`, 'b');
        addNewTypeElem(legend, `${waypoint.species}`, 'p');
        let link = addNewTypeElem(legend, 'More Information', 'a');
        link.setAttribute('href', waypoint.url);
    }

    marker = String.fromCharCode(marker.charCodeAt(0) + 1);
    addNewTypeElem(legend, `${marker}: end`, 'p');

    // Add route distance to legend
    let totalDistance = getRouteDistance(route);
    addNewTypeElem(legend, `Total Route Distance: ${totalDistance} miles`, 'p');

    // Add route duration to legend
    let totalDuration = getRouteDuration(route);
    let durationMetric = 'hours';
    if (totalDuration < 1) {
        totalDuration = Math.round(convertHoursToMinutes(totalDuration) * 10) / 10;
        durationMetric = 'minutes'
    }
    addNewTypeElem(legend, `Total Route Duration: ${totalDuration} ${durationMetric}`, 'p');
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
 * Convert waypoint clusters in JSON form returned by servlet to a list of objects containing Google Maps LatLng objects
 * in place of coordinates.
 */
function convertWaypointClusterstoLatLng(waypoints) {
     let latlngWaypoints = [];
     for (let cluster of waypoints) {
         pts = [];
         for (let pt of cluster) {
            let waypoint = new google.maps.LatLng(pt.y, pt.x);
            pts.push({ latlng : waypoint, label : pt.label, species : pt.species, url : pt.url });
         }
         latlngWaypoints.push(pts);
    }
    return latlngWaypoints;
}

/**
 * Convert waypoints in JSON form returned by servlet to Google Maps LatLng objects.
 */
function convertWaypointstoLatLng(waypoints) {
    let latLngWaypoints = [];
     for (let pt of waypoints) {
        let waypoint = new google.maps.LatLng(pt.y, pt.x);
        latLngWaypoints.push({ latlng: waypoint, label: pt.label, species: pt.species, url : pt.url });
    }
    return latLngWaypoints;
}

/**
 * Get a list of waypoints by querying the waypoint servlet.
 */
async function getWaypoints() {
    let res = await fetch('/query');
    // Catch HTTP status error codes
    if (res.status === SC_REQUEST_ENTITY_TOO_LARGE) {
        alert('Too many waypoints in text input. Please try again.');
    } else if (res.status === SC_BAD_REQUEST) {
        alert('Malformed text input. Please try again.');
    }

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
    globalURL += '&waypoints=';
    waypoints.forEach(pt => globalURL += pt.latlng + '|');
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

/* Check to see if we're running on Node.js or in a browser for tests */
try {
    module.exports.addNewLegendElem = addNewTypeElem;
    module.exports.createColorBoxElem = createColorBoxElem;
    module.exports.createCheckBoxEl = createCheckBoxEl;
    module.exports.createCheckBoxSet = createCheckBoxSet;
    module.exports.createCheckBoxes = createCheckBoxes;
} catch(error) {
    console.log("Not exporting code from this script")
}
