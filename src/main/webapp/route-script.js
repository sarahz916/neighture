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
const FILL_COLORS = ["#FF0000", '#F1C40F', '#3498DB', '#154360', '#D1F2EB', '#D7BDE2', '#DC7633', 
                    '#145A32', '#641E16', '#5B2C6F', '#F1948A', '#FF00FF', '#C0C0C0', '#808080',
                    '#000000', '#33FF39', '#F5D3ED', '#D3F5F4', '#7371DE', '#110EEC', '#FFAA72',
                    '#F8F000', '#F8006D', '#AB0500', '#2DC4BB'
                    ];
const MAX_WAYPOINTS = 23;
const CHOICE_AT_ONCE = 3; 

window.onload = async function setup(event) {
    event.preventDefault();
    let startAddr = await getStartAddr();
    let endAddr = await getEndAddr();
    let startCoord = await getStartCoord;
    await initStartEndDisplay(startAddr, endAddr);
}

document.getElementById('text-git statform').addEventListener('submit', setupUserChoices());
document.getElementById('select-points').addEventListener('submit', createMapWithWaypoints());

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
async function createMapWithWaypoints() {
    var res = await getChosenPoints();
    let waypoints = convertWaypointstoLatLng(res);
    let start = await getStartCoord();
    let end = await getEndCoord();

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
    let start = await getStartCoord();
    let end = await getEndCoord();
    createPointInfoMap(start, end, waypoints);
    createCheckBoxes(res);
}

/**
 * Get waypoints from the servlet and map each cluster of waypoints on the map in a different marker color.
 */
function createPointInfoMap(start, end, waypoints) {
    let map = initMap(start, 'point-map');
    let bounds = createBounds(start, end, waypoints);
    map.fitBounds(bounds);

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
 * Given a start and end coordinate and a map of waypoint labels to a list of LatLng waypoints, 
 * create a Google Maps LatLngBounds object around which a map can be fit.
 */
function createBounds(start, end, waypoints) {
    let bounds = new google.maps.LatLngBounds();
    bounds.extend(start);
    bounds.extend(end);
    for (let i = 0; i < Object.entries(waypoints).length; i++) {
        let [label, cluster] = Object.entries(waypoints)[i];
        for (let pt of cluster) {
            bounds.extend(pt);
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
            strokeWeight: 2,
            labelOrigin: { x: 0, y: 2}
        }
    };
    let marker = new google.maps.Marker(markerOpts);
}

//TODO (zous): create already checked boxes for labels with only one choice.
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
  // create collapse element
  const collapseDiv = document.createElement('div');
  collapseDiv.setAttribute('class', 'collapse');
  collapseDiv.setAttribute('id', setName + "more");
  //intialize letter 
  let letter = 'A';
  set.forEach((choice,index) => {
      if (index === CHOICE_AT_ONCE){ //create a new div that appears with "seemore button"
        //append a See More button
        seeMoreButton = document.createElement('button');
        seeMoreButton.setAttribute('class', 'btn btn-link');
        seeMoreButton.setAttribute('type', 'button');
        seeMoreButton.setAttribute('data-toggle', 'collapse');
        seeMoreButton.setAttribute('data-target', "#" + setName + "more");
        seeMoreButton.innerText = 'see more';
        //add see more button to document
        returnDiv.appendChild(seeMoreButton);
        collapseDiv.appendChild(createCheckBoxEl(choice, letter));
        //only add collapse div if needed
        returnDiv.appendChild(collapseDiv);

      } else if (index > CHOICE_AT_ONCE){//option will be seen in see more 
        collapseDiv.appendChild(createCheckBoxEl(choice, letter));
      } else { //visible choices.
        returnDiv.appendChild(createCheckBoxEl(choice, letter));
        //letter = String.fromCharCode(letter.charCodeAt(0) + 1); update the marker letter label to the next letter
      }
      letter = String.fromCharCode(letter.charCodeAt(0) + 1);
  });
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
    checkBoxEl.setAttribute("class", "checkbox");
    const checkBoxLabel = document.createElement('label');
    checkBoxLabel.innerText = label;
    const labelAndBox = document.createElement('div');
    labelAndBox.addEventListener('click', catchCheckboxErrors);
    labelAndBox.appendChild(checkBoxEl);
    labelAndBox.appendChild(checkBoxLabel);
    return labelAndBox;
}

/**
 * Catch errors when the user selects too many checkboxes.
 */
function catchCheckboxErrors(event) {
    const checkbox = event.target;
    let numChecked = getNumChecked();
    console.log(numChecked);
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
         latlngWaypoints[cluster[0].label] = pts; // cluster[0] is undefined here when entering a sentence
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

try {
    module.exports.addNewLegendElem = addNewLegendElem;
    module.exports.createColorBoxElem = createColorBoxElem;
    module.exports.createCheckBoxEl = createCheckBoxEl;
    module.exports.createCheckBoxSet = createCheckBoxSet;
    module.exports.createCheckBoxes = createCheckBoxes;
} catch(error) {
    console.log("Not exporting code from this script")
}
