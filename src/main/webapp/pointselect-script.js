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

//document.getElementById('form').addEventListener('submit', createDropDowns());
/** Fetches routes from the server and adds them to the DOM. */
function createDropDowns() {
  submitEl = document.createElement("input");
  submitEl.setAttribute("type", "submit");
  //fetch array of arrays from /query
  fetch('/query').then(response => response.json()).then((dropDowns) => {
    const dropDownEl = document.getElementById('select-points');
    dropDowns.forEach((set) => {
      dropDownEl.appendChild(createCheckBoxSet(set));
    })
    dropDownEl.appendChild(submitEl);
  });

}

/** Creates an element that has Name of set and checkpoints of coordinates */
function createCheckBoxSet(set) {
  console.log(set);
  const setName = set[0].label;
  const returnDiv = document.createElement('div');
  const CheckBoxTitle = document.createElement('h4');
  CheckBoxTitle.innerText = setName;
  returnDiv.appendChild(CheckBoxTitle);
  set.forEach((choice)=>{
      returnDiv.appendChild(createCheckBoxEl(choice))
  })
  return returnDiv;
}

/** Creates an checkbox element with label */
function createCheckBoxEl(choice){
    const checkBoxEl = document.createElement('input');
    checkBoxEl.setAttribute("type", "checkbox");
    const checkBoxValue = JSON.stringify(choice);
    checkBoxEl.setAttribute("value", checkBoxValue);
    const checkBoxLabel = document.createElement('label');
    checkBoxLabel.innerText = checkBoxValue;
    const labelAndBox = document.createElement('div');
    labelAndBox.appendChild(checkBoxEl);
    labelAndBox.appendChild(checkBoxLabel);
    return labelAndBox;
}
