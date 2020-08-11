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

window.onload = function() {
    initAutocomplete('startloc-input');
    initAutocomplete('endloc-input');
    document.getElementById("one-way").addEventListener('click', showTabForm);
    document.getElementById("loop").addEventListener('click', showTabForm);
};

function showTabForm(event) {
    const target = event.target;
    if (target.id === 'one-way') {
        document.getElementById('loop-tab').style.display = 'none';
    } else {
        document.getElementById('one-way-tab').style.display = 'none';
    }
    let divID = `${target.id}-tab`;
    document.getElementById(divID).style.display = 'block';
}

/**
 * Initializes an Autocomplete object and binds it to the given id'ed element.
 */
function initAutocomplete(className) {
    let inputs = document.getElementsByClassName(className);
    Array.from(inputs).forEach(input => {
        var autocomplete = new google.maps.places.Autocomplete(input);
    });
}
