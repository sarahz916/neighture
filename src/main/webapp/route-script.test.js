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

/**
 * Note: to install Jest, either run the command 'npm install --save-dev jest -g' whenever Cloud Shell restarts OR
 * create a file calleed '.customize_environment' in your home directory and add the following:
 * '#!/bin/sh
 * npm install --save-dev jest -g
 */


// mock the basic DOM of our webpage
document.body.innerHTML = 
    '<form action="/chosen-waypoints" id="select-points"></form>' +
    '<form action="/query" method="POST" id ="text-git statform">' +
    '<textarea name="text-input" id="text-input"></textarea>' +
    '<input type="submit" id="submit-text"/>' +
    '</form>';

describe('addNewTypeElem', function() {
    const addNewTypeElem = require('./route-script.js').addNewTypeElem;

    const mockParent = document.createElement('div');
    mockParent.setAttribute('id', 'mock-parent');
    const mockText = "test";

    it('should be a function', function() {
        expect(addNewTypeElem).toBeInstanceOf(Function);
    });

    it('should be able to add a single p elem with given text to the DOM', function() {
        addNewTypeElem(mockParent, mockText, 'p');
        expect(mockParent.childNodes.length).toEqual(1);
        expect(mockParent.childNodes[0].tagName).toBe('P');
        expect(mockParent.childNodes[0].textContent).toBe(mockText);
    });
});

describe('createColorBoxElem', function() {
    const createColorBoxElem = require('./route-script.js').createColorBoxElem;
    const testColor = 'red';

    it('should be a function', function() {
        expect(createColorBoxElem).toBeInstanceOf(Function);
    });

    it('should create box with given color', function() {
        let box = createColorBoxElem(testColor);
        expect(box.tagName).toBe('DIV');
        expect(box.style.backgroundColor).toBe(testColor);
    });

    it('should create square of size 20px', function() {
        let box = createColorBoxElem(testColor);
        expect(box.style.width).toEqual(box.style.height);
        expect(box.style.width).toEqual('20px');
        expect(box.style.height).toEqual('20px');
    });
});

describe('createCheckBoxEl', function() {
    const createCheckBoxEl = require('./route-script.js').createCheckBoxEl;

    const testCoords = {x : 2, y : 2, label : 'flower'};
    const expectedCoords = "{\"x\":2,\"y\":2,\"label\":\"flower\"}";
    const testLabel = 'flower';

    it('should be a function', function() {
        expect(createCheckBoxEl).toBeInstanceOf(Function);
    });

    it('should return an input element with the given value', function() {
        const checkbox = createCheckBoxEl(testCoords, testLabel);
        expect(checkbox.tagName).toEqual('INPUT');
        expect(checkbox.value).toBe(expectedCoords);
    });

});

describe('createCheckBoxes', function() {
    const createCheckBoxes = require('./route-script.js').createCheckBoxes;

    let testPts = [ [ { x : 1, y : 1, label : 'flower' } ] ];
    createCheckBoxes(testPts);

    it('should create a new submit HTML input element', function() {
        expect(document.getElementById('submit-checkbox')).not.toBeUndefined();
        expect(document.getElementById('submit-checkbox').tagName).toEqual('INPUT');
    });

    it('should add [waypointChoices.length + 1] children to the "select-points" HTML element for all the waypoint checkboxes and submit button', function() {
        expect(document.getElementById('select-points').childNodes.length).toEqual(testPts.length + 1);
    });
});