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
 * Get a list of StoredRouts from fetching from /route-store
 */
async function getRouteStore() {
    let res = await fetch('/route-store');
    let routestore = await res.json();
    return routestore;
}