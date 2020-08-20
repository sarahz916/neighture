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

import RoutePage from './RoutePage.js'

// 25 fill colors for markers (max number of stops on route is 25)
const FILL_COLORS = ['#F1C40F', '#3498DB', '#154360', '#D1F2EB', '#D7BDE2', '#DC7633', 
                    '#145A32', '#641E16', '#5B2C6F', '#F1948A', '#FF00FF', '#C0C0C0', '#808080',
                    '#000000', '#33FF39', '#F5D3ED', '#D3F5F4', '#7371DE', '#110EEC', '#FFAA72',
                    '#F8F000', '#F8006D', '#AB0500', '#2DC4BB'
                    ];
const CHECKED_COLOR = "#FF0000";
const MAX_WAYPOINTS = 23;
const CHOICE_AT_ONCE = 3; 
const SC_REQUEST_ENTITY_TOO_LARGE = 413;
const SC_BAD_REQUEST = 400;
const SC_INTERNAL_SERVER_ERROR= 500;

window.onload = async function setup() {
    let genRoutePage = new RoutePage();
    await genRoutePage.init();
    let startAddr = await genRoutePage.getStartAddr();
    let endAddr = await genRoutePage.getEndAddr();
    let startCoord = await genRoutePage.getStartCoord();

    await initStartEndDisplay(startAddr, endAddr);

    // Either load the checkbox map or the directions map.
    let enteredText = await genRoutePage.getWaypoints();
    if (JSON.stringify(enteredText) === '[]') {
        let chosenPoints = await genRoutePage.getChosenPoints();
        await genRoutePage.createMapWithWaypoints(chosenPoints, 'point-map', 'route-legend', 'globalURL', true);
    } else {
        await setupUserChoices(enteredText, genRoutePage);
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
 * Set up the two left-hand panels of the page that allow the user to choose what they want in their route.
 */
async function setupUserChoices(res, genRoutePage) {
    let waypoints = convertWaypointClusterstoLatLng(res);
    let startCoord = await genRoutePage.getStartCoord();
    let endCoord = await genRoutePage.getEndCoord();
    let startName = await genRoutePage.getStartAddr();
    let endName = await genRoutePage.getEndAddr();
    createCheckBoxes(res);
    createPointInfoMap(startCoord, endCoord, startName, endName, waypoints, genRoutePage);
}

/**
 * Get waypoints from the servlet and map each cluster of waypoints on the map in a different marker color.
 */
function createPointInfoMap(start, end, startName, endName, waypoints, genRoutePage) {
    let map = genRoutePage.initMap(start, 'point-map');
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
            createCheckableMarker(map, pt, letter, google.maps.SymbolPath.BACKWARD_CLOSED_ARROW, FILL_COLORS[i % MAX_WAYPOINTS], genRoutePage);
            letter = String.fromCharCode(letter.charCodeAt(0) + 1); // update the marker letter label to the next letter
        }
    }
}


/**
 * Create a dynamic marker with an InfoWindow that appears with the label upon clicking.
 */
function createCheckableMarker(map, waypoint, letter, icon, color, genRoutePage) {
    let marker = createMarker(map, waypoint.latlng, letter, icon, color);
    let infowindow = createInfoWindow(waypoint, marker, genRoutePage);
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
function createInfoWindow(waypoint, marker, genRoutePage) {
    const uncheckedIcon = marker.getIcon();
    const CHECKED_ICON = {
                        path: google.maps.SymbolPath.BACKWARD_CLOSED_ARROW,
                        fillColor: CHECKED_COLOR,
                        fillOpacity: 100,
                        scale: 5,
                        strokeWeight: 4,
                        labelOrigin: { x: 0, y: 2}
                    };
    const html = createInfoWindowHTML(waypoint, genRoutePage);
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
function createInfoWindowHTML(waypoint, genRoutePage) {
    let html = document.createElement('div');
    genRoutePage.addNewTypeElem(html, waypoint.label, 'b');
    genRoutePage.addNewTypeElem(html, `species: ${waypoint.species}`, 'p');
    let link = genRoutePage.addNewTypeElem(html, 'More Information', 'a');
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
  let submitEl = document.createElement("input");
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
 * Convert waypoint clusters in JSON form returned by servlet to a list of objects containing Google Maps LatLng objects
 * in place of coordinates.
 */
function convertWaypointClusterstoLatLng(waypoints) {
     let latlngWaypoints = [];
     for (let cluster of waypoints) {
         let pts = [];
         for (let pt of cluster) {
            let waypoint = new google.maps.LatLng(pt.y, pt.x);
            pts.push({ latlng : waypoint, label : pt.label, species : pt.species, url : pt.url });
         }
         latlngWaypoints.push(pts);
    }
    return latlngWaypoints;
}

/* Check to see if we're running on Node.js or in a browser for tests */
try {
    module.exports.addNewTypeElem = addNewTypeElem;
    module.exports.createColorBoxElem = createColorBoxElem;
    module.exports.createCheckBoxes = createCheckBoxes;
    module.exports.createCheckBoxEl = createCheckBoxEl;
    module.exports.getNumChecked = getNumChecked;
} catch(error) {
    console.log("Not exporting code from this script")
}
