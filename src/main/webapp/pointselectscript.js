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

/** Fetches routes from the server and adds them to the DOM. */
function loadRoutes() {
  fetch('/route-store').then(response => response.json()).then((routes) => {
    const routesListElement = document.getElementById('route-list');
    routes.forEach((route) => {
      routesListElement.appendChild(createRouteElement(route));
    })
  });
}

/** Creates an element that represents a route, including its delete button. */
function createRouteElement(route) {
  const routeElement = document.createElement('li');
  routeElement.className = 'route';

  const titleElement = document.createElement('span');
  titleElement.innerText = route.text;

  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteroute(route);

    // Remove the route from the DOM.
    routeElement.remove();
  });

  routeElement.appendChild(titleElement);
  routeElement.appendChild(deleteButtonElement);
  return routeElement;
}

/** Tells the server to delete the route. */
function deleteroute(route) {
  const params = new URLSearchParams();
  params.append('id', route.id);
  fetch('/delete-route', {method: 'POST', body: params});
}
